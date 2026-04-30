/**
 * 动态路由与菜单：拉取后端菜单、生成可访问路由、面包屑、缓存路由名及常量路由初始化。
 */
import { computed, nextTick, ref, shallowRef } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { defineStore } from 'pinia';
import { useBoolean } from '@sa/hooks';
import type { CustomRoute, ElegantConstRoute, LastLevelRouteKey, RouteKey, RouteMap } from '@elegant-router/types';
import { router } from '@/router';
import { fetchGetMenus } from '@/service/api';
import { createStaticRoutes, getAuthVueRoutes } from '@/router/routes';
import { ROOT_ROUTE } from '@/router/routes/builtin';
import { getRouteName } from '@/router/elegant/transform';
import { SetupStoreId } from '@/enum';
import { useAuthStore } from '../auth';
import { useTabStore } from '../tab';
import {
  filterAuthRoutesByRoles,
  getBreadcrumbsByRoute,
  getCacheRouteNames,
  getGlobalMenusByAuthRoutes,
  getSelectedMenuKeyPathByKey,
  isRouteExistByRouteName,
  normalizeAuthRoutesFromBackend,
  resolveDynamicHomeRouteKey,
  resolveRootRedirectPath,
  sortRoutesByOrder,
  transformMenuToSearchMenus,
  updateLocaleOfGlobalMenus
} from './shared';

