import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const META = [' * @修改人 黄碧莲', ' * @修改时间 2026-05-22'];
const REPO = path.join(path.dirname(fileURLToPath(import.meta.url)), '../..');
const APPS = [
  { rel: 'mp/aftersale/src', label: '售后客户端小程序（报修、工单、地址）' },
  { rel: 'mp/contractor/src', label: '承修方小程序（网点/总部工单处理、派工）' }
];

function fileDesc(rel, appLabel) {
  const base = path.basename(rel, '.vue');
  if (rel.includes('/components/')) return `${appLabel} 组件：${base}`;
  if (rel.includes('/pages/')) {
    const page = rel.replace(/^pages\//, '').replace(/\.vue$/, '');
    return `${appLabel} 页面：${page}`;
  }
  return `${appLabel}：${base}`;
}

function walk(dir, rel = '') {
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const r = rel ? `${rel}/${ent.name}` : ent.name;
    if (ent.isDirectory()) out.push(...walk(path.join(dir, ent.name), r));
    else if (ent.name.endsWith('.vue')) out.push(r);
  }
  return out;
}

for (const app of APPS) {
  const root = path.join(REPO, app.rel);
  for (const rel of walk(root)) {
    const fp = path.join(root, rel);
    const content = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n');
    if (content.includes('@修改人')) continue;

    const lines = content.split('\n');
    const si = lines.findIndex(l => /<script/.test(l));
    if (si >= 0) {
      lines.splice(si + 1, 0, '/**', ` * ${fileDesc(rel, app.label)}。`, ...META, ' */');
    } else {
      const ti = lines.findIndex(l => l.trim() === '<template>');
      const note = `  <!-- ${fileDesc(rel, app.label)}。 @修改人 黄碧莲 @修改时间 2026-05-22 -->`;
      if (ti >= 0) lines.splice(ti + 1, 0, note);
    }
    fs.writeFileSync(fp, lines.join('\n'), 'utf8');
    console.log('fixed:', `${app.rel}/${rel}`);
  }
}
