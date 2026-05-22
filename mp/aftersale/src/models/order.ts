/**
 * 工单主状态（mainStatus）枚举：与 contractor / jasic-ui 字面完全一致
 *
 * 真源：后端 `sys_work_order.main_status`，以及 jasic-ui 列表字段 `mainStatus`。
 * - PENDING_ASSIGN：待派单
 * - PENDING_TECH_ACCEPT：已派单待维修员接单
 * - IN_PROGRESS：维修中
 * - COMPLETED：已完成
 * - CLOSED：已关闭
 *
 * 禁止使用 `pending / processing / completed / closed` 小写别名（阶段 4.1）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderMainStatus =
  | 'PENDING_ASSIGN'
  | 'PENDING_TECH_ACCEPT'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CLOSED'

/**
 * 工单主状态常量（与 `WorkOrderMainStatus` 一一对应，供 api/utils 引用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WORK_ORDER_MAIN_STATUS = {
  PENDING_ASSIGN: 'PENDING_ASSIGN',
  PENDING_TECH_ACCEPT: 'PENDING_TECH_ACCEPT',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  CLOSED: 'CLOSED',
} as const satisfies Record<WorkOrderMainStatus, WorkOrderMainStatus>

/**
 * 故障点记录
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type FaultPointRecord = {
  /**
 * 维修说明汇总（faultDesc · 维修主文案），旧缓存可能仅有本字段
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  description: string
  /**
 * 结构化字段（新映射必带，便于历史页按「其它维修说明」规则展示）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultDesc?: string
  repairDesc?: string
  otherDesc?: string
  images: { url: string; label: string }[]
  parts?: { name: string; count: number }[]
  /**
 * repairDesc 非「其它维修说明」时的补充说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  specialInfo?: string
  location: string
  date: string
}
