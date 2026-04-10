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
