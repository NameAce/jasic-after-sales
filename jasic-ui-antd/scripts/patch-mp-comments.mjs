import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const META = [' * @修改人 黄碧莲', ' * @修改时间 2026-05-22'];
const REPO = path.join(path.dirname(fileURLToPath(import.meta.url)), '../..');

const MP_APPS = [
  {
    rel: 'mp/aftersale/src',
    label: '售后客户端小程序（报修、工单、地址）'
  },
  {
    rel: 'mp/contractor/src',
    label: '承修方小程序（网点/总部工单处理、派工）'
  }
];

const SKIP_FILES = new Set(['auto-imports.d.ts', 'env.d.ts', 'shims-uni.d.ts', 'shime-uni.d.ts']);

const fnPatterns = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export async function (\w+)\(/,
  /^export function (\w+)\(/,
  /^export const (\w+) = (?:async )?\(/
];

function walk(dir, rel = '') {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = rel ? `${rel}/${ent.name}` : ent.name;
    const full = path.join(dir, ent.name);
    if (ent.isDirectory()) {
      if (ent.name === 'node_modules' || ent.name === 'dist') continue;
      out.push(...walk(full, r));
    } else if (/\.(vue|ts)$/.test(ent.name) && !SKIP_FILES.has(ent.name)) {
      out.push(r.replace(/\\/g, '/'));
    }
  }
  return out;
}

function inferPurpose(name, rel, appLabel) {
  const n = name;
  if (/^use[A-Z]/.test(n)) return `组合式：${n}`;
  if (/^load|^fetch|^get|^list|^query/.test(n)) return `加载/请求：${n}`;
  if (/^submit|^save|^create|^update|^delete|^remove|^cancel|^close/.test(n)) return `提交/变更：${n}`;
  if (/^open|^close|^toggle|^handle|^on|^goTo|^navigate/.test(n)) return `交互/导航：${n}`;
  if (/^format|^parse|^normalize|^map|^build|^to[A-Z]/.test(n)) return `转换/构造：${n}`;
  if (/^is|^has|^can|^should|^ensure/.test(n)) return `判断：${n}`;
  if (/^reset|^clear|^sync|^apply/.test(n)) return `状态：${n}`;
  if (rel.includes('api/')) return `接口封装：${n}`;
  if (rel.includes('stores/')) return `状态：${n}`;
  return `${appLabel}内方法：${n}`;
}

function inferReturns(line) {
  if (/async/.test(line)) return 'Promise';
  if (/^export function is|^function is|^export function has/.test(line)) return 'boolean';
  return 'void';
}

function fileDesc(rel, appLabel) {
  const base = path.basename(rel).replace(/\.(vue|ts)$/, '');
  if (rel.endsWith('App.vue')) return `${appLabel}根组件`;
  if (rel.endsWith('main.ts')) return `${appLabel}入口：创建 Vue 应用并挂载`;
  if (rel.includes('/api/')) return `${appLabel} API：${base}`;
  if (rel.includes('/stores/')) return `${appLabel} 全局状态：${base}`;
  if (rel.includes('/utils/')) return `${appLabel} 工具：${base}`;
  if (rel.includes('/composables/')) return `${appLabel} 组合式：${base}`;
  if (rel.includes('/constants/')) return `${appLabel} 常量：${base}`;
  if (rel.includes('/models/')) return `${appLabel} 类型/模型：${base}`;
  if (rel.includes('/components/')) return `${appLabel} 组件：${base}`;
  if (rel.includes('/pages/')) {
    const page = rel
      .replace(/^pages\//, '')
      .replace(/\/index\.vue$/, '')
      .replace(/\.vue$/, '');
    return `${appLabel} 页面：${page}`;
  }
  return `${appLabel}：${base}`;
}

function tplDesc(rel, appLabel) {
  if (rel.endsWith('App.vue')) return `${appLabel}应用壳`;
  const page = rel.match(/pages\/(.+)\.vue$/);
  if (page) return `${appLabel}页面 ${page[1].replace(/\//g, ' / ')}`;
  const comp = rel.match(/components\/([^/]+)\//);
  if (comp) return `${appLabel}组件 ${comp[1]}`;
  return `${appLabel}视图`;
}

function injectMeta(content) {
  return content
    .replace(/\/\*\*([^*@][^*]*)\*\//g, (full, inner) => {
      if (full.includes('@修改人')) return full;
      return `/**\n * ${inner.trim()}\n${META.join('\n')}\n */`;
    })
    .replace(/\/\*\*([\s\S]*?)\*\//g, (full, inner) => {
      if (String(inner).includes('@修改人')) return full;
      const trimmed = String(inner).replace(/\s+$/, '');
      return `/**${trimmed}\n${META.join('\n')}\n */`;
    });
}

function processFile(root, rel, appLabel) {
  const fp = path.join(root, rel);
  let lines = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n').split('\n');
  const inserts = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    let name = '';
    for (const re of fnPatterns) {
      const m = line.match(re);
      if (m) {
        name = m[1];
        break;
      }
    }
    if (!name) continue;
    let start = 0;
    for (let k = i - 1; k >= 0; k--) {
      if (fnPatterns.some(re => re.test(lines[k]))) {
        start = k + 1;
        break;
      }
    }
    const prev = lines.slice(start, i).join('\n');
    if (prev.includes('@修改人')) continue;
    if (prev.trim().endsWith('*/')) continue;
    inserts.push({
      line: i,
      block: [
        '/**',
        ` * 作用：${inferPurpose(name, rel, appLabel)}。`,
        ` * @returns ${inferReturns(line)}`,
        ...META,
        ' */'
      ]
    });
  }

  for (let k = inserts.length - 1; k >= 0; k--) {
    lines.splice(inserts[k].line, 0, ...inserts[k].block);
  }

  const content = injectMeta(lines.join('\n'));
  lines = content.split('\n');

  const hasAnyMeta = content.includes('@修改人');
  if (!hasAnyMeta) {
    if (rel.endsWith('.vue')) {
      const si = lines.findIndex((l, i) => i < 5 && l.includes('<script'));
      if (si >= 0) {
        lines.splice(si + 1, 0, '/**', ` * ${fileDesc(rel, appLabel)}。`, ...META, ' */');
      }
    } else if (rel.endsWith('.ts')) {
      const skipShebang = lines[0]?.startsWith('#');
      const at = skipShebang ? 1 : 0;
      lines.splice(at, 0, '/**', ` * ${fileDesc(rel, appLabel)}。`, ...META, ' */', '');
    }
  } else if (rel.endsWith('.vue')) {
    const idx = lines.findIndex((l, i) => i < 25 && l.trim() === '/**');
    if (idx >= 0) {
      const end = lines.findIndex((l, i) => i > idx && i < 45 && l.trim() === '*/');
      if (
        end >= 0 &&
        !lines
          .slice(idx, end + 1)
          .join('\n')
          .includes('@修改人')
      ) {
        lines.splice(end, 0, ...META);
      }
    }
  }

  if (rel.endsWith('.vue')) {
    const tplIdx = lines.findIndex(l => l.trim() === '<template>');
    if (tplIdx >= 0 && !lines.slice(tplIdx + 1, tplIdx + 8).some(l => l.trim().startsWith('<!--'))) {
      lines.splice(tplIdx + 1, 0, `  <!-- ${tplDesc(rel, appLabel)} -->`);
    }
  }

  const out = lines.join('\n');
  if (out !== fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n')) {
    fs.writeFileSync(fp, out, 'utf8');
    return { inserts: inserts.length, tpl: rel.endsWith('.vue') };
  }
  return null;
}

for (const app of MP_APPS) {
  const root = path.join(REPO, app.rel);
  const files = walk(root);
  let changed = 0;
  let fnBlocks = 0;
  let tpl = 0;
  console.log(`\n=== ${app.rel} (${files.length} files) ===\n`);
  for (const rel of files.sort()) {
    const r = processFile(root, rel, app.label);
    if (r) {
      changed++;
      fnBlocks += r.inserts;
      if (r.tpl) tpl++;
      if (r.inserts > 0) console.log(`  ${rel}: +${r.inserts} fn`);
      else console.log(`  ${rel}: meta/tpl`);
    }
  }
  console.log(`done: changed ${changed}, fn blocks ${fnBlocks}, vue tpl ${tpl}`);
}
