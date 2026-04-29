/** 与 jasic-ui `workOrder/index.vue` 列表操作一致 */

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

export const ACTION_META: Record<WorkOrderListActionCode, ListActionMeta> = {
  ASSIGN: { label: '派单', title: '派单', type: 'primary' },
  TECH_ACCEPT: { label: '维修员接单', title: '维修员接单', type: 'primary' },
  TRANSFER: { label: '转单', title: '转单', type: 'warning' },
  REPAIR_FINISH: { label: '维修登记', title: '维修登记', type: 'primary' },
  REVIEW: { label: '复检', title: '复检登记', type: 'warning' },
  UPLOAD_SEND_EXPRESS: { label: '上传寄件单号', title: '上传寄件单号', type: 'primary' },
  CLOSE: { label: '关闭工单', title: '关闭工单', type: 'danger' }
};

/** 列表行内全部展示为链接按钮，不再收入「更多」下拉 */
const LIST_MAX_PRIMARY_ACTIONS = 20;

const LIST_PRIMARY_ACTION_ORDER: Partial<Record<string, WorkOrderListActionCode[]>> = {
  PENDING_ASSIGN: ['ASSIGN', 'UPLOAD_SEND_EXPRESS'],
  PENDING_TECH_ACCEPT: ['TECH_ACCEPT', 'UPLOAD_SEND_EXPRESS'],
  IN_PROGRESS: ['REPAIR_FINISH', 'TRANSFER'],
  COMPLETED: ['REVIEW', 'CLOSE']
};

export interface RowActionButton {
  action: WorkOrderListActionCode;
  label: string;
  title: string;
  type: ListActionType;
}

function isActionCode(s: string): s is WorkOrderListActionCode {
  return Object.hasOwn(ACTION_META, s);
}

export function normalizeRowActionCodes(row: Record<string, unknown>): WorkOrderListActionCode[] {
  const raw = row?.availableActions;
  const actions = Array.isArray(raw) ? raw : [];
  return actions.filter((a): a is WorkOrderListActionCode => {
    if (typeof a !== 'string' || !a) return false;
    if (a === 'RETURN_METHOD') return false;
    return isActionCode(a);
  });
}

export function splitRowActions(row: Record<string, unknown>): {
  primary: RowActionButton[];
  more: RowActionButton[];
} {
  const actionCodes = normalizeRowActionCodes(row);
  const primaryCodes: WorkOrderListActionCode[] = [];
  const consumed = new Set<WorkOrderListActionCode>();
  const mainStatus = String(row?.mainStatus || '');
  const preferredOrder = LIST_PRIMARY_ACTION_ORDER[mainStatus] || [];

  preferredOrder.forEach(action => {
    if (primaryCodes.length >= LIST_MAX_PRIMARY_ACTIONS) return;
    if (actionCodes.includes(action) && !consumed.has(action)) {
      primaryCodes.push(action);
      consumed.add(action);
    }
  });
  actionCodes.forEach(action => {
    if (primaryCodes.length >= LIST_MAX_PRIMARY_ACTIONS || consumed.has(action)) return;
    primaryCodes.push(action);
    consumed.add(action);
  });
  const moreCodes = actionCodes.filter(action => !consumed.has(action));
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

export function getRowPrimaryActions(row: Record<string, unknown>): RowActionButton[] {
  return splitRowActions(row).primary;
}

export function getRowMoreActions(row: Record<string, unknown>): RowActionButton[] {
  return splitRowActions(row).more;
}

export function shouldShowReadonlyReason(row: Record<string, unknown>, isCurrentView: boolean): boolean {
  if (!isCurrentView) return false;
  if (String(row?.mainStatus || '') === 'CLOSED') return false;
  if (normalizeRowActionCodes(row).length) return false;
  const r = row?.readonlyReason;
  return typeof r === 'string' && r.trim().length > 0;
}
