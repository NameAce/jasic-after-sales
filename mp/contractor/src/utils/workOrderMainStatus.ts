import type { OrderListItem } from '@/models/order'
import { ORDER_STATUS_TEXT_MAP } from '@/utils/orderStatus'

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

/** 待派单：pending 且非 PENDING_TECH_ACCEPT（与 `order/list` 卡片逻辑一致） */
export function isMainStatusPendingAssign(order: OrderListItem): boolean {
  return order.status === 'pending' && !isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)
}

/**
 * 列表「待处理」状态角标：派单权限下按 mainStatus 区分待派/待接；「已派给自己待本人接单」强制展示待接单
 */
export function getPendingDisplayLabel(
  order: OrderListItem,
  hasAssignPermission: boolean,
  isAwaitSelfAccept: boolean
): string {
  if (order.status !== 'pending') return ORDER_STATUS_TEXT_MAP[order.status]
  if (!hasAssignPermission) return '待接单'
  if (isAwaitSelfAccept) return '待接单'
  return isWorkOrderPendingTechAcceptMainStatus(order.mainStatus) ? '待接单' : '待派单'
}
