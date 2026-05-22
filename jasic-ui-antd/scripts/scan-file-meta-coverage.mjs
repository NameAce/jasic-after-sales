import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

function walk(dir, rel = '') {
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = rel ? `${rel}/${ent.name}` : ent.name;
    if (ent.isDirectory()) {
      if (ent.name === 'node_modules' || r === 'router/elegant') continue;
      out.push(...walk(path.join(dir, ent.name), r));
    } else if (/\.(vue|ts)$/.test(ent.name) && !ent.name.endsWith('.d.ts')) {
      out.push(r.replace(/\\/g, '/'));
    }
  }
  return out;
}

const dirs = {};
for (const rel of walk(ROOT)) {
  const c = fs.readFileSync(path.join(ROOT, rel), 'utf8');
  const top = rel.startsWith('views/') ? `views/${rel.split('/')[1]}` : rel.split('/')[0];
  if (!dirs[top]) dirs[top] = { total: 0, hasMeta: 0, vue: 0, hasTpl: 0 };
  dirs[top].total++;
  if (c.includes('@修改人')) dirs[top].hasMeta++;
  if (rel.endsWith('.vue')) {
    dirs[top].vue++;
    const tpl = c.indexOf('<template>');
    if (tpl >= 0 && c.slice(tpl, tpl + 800).includes('<!--')) dirs[top].hasTpl++;
  }
}

console.log('目录\t文件\t含@修改人\tVue\t含template<!--');
for (const [k, v] of Object.entries(dirs).sort((a, b) => a[1].total - b[1].total)) {
  const incomplete = v.hasMeta < v.total || v.vue > v.hasTpl;
  if (incomplete) {
    console.log(`${k}\t${v.total}\t${v.hasMeta}\t${v.vue}\t${v.hasTpl}`);
  }
}
