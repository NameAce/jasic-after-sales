/**
 * 动作驱动灰度开关：
 * - true: availableActions 缺失时回退旧状态逻辑（默认，便于平滑过渡）
 * - false: 仅按 availableActions 渲染，不再走状态硬编码
 */
export const ENABLE_LEGACY_STATUS_ACTION_FALLBACK = String(
  import.meta.env.VITE_ENABLE_LEGACY_STATUS_ACTION_FALLBACK ?? 'true'
).toLowerCase() !== 'false'

type FallbackAuditPayload = {
  orderId: string | number
  role: 'hq' | 'dispatcher' | 'engineer' | 'unknown'
  status: string
  primaryTab: string
  secondaryTab: string
  fallbackActions: string[]
}

const seenFallbackAuditKeys = new Set<string>()

/**
 * 记录回退命中（仅开发环境），用于观察“无 availableActions”场景命中率。
 * 同一订单+状态+标签只记录一次，避免刷屏。
 */
export const auditLegacyStatusFallbackHit = (payload: FallbackAuditPayload) => {
  if (!import.meta.env.DEV) return
  const key = [
    String(payload.orderId ?? '').trim(),
    payload.role,
    payload.status,
    payload.primaryTab,
    payload.secondaryTab
  ].join('|')
  if (seenFallbackAuditKeys.has(key)) return
  seenFallbackAuditKeys.add(key)
  console.info('[order-action-fallback-hit]', payload)
}
