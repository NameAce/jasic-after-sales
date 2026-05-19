/**
 * 路由跳转封装：按 RouteKey、登录模块、首页等统一 `push`/`replace` 与 query 拼接。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { useRouter } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';
import type { RouteKey, RoutePath } from '@elegant-router/types';
import { router as globalRouter } from '@/router';
import { getRouteName, getRoutePath } from '@/router/elegant/transform';

/**
 * 登录页在 elegant-router 中会被拆成「父 path 带 :module + 子 name=login」的嵌套路由，
 * 通过 `name: login` + `params.module` 跳转时 Vue Router 无法解析，需改用 path。
 *
 * @param module - 登录子模块
 * @returns 可匹配的登录 path
 * @修改人 黄碧莲
 * @修改时间 2026-05-19
 */
function buildLoginPath(module: UnionKey.LoginModule) {
  return module === 'pwd-login' ? '/login' : `/login/${module}`;
}

/**
 * 构造登录页 redirect query，规则与 `router/guard/route.ts` 中 `getRouteQueryOfLoginRoute` 保持一致。
 *
 * @param redirect - 回跳地址（一般为当前 fullPath）
 * @returns 登录页 query；无需 redirect 时返回空对象
 * @修改人 黄碧莲
 * @修改时间 2026-05-19
 */
function buildLoginRedirectQuery(redirect: string): Record<string, string> {
  if (!redirect || redirect.startsWith('/login')) {
    return {};
  }

  const routeHome = (import.meta.env.VITE_ROUTE_HOME || 'home') as RouteKey;
  const [redirectPath, redirectQuery] = redirect.split('?');
  const redirectName = getRouteName(redirectPath as RoutePath);
  const isRedirectHome = routeHome === redirectName || redirectPath === '/' || redirectPath === getRoutePath(routeHome);

  if (!isRedirectHome) {
    return { redirect };
  }

  if (redirectQuery) {
    return { redirect: `/?${redirectQuery}` };
  }

  return {};
}

/**
 * 作用：封装常用路由跳转（按 name、登录模块、首页等），可选择在 setup 内或外使用全局 router。
 * @param inSetup 是否在 `setup()` 中调用（决定用 `useRouter()` 还是全局实例）
 * @returns 路由操作方法集合
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useRouterPush(inSetup = true) {
  const router = inSetup ? useRouter() : globalRouter;
  const route = globalRouter.currentRoute;

  const routerPush = router.push;

  const routerBack = router.back;

  interface RouterPushOptions {
    query?: Record<string, string>;
    params?: Record<string, string>;
  }

  async function routerPushByKey(key: RouteKey, options?: RouterPushOptions) {
    const { query, params } = options || {};

    const routeLocation: RouteLocationRaw = {
      name: key
    };

    if (Object.keys(query || {}).length) {
      routeLocation.query = query;
    }

    if (Object.keys(params || {}).length) {
      routeLocation.params = params;
    }

    return routerPush(routeLocation);
  }

  function routerPushByKeyWithMetaQuery(key: RouteKey) {
    const allRoutes = router.getRoutes();
    const meta = allRoutes.find(item => item.name === key)?.meta || null;

    const query: Record<string, string> = {};

    meta?.query?.forEach(item => {
      query[item.key] = item.value;
    });

    return routerPushByKey(key, { query });
  }

  async function toHome() {
    return routerPushByKey('root');
  }

  /**
   * 作用：跳转登录页并带上当前页 redirect。
   * @param loginModule 登录子模块（如 pwd-login）
   * @param redirectUrl 自定义回跳地址，默认当前 fullPath
   * @returns {Promise} `router.push` 结果
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function toLogin(loginModule?: UnionKey.LoginModule, redirectUrl?: string) {
    const module = loginModule || 'pwd-login';
    const redirect = redirectUrl || route.value.fullPath;
    const query = buildLoginRedirectQuery(redirect);

    return routerPush({ path: buildLoginPath(module), query }).catch(() => {});
  }

  /**
   * 作用：留在登录路由仅切换 path 中的 module 段，并保留现有 query（如 redirect）。
   * @param module 登录模块名
   * @returns {Promise}
   * @修改人 黄碧莲
   * @修改时间 2026-05-19
   */
  async function toggleLoginModule(module: UnionKey.LoginModule) {
    const query = route.value.query as Record<string, string>;

    return routerPush({ path: buildLoginPath(module), query }).catch(() => {});
  }

  /**
   * 作用：登录成功后按 query.redirect 回跳或回首页。
   * @param needRedirect 是否执行 redirect，默认 true
   * @returns {Promise}
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function redirectFromLogin(needRedirect = true) {
    const redirect = route.value.query?.redirect as string;

    if (needRedirect && redirect) {
      await routerPush(redirect);
    } else {
      await toHome();
    }
  }

  return {
    routerPush,
    routerBack,
    routerPushByKey,
    routerPushByKeyWithMetaQuery,
    toHome,
    toLogin,
    toggleLoginModule,
    redirectFromLogin
  };
}
