import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const REPO = path.join(path.dirname(fileURLToPath(import.meta.url)), '../..');
const APPS = ['mp/aftersale/src', 'mp/contractor/src'];
const SKIP = new Set(['auto-imports.d.ts', 'env.d.ts', 'shims-uni.d.ts', 'shime-uni.d.ts']);
const fnP = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export (?:async )?function (\w+)\(/,
  /^export function (\w+)\(/
];

function walk(dir, rel = '') {
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = rel ? `${rel}/${ent.name}` : ent.name;
    if (ent.isDirectory()) out.push(...walk(path.join(dir, ent.name), r));
    else if (/\.(vue|ts)$/.test(ent.name) && !SKIP.has(ent.name)) out.push(r);
  }
  return out;
}

for (const app of APPS) {
  const root = path.join(REPO, app);
  let fnMiss = 0;
  let tplMiss = 0;
  let noMeta = 0;
  const missFiles = [];
  for (const rel of walk(root)) {
    const c = fs.readFileSync(path.join(root, rel), 'utf8').replace(/\r\n/g, '\n');
    const lines = c.split('\n');
    if (!c.includes('@修改人')) {
      noMeta++;
      missFiles.push(`${rel} (no meta)`);
    }
    if (rel.endsWith('.vue')) {
      const ti = lines.findIndex(l => l.trim() === '<template>');
      if (ti >= 0 && !lines.slice(ti + 1, ti + 8).some(l => l.trim().startsWith('<!--'))) tplMiss++;
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
      let start = 0;
      for (let k = i - 1; k >= 0; k--) {
        if (fnP.some(re => re.test(lines[k]))) {
          start = k + 1;
          break;
        }
      }
      if (!lines.slice(start, i).join('\n').includes('@修改人')) {
        fnMiss++;
        missFiles.push(`${rel}:${i + 1} ${name}`);
      }
    }
  }
  console.log(`${app}: noMeta=${noMeta} fnMiss=${fnMiss} tplMiss=${tplMiss}`);
  missFiles.slice(0, 15).forEach(f => console.log('  ', f));
}
