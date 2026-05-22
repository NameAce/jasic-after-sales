/**
 * 修复误包裹的多行根组件：撤销错误的 <div><!-- --><Tag\n</div>\n  attrs> 结构。
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

function repairLines(lines) {
  let changed = false;
  for (let i = 0; i < lines.length - 3; i++) {
    if (!/^\s*<div>\s*$/.test(lines[i])) continue;
    if (!/^\s*<!--/.test(lines[i + 1])) continue;
    const tagLine = lines[i + 2];
    if (!tagLine || tagLine.includes('>')) continue;
    if (!/^\s*<[A-Za-z]/.test(tagLine)) continue;
    if (!/^\s*<\/div>\s*$/.test(lines[i + 3])) continue;

    const comment = lines[i + 1];
    const tagStart = lines[i + 2];
    const indent = tagStart.match(/^(\s*)/)[1];
    const innerIndent = `${indent}  `;
    const commentInside = comment.replace(/^(\s*)/, () => innerIndent);

    let j = i + 4;
    const attrLines = [];
    while (j < lines.length) {
      attrLines.push(lines[j]);
      if (lines[j].trim().endsWith('>')) break;
      j += 1;
    }
    if (j >= lines.length || !lines[j].trim().endsWith('>')) continue;

    const replacement = [tagStart, ...attrLines, commentInside];
    lines.splice(i, j - i + 1, ...replacement);
    changed = true;
  }
  return changed;
}

let fixed = 0;
for (const fp of walk(ROOT)) {
  const raw = fs.readFileSync(fp, 'utf8');
  const eol = raw.includes('\r\n') ? '\r\n' : '\n';
  const lines = raw.replace(/\r\n/g, '\n').split('\n');
  if (repairLines(lines)) {
    fs.writeFileSync(fp, lines.join(eol), 'utf8');
    fixed += 1;
    console.log(path.relative(ROOT, fp).replace(/\\/g, '/'));
  }
}
console.log(`\nRepaired: ${fixed} files`);
