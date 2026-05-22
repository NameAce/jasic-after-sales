import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const META = [' * @修改人 黄碧莲', ' * @修改时间 2026-05-22'];
const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');
const CHECK = ['components', 'views/home', 'views/system'];

function walk(rel) {
  const dir = path.join(ROOT, rel);
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = `${rel}/${ent.name}`.replace(/\\/g, '/');
    if (ent.isDirectory()) out.push(...walk(r));
    else if (/\.vue$/.test(ent.name)) out.push(r);
  }
  return out;
}

for (const dir of CHECK) {
  for (const rel of walk(dir)) {
    const lines = fs.readFileSync(path.join(ROOT, rel), 'utf8').replace(/\r\n/g, '\n').split('\n');
    if (lines.slice(0, 35).join('\n').includes('@修改人')) continue;
    const si = lines.findIndex((l, i) => i < 5 && l.includes('<script'));
    if (si < 0) continue;
    const title =
      rel === 'components/common/system-logo.vue' ? '系统 Logo 展示组件。' : `模块：${path.basename(rel, '.vue')}。`;
    lines.splice(si + 1, 0, '/**', ` * ${title}`, ...META, ' */');
    fs.writeFileSync(path.join(ROOT, rel), lines.join('\n'), 'utf8');
    console.log('header:', rel);
  }
}
