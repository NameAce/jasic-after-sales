/**
 * 工单详情跳转历史页时，暂存 `processFlows`（后端 flows）的 storage key
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WORK_ORDER_FLOWS_HISTORY_STORAGE_KEY = 'contractor.workOrder.flowsHistory.v1'

/**
 * 故障点「查看历史记录」：暂存由 `repairs[].faults` 映射的 `FaultPointRecord[]`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY = 'contractor.workOrder.repairFaultsHistory.v1'
