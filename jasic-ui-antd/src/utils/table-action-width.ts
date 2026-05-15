/**
 * 作用：估算 Ant Design Vue「小号 + type=link」按钮单行横排时的总宽度，用于表格操作列 `width`。
 * @remarks 按中文等宽近似（每字约 14px）+ 左右留白；与 `ASpace` 默认间距叠加。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */

/**
 * 作用：单个小号 link 按钮的估算宽度（像素）。
 * @param label 按钮可见文案
 * @returns {number} 估算宽度，含最小点击宽度
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function estimateLinkActionButtonWidth(label: string): number {
  // 小号 link：左右留白 + 按字符数估算文本宽，并保证最小可点区域
  const charCount = [...String(label || '')].length;
  const textPx = 10 + charCount * 14;
  return Math.max(48, Math.ceil(textPx));
}

/**
 * 作用：按一行内可能出现的全部操作按钮文案，估算操作列最小宽度（横排不换行）。
 * @param labels 同一行内「最多」会同时出现的按钮文案（与模板中 `AButton` 文案一致，含最长组合）
 * @param gapPx 按钮间距（与 `ASpace :size` 接近时可取 6~10）
 * @returns {number} 建议写入表格列 `width` 的像素值（已向上取整并留少量余量）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function estimateAntTableActionColWidth(labels: readonly string[], gapPx = 8): number {
  if (!labels.length) return 80;
  // 各按钮宽度之和
  const sumBtn = labels.reduce((acc, t) => acc + estimateLinkActionButtonWidth(t), 0);
  // 按钮间隙：n 个按钮有 n-1 段间距；再留 8px 边距防止字体渲染差异裁切
  const gaps = Math.max(0, labels.length - 1) * gapPx;
  return Math.ceil(sumBtn + gaps + 8);
}
