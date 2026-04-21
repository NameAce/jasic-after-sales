/**
 * 兼容层：原 `WorkOrderMainStatus` / `WORK_ORDER_MAIN_STATUS` 真源已迁至
 * `@/models/order`，工单状态相关 UI 工具函数（`ORDER_STATUS_TEXT_MAP`、
 * `getStatusDesc`、`getStatusIcon`、`getStepIndex`、`isOrderStatus`、
 * `isPendingMainStatus`）迁至 `@/utils/orderStatus`，字面镜像 contractor
 * `mp/contractor/src/utils/orderStatus.ts`。
 *
 * 本文件仅做 re-export，保留旧引用位不破坏；C 端特有的 UI 标签映射
 * （`WORK_ORDER_MAIN_STATUS_UI_LABEL`）与字面归一化工具
 * （`normalizeWorkOrderMainStatus`）继续留在此处，与
 * `api/workOrder.ts` 内 `MAIN_STATUS_TO_UI` 等价但不强制镜像至 contractor。
 */
import type { WorkOrderMainStatus } from '@/models/order'

export { WORK_ORDER_MAIN_STATUS } from '@/models/order'
export type { WorkOrderMainStatus } from '@/models/order'

/** 工单主状态 → 客户端 UI 中文桶（与 api/workOrder.ts 的 MAIN_STATUS_TO_UI 等价） */
export const WORK_ORDER_MAIN_STATUS_UI_LABEL: Record<WorkOrderMainStatus, string> = {
  PENDING_ASSIGN: '待接单',
  PENDING_TECH_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
}

/** 规范化接口 mainStatus 字面（大写，连字符转下划线） */
export function normalizeWorkOrderMainStatus(mainStatus: string | undefined): string {
  return (mainStatus ?? '').trim().toUpperCase().replace(/-/g, '_')
}
