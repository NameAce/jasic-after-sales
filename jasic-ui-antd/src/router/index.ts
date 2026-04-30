/**
 * 路由入口：按环境变量选择 history 模式，创建仅含内置路由的 router，再由守卫与 store 注入动态路由。
 */
import type { App } from 'vue';
import {
  type RouterHistory,
  createMemoryHistory,
  createRouter,
  createWebHashHistory,
  createWebHistory
} from 'vue-router';
import { createBuiltinVueRoutes } from './routes/builtin';
import { createRouterGuard } from './guard';

const { VITE_ROUTER_HISTORY_MODE = 'history', VITE_BASE_URL } = import.meta.env;

// 环境变量到 Vue Router history 工厂函数的映射
const historyCreatorMap: Record<Env.RouterHistoryMode, (base?: string) => RouterHistory> = {
  hash: createWebHashHistory,
  history: createWebHistory,
  memory: createMemoryHistory
};

/** 全局 Vue Router 单例（初始仅内置路由，权限路由由 store 动态注入） */
export const router = createRouter({
  history: historyCreatorMap[VITE_ROUTER_HISTORY_MODE](VITE_BASE_URL),
  routes: createBuiltinVueRoutes()
});

/**
 * 将 router 挂载到应用并注册全局导航守卫，等待首次导航就绪。
 *
 * @param app - Vue 应用实例
 * @returns {Promise<void>} 无返回值
 */
export async function setupRouter(app: App) {
  app.use(router);
  createRouterGuard(router);
  await router.isReady();
}
