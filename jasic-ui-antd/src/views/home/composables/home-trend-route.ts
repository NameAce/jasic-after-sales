import type { HomeRouteTargetVO } from '@/service/api';

/**
 * 首页近七天趋势折线点击跳转口径。
 * 趋势按流水事件发生数统计；转出序列跳转使用 transferDirection=OUT（与首页「已转出」一致）。
 * 与总部、网点看板 KPI 及后端 series.code 对齐。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

// 工单列表路由 name（总部/网点趋势点击统一落点）
const WORK_ORDER_ROUTE = 'after-sales_work-order';

/**
 * 趋势序列 code → 工单列表 query（仅包含列表页已有搜索字段）。
 * TRANSFER_OUT 的 hasTransfer 由 navigateHomeRoute 经 enrichWorkOrderListRouteQuery 补全。
 */
const TREND_SERIES_ROUTE_QUERY: Record<string, Record<string, string>> = {
  /** 网点：近 7 天接单事件 → 当前承接 + 待接单 */
  TECH_ACCEPT: { viewScope: 'CURRENT', mainStatus: 'PENDING_TECH_ACCEPT' },
  /** 完成事件 → 当前承接 + 已完成 */
  REPAIR_FINISH: { viewScope: 'CURRENT', mainStatus: 'COMPLETED' },
  /** 转出事件 → 已转出（transferDirection=OUT） */
  TRANSFER_OUT: { transferDirection: 'OUT' },
  /** 总部：流入事件 → 当前承接（无单独主状态筛选项） */
  FLOW_IN: { viewScope: 'CURRENT' }
};

/**
 * 作用：解析趋势折线点击后的跳转目标；未知 code 时降级为图表右上角「工单列表」等默认入口。
 * @param seriesCode - 后端 HomeTrendSeriesVO.code（如 TECH_ACCEPT、FLOW_IN）
 * @param fallbackRoute - 图表 extra 链接等默认入口，未知 code 时使用
 * @returns 可传给 navigateHomeRoute 的 HomeRouteTargetVO；无法解析时返回 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolveTrendSeriesRouteTarget(
  seriesCode: string | undefined,
  fallbackRoute?: { name: string; query?: Record<string, string> }
): HomeRouteTargetVO | null {
  const code = String(seriesCode || '').trim();
  const mapped = code ? TREND_SERIES_ROUTE_QUERY[code] : undefined;
  if (mapped) {
    return {
      routeName: WORK_ORDER_ROUTE,
      query: { ...mapped }
    };
  }
  // 未配置映射的序列：沿用卡片右上角默认工单列表入口
  if (fallbackRoute?.name) {
    return {
      routeName: fallbackRoute.name,
      query: fallbackRoute.query ? { ...fallbackRoute.query } : undefined
    };
  }
  return null;
}
