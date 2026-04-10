import type { OrderStatus } from '@/models/order'

/**
 * 工单状态 → 展示文本（基础映射）
 * @returns 工单状态 → 展示文本
 */
export const ORDER_STATUS_TEXT_MAP: Record<OrderStatus, string> = {
  pending: '待接单',
  processing: '维修中',
  completed: '已完成',
  closed: '已关闭'
}

/**
 * 类型守卫：判断值是否为合法 OrderStatus
 * @param value 值
 * @returns 是否为合法 OrderStatus
 */
export function isOrderStatus(value: unknown): value is OrderStatus {
  return value === 'pending' || value === 'processing' || value === 'completed' || value === 'closed'
}

/**
 * 工单状态描述文案
 * @param status 工单状态
 * @returns 工单状态描述文案
 */
export function getStatusDesc(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    pending: '工单已提交，等待网点接单',
    processing: '网点已接单，正在为您维修',
    completed: '维修已完成，感谢您的支持',
    closed: '该工单已关闭，感谢您的配合'
  }
  return map[status] || ''
}

/**
 * 工单状态 Material Icon 名称
 * @param status 工单状态
 * @returns 工单状态 Material Icon 名称
 */
export function getStatusIcon(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    pending: 'pending_actions',
    processing: 'build_circle',
    completed: 'check_circle',
    closed: 'task_alt'
  }
  return map[status] || 'info'
}

/**
 * 工单进度步骤索引（0-based）
 * @param status 工单状态
 * @returns 工单进度步骤索引
 */
export function getStepIndex(status: OrderStatus): number {
  const map: Record<OrderStatus, number> = {
    pending: 0,
    processing: 1,
    completed: 2,
    closed: 3
  }
  return map[status] ?? 0
}
