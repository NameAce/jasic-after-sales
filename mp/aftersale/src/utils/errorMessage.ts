export function parseUnknownError(err: unknown, fallback: string): string {
  const e = err as { msg?: unknown; message?: unknown }
  return (
    (typeof e?.msg === 'string' && e.msg) ||
    (typeof e?.message === 'string' && e.message) ||
    fallback
  )
}
