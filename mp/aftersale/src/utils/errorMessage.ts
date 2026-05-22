/**
 * 作用：转换/构造：parseUnknownError。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function parseUnknownError(err: unknown, fallback: string): string {
  const e = err as { msg?: unknown; message?: unknown }
  return (
    (typeof e?.msg === 'string' && e.msg) ||
    (typeof e?.message === 'string' && e.message) ||
    fallback
  )
}
