/**
 * 路由与菜单共享工具：后端菜单与 elegant 路由的归一化、排序、按角色过滤、全局菜单/面包屑/缓存路由名等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { RouteLocationNormalizedLoaded, RouteRecordRaw, _RouteRecordBase } from 'vue-router';
import type { ElegantConstRoute, LastLevelRouteKey, RouteKey, RouteMap } from '@elegant-router/types';
import { MENU_ICON_OVERRIDES, resolveMenuIconFromApi } from '@/constants/menu-icon';
import { useSvgIcon } from '@/hooks/common/icon';
import { $t } from '@/locales';
import { getRoutePath } from '@/router/elegant/transform';
import { getRouteMenuTitle } from '@/utils/route-menu-title';

type RouteMetaLike = Partial<NonNullable<ElegantConstRoute['meta']>> & Record<string, unknown>;
type BackendMenuRoute = Api.Route.BackendMenuRoute;
const useBackendMenuI18n = import.meta.env.VITE_BACKEND_MENU_USE_I18N === 'Y';

function isLegacyBackendRoute(route: ElegantConstRoute | BackendMenuRoute): boolean {
  const routeRecord = route as Record<string, unknown>;
  const rawPath = String(route.path ?? routeRecord.path ?? '').toLowerCase();
  const rawComponent = String(route.component ?? routeRecord.component ?? '').toLowerCase();
  const rawName = String(route.name ?? routeRecord.name ?? routeRecord.routeName ?? '').toLowerCase();
  const menuName = String(routeRecord.menuName ?? '').toLowerCase();

  // Block old manage/systemManage chain from dynamic menu payload.
  return (
    rawPath.includes('/manage') ||
    rawPath.includes('/systemmanage') ||
    rawComponent.includes('systemmanage') ||
    rawName.startsWith('manage') ||
    menuName.includes('manage')
  );
}

function normalizeBooleanMetaField(value: unknown): boolean | undefined {
  if (value === null || value === undefined) return undefined;
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value === 1;
  if (typeof value === 'string') {
    return ['1', 'true', 'y', 'yes'].includes(value.toLowerCase());
  }
  return undefined;
}

function normalizeNumberMetaField(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') return undefined;
  const num = Number(value);
  return Number.isNaN(num) ? undefined : num;
}

function firstNonEmptyIconField(...vals: unknown[]): string {
  for (const v of vals) {
    if (v !== null && v !== undefined) {
      const s = String(v).trim();
      if (s) return s;
    }
  }
  return '';
}

function normalizeRouteMeta(route: ElegantConstRoute | BackendMenuRoute): NonNullable<ElegantConstRoute['meta']> {
  const routeRecord = route as Record<string, unknown>;
  const rawMeta = (route.meta || {}) as RouteMetaLike;
  const isVisible = normalizeBooleanMetaField(routeRecord.isVisible ?? rawMeta.isVisible);
  const hideInMenu = normalizeBooleanMetaField(rawMeta.hideInMenu ?? routeRecord.hideInMenu);
  const order = normalizeNumberMetaField(rawMeta.order ?? routeRecord.order ?? routeRecord.orderNum);
  const icon = firstNonEmptyIconField(rawMeta.icon, routeRecord.icon, routeRecord.menuIcon, routeRecord.svgIcon);
  const localIconFromApi = firstNonEmptyIconField(rawMeta.localIcon, routeRecord.localIcon);
  const i18nKey = (rawMeta.i18nKey || routeRecord.i18nKey) as NonNullable<ElegantConstRoute['meta']>['i18nKey'];

  const meta: NonNullable<ElegantConstRoute['meta']> = {
    title: String(rawMeta.title || routeRecord.title || routeRecord.menuName || route.name),
    ...rawMeta
  };

  meta.i18nKey = i18nKey;
  meta.icon = icon || String(meta.icon || '');
  if (localIconFromApi) {
    meta.localIcon = localIconFromApi;
  }
  const metaIconFont = normalizeNumberMetaField(rawMeta.iconFontSize ?? routeRecord.iconFontSize);
  if (metaIconFont !== undefined) {
    meta.iconFontSize = metaIconFont;
  }
  meta.order = order;
  // SysMenuVO uses isVisible: 0 hidden, 1 visible; keep hideInMenu higher priority when both exist.
  meta.hideInMenu = hideInMenu ?? (isVisible === undefined ? undefined : !isVisible);
  meta.activeMenu = (rawMeta.activeMenu || routeRecord.activeMenu) as NonNullable<
    ElegantConstRoute['meta']
  >['activeMenu'];
  meta.keepAlive = normalizeBooleanMetaField(rawMeta.keepAlive ?? routeRecord.keepAlive);

  return meta;
}

function resolveFallbackOrder(
  explicitOrder: number | null | undefined,
  siblingIndex: number,
  siblingCount: number
): number {
  if (explicitOrder !== undefined && explicitOrder !== null) return explicitOrder;

  // Keep backend sibling sequence stable when order is missing.
  return siblingIndex + 1 + siblingCount * 100;
}

/** Ensure Vue Router absolute path (leading `/`).
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function normalizeLeadingSlash(path: string): string {
  const p = String(path || '').trim();
  if (!p) return '/';
  return p.startsWith('/') ? p : `/${p}`;
}

/**
 * Join parent absolute path with backend segment (may be absolute or relative).
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function resolveBackendAbsolutePath(parentAbs: string | null, segment: string): string {
  const seg = String(segment || '').trim();
  if (!seg) return parentAbs ? normalizeLeadingSlash(parentAbs) : '/';
  if (seg.startsWith('/')) return normalizeLeadingSlash(seg);
  if (!parentAbs) return normalizeLeadingSlash(seg);
  return `${normalizeLeadingSlash(parentAbs).replace(/\/$/, '')}/${seg}`;
}

/**
 * Build elegant-router route name from absolute path (underscore between segments, kebab per segment).
 * Example: `/system/user` -> `system_user`, `/log/operLog` -> `log_oper-log`
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function pathToElegantRouteName(fullPath: string): string {
  const segments = normalizeLeadingSlash(fullPath)
    .replace(/^\//, '')
    .split('/')
    .filter(s => s && !s.startsWith(':'));
  return segments.map(seg => seg.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()).join('_');
}

/**
 * Convert jasic-ui style component path to elegant `view` key (`system/user/index` -> `system_user`).
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function backendFilePathToViewKey(filePath: string): string {
  let s = filePath.trim();
  if (!s) return '';
  s = s.replace(/^@\//, '').replace(/^\.\//, '');
  s = s
    .replace(/^src\/views\//i, '')
    .replace(/^views\//i, '')
    .replace(/^\/+/, '');
  s = s.replace(/\.vue$/i, '');
  const parts = s.split('/').filter(Boolean);
  if (parts.length && parts[parts.length - 1].toLowerCase() === 'index') parts.pop();
  if (!parts.length) return '';
  return parts.map(p => p.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()).join('_');
}

function isBackendLayoutPlaceholder(raw: unknown): boolean {
  if (raw === null || raw === undefined) return true;
  const s = String(raw).trim();
  if (!s) return true;
  const lower = s.toLowerCase();
  return (
    lower === 'layout' || lower === 'parentview' || lower.includes('parentview') || lower.includes('layout/parent')
  );
}

/**
 * Backend pages not yet split into separate Vue files: map generated view key -> existing registered view key.
 * Keeps dynamic mode from crashing when menus reference more pages than the PC bundle registers.
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
/**
 * 后端路径里的驼峰段会变成 kebab（如 roleTemplate -> role-template），
 * 路由名形如 system_role-template；别名必须用最终 routeName / viewKey，否则会匹配失败。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
const BACKEND_VIEW_KEY_ALIASES: Record<string, string> = {
  'log_oper-log': 'log',
  'org_company-type': 'org',
  org_company: 'org',
  org_contract: 'org',
  org_region: 'org',
  system_config: 'advanced-modules',
  'system_role-template': 'advanced-modules',
  'system_dict-type': 'advanced-modules',
  'system_dict-data': 'advanced-modules',
  'system_machine-barcode': 'advanced-modules',
  'system_fault-repair-config': 'advanced-modules',
  'system_notify-template': 'advanced-modules',
  'system_sync-task': 'advanced-modules',
  system_region: 'advanced-modules'
};

function resolveRegisteredViewKey(viewKey: string): string {
  return BACKEND_VIEW_KEY_ALIASES[viewKey] || viewKey;
}

function resolveElegantComponentString(params: {
  rawComponent: unknown;
  hasChildren: boolean;
  isTreeRoot: boolean;
  routeName: string;
}): string {
  if (params.hasChildren) {
    return 'layout.base';
  }

  const rawStr = typeof params.rawComponent === 'string' ? params.rawComponent.trim() : '';

  if (rawStr.includes('$') && rawStr.startsWith('layout.')) {
    return rawStr;
  }
  if (rawStr.startsWith('layout.') || rawStr.startsWith('view.')) {
    if (rawStr.startsWith('view.')) {
      if (params.isTreeRoot) {
        return `layout.base$${rawStr}`;
      }
      return rawStr;
    }
    return rawStr;
  }

  if (isBackendLayoutPlaceholder(params.rawComponent)) {
    const vk = resolveRegisteredViewKey(params.routeName);
    const viewSpec = `view.${vk}`;
    return params.isTreeRoot ? `layout.base$${viewSpec}` : viewSpec;
  }

  const fromPath = backendFilePathToViewKey(rawStr);
  const viewKey = resolveRegisteredViewKey(fromPath || params.routeName);
  const viewSpec = `view.${viewKey}`;
  return params.isTreeRoot ? `layout.base$${viewSpec}` : viewSpec;
}

/**
 * Normalize backend auth routes so menu-related fields are always read from `route.meta`.
 *
 * Backend may return these fields either at route root or in `meta`.
 * - Resolves relative `path` against parent (e.g. `user` under `/system` -> `/system/user`).
 * - Maps jasic `component` like `system/user/index` to elegant `view.system_user` / `layout.base`.
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function normalizeAuthRoutesFromBackend(
  routes: Array<ElegantConstRoute | BackendMenuRoute>,
  parentAbsPath: string | null = null
): ElegantConstRoute[] {
  const sanitizedRoutes = routes.filter(route => !isLegacyBackendRoute(route));
  const siblingCount = sanitizedRoutes.length;
  const isTreeRoot = parentAbsPath === null;

  return sanitizedRoutes.map((route, siblingIndex) => {
    const routeRecord = route as Record<string, unknown>;
    const normalizedMeta = normalizeRouteMeta(route);
    const rawChildren = Array.isArray(route.children)
      ? route.children
          .filter(child => !isLegacyBackendRoute(child as BackendMenuRoute))
          .filter(child => String((child as Record<string, unknown>).menuType || '').toUpperCase() !== 'F')
      : [];
    const absolutePath = resolveBackendAbsolutePath(parentAbsPath, String(route.path ?? routeRecord.path ?? ''));
    let routeName = pathToElegantRouteName(absolutePath);
    if (!routeName) {
      routeName =
        String(route.name || routeRecord.routeName || routeRecord.menuName || routeRecord.name || 'menu')
          .replace(/\s+/g, '-')
          .replace(/[^a-zA-Z0-9_-]/g, '') || 'menu';
    }

    const normalizedChildren = normalizeAuthRoutesFromBackend(rawChildren as BackendMenuRoute[], absolutePath);
    const hasChildren = normalizedChildren.length > 0;

    const elegantComponent = resolveElegantComponentString({
      rawComponent: route.component ?? routeRecord.component,
      hasChildren,
      isTreeRoot,
      routeName
    }) as ElegantConstRoute['component'];

    const normalized: ElegantConstRoute = {
      ...route,
      name: routeName,
      path: absolutePath,
      component: elegantComponent,
      meta: {
        ...normalizedMeta,
        order: resolveFallbackOrder(normalizedMeta.order, siblingIndex, siblingCount),
        hideInMenu: normalizedMeta.hideInMenu ?? false
      },
      children: undefined
    };

    if (hasChildren) {
      normalized.children = normalizedChildren;
    }

    return normalized;
  });
}

function findRouteInTreeByName(name: string, routes: ElegantConstRoute[]): ElegantConstRoute | null {
  for (const route of routes) {
    if (route.name === name) return route;
    if (route.children?.length) {
      const found = findRouteInTreeByName(name, route.children);
      if (found) return found;
    }
  }
  return null;
}

function getFirstLeafRoute(routes: ElegantConstRoute[]): ElegantConstRoute | null {
  for (const route of routes) {
    if (route.children?.length) {
      const leaf = getFirstLeafRoute(route.children);
      if (leaf) return leaf;
    } else {
      return route;
    }
  }
  return null;
}

/**
 * 动态路由下后端菜单的 route name 往往不在前端的 routeMap 中，且可能与 VITE_ROUTE_HOME（默认 home）不一致。
 * 解析实际存在的首页 name，避免根路径仍重定向到未注册的 /home 而进入 not-found。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function resolveDynamicHomeRouteKey(
  preferred: LastLevelRouteKey,
  routes: ElegantConstRoute[]
): LastLevelRouteKey {
  if (!routes.length) return preferred;
  if (findRouteInTreeByName(preferred, routes)) return preferred;
  const leaf = getFirstLeafRoute(routes);
  return (leaf?.name as LastLevelRouteKey) ?? preferred;
}

/** 根路由 redirect 使用菜单树中的绝对 path，避免仅依赖 getRoutePath（静态表）导致 404
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function resolveRootRedirectPath(
  redirectKey: LastLevelRouteKey,
  routes: ElegantConstRoute[]
): string | undefined {
  if (routes.length) {
    const hit = findRouteInTreeByName(redirectKey, routes);
    if (hit?.path) return hit.path;
    const leaf = getFirstLeafRoute(routes);
    if (leaf?.path) return leaf.path;
  }
  return getRoutePath(redirectKey);
}

/**
 * Filter auth routes by roles
 *
 * @param routes Auth routes
 * @param roles Roles
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function filterAuthRoutesByRoles(routes: ElegantConstRoute[], roles: string[]) {
  return routes.flatMap(route => filterAuthRouteByRoles(route, roles));
}

/**
 * Filter auth route by roles
 *
 * @param route Auth route
 * @param roles Roles
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function filterAuthRouteByRoles(route: ElegantConstRoute, roles: string[]): ElegantConstRoute[] {
  const routeRoles = (route.meta && route.meta.roles) || [];

  // if the route's "roles" is empty, then it is allowed to access
  const isEmptyRoles = !routeRoles.length;

  // if the user's role is included in the route's "roles", then it is allowed to access
  const hasPermission = routeRoles.some(role => roles.includes(role));

  const filterRoute = { ...route };

  if (filterRoute.children?.length) {
    filterRoute.children = filterRoute.children.flatMap(item => filterAuthRouteByRoles(item, roles));
  }

  // Exclude the route if it has no children after filtering
  if (filterRoute.children?.length === 0) {
    return [];
  }

  return hasPermission || isEmptyRoles ? [filterRoute] : [];
}

/**
 * sort route by order
 *
 * @param route route
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function sortRouteByOrder(route: ElegantConstRoute) {
  if (route.children?.length) {
    route.children.sort((next, prev) => (Number(next.meta?.order) || 0) - (Number(prev.meta?.order) || 0));
    route.children.forEach(sortRouteByOrder);
  }

  return route;
}

/**
 * sort routes by order
 *
 * @param routes routes
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function sortRoutesByOrder(routes: ElegantConstRoute[]) {
  routes.sort((next, prev) => (Number(next.meta?.order) || 0) - (Number(prev.meta?.order) || 0));
  routes.forEach(sortRouteByOrder);

  return routes;
}

/**
 * Get global menus by auth routes
 *
 * @param routes Auth routes
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getGlobalMenusByAuthRoutes(routes: ElegantConstRoute[]) {
  const menus: App.Global.Menu[] = [];

  routes.forEach(route => {
    if (!route.meta?.hideInMenu) {
      const menu = getGlobalMenuByBaseRoute(route);

      if (route.children?.some(child => !child.meta?.hideInMenu)) {
        menu.children = getGlobalMenusByAuthRoutes(route.children);
      }

      menus.push(menu);
    }
  });

  return menus;
}

/**
 * Update locale of global menus
 *
 * @param menus
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function updateLocaleOfGlobalMenus(menus: App.Global.Menu[]) {
  const result: App.Global.Menu[] = [];

  menus.forEach(menu => {
    const { i18nKey, label, children } = menu;

    const newLabel = i18nKey && useBackendMenuI18n ? $t(i18nKey) : label;

    const newMenu: App.Global.Menu = {
      ...menu,
      label: newLabel,
      title: newLabel
    };

    if (children?.length) {
      newMenu.children = updateLocaleOfGlobalMenus(children);
    }

    result.push(newMenu);
  });

  return result;
}

/**
 * Get global menu by route
 *
 * @param route
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
// eslint-disable-next-line complexity
function getGlobalMenuByBaseRoute(route: RouteLocationNormalizedLoaded | ElegantConstRoute) {
  const { SvgIconVNode } = useSvgIcon();

  const { name, path } = route;
  const { i18nKey, icon: metaIcon, localIcon: metaLocalIcon, iconFontSize: metaIconFontSize } = route.meta ?? {};
  const override = name ? MENU_ICON_OVERRIDES[String(name)] : undefined;

  const localIcon = override?.localIcon ?? metaLocalIcon;
  const iconFontSize = override?.iconFontSize ?? metaIconFontSize ?? 20;
  const metaIconStr = metaIcon === undefined || metaIcon === '' ? undefined : String(metaIcon).trim() || undefined;
  const fromApiIcon = metaIconStr ? resolveMenuIconFromApi(metaIconStr) : undefined;
  const fallbackIcon = localIcon ? undefined : import.meta.env.VITE_MENU_ICON;
  const icon = override?.icon ?? fromApiIcon ?? fallbackIcon;

  const label = getRouteMenuTitle(route as RouteLocationNormalizedLoaded);

  const menu: App.Global.Menu = {
    key: name as string,
    label,
    i18nKey,
    routeKey: name as RouteKey,
    routePath: path as RouteMap[RouteKey],
    icon: SvgIconVNode({ icon, localIcon, fontSize: iconFontSize }),
    class: name === 'home' ? 'menu-item-home' : 'menu-item-jasic',
    title: label
  };

  return menu;
}

/**
 * Get cache route names
 *
 * @param routes Vue routes (two levels)
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getCacheRouteNames(routes: RouteRecordRaw[]) {
  const cacheNames: LastLevelRouteKey[] = [];

  routes.forEach(route => {
    // only get last two level route, which has component
    route.children?.forEach(child => {
      if (child.component && child.meta?.keepAlive) {
        cacheNames.push(child.name as LastLevelRouteKey);
      }
    });
  });

  return cacheNames;
}

/**
 * Is route exist by route name
 *
 * @param routeName
 * @param routes
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function isRouteExistByRouteName(routeName: RouteKey, routes: ElegantConstRoute[]) {
  return routes.some(route => recursiveGetIsRouteExistByRouteName(route, routeName));
}

/**
 * Recursive get is route exist by route name
 *
 * @param route
 * @param routeName
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function recursiveGetIsRouteExistByRouteName(route: ElegantConstRoute, routeName: RouteKey) {
  let isExist = route.name === routeName;

  if (isExist) {
    return true;
  }

  if (route.children && route.children.length) {
    isExist = route.children.some(item => recursiveGetIsRouteExistByRouteName(item, routeName));
  }

  return isExist;
}

/**
 * Get selected menu key path
 *
 * @param selectedKey
 * @param menus
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getSelectedMenuKeyPathByKey(selectedKey: string, menus: App.Global.Menu[]) {
  const keyPath: string[] = [];

  menus.some(menu => {
    const path = findMenuPath(selectedKey, menu);

    const find = Boolean(path?.length);

    if (find) {
      keyPath.push(...path!);
    }

    return find;
  });

  return keyPath;
}

/**
 * Find menu path
 *
 * @param targetKey Target menu key
 * @param menu Menu
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function findMenuPath(targetKey: string, menu: App.Global.Menu): string[] | null {
  const path: string[] = [];

  function dfs(item: App.Global.Menu): boolean {
    path.push(item.key);

    if (item.key === targetKey) {
      return true;
    }

    if (item.children) {
      for (const child of item.children) {
        if (dfs(child)) {
          return true;
        }
      }
    }

    path.pop();

    return false;
  }

  if (dfs(menu)) {
    return path;
  }

  return null;
}

/**
 * Get breadcrumbs by route
 *
 * @param route
 * @param menus
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getBreadcrumbsByRoute(
  route: RouteLocationNormalizedLoaded,
  menus: App.Global.Menu[]
): App.Global.Menu[] {
  const key = route.name as string;
  const activeKey = route.meta?.activeMenu;

  const menuKey = activeKey || key;

  for (const menu of menus) {
    if (menu.key === menuKey) {
      return [menu];
    }

    if (menu.key === activeKey) {
      const ROUTE_DEGREE_SPLITTER = '_';

      const parentKey = key.split(ROUTE_DEGREE_SPLITTER).slice(0, -1).join(ROUTE_DEGREE_SPLITTER);

      const breadcrumbMenu = getGlobalMenuByBaseRoute(route);

      if (parentKey !== activeKey) {
        return [breadcrumbMenu];
      }

      return [menu, breadcrumbMenu];
    }

    if (menu.children?.length) {
      const result = getBreadcrumbsByRoute(route, menu.children);
      if (result.length > 0) {
        return [menu, ...result];
      }
    }
  }

  return [];
}

/**
 * Transform menu to searchMenus
 *
 * @param menus - menus
 * @param treeMap
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function transformMenuToSearchMenus(menus: App.Global.Menu[], treeMap: App.Global.Menu[] = []) {
  if (menus && menus.length === 0) return [];
  return menus.reduce((acc, cur) => {
    if (!cur.children) {
      acc.push(cur);
    }
    if (cur.children && cur.children.length > 0) {
      transformMenuToSearchMenus(cur.children, treeMap);
    }
    return acc;
  }, treeMap);
}
