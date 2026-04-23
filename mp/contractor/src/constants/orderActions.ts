/**
 * 工单动作元数据：与 jasic-ui `ACTION_META` 保持一致。
 */
export const ACTION_META = {
  ASSIGN: { label: '派单', title: '派单', type: 'primary' },
  TECH_ACCEPT: { label: '维修员接单', title: '维修员接单', type: 'primary' },
  TRANSFER: { label: '转单', title: '转单', type: 'warning' },
  REPAIR_FINISH: { label: '维修登记', title: '维修登记', type: 'primary' },
  REVIEW: { label: '复检', title: '复检登记', type: 'warning' },
  UPLOAD_SEND_EXPRESS: { label: '上传寄件单号', title: '上传寄件单号', type: 'primary' },
  CLOSE: { label: '关闭工单', title: '关闭工单', type: 'danger' },
} as const

/** 可识别动作 key（与 ACTION_META 一一对应） */
export type WorkOrderActionKey = keyof typeof ACTION_META

/** 动作 key 列表（用于遍历/排序） */
export const WORK_ORDER_ACTION_KEYS = Object.keys(ACTION_META) as WorkOrderActionKey[]

/**
 * 判断输入是否为合法工单动作 key。
 * 说明：`RETURN_METHOD` 等不在 ACTION_META 中的动作会返回 false。
 */
export const isWorkOrderActionKey = (value: unknown): value is WorkOrderActionKey =>
  typeof value === 'string' && value in ACTION_META

/**
 * 将接口 `availableActions` 归一化为可用动作数组：
 * - 仅保留 ACTION_META 中存在的动作；
 * - 去重；
 * - 保持原始顺序。
 */
export const normalizeAvailableActions = (actions: unknown): WorkOrderActionKey[] => {
  if (!Array.isArray(actions)) return []

  const normalized: WorkOrderActionKey[] = []
  const seen = new Set<WorkOrderActionKey>()

  for (const action of actions) {
    if (!isWorkOrderActionKey(action) || seen.has(action)) continue
    normalized.push(action)
    seen.add(action)
  }

  return normalized
}
