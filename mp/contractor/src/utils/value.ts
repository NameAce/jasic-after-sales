/** 非空字符串或非空数组 */
export function hasVal(v: unknown): boolean {
  if (v === null || v === undefined) return false
  if (Array.isArray(v)) return v.length > 0
  return String(v).trim() !== ''
}

/** 非空字符串 */
export function hasStr(v: unknown): boolean {
  return typeof v === 'string' && v.trim() !== ''
}

/**
 * 工单详情「维修报价」是否有可展示的有效金额（接口缺省常映射为 0 / 0.00，视为未返回）
 */
export function hasMeaningfulRepairQuoteAmount(v: unknown): boolean {
  const s = String(v ?? '').trim().replace(/,/g, '')
  if (!s) return false
  const n = Number(s)
  return Number.isFinite(n) && n > 0
}
