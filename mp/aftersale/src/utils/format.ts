/**
 * 格式化工具函数
 *
 * 三端对齐口径（`strict_jasic_ui`，阶段 D）：
 * - aftersale / contractor 两端 `src/utils/format.ts` 字面镜像（同名 + 同签名 + 同实现）。
 * - 真源与设计口径见 [mp/MIRROR_FILE_PAIRS.md](../../../MIRROR_FILE_PAIRS.md) 的
 *   「格式化函数名表」章节与「三端契约对齐落地」计划的阶段 D。
 *
 * 对齐签名：
 * - formatIsoDateTime(raw: unknown): string
 * - formatAmount(raw: unknown, fallback?: string): string
 * - maskMobile(raw: unknown): string
 * - maskAddress(raw: unknown): string
 *
 * 注：`formatTimeHHMM` 为两端共用的 UI 时间展示函数，沿用既有调用点，不迁移、不改签名。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

/**
 * 将 ISO / 类 ISO 时间字符串规范化为 `YYYY-MM-DD HH:mm:ss` 形式的字符串。
 *
 * 规则：
 * - 中间 `T` 替换为空格
 * - 去除毫秒 `.SSS` 与末尾 `Z`
 * - `null / undefined / ''` → 返回空串
 *
 * @param raw 原始时间字符串
 * @returns 规范化后的字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatIsoDateTime(raw: unknown): string {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  return s
    .replace('T', ' ')
    .replace(/\.\d{3}Z?$/, '')
    .replace(/Z$/, '')
    .trim()
}

/**
 * 规范化金额为保留两位小数的字符串。
 *
 * 规则：
 * - `null / undefined / ''` → `fallback`
 * - 可解析为有限数值 → `n.toFixed(2)`
 * - 非数值字符串 → 原始字符串（trim 后）
 *
 * @param raw 原始金额
 * @param fallback 无法解析时的兜底值，默认空串
 * @returns 金额展示字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatAmount(raw: unknown, fallback: string = ''): string {
  if (raw === undefined || raw === null || raw === '') return fallback
  if (typeof raw === 'number') {
    return Number.isFinite(raw) ? raw.toFixed(2) : fallback
  }
  const s = String(raw).trim()
  if (!s) return fallback
  const n = Number(s)
  if (Number.isFinite(n)) return n.toFixed(2)
  return s
}

/**
 * 手机号脱敏：保留前 3、后 4 位，中间 4 位用 `*` 覆盖。
 *
 * 规则：
 * - 长度 `<= 7` 的输入原样返回（不足以脱敏）
 * - `null / undefined / ''` → 返回空串
 *
 * @param raw 原始手机号
 * @returns 脱敏后字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function maskMobile(raw: unknown): string {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  if (s.length <= 7) return s
  return `${s.slice(0, 3)}****${s.slice(-4)}`
}

/**
 * 地址脱敏：保留前 6 字符与末 4 字符，中间用 `****` 覆盖。
 *
 * 规则：
 * - 长度 `<= 10` 的输入原样返回（不足以脱敏）
 * - `null / undefined / ''` → 返回空串
 *
 * @param raw 原始地址
 * @returns 脱敏后字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function maskAddress(raw: unknown): string {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  if (s.length <= 10) return s
  return `${s.slice(0, 6)}****${s.slice(-4)}`
}

/**
 * 格式化时间为 HH:MM 格式
 * @param date 日期
 * @returns 格式化后的时间
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatTimeHHMM(date: Date = new Date()): string {
  return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}
