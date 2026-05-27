/**
 * 路由缓存辅助：默认 keepAlive 策略、递归收集缓存路由名、为异步页面组件注入与路由一致的 name。
 * KeepAlive 的 include 按组件 name 匹配，必须与 route.name 一致才能命中缓存。
 * @修改人 黄碧莲
 * @修改时间 2026-05-27
 */
import type { RouteRecordRaw, RouteComponent } from 'vue-router';
import type { LastLevelRouteKey } from '@elegant-router/types';

/** 不参与 keep-alive 的常量路由 name（登录、异常页等） */
const CONSTANT_ROUTE_NAMES = new Set(['login', 'choose-company', '403', '404', '500']);

/**
 * 判断业务路由是否应默认开启 keep-alive（全开策略：仅排除首页与常量页）。
 *
 * @param routeName - 路由 name
 * @param isConstant - 是否为常量路由（meta.constant）
 * @returns 是否应缓存
 */
export function resolveDefaultRouteKeepAlive(routeName: string, isConstant = false): boolean {
  if (isConstant || CONSTANT_ROUTE_NAMES.has(routeName)) {
    return false;
  }
  return routeName !== 'home';
}

/**
 * 根据菜单配置解析动态路由是否开启 keep-alive。
 * 仅页面菜单（C）且后端 keepAlive=1 时缓存；首页与常量路由始终不缓存。
 *
 * @param options.routeName - 路由 name
 * @param options.isConstant - 是否常量路由
 * @param options.menuType - 菜单类型 M/C/F
 * @param options.hasChildren - 是否仍有可访问子路由（目录场景不缓存父级）
 * @param options.backendKeepAlive - 菜单表 keep_alive 归一化结果
 * @returns 是否写入 meta.keepAlive
 */
export function resolveMenuRouteKeepAlive(options: {
  routeName: string;
  isConstant?: boolean;
  menuType?: string;
  hasChildren?: boolean;
  backendKeepAlive?: boolean;
}): boolean {
  const { routeName, isConstant, menuType, hasChildren, backendKeepAlive } = options;
  if (isConstant || CONSTANT_ROUTE_NAMES.has(routeName) || routeName === 'home') {
    return false;
  }
  const type = String(menuType || '').toUpperCase();
  if (type !== 'C' || hasChildren) {
    return false;
  }
  return Boolean(backendKeepAlive);
}

/**
 * 递归收集需进入 KeepAlive include 的路由 name（任意层级叶子路由）。
 *
 * @param routes - 已注册的 Vue 路由树
 * @returns 需缓存的路由 name 列表
 */
export function getCacheRouteNamesFromVueRoutes(routes: RouteRecordRaw[]): LastLevelRouteKey[] {
  const cacheNames: LastLevelRouteKey[] = [];

  function walk(routeList: RouteRecordRaw[]) {
    routeList.forEach(route => {
      if (route.component && route.meta?.keepAlive && route.name) {
        cacheNames.push(route.name as LastLevelRouteKey);
      }
      if (route.children?.length) {
        walk(route.children);
      }
    });
  }

  walk(routes);
  return cacheNames;
}

/**
 * 为懒加载页面组件注入与路由 name 一致的组件名，使 KeepAlive include 能正确命中。
 *
 * @param routeName - 路由 name（与 cacheRoutes 中一致）
 * @param loader - 原始 view 懒加载函数或同步组件
 * @returns 包装后的路由 component
 */
function wrapViewComponentWithRouteName(
  routeName: string,
  loader: RouteComponent | (() => Promise<RouteComponent>)
): RouteComponent | (() => Promise<RouteComponent>) {
  if (typeof loader !== 'function') {
    return loader;
  }

  return () =>
    Promise.resolve((loader as () => Promise<{ default?: RouteComponent }>)()).then(mod => {
      const resolved = (mod && typeof mod === 'object' && 'default' in mod ? mod.default : mod) as RouteComponent;
      if (!resolved || typeof resolved !== 'object') {
        return resolved;
      }
      return {
        ...resolved,
        name: routeName
      };
    });
}

/**
 * 递归为所有带 name 的叶子路由组件注入组件名（布局路由不处理）。
 *
 * @param routes - transform 后的 Vue 路由树
 * @returns 注入后的同引用路由树
 */
export function wrapVueRoutesForKeepAlive(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  function walk(routeList: RouteRecordRaw[]) {
    routeList.forEach(route => {
      if (route.name && route.component) {
        route.component = wrapViewComponentWithRouteName(String(route.name), route.component);
      }
      if (route.children?.length) {
        walk(route.children);
      }
    });
  }

  walk(routes);
  return routes;
}
