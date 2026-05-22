import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');
const SKIP_DIRS = new Set(['node_modules', 'router/elegant']);
const fnP = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export (?:async )?function (\w+)\(/,
  /^export function (\w+)\(/,
  /^export const (\w+) = (?:async )?\(/
];

function walk(dir, rel = '') {
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = rel ? `${rel}/${ent.name}` : ent.name;
    if (ent.isDirectory()) {
      if (SKIP_DIRS.has(r) || ent.name === 'node_modules') continue;
      out.push(...walk(path.join(dir, ent.name), r));
    } else if (/\.(vue|ts|tsx)$/.test(ent.name) && !ent.name.endsWith('.d.ts')) {
      out.push(r.replace(/\\/g, '/'));
    }
  }
  return out;
}

function analyze(rel) {
  const fp = path.join(ROOT, rel);
  const lines = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n').split('\n');
  let fns = 0;
  let miss = 0;
  const hasFileMeta = lines.slice(0, 30).join('\n').includes('@修改人');
  const tplIdx = lines.findIndex(l => l.trim() === '<template>');
  const hasTplComment = tplIdx >= 0 && lines.slice(tplIdx + 1, tplIdx + 6).some(l => l.trim().startsWith('<!--'));

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
    fns++;
    let start = 0;
    for (let k = i - 1; k >= 0; k--) {
      if (fnP.some(re => re.test(lines[k]))) {
        start = k + 1;
        break;
      }
    }
    if (!lines.slice(start, i).join('\n').includes('@修改人')) miss++;
  }

  return { fns, miss, hasFileMeta, hasTplComment, isVue: rel.endsWith('.vue') };
}

const byDir = {};
let totalFns = 0;
let totalMiss = 0;
let vueNoTpl = 0;
const topMiss = [];

for (const rel of walk(ROOT)) {
  const { fns, miss, hasFileMeta, hasTplComment, isVue } = analyze(rel);
  if (fns === 0 && !isVue) continue;
  const top = rel.split('/')[0] === 'views' ? `views/${rel.split('/')[1] || 'root'}` : rel.split('/')[0];
  if (!byDir[top]) byDir[top] = { files: 0, fns: 0, miss: 0, vueFiles: 0, vueNoTpl: 0, noFileMeta: 0 };
  byDir[top].files++;
  byDir[top].fns += fns;
  byDir[top].miss += miss;
  totalFns += fns;
  totalMiss += miss;
  if (!hasFileMeta && (fns > 0 || rel.endsWith('.vue'))) byDir[top].noFileMeta++;
  if (isVue) {
    byDir[top].vueFiles++;
    if (!hasTplComment) {
      byDir[top].vueNoTpl++;
      vueNoTpl++;
    }
  }
  if (miss > 0) topMiss.push({ rel, fns, miss });
}

topMiss.sort((a, b) => b.miss - a.miss);

console.log('=== 按顶层目录汇总（函数缺 @修改人）===\n');
Object.entries(byDir)
  .sort((a, b) => b[1].miss - a[1].miss)
  .forEach(([k, v]) => {
    if (v.miss === 0 && v.vueNoTpl === 0 && v.noFileMeta === 0) return;
    console.log(
      `${k}: 文件${v.files} 函数${v.fns} 缺JSDoc${v.miss} | Vue${v.vueFiles}缺template注释${v.vueNoTpl} | 缺文件头元信息${v.noFileMeta}`
    );
  });

console.log(`\n总计: 函数${totalFns} 缺@修改人${totalMiss} | Vue缺template根注释约${vueNoTpl}个文件`);

console.log('\n=== 缺 JSDoc 最多的文件（前 25）===\n');
topMiss.slice(0, 25).forEach(x => console.log(`  ${x.rel}  fns=${x.fns} miss=${x.miss}`));

if (topMiss.length === 0) console.log('  (无)');
