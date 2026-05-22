import type { HomeRouteTargetVO } from '@/service/api';

/**
 * 平台超管「基础配置」指标前端跳转口径。
 * 仅在前端覆盖后端 routeTarget，不修改 `/dashboard/platform/home` 接口契约。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const PLATFORM_BASIC_CONFIG_ROUTE_OVERRIDES: Record<string, HomeRouteTargetVO> = {
  PRODUCT_COUNT: { routeName: 'system_machine-barcode' },
  SERVICE_TYPE_COUNT: { routeName: 'org_contract' },
  DICT_ITEM_COUNT: { routeName: 'system_dict-type' },
  REGION_COUNT: { routeName: 'org_region' }
};

/**
 * 作用：解析指标实际跳转目标；平台基础配置等场景优先前端覆盖表，否则沿用接口 routeTarget。
 * @param metric - 首页指标项（含 code、routeTarget）
 * @param overrides - 按指标 code 覆盖的跳转表（如 PLATFORM_BASIC_CONFIG_ROUTE_OVERRIDES）
 * @returns 解析后的跳转目标；metric 为空时返回 undefined
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolveMetricRouteTarget(
  metric?: { code?: string; routeTarget?: HomeRouteTargetVO } | null,
  overrides?: Record<string, HomeRouteTargetVO>
): HomeRouteTargetVO | null | undefined {
  if (!metric) return undefined;
  const code = metric.code;
  // 前端覆盖优先：后端未配置或需固定落点的指标（基础配置四项）
  if (code && overrides?.[code]) {
    return overrides[code];
  }
  return metric.routeTarget;
}
