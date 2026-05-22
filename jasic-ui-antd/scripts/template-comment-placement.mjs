/**
 * template 根注释放置工具：注释必须写在唯一根元素内部，避免与根节点并列导致 Transition 多根白屏。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

/**
 * 判断 template 下是否已有根内注释（紧跟根元素开标签之后）
 */
export function hasTemplateRootComment(lines, tplIdx) {
  let i = tplIdx + 1;
  while (i < lines.length && lines[i].trim() === '') i += 1;
  if (i >= lines.length) return false;
  const first = lines[i].trim();
  if (first.startsWith('<!--')) return true;
  const isRootOpen = first.endsWith('>') && !first.startsWith('<!--');
  if (!isRootOpen) return false;
  let j = i + 1;
  while (j < lines.length && lines[j].trim() === '') j += 1;
  return j < lines.length && lines[j].trim().startsWith('<!--');
}

/**
 * 在 template 第一个根元素内部插入说明注释（保持单根）
 */
export function insertTemplateRootComment(lines, tplIdx, text) {
  let rootIdx = tplIdx + 1;
  while (rootIdx < lines.length && lines[rootIdx].trim() === '') rootIdx += 1;
  if (rootIdx >= lines.length || lines[rootIdx].trim() === '</template>') return false;

  const rootLine = lines[rootIdx];
  const indent = rootLine.match(/^(\s*)/)[1];
  const trimmed = rootLine.trim();
  const innerIndent = `${indent}  `;
  const commentLine = `${innerIndent}<!-- ${text} -->`;

  // 多行开标签：注释放在完整开标签块之后
  if (!trimmed.includes('>')) {
    let endIdx = rootIdx;
    while (endIdx < lines.length && !lines[endIdx].trim().endsWith('>')) endIdx += 1;
    if (endIdx >= lines.length) return false;
    lines.splice(endIdx + 1, 0, commentLine);
    return true;
  }

  if (/\/>\s*$/.test(trimmed)) {
    lines[rootIdx] = `${indent}<div>`;
    lines.splice(rootIdx + 1, 0, commentLine, `${innerIndent}${trimmed}`, `${indent}</div>`);
    return true;
  }

  if (trimmed.endsWith('>') && !trimmed.endsWith('/>')) {
    lines.splice(rootIdx + 1, 0, commentLine);
    return true;
  }

  lines[rootIdx] = `${indent}<div class="h-full min-h-0">`;
  lines.splice(rootIdx + 1, 0, commentLine, `${innerIndent}${trimmed}`, `${indent}</div>`);
  return true;
}
