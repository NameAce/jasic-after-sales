/**
 * 将 <template> 下与根元素并列的 HTML 注释移入根元素内部，保证模板仅一个根节点（兼容 Transition）。
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

/**
 * 判断是否为自闭合单标签根节点（需外包 div 才能放注释）
 */
function shouldWrapRoot(trimmed) {
  return /\/>\s*$/.test(trimmed);
}

/**
 * 移动 template 顶层的并列注释到第一个根元素内部
 */
function fixFile(filePath) {
  const raw = fs.readFileSync(filePath, 'utf8');
  const eol = raw.includes('\r\n') ? '\r\n' : '\n';
  const lines = raw.replace(/\r\n/g, '\n').split('\n');
  const tplIdx = lines.findIndex(l => l.trim() === '<template>');
  if (tplIdx < 0) return false;

  const comments = [];
  let scan = tplIdx + 1;
  while (scan < lines.length && /^\s*<!--/.test(lines[scan])) {
    comments.push(lines[scan]);
    scan += 1;
  }
  if (comments.length === 0) return false;

  lines.splice(tplIdx + 1, comments.length);

  let rootIdx = tplIdx + 1;
  while (rootIdx < lines.length && lines[rootIdx].trim() === '') rootIdx += 1;
  if (rootIdx >= lines.length || lines[rootIdx].trim() === '</template>') return false;

  const rootLine = lines[rootIdx];
  const indent = rootLine.match(/^(\s*)/)[1];
  const trimmed = rootLine.trim();
  const innerIndent = `${indent}  `;
  const normalizedComments = comments.map(line => {
    const m = line.trim().match(/^<!--\s*(.*?)\s*-->$/);
    const text = m ? m[1] : line.trim();
    return `${innerIndent}<!-- ${text} -->`;
  });

  // 多行开标签：从根行起收集到以 > 结尾的行为止，注释放在开标签块之后
  if (!trimmed.includes('>')) {
    let endIdx = rootIdx;
    while (endIdx < lines.length && !lines[endIdx].trim().endsWith('>')) endIdx += 1;
    if (endIdx >= lines.length) return false;
    lines.splice(endIdx + 1, 0, ...normalizedComments);
    return true;
  }

  const isOpeningWithChildren = trimmed.endsWith('>') && !trimmed.endsWith('/>');

  if (shouldWrapRoot(trimmed)) {
    lines[rootIdx] = `${indent}<div>`;
    lines.splice(rootIdx + 1, 0, ...normalizedComments, `${innerIndent}${trimmed}`, `${indent}</div>`);
  } else if (isOpeningWithChildren) {
    lines.splice(rootIdx + 1, 0, ...normalizedComments);
  } else {
    lines[rootIdx] = `${indent}<div>`;
    lines.splice(rootIdx + 1, 0, ...normalizedComments, `${innerIndent}${trimmed}`, `${indent}</div>`);
  }

  fs.writeFileSync(filePath, lines.join(eol), 'utf8');
  return true;
}

let fixed = 0;
for (const fp of walk(ROOT)) {
  if (fixFile(fp)) {
    fixed += 1;
    console.log(path.relative(ROOT, fp).replace(/\\/g, '/'));
  }
}
console.log(`\nDone: ${fixed} files fixed.`);
