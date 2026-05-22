import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');
const SKIP = new Set(['router/elegant', 'node_modules']);

const fnP = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export (?:async )?function (\w+)\(/,
  /^export function (\w+)\(/
];

function walk(rel = '') {
  const dir = path.join(ROOT, rel);
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = rel ? `${rel}/${ent.name}` : ent.name;
    if (ent.isDirectory()) {
      if (SKIP.has(r) || ent.name === 'node_modules') continue;
      out.push(...walk(r));
    } else if (/\.(vue|ts|tsx)$/.test(ent.name) && !ent.name.endsWith('.d.ts')) {
      out.push(r.replace(/\\/g, '/'));
    }
  }
  return out;
}

function topDir(rel) {
  if (rel.startsWith('views/')) return `views/${rel.split('/')[1]}`;
  return rel.split('/')[0];
}

const byDir = {};
const fnMissFiles = [];
const vueNoTpl = [];
const noMetaFiles = [];
const noScriptVue = [];

for (const rel of walk()) {
  const fp = path.join(ROOT, rel);
  const content = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n');
  const lines = content.split('\n');
  const isVue = rel.endsWith('.vue');
  const hasScript = isVue && /<script/.test(content);
  const head = lines.slice(0, 40).join('\n');
  const hasMeta = head.includes('@修改人') || (content.match(/@修改人/g) || []).length > 0;

  if (!byDir[topDir(rel)]) {
    byDir[topDir(rel)] = { files: 0, fns: 0, fnMiss: 0, vue: 0, vueNoTpl: 0, noMeta: 0, noScript: 0 };
  }
  const d = byDir[topDir(rel)];
  d.files++;

  if (!hasMeta) {
    d.noMeta++;
    noMetaFiles.push(rel);
  }

  if (isVue) {
    d.vue++;
    if (!hasScript) {
      d.noScript++;
      noScriptVue.push(rel);
    }
    const tplIdx = lines.findIndex(l => l.trim() === '<template>');
    const hasTpl = tplIdx >= 0 && lines.slice(tplIdx + 1, tplIdx + 8).some(l => l.trim().startsWith('<!--'));
    if (!hasTpl) {
      d.vueNoTpl++;
      vueNoTpl.push(rel);
    }
  }

  for (let i = 0; i < lines.length; i++) {
    let name = '';
    for (const re of fnP) {
      const m = lines[i].match(re);
      if (m) {
        name = m[1];
        break;
      }
    }
    if (!name) continue;
    d.fns++;
    let start = 0;
    for (let k = i - 1; k >= 0; k--) {
      if (fnP.some(re => re.test(lines[k]))) {
        start = k + 1;
        break;
      }
    }
    if (!lines.slice(start, i).join('\n').includes('@修改人')) {
      d.fnMiss++;
      fnMissFiles.push({ rel, line: i + 1, name });
    }
  }
}

console.log('=== jasic-ui-antd/src 注释审计 ===\n');
let totalFns = 0;
let totalMiss = 0;
let totalVueNoTpl = 0;
let totalNoMeta = 0;

const rows = Object.entries(byDir).sort(
  (a, b) => b[1].fnMiss - a[1].fnMiss || b[1].vueNoTpl - a[1].vueNoTpl || b[1].noMeta - a[1].noMeta
);
for (const [k, v] of rows) {
  totalFns += v.fns;
  totalMiss += v.fnMiss;
  totalVueNoTpl += v.vueNoTpl;
  totalNoMeta += v.noMeta;
  const flag = v.fnMiss || v.vueNoTpl || v.noMeta;
  if (!flag && v.files > 0) continue;
  console.log(
    `${k.padEnd(22)} 文件${String(v.files).padStart(3)}  函数${String(v.fns).padStart(4)} 缺JSDoc${String(v.fnMiss).padStart(3)}  Vue${String(v.vue).padStart(3)} 缺tpl注释${String(v.vueNoTpl).padStart(3)}  缺@修改人${String(v.noMeta).padStart(3)}`
  );
}

console.log('\n--- 合计 ---');
console.log(`源码文件(不含 elegant 生成): ${walk().length}`);
console.log(`函数总数: ${totalFns}，缺 @修改人 的函数块: ${totalMiss}`);
console.log(`Vue 缺 template 根注释: ${totalVueNoTpl}`);
console.log(`文件级未见 @修改人: ${totalNoMeta}`);

if (fnMissFiles.length) {
  console.log('\n--- 仍缺函数 JSDoc 元信息 ---');
  fnMissFiles.slice(0, 30).forEach(x => console.log(`  ${x.rel}:${x.line} ${x.name}`));
  if (fnMissFiles.length > 30) console.log(`  ... 另有 ${fnMissFiles.length - 30} 处`);
}

if (vueNoTpl.length) {
  console.log('\n--- 仍缺 template <!-- --> ---');
  vueNoTpl.forEach(r => console.log(`  ${r}`));
}

if (noMetaFiles.length) {
  console.log('\n--- 文件内无任何 @修改人（多为纯模板/生成/入口）---');
  noMetaFiles.forEach(r => console.log(`  ${r}`));
}

if (noScriptVue.length) {
  console.log('\n--- 无 script 的 .vue（仅 template，一般只需 <!-- -->）---');
  noScriptVue.forEach(r => console.log(`  ${r}`));
}

console.log('\n--- 未扫描/通常不要求 ---');
console.log('  router/elegant/* (路由生成)');
console.log('  **/*.d.ts, styles/css, assets, locales 大段文案');
console.log('  mp/* 不在 jasic-ui-antd 包内');
