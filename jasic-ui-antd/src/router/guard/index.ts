/**
 * 路由守卫聚合入口：进度条、业务路由守卫、文档标题。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { Router } from 'vue-router';
import { createRouteGuard } from './route';
import { createProgressGuard } from './progress';
import { createDocumentTitleGuard } from './title';

/**
 * 作用：注册路由守卫：进度条、鉴权与重定向、文档标题。
 * @param router Vue Router 实例
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function createRouterGuard(router: Router) {
  createProgressGuard(router);
  createRouteGuard(router);
  createDocumentTitleGuard(router);
}
