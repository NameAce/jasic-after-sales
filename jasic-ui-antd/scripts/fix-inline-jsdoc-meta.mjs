import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const META = ' * @修改人 黄碧莲\n * @修改时间 2026-05-22';
const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

const TARGETS = [
  'layouts',
  'utils/datetime.ts',
  'utils/table-action-width.ts',
  'utils/route-query-filter-sync.ts',
  'hooks/common/modal-form-layout.ts'
];

function walk(rel) {
  const dir = path.join(ROOT, rel);
  if (!fs.existsSync(dir)) return [];
  if (rel.endsWith('.ts')) return [rel];
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...walk(path.join(rel, ent.name)));
    else if (ent.name.endsWith('.vue') || ent.name.endsWith('.ts'))
      out.push(path.join(rel, ent.name).replace(/\\/g, '/'));
  }
  return out;
}

const files = new Set();
for (const t of TARGETS) {
  for (const f of walk(t)) files.add(f);
}

let total = 0;
for (const rel of files) {
  const fp = path.join(ROOT, rel);
  let content = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n');
  const before = content;

  // 单行 JSDoc：/** ... */（不含 @修改人）
  content = content.replace(/\/\*\*([^*@][^*]*)\*\//g, (full, inner) => {
    if (full.includes('@修改人')) return full;
    return `/**\n * ${inner.trim()}\n${META}\n */`;
  });

  // 多行块缺元信息
  content = content.replace(/\/\*\*([\s\S]*?)\*\//g, (full, inner) => {
    if (String(inner).includes('@修改人')) return full;
    const trimmed = String(inner).replace(/\s+$/, '');
    return `/**${trimmed}\n${META}\n */`;
  });

  if (content !== before) {
    fs.writeFileSync(fp, content, 'utf8');
    total++;
    console.log('updated:', rel);
  }
}

console.log(`done, files changed: ${total}`);
