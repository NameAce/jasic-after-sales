import type { DashboardWorkOrderStatusVO } from '@/service/api';

/**
 * 首页看板通用数值兜底：将接口返回值转为非负整数。
 */
export function toDashboardCount(value: unknown) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
}

/** 工单状态主状态 → 中文展示 */
export const WORK_ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_ASSIGN: '待派单',
  PENDING_TECH_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
};

/** 工单状态展示顺序（不含 ALL） */
export const WORK_ORDER_STATUS_ORDER = [
  'PENDING_ASSIGN',
  'PENDING_TECH_ACCEPT',
  'IN_PROGRESS',
  'COMPLETED',
  'CLOSED'
] as const;

/**
 * 将首页固定字段工单状态转为图表条目（排除 ALL、仅保留大于 0 的项）。
 */
export function buildStatusChartItems(status?: DashboardWorkOrderStatusVO | null) {
  const map: Record<string, number> = {
    PENDING_ASSIGN: toDashboardCount(status?.pendingAssign),
    PENDING_TECH_ACCEPT: toDashboardCount(status?.pendingTechAccept),
    IN_PROGRESS: toDashboardCount(status?.inProgress),
    COMPLETED: toDashboardCount(status?.completed),
    CLOSED: toDashboardCount(status?.closed)
  };

  return WORK_ORDER_STATUS_ORDER.map(key => ({
    key,
    label: WORK_ORDER_STATUS_LABELS[key] || key,
    value: map[key] || 0
  })).filter(item => item.value > 0);
}

/**
 * 将首页固定字段工单状态转为卡片 countMap（含 ALL）。
 */
export function buildStatusCountMap(status?: DashboardWorkOrderStatusVO | null) {
  return {
    ALL: toDashboardCount(status?.all),
    PENDING_ASSIGN: toDashboardCount(status?.pendingAssign),
    PENDING_TECH_ACCEPT: toDashboardCount(status?.pendingTechAccept),
    IN_PROGRESS: toDashboardCount(status?.inProgress),
    COMPLETED: toDashboardCount(status?.completed),
    CLOSED: toDashboardCount(status?.closed)
  };
}

/**
 * 将 yyyy-MM-dd 格式化为坐标轴短标签 MM-DD。
 */
export function toAxisDayLabel(dayKey: string) {
  return dayKey.length >= 10 ? dayKey.slice(5) : dayKey;
}
