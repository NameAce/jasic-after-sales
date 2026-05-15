/**
 * 多页签共享逻辑：tab id、首页/固定排序、与路由 meta 同步的标题与图标更新等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { Router } from 'vue-router';
import type { LastLevelRouteKey, RouteKey, RouteMap } from '@elegant-router/types';
import { $t } from '@/locales';
import { getRoutePath } from '@/router/elegant/transform';

/**
 * 作用：合并首页、固定页签与普通页签，并统一刷新展示文案。
 * @param tabs 当前页签列表
 * @param homeTab 首页页签
 * @returns {App.Global.Tab[]} 排序后的完整列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getAllTabs(tabs: App.Global.Tab[], homeTab?: App.Global.Tab) {
  if (!homeTab) {
    return [];
  }

  const filterHomeTabs = tabs.filter(tab => tab.id !== homeTab.id);

  const fixedTabs = filterHomeTabs.filter(isFixedTab).sort((a, b) => a.fixedIndex! - b.fixedIndex!);

  const remainTabs = filterHomeTabs.filter(tab => !isFixedTab(tab));

  const allTabs = [homeTab, ...fixedTabs, ...remainTabs];

  return updateTabsLabel(allTabs);
}

/**
 * 作用：判断页签是否带固定顺序索引。
 * @param tab 页签
 * @returns {boolean}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function isFixedTab(tab: App.Global.Tab) {
  return tab.fixedIndex !== undefined && tab.fixedIndex !== null;
}

/**
 * 作用：由路由生成页签唯一 id（支持 multiTab 时附排序后的 query）。
 * @param route 当前 tab 路由快照
 * @returns {string} 页签 id
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getTabIdByRoute(route: App.Global.TabRoute) {
  const { path, query = {}, meta } = route;

  let id = path;

  if (meta.multiTab) {
    const queryKeys = Object.keys(query).sort();
    const qs = queryKeys.map(key => `${key}=${query[key]}`).join('&');

    id = `${path}?${qs}`;
  }

  return id;
}

/**
 * 作用：从路由 meta 构建完整 Tab 结构（含图标、固定序、国际化标题）。
 * @param route 路由对象
 * @returns {App.Global.Tab}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getTabByRoute(route: App.Global.TabRoute) {
  const { name, path, fullPath = path, meta } = route;
  const { title, i18nKey, fixedIndexInTab } = meta;

  // Get icon and localIcon from getRouteIcons function
  const { icon, localIcon } = getRouteIcons(route);

  const label = i18nKey ? $t(i18nKey) : title;

  const tab: App.Global.Tab = {
    id: getTabIdByRoute(route),
    label,
    routeKey: name as LastLevelRouteKey,
    routePath: path as RouteMap[LastLevelRouteKey],
    fullPath,
    fixedIndex: fixedIndexInTab,
    icon,
    localIcon,
    i18nKey
  };

  return tab;
}

/**
 * 作用：解析页签图标；多段 matched 时优先取与当前 name 一致记录上的 meta，避免合并 meta 污染。
 * @param route 路由对象
 * @returns {{ icon: string; localIcon?: string }} 图标配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getRouteIcons(route: App.Global.TabRoute) {
  // Set default value for icon at the beginning
  let icon: string = route?.meta?.icon || import.meta.env.VITE_MENU_ICON;
  let localIcon: string | undefined = route?.meta?.localIcon;

  // Route.matched only appears when there are multiple matches,so check if route.matched exists
  if (route.matched) {
    // Find the meta of the current route from matched
    const currentRoute = route.matched.find(r => r.name === route.name);
    // If icon exists in currentRoute.meta, it will overwrite the default value
    icon = currentRoute?.meta?.icon || icon;
    localIcon = currentRoute?.meta?.localIcon;
  }

  return { icon, localIcon };
}

/**
 * 作用：解析默认首页页签（优先用路由表中带 meta 的 home 路由）。
 * @param router Router 实例
 * @param homeRouteName 首页路由名
 * @returns {App.Global.Tab}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getDefaultHomeTab(router: Router, homeRouteName: LastLevelRouteKey) {
  const homeRoutePath = getRoutePath(homeRouteName);
  const i18nLabel = $t(`route.${homeRouteName}`);

  let homeTab: App.Global.Tab = {
    id: getRoutePath(homeRouteName),
    label: i18nLabel || homeRouteName,
    routeKey: homeRouteName,
    routePath: homeRoutePath,
    fullPath: homeRoutePath
  };

  const routes = router.getRoutes();
  const homeRoute = routes.find(route => route.name === homeRouteName);
  if (homeRoute) {
    homeTab = getTabByRoute(homeRoute);
  }

  return homeTab;
}

/**
 * 作用：判断列表中是否包含指定 id 的页签。
 * @param tabId 页签 id
 * @param tabs 页签列表
 * @returns {boolean}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function isTabInTabs(tabId: string, tabs: App.Global.Tab[]) {
  return tabs.some(tab => tab.id === tabId);
}

/**
 * 作用：按 id 排除单个页签。
 * @param tabId 要移除的 id
 * @param tabs 原列表
 * @returns {App.Global.Tab[]} 新列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function filterTabsById(tabId: string, tabs: App.Global.Tab[]) {
  return tabs.filter(tab => tab.id !== tabId);
}

/**
 * 作用：批量按 id 排除页签。
 * @param tabIds id 列表
 * @param tabs 原列表
 * @returns {App.Global.Tab[]}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function filterTabsByIds(tabIds: string[], tabs: App.Global.Tab[]) {
  return tabs.filter(tab => !tabIds.includes(tab.id));
}

/**
 * 作用：过滤掉路由表中已不存在的页签（清理失效路由）。
 * @param router Router
 * @param tabs 当前页签
 * @returns {App.Global.Tab[]}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function extractTabsByAllRoutes(router: Router, tabs: App.Global.Tab[]) {
  const routes = router.getRoutes();

  const routeNames = routes.map(route => route.name);

  return tabs.filter(tab => routeNames.includes(tab.routeKey));
}

/**
 * 作用：取出所有带 fixedIndex 的页签。
 * @param tabs 页签列表
 * @returns {App.Global.Tab[]}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getFixedTabs(tabs: App.Global.Tab[]) {
  return tabs.filter(tab => tab.fixedIndex !== undefined);
}

/**
 * 作用：固定页签的 id 列表。
 * @param tabs 页签列表
 * @returns {string[]}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getFixedTabIds(tabs: App.Global.Tab[]) {
  const fixedTabs = getFixedTabs(tabs);

  return fixedTabs.map(tab => tab.id);
}

/**
 * 作用：将 newLabel/oldLabel 合并进最终展示 label。
 * @param tabs 页签列表
 * @returns {App.Global.Tab[]}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function updateTabsLabel(tabs: App.Global.Tab[]) {
  const updated = tabs.map(tab => ({
    ...tab,
    label: tab.newLabel || tab.oldLabel || tab.label
  }));

  return updated;
}

/**
 * 作用：按 i18nKey 重新解析单个页签标题。
 * @param tab 页签
 * @returns {App.Global.Tab}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function updateTabByI18nKey(tab: App.Global.Tab) {
  const { i18nKey, label } = tab;

  return {
    ...tab,
    label: i18nKey ? $t(i18nKey) : label
  };
}

/**
 * 作用：批量更新页签的 i18n 标题。
 * @param tabs 页签列表
 * @returns {App.Global.Tab[]}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function updateTabsByI18nKey(tabs: App.Global.Tab[]) {
  return tabs.map(tab => updateTabByI18nKey(tab));
}

/**
 * 作用：按路由 name 查找已打开的页签（兼容带 query 的 multiTab id）。
 * @param name 路由 name
 * @param tabs 页签列表
 * @returns {App.Global.Tab | undefined}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function findTabByRouteName(name: RouteKey, tabs: App.Global.Tab[]) {
  const routePath = getRoutePath(name);

  const tabId = routePath;
  const multiTabId = `${routePath}?`;

  return tabs.find(tab => tab.id === tabId || tab.id.startsWith(multiTabId));
}
