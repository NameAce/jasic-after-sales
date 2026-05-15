/**
 * 弹窗/抽屉表单自适应：表单项严格多于 6 个（即从第 7 个起）时宽度至少加宽到 {@link ADAPTIVE_MODAL_MIN_WIDE_WIDTH}，
 * 并启用两列栅格类名（配合全局 CSS）。6 个及以下保持基准宽度与单列。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */

/** 达到该数量及以上时才加宽并两列（不含 6：恰好 6 项不触发） */
export const ADAPTIVE_MODAL_FORM_WIDE_MIN_COUNT = 7;

/** 表单项超过阈值时，抽屉/弹窗宽度的下限（像素）；与基准宽度取较大值，避免已很宽的弹窗被缩小 */
export const ADAPTIVE_MODAL_MIN_WIDE_WIDTH = 720;

/**
 * 作用：表单项达到 {@link ADAPTIVE_MODAL_FORM_WIDE_MIN_COUNT} 及以上时，将宽度设为「基准宽度」与 {@link ADAPTIVE_MODAL_MIN_WIDE_WIDTH} 的较大值。
 * @param baseWidthPx 当前设计基准宽度（像素）
 * @param fieldCount 本弹窗内可见表单项数量（与 AFormItem 数量一致或业务等价计数）
 * @param minWideCount 触发加宽的最少表单项数，默认 7（即多于 6 个才加宽）
 * @returns {number} 实际应设置的 `width`（像素）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function adaptiveModalWidth(
  baseWidthPx: number,
  fieldCount: number,
  minWideCount: number = ADAPTIVE_MODAL_FORM_WIDE_MIN_COUNT
): number {
  return fieldCount >= minWideCount ? Math.max(baseWidthPx, ADAPTIVE_MODAL_MIN_WIDE_WIDTH) : baseWidthPx;
}

/**
 * 作用：表单项达到最少数量时返回两列栅格容器类名，否则空串。
 * @param fieldCount 表单项数量
 * @param minWideCount 触发两列的最少表单项数，默认 7（即多于 6 个才两列）
 * @returns {string} 附加到包裹 `AFormItem` 的容器上的 class
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function adaptiveModalFormGridClass(
  fieldCount: number,
  minWideCount: number = ADAPTIVE_MODAL_FORM_WIDE_MIN_COUNT
): string {
  return fieldCount >= minWideCount ? 'adaptive-modal-form-grid' : '';
}
