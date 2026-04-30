/**
 * Pinia 根安装：创建实例、注册 `$reset` 插件并挂到 Vue 应用。
 */
import type { App } from 'vue';
import { createPinia } from 'pinia';
import { resetSetupStore } from './plugins';

/**
 * 注册 Pinia 状态管理，并挂载到 Vue 应用。
 *
 * @param app - Vue 应用实例
 * @returns {void} 无返回值
 */
export function setupStore(app: App) {
  // Pinia 根实例
  const store = createPinia();

  store.use(resetSetupStore);

  app.use(store);
}
