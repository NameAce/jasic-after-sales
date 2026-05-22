import type { Router } from 'vue-router';
import type { HomeRouteTargetVO } from '@/service/api';

/**
 * 作用：将首页接口下发的 query 转为 Vue Router 可识别的字符串查询参数（过滤 undefined/null）。
 * @param query - 接口 HomeRouteTargetVO.query，键值可为任意类型
 * @returns 仅含字符串值的 query 对象，供 router.push 使用
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：工单列表跳转 query 补全；接口「已转出」仅下发 transferDirection=OUT 时，前端补 hasTransfer=1 供搜索区回显。
 * @param routeName - 目标路由 name
 * @param query - 经 toRouteQuery 转换后的查询参数
 * @returns 补全后的 query；非工单列表或非 OUT 方向时原样返回
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：按首页卡片/图表下发的 routeTarget 执行业务页跳转（支持接口下发或前端覆盖后的目标）。
 * @param router - 当前 Vue Router 实例
 * @param target - 跳转目标（routeName + query）；无 routeName 时不跳转
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function navigateHomeRoute(router: Router, target?: HomeRouteTargetVO | null) {
  if (!target?.routeName) return;

  const routeName = target.routeName;
  // 工单列表「已转出」需补 hasTransfer，否则列表搜索区无法回显转出筛选
  const query = enrichWorkOrderListRouteQuery(routeName, toRouteQuery(target.query));

  // 优先按路由 name 跳转（与动态路由注册一致）
  if (router.hasRoute(routeName)) {
    router.push({ name: routeName, query });
    return;
  }

  // name 未注册时走 path 兜底，避免首页指标点击无响应
  const path = ROUTE_NAME_PATH_FALLBACK[routeName];
  if (path) {
    router.push({ path, query });
  }
}
