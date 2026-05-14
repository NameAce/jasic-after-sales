/**
 * 核心路由守卫：常量/动态路由初始化、登录与选公司流程、角色与外链处理。
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

/**
 * 作用：注册全局前置守卫：初始化常量/鉴权路由、登录态与公司选择流程、meta.roles 与外链等。
 * @param router Vue Router 实例
 * @returns {void}
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
    const chooseCompanyRoute: RouteKey = 'choose-company';
    const isLogin = authStore.isLogin;
    const needLogin = !to.meta.constant;
    const routeRoles = to.meta.roles || [];
    const authRouteMode = import.meta.env.VITE_AUTH_ROUTE_MODE;
    const useMetaRolesFallback = authRouteMode === 'static' || !routeStore.isInitAuthRoute;
    const hasAuthByMetaRoles = !routeRoles.length || authStore.hasAnyRole(routeRoles);
    // dynamic 下主判定依赖后端已下发路由；meta.roles 仅在静态模式或未初始化阶段兜底
    const hasAuth = authStore.isStaticSuper || !useMetaRolesFallback || hasAuthByMetaRoles;

    // 与 jasic-ui permission.js：已登录访问登录页时跳转；若仍待选公司则进选公司页
    if (to.name === loginRoute && isLogin) {
      if (authStore.needChooseCompany) {
        next({ name: chooseCompanyRoute });
      } else {
        next({ name: rootRoute });
      }
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

    if (authStore.needChooseCompany && to.name !== chooseCompanyRoute) {
      next({ name: chooseCompanyRoute });
      return;
    }

    if (!authStore.needChooseCompany && to.name === chooseCompanyRoute) {
      next({ name: rootRoute });
      return;
    }

    if (authStore.needChooseCompany && to.name === chooseCompanyRoute) {
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
    // if the user is not logged in and the route is a constant route but not the "not-found" route, then it is allowed to access.
    if (to.meta.constant && !isNotFoundRoute) {
      routeStore.onRouteSwitchWhenNotLoggedIn();

      return null;
    }

    // if the user is not logged in, then switch to the login page
    const loginRoute: RouteKey = 'login';
    const query = getRouteQueryOfLoginRoute(to, routeStore.routeHome);

    const location: RouteLocationRaw = {
      name: loginRoute,
      query
    };

    return location;
  }

  if (!routeStore.isInitAuthRoute) {
    // 与 jasic-ui 登录后时序对齐：先拿用户信息，再判断是否待选公司与初始化鉴权路由
    if (!authStore.userInfo.userId && !authStore.needChooseCompany) {
      await authStore.initUserInfo();
    }

    // 与 jasic-ui：选公司完成前不拉菜单；待选公司阶段仅允许停留在 choose-company
    if (authStore.needChooseCompany) {
      if (to.name === chooseCompanyRoute) {
        return null;
      }
      return { name: chooseCompanyRoute };
    }

    if (to.name === chooseCompanyRoute && !authStore.needChooseCompany) {
      return { name: 'root' };
    }

    // 与 jasic-ui：选公司完成后再初始化鉴权路由
    if (to.name === chooseCompanyRoute) {
      return null;
    }

    // initialize the auth route
    await routeStore.initAuthRoute();

    // the route is captured by the "not-found" route because the auth route is not initialized
    // after the auth route is initialized, redirect to the original route
    if (isNotFoundRoute) {
      const rootRoute: RouteKey = 'root';
      const path = to.redirectedFrom?.name === rootRoute ? '/' : to.fullPath;

      const location: RouteLocationRaw = {
        path,
        replace: true,
        query: to.query,
        hash: to.hash
      };

      return location;
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

