import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');
const fnP = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export (?:async )?function (\w+)\(/,
  /^export const (\w+) = /
];

function countFile(fp) {
  const lines = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n').split('\n');
  let fns = 0;
  let miss = 0;
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
  return { fns, miss };
}

function walk(rel, exts) {
  const dir = path.join(ROOT, rel);
  let out = [];
  if (!fs.existsSync(dir)) return out;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out = out.concat(walk(path.join(rel, ent.name), exts));
    else if (exts.some(e => ent.name.endsWith(e))) out.push(p);
  }
  return out;
}

const groups = {
  manage: ['views/manage', ['.vue', '.ts']],
  layouts: ['layouts', ['.vue', '.ts']],
  utils: ['utils', ['.ts']],
  hooks: ['hooks', ['.ts']],
  store: ['store', ['.ts']],
  router: ['router', ['.ts']]
};

for (const [g, [rel, exts]] of Object.entries(groups)) {
  const files = walk(rel, exts);
  let tf = 0;
  let tm = 0;
  const top = [];
  for (const f of files) {
    const { fns, miss } = countFile(f);
    if (fns > 0) {
      tf += fns;
      tm += miss;
      if (miss > 0) top.push({ f: path.relative(ROOT, f), fns, miss });
    }
  }
  top.sort((a, b) => b.miss - a.miss || b.fns - a.fns);
  console.log(`\n## ${g} files=${files.length} fns=${tf} missing=${tm}`);
  top.slice(0, 20).forEach(x => console.log(`  ${x.f} fns=${x.fns} miss=${x.miss}`));
}
