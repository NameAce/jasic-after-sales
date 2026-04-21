import type { OrderListItem, WorkOrderMainStatus } from '@/models/order'
import { WORK_ORDER_MAIN_STATUS } from '@/models/order'
import { ORDER_STATUS_TEXT_MAP, isPendingMainStatus } from '@/utils/orderStatus'

/**
 * 工单列表/详情接口 mainStatus 规范化（与 list 页查询参数一致）
 */
export function normalizeWorkOrderMainStatus(mainStatus: string | undefined): string {
  return (mainStatus ?? '').trim().toUpperCase().replace(/-/g, '_')
}

/** 待接单：仅认接口枚举 PENDING_TECH_ACCEPT */
export function isWorkOrderPendingTechAcceptMainStatus(mainStatus: string | undefined): boolean {
  return normalizeWorkOrderMainStatus(mainStatus) === WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT
}

/** 待派单：`status === PENDING_ASSIGN`（与 `order/list` 卡片逻辑一致） */
export function isMainStatusPendingAssign(order: OrderListItem): boolean {
  return order.status === WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN
}

/**
 * 列表「待处理」状态角标：派单权限下按 mainStatus 区分待派/待接；「已派给自己待本人接单」强制展示待接单
 */
export function getPendingDisplayLabel(
  order: OrderListItem,
  hasAssignPermission: boolean,
  isAwaitSelfAccept: boolean,
): string {
  const status: WorkOrderMainStatus | undefined = order.status
  if (!isPendingMainStatus(status)) {
    return status ? ORDER_STATUS_TEXT_MAP[status] : ''
  }
  if (!hasAssignPermission) return '待接单'
  if (isAwaitSelfAccept) return '待接单'
  return status === WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT ? '待接单' : '待派单'
}
