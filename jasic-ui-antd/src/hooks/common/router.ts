/**
 * 路由跳转封装：按 RouteKey、登录模块、首页等统一 `push`/`replace` 与 query 拼接。
 */
import { useRouter } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';
import type { RouteKey } from '@elegant-router/types';
import { router as globalRouter } from '@/router';

/**
 * 作用：封装常用路由跳转（按 name、登录模块、首页等），可选择在 setup 内或外使用全局 router。
 * @param inSetup 是否在 `setup()` 中调用（决定用 `useRouter()` 还是全局实例）
 * @returns 路由操作方法集合
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
   */
  async function toLogin(loginModule?: UnionKey.LoginModule, redirectUrl?: string) {
    const module = loginModule || 'pwd-login';

    const options: RouterPushOptions = {
      params: {
        module
      }
    };

    const redirect = redirectUrl || route.value.fullPath;

    options.query = {
      redirect
    };

    return routerPushByKey('login', options);
  }

  /**
   * 作用：留在登录路由仅切换 query 中的 module 参数。
   * @param module 登录模块名
   * @returns {Promise}
   */
  async function toggleLoginModule(module: UnionKey.LoginModule) {
    const query = route.value.query as Record<string, string>;

    return routerPushByKey('login', { query, params: { module } });
  }

  /**
   * 作用：登录成功后按 query.redirect 回跳或回首页。
   * @param needRedirect 是否执行 redirect，默认 true
   * @returns {Promise}
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
