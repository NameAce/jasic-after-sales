import type { WorkOrderMainStatus } from '@/models/order'
import { WORK_ORDER_MAIN_STATUS } from '@/models/order'

export { WORK_ORDER_MAIN_STATUS } from '@/models/order'
export type { WorkOrderMainStatus } from '@/models/order'

/**
 * 工单主状态 → 展示文本（卡片/列表/详情通用）
 *
 * PENDING_ASSIGN / PENDING_TECH_ACCEPT 共享「待接单/待派单」桶：具体文案由
 * `getPendingDisplayLabel` 根据当前用户权限与派单对象再细分。
 */
export const ORDER_STATUS_TEXT_MAP: Record<WorkOrderMainStatus, string> = {
  PENDING_ASSIGN: '待派单',
  PENDING_TECH_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
}

/**
 * 类型守卫：判断值是否为合法 WorkOrderMainStatus
 * @param value 值
 * @returns 是否为合法 WorkOrderMainStatus
 */
export function isOrderStatus(value: unknown): value is WorkOrderMainStatus {
  return (
    value === WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN ||
    value === WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT ||
    value === WORK_ORDER_MAIN_STATUS.IN_PROGRESS ||
    value === WORK_ORDER_MAIN_STATUS.COMPLETED ||
    value === WORK_ORDER_MAIN_STATUS.CLOSED
  )
}

/** 是否属于「待派单 / 待接单」（历史 `status === 'pending'` 桶的语义替代） */
export function isPendingMainStatus(status: WorkOrderMainStatus | undefined | null): boolean {
  return (
    status === WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN ||
    status === WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT
  )
}

/**
 * 工单状态描述文案（详情页顶部提示）
 * @param status 工单状态
 * @returns 工单状态描述文案
 */
export function getStatusDesc(status: WorkOrderMainStatus): string {
  const map: Record<WorkOrderMainStatus, string> = {
    PENDING_ASSIGN: '工单已提交，等待网点接单',
    PENDING_TECH_ACCEPT: '已派单，等待维修员接单',
    IN_PROGRESS: '网点已接单，正在为您维修',
    COMPLETED: '维修已完成，感谢您的支持',
    CLOSED: '该工单已关闭，感谢您的配合',
  }
  return map[status] || ''
}

/**
 * 工单状态 Material Icon 名称
 * @param status 工单状态
 * @returns 工单状态 Material Icon 名称
 */
export function getStatusIcon(status: WorkOrderMainStatus): string {
  const map: Record<WorkOrderMainStatus, string> = {
    PENDING_ASSIGN: 'pending_actions',
    PENDING_TECH_ACCEPT: 'pending_actions',
    IN_PROGRESS: 'build_circle',
    COMPLETED: 'check_circle',
    CLOSED: 'task_alt',
  }
  return map[status] || 'info'
}

/**
 * 工单进度步骤索引（0-based）
 * 五步进度：待派单 → 待接单 → 维修中 → 已完成 → 已关闭
 * @param status 工单状态
 * @returns 工单进度步骤索引
 */
export function getStepIndex(status: WorkOrderMainStatus): number {
  const map: Record<WorkOrderMainStatus, number> = {
    PENDING_ASSIGN: 0,
    PENDING_TECH_ACCEPT: 1,
    IN_PROGRESS: 2,
    COMPLETED: 3,
    CLOSED: 4,
  }
  return map[status] ?? 0
}
