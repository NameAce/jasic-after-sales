import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rel = process.argv[2];
const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');
const fp = path.join(ROOT, rel);
const fnP = [/^async function (\w+)\(/, /^function (\w+)\(/, /^const (\w+) = async \(/, /^const (\w+) = \([^)]*\) =>/];
const lines = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n').split('\n');
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
  const prev = lines.slice(start, i).join('\n');
  if (!prev.includes('@修改人')) {
    console.log(`${i + 1}: ${name}`);
    console.log('prev:', prev.slice(-200));
  }
}
