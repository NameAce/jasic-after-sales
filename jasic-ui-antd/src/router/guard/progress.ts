/**
 * 导航进度条守卫：beforeEach 启动、afterEach 结束 NProgress。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { Router } from 'vue-router';

/**
 * 作用：在进入/离开路由时启动与结束 NProgress 顶部进度条。
 * @param router Vue Router 实例
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function createProgressGuard(router: Router) {
  router.beforeEach((_to, _from, next) => {
    window.NProgress?.start?.();
    next();
  });
  router.afterEach(_to => {
    window.NProgress?.done?.();
  });
}
