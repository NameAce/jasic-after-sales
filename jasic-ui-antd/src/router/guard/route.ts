/**
 * 核心路由守卫：常量/动态路由初始化、登录与选公司流程、角色与外链处理。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type {
  LocationQueryRaw,
  NavigationGuardNext,
  RouteLocationNormalized,
  RouteLocationRaw,
  Router
} from 'vue-router';
import type { RouteKey, RoutePath } from '@elegant-router/types';
import { useAuthStore } from '@/store/modules/auth';
import { useRouteStore } from '@/store/modules/route';
import { localStg } from '@/utils/storage';
import { getRouteName } from '@/router/elegant/transform';
import { $t } from '@/locales';

type AuthStore = ReturnType<typeof useAuthStore>;
type RouteStore = ReturnType<typeof useRouteStore>;

/**
 * 作用：已登录用户访问登录页时，重定向到选公司或首页。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getLoggedInLoginRedirect(to: RouteLocationNormalized, authStore: AuthStore): RouteLocationRaw | null {
  const loginRoute: RouteKey = 'login';
  const rootRoute: RouteKey = 'root';
  const chooseCompanyRoute: RouteKey = 'choose-company';
  if (to.name !== loginRoute || !authStore.isLogin) {
    return null;
  }
  if (authStore.needChooseCompany) {
    return { name: chooseCompanyRoute };
  }
  return { name: rootRoute };
}

/**
 * 作用：选公司流程中的路由拦截（待选公司 / 已选完误入选公司页）。
 * @returns 需重定向的目标；`proceed-switch` 表示在选公司页放行并由 handleRouteSwitch 继续
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getChooseCompanyGuardResult(
  to: RouteLocationNormalized,
  authStore: AuthStore
): RouteLocationRaw | 'proceed-switch' | null {
  const chooseCompanyRoute: RouteKey = 'choose-company';
  const rootRoute: RouteKey = 'root';
  if (authStore.needChooseCompany && to.name !== chooseCompanyRoute) {
    return { name: chooseCompanyRoute };
  }
  if (!authStore.needChooseCompany && to.name === chooseCompanyRoute) {
    return { name: rootRoute };
  }
  if (authStore.needChooseCompany && to.name === chooseCompanyRoute) {
    return 'proceed-switch';
  }
  return null;
}

/**
 * 作用：未登录时根据目标路由决定放行或跳转登录。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveLocationWhenNotLoggedIn(
  to: RouteLocationNormalized,
  routeStore: RouteStore,
  isNotFoundRoute: boolean
): RouteLocationRaw | null {
  if (to.meta.constant && !isNotFoundRoute) {
    routeStore.onRouteSwitchWhenNotLoggedIn();
    return null;
  }
  return {
    name: 'login',
    query: getRouteQueryOfLoginRoute(to, routeStore.routeHome)
  };
}

/**
 * 作用：鉴权路由未初始化时拉用户信息、处理选公司与 initAuthRoute，必要时重定向。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function resolveLocationWhenAuthRouteUninitialized(ctx: {
  to: RouteLocationNormalized;
  authStore: AuthStore;
  routeStore: RouteStore;
  isNotFoundRoute: boolean;
}): Promise<RouteLocationRaw | null> {
  const { to, authStore, routeStore, isNotFoundRoute } = ctx;
  const chooseCompanyRoute: RouteKey = 'choose-company';
  if (!authStore.userInfo.userId && !authStore.needChooseCompany) {
    await authStore.initUserInfo();
  }
  if (authStore.needChooseCompany) {
    if (to.name === chooseCompanyRoute) {
      return null;
    }
    return { name: chooseCompanyRoute };
  }
  if (to.name === chooseCompanyRoute && !authStore.needChooseCompany) {
    return { name: 'root' };
  }
  if (to.name === chooseCompanyRoute) {
    return null;
  }
  await routeStore.initAuthRoute();
  if (isNotFoundRoute) {
    const rootRoute: RouteKey = 'root';
    const path = to.redirectedFrom?.name === rootRoute ? '/' : to.fullPath;
    return {
      path,
      replace: true,
      query: to.query,
      hash: to.hash
    };
  }
  return null;
}

/**
 * 作用：注册全局前置守卫：初始化常量/鉴权路由、登录态与公司选择流程、meta.roles 与外链等。
 * @param router Vue Router 实例
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function createRouteGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const location = await initRoute(to);

    if (location) {
      next(location);
      return;
    }

    const authStore = useAuthStore();
    const routeStore = useRouteStore();

    const rootRoute: RouteKey = 'root';
    const loginRoute: RouteKey = 'login';
    const isLogin = authStore.isLogin;
    const needLogin = !to.meta.constant;
    const routeRoles = to.meta.roles || [];
    const authRouteMode = import.meta.env.VITE_AUTH_ROUTE_MODE;
    const useMetaRolesFallback = authRouteMode === 'static' || !routeStore.isInitAuthRoute;
    const hasAuthByMetaRoles = !routeRoles.length || authStore.hasAnyRole(routeRoles);
    // dynamic 下主判定依赖后端已下发路由；meta.roles 仅在静态模式或未初始化阶段兜底
    const hasAuth = authStore.isStaticSuper || !useMetaRolesFallback || hasAuthByMetaRoles;

    const loggedInLoginRedirect = getLoggedInLoginRedirect(to, authStore);
    if (loggedInLoginRedirect) {
      next(loggedInLoginRedirect);
      return;
    }

    // if the route does not need login, then it is allowed to access directly
    if (!needLogin) {
      handleRouteSwitch(to, from, next);
      return;
    }

    // the route need login but the user is not logged in, then switch to the login page
    if (!isLogin) {
      next({ name: loginRoute, query: { redirect: to.fullPath } });
      return;
    }

    const chooseCompanyGuard = getChooseCompanyGuardResult(to, authStore);
    if (chooseCompanyGuard && chooseCompanyGuard !== 'proceed-switch') {
      next(chooseCompanyGuard);
      return;
    }
    if (chooseCompanyGuard === 'proceed-switch') {
      handleRouteSwitch(to, from, next);
      return;
    }

    // 路由级无权限：提示后回首页，不进入 403 异常页（403 页仅用于接口返回的无权限等场景）
    if (!hasAuth) {
      window.$message?.warning($t('route.403'));
      if (to.name !== rootRoute) {
        next({ name: rootRoute, replace: true });
      } else {
        next(false);
      }
      return;
    }

    // switch route normally
    handleRouteSwitch(to, from, next);
  });
}

/**
 * 作用：在登录后首次或常量路由未就绪时补全路由，必要时重定向登录或选公司页。
 * @param to 目标路由
 * @returns {Promise<RouteLocationRaw | null>} 需要重定向时返回目标，否则 null 表示继续当前导航
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
async function initRoute(to: RouteLocationNormalized): Promise<RouteLocationRaw | null> {
  const routeStore = useRouteStore();
  const authStore = useAuthStore();

  const notFoundRoute: RouteKey = 'not-found';
  const isNotFoundRoute = to.name === notFoundRoute;
  const chooseCompanyRoute: RouteKey = 'choose-company';

  // if the constant route is not initialized, then initialize the constant route
  if (!routeStore.isInitConstantRoute) {
    await routeStore.initConstantRoute();

    // the route is captured by the "not-found" route because the constant route is not initialized
    // after the constant route is initialized, redirect to the original route
    const path = to.fullPath;
    const location: RouteLocationRaw = {
      path,
      replace: true,
      query: to.query,
      hash: to.hash
    };

    return location;
  }

  const isLogin = Boolean(localStg.get('token'));

  if (!isLogin) {
    return resolveLocationWhenNotLoggedIn(to, routeStore, isNotFoundRoute);
  }

  if (!routeStore.isInitAuthRoute) {
    const authBootstrapLocation = await resolveLocationWhenAuthRouteUninitialized({
      to,
      authStore,
      routeStore,
      isNotFoundRoute
    });
    if (authBootstrapLocation) {
      return authBootstrapLocation;
    }
  }

  if (!(to.name === chooseCompanyRoute && authStore.needChooseCompany)) {
    routeStore.onRouteSwitchWhenLoggedIn();
  }

  // the auth route is initialized
  // it is not the "not-found" route, then it is allowed to access
  if (!isNotFoundRoute) {
    return null;
  }

  // it is captured by the "not-found" route, then check whether the route exists
  const exist = await routeStore.getIsAuthRouteExist(to.path as RoutePath);

  if (exist) {
    window.$message?.warning($t('route.403'));
    return { name: 'root', replace: true };
  }

  return null;
}

/**
 * 作用：处理带外链 meta.href 的跳转，并在新窗口打开后用原路径 replace 当前历史。
 * @param to 目标路由
 * @param from 来源路由
 * @param next 导航 next
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function handleRouteSwitch(to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) {
  // route with href
  if (to.meta.href) {
    window.open(to.meta.href, '_blank');

    next({ path: from.fullPath, replace: true, query: from.query, hash: to.hash });

    return;
  }

  next();
}

/**
 * 作用：构造跳转登录页时的 query（redirect、首页带 query 的特殊拼接）。
 * @param to 当前目标路由
 * @param routeHome 应用首页路由 name
 * @returns {LocationQueryRaw} 登录页 query
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function getRouteQueryOfLoginRoute(to: RouteLocationNormalized, routeHome: RouteKey) {
  const loginRoute: RouteKey = 'login';
  const redirect = to.fullPath;
  const [redirectPath, redirectQuery] = redirect.split('?');
  const redirectName = getRouteName(redirectPath as RoutePath);

  const isRedirectHome = routeHome === redirectName;

  const query: LocationQueryRaw = to.name !== loginRoute && !isRedirectHome ? { redirect } : {};

  if (isRedirectHome && redirectQuery) {
    query.redirect = `/?${redirectQuery}`;
  }

  return query;
}
