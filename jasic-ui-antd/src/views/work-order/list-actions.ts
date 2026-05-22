/**
 * 作用：工单列表主操作配置，按接口 availableActions 渲染行内按钮（顺序与后端 WorkOrderPermissionService 一致）。
 * @see jasic-ui `workOrder/index.vue` 列表操作
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderListActionCode =
  | 'ASSIGN'
  | 'TECH_ACCEPT'
  | 'TRANSFER'
  | 'REPAIR_FINISH'
  | 'REVIEW'
  | 'UPLOAD_SEND_EXPRESS'
  | 'CLOSE';

export type ListActionType = 'primary' | 'warning' | 'danger';

export interface ListActionMeta {
  label: string;
  title: string;
  type: ListActionType;
}

/** 各列表动作编码对应的展示文案与按钮样式 */
export const ACTION_META: Record<WorkOrderListActionCode, ListActionMeta> = {
  ASSIGN: { label: '派单', title: '派单', type: 'primary' },
  TECH_ACCEPT: { label: '维修员接单', title: '维修员接单', type: 'primary' },
  TRANSFER: { label: '转单', title: '转单', type: 'warning' },
  REPAIR_FINISH: { label: '维修登记', title: '维修登记', type: 'primary' },
  REVIEW: { label: '复检', title: '复检登记', type: 'warning' },
  UPLOAD_SEND_EXPRESS: { label: '上传寄件单号', title: '上传寄件单号', type: 'primary' },
  CLOSE: { label: '关闭工单', title: '关闭工单', type: 'danger' }
};

// 列表行内主操作按钮最大展示数量（超出部分归入 more，当前 antd 列表为横排全展示）
const LIST_MAX_PRIMARY_ACTIONS = 20;

export interface RowActionButton {
  action: WorkOrderListActionCode;
  label: string;
  title: string;
  type: ListActionType;
}

export type WorkOrderListViewScope = 'CURRENT' | 'HISTORY' | 'ALL' | undefined;

/**
 * 作用：判断字符串是否为有效的工单列表动作编码。
 * @param s - 待判断字符串
 * @returns 是否为 WorkOrderListActionCode
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isActionCode(s: string): s is WorkOrderListActionCode {
  return Object.hasOwn(ACTION_META, s);
}

/**
 * 作用：从行数据解析并规范化可用动作编码列表（保持接口返回顺序，过滤 RETURN_METHOD）。
 * @param row - 表格行对象（含 availableActions）
 * @returns 有效动作编码数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function normalizeRowActionCodes(row: Record<string, unknown>): WorkOrderListActionCode[] {
  const raw = row?.availableActions;
  const actions = Array.isArray(raw) ? raw : [];
  return actions.filter((a): a is WorkOrderListActionCode => {
    if (typeof a !== 'string' || !a) return false;
    // 后端可能下发详情专用码，列表行内不展示
    if (a === 'RETURN_METHOD') return false;
    return isActionCode(a);
  });
}

/**
 * 作用：按当前列表视图过滤行内可展示动作（当前处理全量；历史转出仅保留补寄件等例外）。
 * @param row - 表格行对象（含 availableActions）
 * @param viewScope - 当前列表视图 CURRENT / HISTORY
 * @returns 当前视图允许展示的动作编码数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolveVisibleRowActionCodes(
  row: Record<string, unknown>,
  viewScope: WorkOrderListViewScope = 'CURRENT'
): WorkOrderListActionCode[] {
  const actionCodes = normalizeRowActionCodes(row);
  if (viewScope === 'CURRENT') {
    return actionCodes;
  }
  if (viewScope === 'HISTORY') {
    // 历史转出视图仍是流程只读，只透出后端允许的“建单人补寄件单号”例外动作。
    return actionCodes.filter(action => action === 'UPLOAD_SEND_EXPRESS');
  }
  return [];
}

/**
 * 作用：将行可用动作拆成主按钮区与 more 区（主区按接口顺序，more 保留结构供扩展）。
 * @param row - 表格行对象
 * @param viewScope - 当前列表视图
 * @returns primary / more 按钮配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function splitRowActions(
  row: Record<string, unknown>,
  viewScope: WorkOrderListViewScope = 'CURRENT'
): {
  primary: RowActionButton[];
  more: RowActionButton[];
} {
  const actionCodes = resolveVisibleRowActionCodes(row, viewScope);
  const primaryCodes = actionCodes.slice(0, LIST_MAX_PRIMARY_ACTIONS);
  const moreCodes = actionCodes.slice(LIST_MAX_PRIMARY_ACTIONS);
  const toButton = (action: WorkOrderListActionCode): RowActionButton => {
    const m = ACTION_META[action];
    return {
      action,
      label: m.label,
      title: m.title,
      type: m.type
    };
  };
  return {
    primary: primaryCodes.map(toButton),
    more: moreCodes.map(toButton)
  };
}

/**
 * 作用：获取行数据对应的主操作按钮列表。
 * @param row - 表格行对象
 * @param viewScope - 当前列表视图
 * @returns 主操作按钮配置数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getRowPrimaryActions(
  row: Record<string, unknown>,
  viewScope: WorkOrderListViewScope = 'CURRENT'
): RowActionButton[] {
  return splitRowActions(row, viewScope).primary;
}

/**
 * 作用：获取行数据中归入「更多」的动作按钮列表。
 * @param row - 表格行对象
 * @param viewScope - 当前列表视图
 * @returns 更多区按钮配置数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getRowMoreActions(
  row: Record<string, unknown>,
  viewScope: WorkOrderListViewScope = 'CURRENT'
): RowActionButton[] {
  return splitRowActions(row, viewScope).more;
}

/**
 * 作用：当前视图下是否应展示只读原因提示（无 availableActions 且接口返回 readonlyReason）。
 * @param row - 表格行对象
 * @param viewScope - 当前列表视图
 * @returns 是否展示只读原因
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function shouldShowReadonlyReason(
  row: Record<string, unknown>,
  viewScope: WorkOrderListViewScope = 'CURRENT'
): boolean {
  if (viewScope !== 'CURRENT' && viewScope !== 'HISTORY') return false;
  if (String(row?.mainStatus || '') === 'CLOSED') return false;
  if (resolveVisibleRowActionCodes(row, viewScope).length) return false;
  const r = row?.readonlyReason;
  return typeof r === 'string' && r.trim().length > 0;
}
