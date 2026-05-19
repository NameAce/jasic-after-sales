/**
 * 工单列表主操作：按接口 `availableActions` 渲染行内按钮（顺序与后端 WorkOrderPermissionService 一致）。
 *
 * @see jasic-ui `workOrder/index.vue` 列表操作
 * @修改人 黄碧莲
 * @修改时间 2026-05-18
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

/** 列表行内主操作按钮最大展示数量（超出部分归入 more，当前 antd 列表为横排全展示） */
const LIST_MAX_PRIMARY_ACTIONS = 20;

export interface RowActionButton {
  action: WorkOrderListActionCode;
  label: string;
  title: string;
  type: ListActionType;
}

/**
 * 判断字符串是否为有效的工单列表动作编码。
 *
 * @param s - 待判断字符串
 * @returns 是否为 WorkOrderListActionCode
 */
function isActionCode(s: string): s is WorkOrderListActionCode {
  return Object.hasOwn(ACTION_META, s);
}

/**
 * 从行数据解析并规范化可用动作编码列表（保持接口返回顺序）。
 *
 * @param row - 表格行对象（含 availableActions）
 * @returns 有效动作编码数组
 */
export function normalizeRowActionCodes(row: Record<string, unknown>): WorkOrderListActionCode[] {
  const raw = row?.availableActions;
  const actions = Array.isArray(raw) ? raw : [];
  return actions.filter((a): a is WorkOrderListActionCode => {
    if (typeof a !== 'string' || !a) return false;
    if (a === 'RETURN_METHOD') return false;
    return isActionCode(a);
  });
}

/**
 * 将行可用动作拆成主按钮区与 more 区（主区按接口顺序，more 保留结构供扩展）。
 *
 * @param row - 表格行对象
 * @returns primary / more 按钮配置
 */
export function splitRowActions(row: Record<string, unknown>): {
  primary: RowActionButton[];
  more: RowActionButton[];
} {
  const actionCodes = normalizeRowActionCodes(row);
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
 * 获取行数据对应的主操作按钮列表。
 *
 * @param row - 表格行对象
 * @returns 主操作按钮配置数组
 */
export function getRowPrimaryActions(row: Record<string, unknown>): RowActionButton[] {
  return splitRowActions(row).primary;
}

/**
 * 获取行数据中归入「更多」的动作按钮列表。
 *
 * @param row - 表格行对象
 * @returns 更多区按钮配置数组
 */
export function getRowMoreActions(row: Record<string, unknown>): RowActionButton[] {
  return splitRowActions(row).more;
}

/**
 * 当前视图下是否应展示只读原因提示（无 availableActions 且接口返回 readonlyReason）。
 *
 * @param row - 表格行对象
 * @param isCurrentView - 是否为「当前处理」视图
 * @returns 是否展示只读原因
 */
export function shouldShowReadonlyReason(row: Record<string, unknown>, isCurrentView: boolean): boolean {
  if (!isCurrentView) return false;
  if (String(row?.mainStatus || '') === 'CLOSED') return false;
  if (normalizeRowActionCodes(row).length) return false;
  const r = row?.readonlyReason;
  return typeof r === 'string' && r.trim().length > 0;
}
