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
 * 作用：将接口返回值转为非负整数，供 CountTo、图表等展示兜底。
 * @param value - 接口原始指标值（可能为字符串、null）
 * @returns 大于 0 的整数，否则为 0
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：将首页固定字段工单状态转为柱状/条形图条目（排除 ALL、仅保留大于 0 的项）。
 * @param status - 接口 DashboardWorkOrderStatusVO
 * @returns 含 key、label、value 的图表数据项数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：将首页固定字段工单状态转为 KPI 卡片 countMap（含 ALL）。
 * @param status - 接口 DashboardWorkOrderStatusVO
 * @returns 各主状态及 ALL 的数量映射
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：将 yyyy-MM-dd 格式化为坐标轴短标签 MM-DD。
 * @param dayKey - 趋势 days 数组元素
 * @returns 坐标轴展示文案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function toAxisDayLabel(dayKey: string) {
  return dayKey.length >= 10 ? dayKey.slice(5) : dayKey;
}

/**
 * 作用：将 trend.series[].values 与 trend.days 按索引对齐（后端保证等长；前端按 days 长度逐日取值）。
 * @param days - 趋势日期轴
 * @param values - 单条序列的日计数值
 * @returns 与 days 等长的非负整数数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function alignTrendValuesToDays(days: string[], values?: number[]) {
  return days.map((_, index) => toDashboardCount(values?.[index]));
}

/**
 * 总部/网点首页顶部 KPI 不展示的状态指标 code（仅前端过滤，不改后端接口）。
 * 饼图等图表仍使用未过滤的承接池数据，保留待接单、已完成、已关闭分布。
 */
export const HOME_KPI_HIDDEN_METRIC_CODES = new Set(['PENDING_TECH_ACCEPT', 'COMPLETED', 'CLOSED']);

/**
 * 作用：过滤承接池指标，隐藏待接单、已完成、已关闭，供顶部 KPI 卡片使用。
 * @param section - 接口下发的 HomeSectionVO（当前承接池等）
 * @returns 过滤后的分区副本；section 为空时返回 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function filterHomeKpiPoolMetrics(section: HomeSectionVO | null): HomeSectionVO | null {
  if (!section) return null;
  const metrics = (section.metrics ?? []).filter(item => !HOME_KPI_HIDDEN_METRIC_CODES.has(item.code || ''));
  return { ...section, metrics };
}

/**
 * 作用：承接池状态分布饼图图例，按固定顺序补齐五种主状态（含 0 值占位，避免图例缺项）。
 * @param section - 承接池 HomeSectionVO
 * @param colorByCode - 按指标 code 指定扇区/图例颜色
 * @param fallbackColors - 无 colorByCode 时按顺序取色的色板
 * @returns 图例项列表（含 routeTarget，供点击跳转）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function buildWorkOrderPoolPieLegendItems(
  section: HomeSectionVO | null | undefined,
  colorByCode?: Record<string, string>,
  fallbackColors: readonly string[] = []
): WorkOrderPoolPieLegendItem[] {
  // 按 code 建索引，便于固定顺序遍历时尚未返回的状态也能取到标题与跳转
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
