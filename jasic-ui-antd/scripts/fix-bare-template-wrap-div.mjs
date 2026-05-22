/**
 * 为注释脚本误加的裸 <div> 包裹层补上 h-full，避免登录页等 size-full 高度链断裂。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

function walk(dir, out = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) walk(p, out);
    else if (ent.name.endsWith('.vue')) out.push(p);
  }
  return out;
}

let fixed = 0;
for (const fp of walk(ROOT)) {
  const raw = fs.readFileSync(fp, 'utf8');
  const eol = raw.includes('\r\n') ? '\r\n' : '\n';
  const content = raw.replace(/\r\n/g, '\n');
  const next = content.replace(/^(\s*)<div>\n(\s*<!--)/gm, '$1<div class="h-full min-h-0">\n$2');
  if (next !== content) {
    fs.writeFileSync(fp, next.split('\n').join(eol), 'utf8');
    fixed += 1;
    console.log(path.relative(ROOT, fp).replace(/\\/g, '/'));
  }
}
console.log(`\nDone: ${fixed} files`);
