import type { HomeRouteTargetVO } from '@/service/api';

/**
 * 平台超管「基础配置」指标前端跳转口径。
 * 仅在前端覆盖后端 routeTarget，不修改 `/dashboard/platform/home` 接口契约。
 */
export const PLATFORM_BASIC_CONFIG_ROUTE_OVERRIDES: Record<string, HomeRouteTargetVO> = {
  PRODUCT_COUNT: { routeName: 'system_machine-barcode' },
  SERVICE_TYPE_COUNT: { routeName: 'org_contract' },
  DICT_ITEM_COUNT: { routeName: 'system_dict-type' },
  REGION_COUNT: { routeName: 'org_region' }
};

/**
 * 解析指标实际跳转目标：优先使用前端覆盖表，否则沿用接口下发的 routeTarget。
 */
export function resolveMetricRouteTarget(
  metric?: { code?: string; routeTarget?: HomeRouteTargetVO } | null,
  overrides?: Record<string, HomeRouteTargetVO>
): HomeRouteTargetVO | null | undefined {
  if (!metric) return undefined;
  const code = metric.code;
  if (code && overrides?.[code]) {
    return overrides[code];
  }
  return metric.routeTarget;
}