/** 路由表、菜单、面包屑与缓存策略的 Pinia store */
export const useRouteStore = defineStore(SetupStoreId.Route, () => {
  const authStore = useAuthStore();
  const tabStore = useTabStore();
  const { bool: isInitConstantRoute, setBool: setIsInitConstantRoute } = useBoolean();
  const { bool: isInitAuthRoute, setBool: setIsInitAuthRoute } = useBoolean();

  // 权限路由来源：static 使用 elegant 生成；dynamic 使用后端菜单
  const authRouteMode = ref(import.meta.env.VITE_AUTH_ROUTE_MODE);

  // 登录后默认首页的路由 name
  const routeHome = ref(import.meta.env.VITE_ROUTE_HOME);

  /**
   * 设置首页路由 key（动态模式下可能被后端 home 覆盖）。
   *
   * @param routeKey - 末级路由 key
   * @returns {void} 无返回值
   */
  function setRouteHome(routeKey: LastLevelRouteKey) {
    routeHome.value = routeKey;
  }

  // 无需登录即可访问的常量路由（登录页、404 等）
  const constantRoutes = shallowRef<ElegantConstRoute[]>([]);

  /**
   * 合并写入常量路由表（按 name 去重）。
   *
   * @param routes - 常量路由数组
   * @returns {void} 无返回值
   */
  function addConstantRoutes(routes: ElegantConstRoute[]) {
    const constantRoutesMap = new Map<string, ElegantConstRoute>([]);

    routes.forEach(route => {
      constantRoutesMap.set(route.name, route);
    });

    constantRoutes.value = Array.from(constantRoutesMap.values());
  }

  // 需权限的业务路由表
  const authRoutes = shallowRef<ElegantConstRoute[]>([]);

  /**
   * 合并写入权限路由表（按 name 去重）。
   *
   * @param routes - 权限路由数组
   * @returns {void} 无返回值
   */
  function addAuthRoutes(routes: ElegantConstRoute[]) {
    const authRoutesMap = new Map<string, ElegantConstRoute>([]);

    routes.forEach(route => {
      authRoutesMap.set(route.name, route);
    });

    authRoutes.value = Array.from(authRoutesMap.values());
  }

  // 动态 addRoute 时记录的移除函数，重置 store 时调用
  const removeRouteFns: (() => void)[] = [];

  // 侧栏/顶栏使用的菜单树
  const menus = ref<App.Global.Menu[]>([]);
  // 扁平化后的菜单，用于全局搜索
  const searchMenus = computed(() => transformMenuToSearchMenus(menus.value));

  /**
   * 由权限路由生成全局菜单数据。
   *
   * @param routes - 已排序的权限路由
   * @returns {void} 无返回值
   */
  function getGlobalMenus(routes: ElegantConstRoute[]) {
    menus.value = getGlobalMenusByAuthRoutes(routes);
  }

  /**
   * 语言切换后刷新菜单上的 i18n 文案。
   *
   * @returns {void} 无返回值
   */
  function updateGlobalMenusByLocale() {
    menus.value = updateLocaleOfGlobalMenus(menus.value);
  }

  // 需要 keep-alive 缓存的路由 name 列表
  const cacheRoutes = ref<RouteKey[]>([]);

  // 临时排除缓存的路由 name，用于强制刷新某一页
  const excludeCacheRoutes = ref<RouteKey[]>([]);

  /**
   * 根据 Vue Router 记录计算需缓存的页面 name。
   *
   * @param routes - 已注册的 Vue 路由表
   * @returns {void} 无返回值
   */
  function getCacheRoutes(routes: RouteRecordRaw[]) {
    cacheRoutes.value = getCacheRouteNames(routes);
  }

  /**
   * 通过短暂加入 exclude 再清空，使指定路由的 keep-alive 实例销毁重建。
   *
   * @param routeKey - 要刷新的路由 name，默认当前路由
   * @returns {Promise<void>} 无返回值
   */
  async function resetRouteCache(routeKey?: RouteKey) {
    const routeName = routeKey || (router.currentRoute.value.name as RouteKey);

    excludeCacheRoutes.value.push(routeName);

    await nextTick();

    excludeCacheRoutes.value = [];
  }

  // 当前路由对应的面包屑链
  const breadcrumbs = computed(() => getBreadcrumbsByRoute(router.currentRoute.value, menus.value));

  /**
   * 清空路由 store、移除动态路由并重新初始化常量路由。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function resetStore() {
    const routeStore = useRouteStore();

    routeStore.$reset();

    resetVueRoutes();

    // after reset store, need to re-init constant route
    await initConstantRoute();
  }

  /**
   * 调用此前记录的 remove 函数，从 Vue Router 卸载动态添加的路由。
   *
   * @returns {void} 无返回值
   */
  function resetVueRoutes() {
    removeRouteFns.forEach(fn => fn());
    removeRouteFns.length = 0;
  }

  /**
   * 首次注册常量路由、合并菜单与缓存配置，并初始化首页标签。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function initConstantRoute() {
    if (isInitConstantRoute.value) return;

    const staticRoute = createStaticRoutes();

    addConstantRoutes(staticRoute.constantRoutes);

    handleConstantAndAuthRoutes();

    setIsInitConstantRoute(true);

    tabStore.initHomeTab();
  }

  /**
   * 在用户已登录前提下初始化权限路由（静态过滤或动态拉菜单）。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function initAuthRoute() {
    // check if user info is initialized
    if (!authStore.userInfo.userId && !authStore.needChooseCompany) {
      await authStore.initUserInfo();
    }

    if (authStore.needChooseCompany) {
      return;
    }

    if (authRouteMode.value === 'static') {
      initStaticAuthRoute();
    } else {
      await initDynamicAuthRoute();
    }

    tabStore.initHomeTab();
  }

  /**
   * 静态模式：从 elegant 生成路由并按角色过滤后加入 Router。
   *
   * @returns {void} 无返回值
   */
  function initStaticAuthRoute() {
    const { authRoutes: staticAuthRoutes } = createStaticRoutes();

    if (authStore.isStaticSuper) {
      addAuthRoutes(staticAuthRoutes);
    } else {
      // static 模式与 guard 一致：使用 roleKey（authStore.roleKeys）+ meta.roles 过滤
      const filteredAuthRoutes = filterAuthRoutesByRoles(staticAuthRoutes, authStore.roleKeys);

      addAuthRoutes(filteredAuthRoutes);
    }

    handleConstantAndAuthRoutes();

    setIsInitAuthRoute(true);
  }

  /**
   * 动态模式：请求后端菜单、规范化后注册并解析首页与根重定向。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function initDynamicAuthRoute() {
    // dynamic 模式主判定依赖后端下发路由；guard 中 meta.roles 仅兜底
    const { data, error } = await fetchGetMenus();

    if (error) {
      // dynamic 拉取失败时主动清理，避免保留半初始化状态
      await authStore.resetStore();
      return;
    }

    const backendRoutes = Array.isArray(data) ? data : data?.routes;
    const backendHome = Array.isArray(data) ? undefined : data?.home;
    const normalizedRoutes = normalizeAuthRoutesFromBackend(backendRoutes || []);

    addAuthRoutes(normalizedRoutes);

    handleConstantAndAuthRoutes();

    const routesForHome = [...constantRoutes.value, ...normalizedRoutes];
    const preferredHome = (backendHome || routeHome.value) as LastLevelRouteKey;
    const home = resolveDynamicHomeRouteKey(preferredHome, routesForHome);
    setRouteHome(home);
    handleUpdateRootRouteRedirect(home, routesForHome);

    setIsInitAuthRoute(true);
  }

  /**
   * 合并常量与权限路由、排序、注册到 Vue Router，并同步菜单与 keep-alive 列表。
   *
   * @returns {void} 无返回值
   */
  function handleConstantAndAuthRoutes() {
    const allRoutes = [...constantRoutes.value, ...authRoutes.value];

    const sortRoutes = sortRoutesByOrder(allRoutes);

    const vueRoutes = getAuthVueRoutes(sortRoutes);

    resetVueRoutes();

    addRoutesToVueRouter(vueRoutes);

    getGlobalMenus(sortRoutes);

    getCacheRoutes(vueRoutes);
  }

  /**
   * 将一组 Vue Router 记录动态挂载到全局 router，并登记移除函数。
   *
   * @param routes - Vue 路由配置数组
   * @returns {void} 无返回值
   */
  function addRoutesToVueRouter(routes: RouteRecordRaw[]) {
    routes.forEach(route => {
      const removeFn = router.addRoute(route);
      addRemoveRouteFn(removeFn);
    });
  }

  /**
   * 记录 addRoute 返回的移除函数，供 reset 时批量卸载。
   *
   * @param fn - router.addRoute 返回的卸载函数
   * @returns {void} 无返回值
   */
  function addRemoveRouteFn(fn: () => void) {
    removeRouteFns.push(fn);
  }

  /**
   * 动态模式下根据解析出的首页 key 更新根路由的 redirect。
   *
   * @param redirectKey - 作为首页的路由 key
   * @param lookupRoutes - 用于解析重定向路径的完整路由表
   * @returns {void} 无返回值
   */
  function handleUpdateRootRouteRedirect(redirectKey: LastLevelRouteKey, lookupRoutes: ElegantConstRoute[]) {
    const redirect = resolveRootRedirectPath(redirectKey, lookupRoutes);

    if (redirect) {
      const rootRoute: CustomRoute = { ...ROOT_ROUTE, redirect };

      router.removeRoute(rootRoute.name);

      const [rootVueRoute] = getAuthVueRoutes([rootRoute]);

      router.addRoute(rootVueRoute);
    }
  }

  /**
   * 判断给定 path 是否在静态定义或已下发的权限路由中存在。
   *
   * @param routePath - 路由完整 path
   * @returns {Promise<boolean>} 是否存在对应 name 的路由
   */
  async function getIsAuthRouteExist(routePath: RouteMap[RouteKey]) {
    const routeName = getRouteName(routePath);

    if (!routeName) {
      return false;
    }

    if (authRouteMode.value === 'static') {
      const { authRoutes: staticAuthRoutes } = createStaticRoutes();
      return isRouteExistByRouteName(routeName, staticAuthRoutes);
    }

    // 动态路由：以后端已下发的常量路由 + 权限路由为准，不请求不存在的 /route/isRouteExist
    const merged = [...constantRoutes.value, ...authRoutes.value];
    return isRouteExistByRouteName(routeName, merged);
  }

  /**
   * 根据当前选中的菜单 key 计算展开的父级 key 路径。
   *
   * @param selectedKey - 当前选中菜单项 id
   * @returns {string[]} 父级到当前项的 key 路径
   */
  function getSelectedMenuKeyPath(selectedKey: string) {
    return getSelectedMenuKeyPathByKey(selectedKey, menus.value);
  }

  /**
   * 已登录用户切换路由时：若无 userId 则尝试补拉用户信息。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function onRouteSwitchWhenLoggedIn() {
    if (!authStore.userInfo.userId && !authStore.needChooseCompany) {
      await authStore.initUserInfo();
    }
  }

  /**
   * 未登录用户切换路由时的占位钩子（可扩展全局初始化）。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function onRouteSwitchWhenNotLoggedIn() {
    // some global init logic if it does not need to be logged in
  }

  return {
    resetStore,
    routeHome,
    menus,
    searchMenus,
    updateGlobalMenusByLocale,
    cacheRoutes,
    excludeCacheRoutes,
    resetRouteCache,
    breadcrumbs,
    initConstantRoute,
    isInitConstantRoute,
    initAuthRoute,
    isInitAuthRoute,
    setIsInitAuthRoute,
    getIsAuthRouteExist,
    getSelectedMenuKeyPath,
    onRouteSwitchWhenLoggedIn,
    onRouteSwitchWhenNotLoggedIn
  };
});
