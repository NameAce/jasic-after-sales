/**
 * 自定义与静态路由：在 elegant 生成路由上追加业务/异常路由，并导出 `createStaticRoutes` 等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { CustomRoute, ElegantConstRoute, ElegantRoute } from '@elegant-router/types';
import { generatedRoutes } from '../elegant/routes';
import { layouts, views } from '../elegant/imports';
import { transformElegantRoutesToVueRoutes } from '../elegant/transform';
import { resolveDefaultRouteKeepAlive, wrapVueRoutesForKeepAlive } from '../helpers/keep-alive';

/** 追加在生成路由之外的自定义路由（如 exception 分组）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
const customRoutes: CustomRoute[] = [
  {
    name: 'exception',
    path: '/exception',
    component: 'layout.base',
    meta: {
      title: 'exception',
      i18nKey: 'route.exception',
      icon: 'ant-design:exception-outlined',
      order: 7,
      hideInMenu: true
    },
    children: [
      {
        name: 'exception_403',
        path: '/exception/403',
        component: 'view.403',
        meta: {
          title: 'exception_403',
          i18nKey: 'route.exception_403',
          icon: 'ic:baseline-block'
        }
      },
      {
        name: 'exception_404',
        path: '/exception/404',
        component: 'view.404',
        meta: {
          title: 'exception_404',
          i18nKey: 'route.exception_404',
          icon: 'ic:baseline-web-asset-off'
        }
      },
      {
        name: 'exception_500',
        path: '/exception/500',
        component: 'view.500',
        meta: {
          title: 'exception_500',
          i18nKey: 'route.exception_500',
          icon: 'ic:baseline-wifi-off'
        }
      }
    ]
  }
];

/** create routes when the auth route mode is static
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function createStaticRoutes() {
  const constantRoutes: ElegantRoute[] = [];

  const authRoutes: ElegantRoute[] = [];

  [...customRoutes, ...generatedRoutes].forEach(item => {
    const isConstantRoute = Boolean(item.meta?.constant);
    const shouldKeepAlive = resolveDefaultRouteKeepAlive(String(item.name), isConstantRoute);
    const normalizedItem: ElegantRoute = {
      ...item,
      meta: {
        ...(item.meta || {}),
        keepAlive: shouldKeepAlive
      }
    };

    if (isConstantRoute) {
      constantRoutes.push(normalizedItem);
    } else {
      authRoutes.push(normalizedItem);
    }
  });

  // Backend-first mode: keep only system-level constant routes from frontend.
  if (import.meta.env.VITE_AUTH_ROUTE_MODE === 'dynamic') {
    const staticHome = generatedRoutes.find(r => r.name === 'home');
    if (staticHome && !constantRoutes.some(r => r.name === 'home')) {
      constantRoutes.push(staticHome);
    }
    // 个人中心通过右上角头像入口触发，动态路由模式下也需要先注册前端路由。
    const staticUserCenter = generatedRoutes.find(r => r.name === 'user-center');
    if (staticUserCenter && !constantRoutes.some(r => r.name === 'user-center')) {
      constantRoutes.push(staticUserCenter);
    }
    return {
      constantRoutes,
      authRoutes: []
    };
  }

  return {
    constantRoutes,
    authRoutes
  };
}

/**
 * Get auth vue routes
 *
 * @param routes Elegant routes
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getAuthVueRoutes(routes: ElegantConstRoute[]) {
  const vueRoutes = transformElegantRoutesToVueRoutes(routes, layouts, views);
  return wrapVueRoutesForKeepAlive(vueRoutes);
}
