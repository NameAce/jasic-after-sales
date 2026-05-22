import type { DashboardWorkOrderStatusVO, HomeMetricVO, HomeSectionVO } from '@/service/api';

/** 承接池饼图图例项（固定五种主状态，含 0 值占位） */
export interface WorkOrderPoolPieLegendItem {
  code: string;
  name: string;
  value: number;
  color: string;
  routeTarget?: HomeMetricVO['routeTarget'];
}

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

/**
 * 将 trend.series[].values 与 trend.days 按索引对齐（后端保证等长；前端按 days 长度逐日取值）。
 */
export function alignTrendValuesToDays(days: string[], values?: number[]) {
  return days.map((_, index) => toDashboardCount(values?.[index]));
}

/**
 * 总部/网点首页顶部 KPI 不展示的状态指标 code（仅前端过滤，不改后端接口）。
 * 饼图等图表仍使用未过滤的承接池数据，保留待接单、已关闭分布。
 */
export const HOME_KPI_HIDDEN_METRIC_CODES = new Set(['PENDING_TECH_ACCEPT', 'CLOSED']);

/**
 * 过滤承接池指标：隐藏待接单、已关闭，供顶部 KPI 卡片使用。
 */
export function filterHomeKpiPoolMetrics(section: HomeSectionVO | null): HomeSectionVO | null {
  if (!section) return null;
  const metrics = (section.metrics ?? []).filter(item => !HOME_KPI_HIDDEN_METRIC_CODES.has(item.code || ''));
  return { ...section, metrics };
}

/**
 * 承接池状态分布饼图图例：按固定顺序补齐五种主状态。
 * 接口未返回或数量为 0 的「待接单」等仍占位，避免图例缺项。
 */
export function buildWorkOrderPoolPieLegendItems(
  section: HomeSectionVO | null | undefined,
  colorByCode?: Record<string, string>,
  fallbackColors: readonly string[] = []
): WorkOrderPoolPieLegendItem[] {
  const metricByCode = new Map((section?.metrics ?? []).map(item => [item.code || '', item]));

  return WORK_ORDER_STATUS_ORDER.map((code, index) => {
    const metric = metricByCode.get(code);
    const color = colorByCode?.[code] || fallbackColors[index % (fallbackColors.length || 1)] || '#8c8c8c';
    return {
      code,
      name: metric?.title || WORK_ORDER_STATUS_LABELS[code] || code,
      value: toDashboardCount(metric?.value),
      color,
      routeTarget: metric?.routeTarget
    };
  });
}
