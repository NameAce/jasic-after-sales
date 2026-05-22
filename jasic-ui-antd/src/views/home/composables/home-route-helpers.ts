import type { Router } from 'vue-router';
import type { HomeRouteTargetVO } from '@/service/api';

/**
 * 将首页接口返回的 query 转为 Vue Router 可识别的字符串查询参数。
 */
export function toRouteQuery(query?: Record<string, unknown> | null): Record<string, string> {
  if (!query) return {};
  const result: Record<string, string> = {};
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null) {
      result[key] = String(value);
    }
  }
  return result;
}

/** 路由 name 无法注册时的 path 兜底（与 elegant transform 保持一致） */
const ROUTE_NAME_PATH_FALLBACK: Record<string, string> = {
  org: '/org',
  org_contract: '/org/contract',
  org_region: '/org/region',
  system_user: '/system/user',
  system_role: '/system/role',
  'system_dict-type': '/system/dictType',
  'system_machine-barcode': '/system/machineBarcode',
  'advanced-modules': '/advanced-modules',
  'after-sales_work-order': '/work-order'
};

/**
 * 工单列表跳转 query 补全：接口「已转出」仅下发 transferDirection=OUT，前端补 hasTransfer=1 供搜索区回显。
 */
export function enrichWorkOrderListRouteQuery(
  routeName: string,
  query: Record<string, string>
): Record<string, string> {
  if (routeName !== 'after-sales_work-order') return query;
  if (String(query.transferDirection || '').toUpperCase() !== 'OUT') return query;
  return { ...query, hasTransfer: '1' };
}

/**
 * 按 routeTarget 执行首页卡片/入口跳转（可为接口下发或前端覆盖后的目标）。
 */
export function navigateHomeRoute(router: Router, target?: HomeRouteTargetVO | null) {
  if (!target?.routeName) return;

  const routeName = target.routeName;
  const query = enrichWorkOrderListRouteQuery(routeName, toRouteQuery(target.query));

  if (router.hasRoute(routeName)) {
    router.push({ name: routeName, query });
    return;
  }

  const path = ROUTE_NAME_PATH_FALLBACK[routeName];
  if (path) {
    router.push({ path, query });
  }
}
