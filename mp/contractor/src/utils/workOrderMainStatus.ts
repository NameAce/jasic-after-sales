/**
 * 工单列表/详情接口 mainStatus 规范化（与 list 页查询参数一致）
 */
export function normalizeWorkOrderMainStatus(mainStatus: string | undefined): string {
  return (mainStatus ?? '').trim().toUpperCase().replace(/-/g, '_')
}

/** 待接单：仅认接口枚举 PENDING_TECH_ACCEPT */
export function isWorkOrderPendingTechAcceptMainStatus(mainStatus: string | undefined): boolean {
  return normalizeWorkOrderMainStatus(mainStatus) === 'PENDING_TECH_ACCEPT'
}
