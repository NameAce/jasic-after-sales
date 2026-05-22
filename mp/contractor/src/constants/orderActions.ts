/**
 * 工单动作元数据：与 jasic-ui `ACTION_META` 保持一致。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

/**
 * 可识别动作 key（与 ACTION_META 一一对应）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderActionKey = keyof typeof ACTION_META

/**
 * 动作 key 列表（用于遍历/排序）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WORK_ORDER_ACTION_KEYS = Object.keys(ACTION_META) as WorkOrderActionKey[]

/**
 * 判断输入是否为合法工单动作 key。
 * 说明：`RETURN_METHOD` 等不在 ACTION_META 中的动作会返回 false。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const isWorkOrderActionKey = (value: unknown): value is WorkOrderActionKey =>
  typeof value === 'string' && value in ACTION_META

/**
 * 后端与 `WorkOrderActionEnum` / `WorkOrderPermissionService.DETAIL_ACTION_ORDER` 对齐的别名。
 * 小程序列表「机器返回方式」与 `CLOSE` 使用同一套处理，统一映射为 `CLOSE`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const ACTION_CODE_ALIASES: Record<string, WorkOrderActionKey> = {
  RETURN_METHOD: 'CLOSE',
}

/**
 * 将接口 `availableActions` 归一化为可用动作数组：
 * - 将 `RETURN_METHOD` 等别名映射为 `ACTION_META` 中的 key；
 * - 仅保留 ACTION_META 中存在的动作；
 * - 去重（映射后去重，避免 RETURN_METHOD 与 CLOSE 同时存在时重复展示）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const normalizeAvailableActions = (actions: unknown): WorkOrderActionKey[] => {
  if (!Array.isArray(actions)) return []

  const normalized: WorkOrderActionKey[] = []
  const seen = new Set<WorkOrderActionKey>()

  for (const action of actions) {
    if (typeof action !== 'string') continue
    const raw = action.trim()
    if (!raw) continue
    const resolved = ACTION_CODE_ALIASES[raw] ?? (isWorkOrderActionKey(raw) ? raw : null)
    if (resolved == null || seen.has(resolved)) continue
    normalized.push(resolved)
    seen.add(resolved)
  }

  return normalized
}

/**
 * 与后端详情页动作返回顺序一致（见 `WorkOrderPermissionService.DETAIL_ACTION_ORDER`），
 * 仅对列表会展示的动作排序，保证按钮位置稳定、与 jasic-ui 一致。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const DISPLAY_ORDER: WorkOrderActionKey[] = [
  'ASSIGN',
  'UPLOAD_SEND_EXPRESS',
  'TECH_ACCEPT',
  'TRANSFER',
  'REPAIR_FINISH',
  'REVIEW',
  'CLOSE',
]

/**
 * 作用：承修方小程序（网点/总部工单处理、派工）内方法：sortWorkOrderActionsForDisplay。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const sortWorkOrderActionsForDisplay = (keys: WorkOrderActionKey[]): WorkOrderActionKey[] => {
  const rank = (k: WorkOrderActionKey) => {
    const i = DISPLAY_ORDER.indexOf(k)
    return i === -1 ? 999 : i
  }
  return [...keys].sort((a, b) => rank(a) - rank(b))
}

/**
 * 承修方小程序列表按钮文案。
 * CLOSE / RETURN_METHOD 在列表统一展示为「机器返回方式」，与详情底栏一致。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const getContractorListActionLabel = (key: WorkOrderActionKey): string => {
  if (key === 'CLOSE') return '机器返回方式'
  return ACTION_META[key].label
}

/**
 * 承修方小程序列表按钮样式：关闭/返回方式使用 outline，其余 primary
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const getContractorListActionClassName = (
  key: WorkOrderActionKey
): 'primary' | 'outline' => (key === 'CLOSE' ? 'outline' : 'primary')
