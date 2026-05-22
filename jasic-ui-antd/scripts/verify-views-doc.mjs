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
const fnPatterns = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/
];

let totalMissing = 0;
let totalNoTemplate = 0;

for (const rel of TARGET_FILES) {
  const filePath = path.join(ROOT, rel);
  const lines = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n').split('\n');
  const missing = [];

  for (let i = 0; i < lines.length; i++) {
    if (!fnPatterns.some(re => re.test(lines[i]))) continue;
    let start = 0;
    for (let k = i - 1; k >= 0; k--) {
      if (fnPatterns.some(re => re.test(lines[k]))) {
        start = k + 1;
        break;
      }
    }
    const prev = lines.slice(start, i).join('\n');
    if (!prev.includes('@修改人')) missing.push(lines[i].trim());
  }

  const tplIdx = lines.findIndex(l => l.trim() === '<template>');
  const hasTplComment = tplIdx >= 0 && lines.slice(tplIdx + 1, tplIdx + 6).some(l => l.trim().startsWith('<!--'));

  if (missing.length) {
    console.log(`${rel}: missing JSDoc ${missing.length}`);
    totalMissing += missing.length;
  }
  if (!hasTplComment) {
    console.log(`${rel}: no template root comment`);
    totalNoTemplate += 1;
  }
}

console.log(`\nSummary: missing JSDoc blocks=${totalMissing}, files without template comment=${totalNoTemplate}`);
