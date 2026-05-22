import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const TARGET_FILES = [
  'views/org/index.vue',
  'views/advanced-modules/index.vue',
  'views/advanced-modules/config-form-panel.vue',
  'views/system/notify-scene/index.vue',
  'views/system/notify-trace/index.vue',
  'views/system/user/index.vue',
  'views/system/menu/index.vue',
  'views/system/role/index.vue',
  'views/system/branch/index.vue',
  'views/system/branch/branch-site-orders-drawer.vue',
  'views/notify/index.vue',
  'views/user-center/index.vue',
  'views/log/index.vue',
  'views/company-address/index.vue'
];

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

function isJsdocStart(line) {
  return line.trim() === '/**';
}

function isJsdocEnd(line) {
  return line.trim() === '*/';
}

function processFile(relPath) {
  const filePath = path.join(ROOT, relPath);
  const lines = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n').split('\n');
  const out = [];
  let removed = 0;

  for (let i = 0; i < lines.length; i++) {
    if (!isJsdocStart(lines[i])) {
      out.push(lines[i]);
      continue;
    }
    // 收集连续 JSDoc 块
    const blocks = [];
    let j = i;
    while (j < lines.length && isJsdocStart(lines[j])) {
      const block = [];
      block.push(lines[j]);
      j++;
      while (j < lines.length && !isJsdocEnd(lines[j])) {
        block.push(lines[j]);
        j++;
      }
      if (j < lines.length) {
        block.push(lines[j]);
        j++;
      }
      blocks.push(block);
    }
    if (blocks.length <= 1) {
      blocks[0]?.forEach(l => out.push(l));
      i = j - 1;
      continue;
    }
    // 多个连续块：保留含 @修改人 的最后一块，否则保留最后一块
    const withMeta = blocks.filter(b => b.some(l => l.includes('@修改人')));
    const keep = withMeta.length ? withMeta[withMeta.length - 1] : blocks[blocks.length - 1];
    removed += blocks.length - 1;
    keep.forEach(l => out.push(l));
    i = j - 1;
  }

  if (removed > 0) {
    fs.writeFileSync(filePath, out.join('\n'), 'utf8');
    console.log(`${relPath}: -${removed} duplicate blocks`);
  }
}

for (const f of TARGET_FILES) processFile(f);
