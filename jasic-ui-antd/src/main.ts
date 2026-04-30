/**
 * 应用入口：静态资源与插件初始化后创建 Vue 实例，挂载 Pinia、路由再挂到 #app。
 */
import { createApp } from 'vue';
import './plugins/assets';
import {
  setupAppErrorHandle,
  setupAppVersionNotification,
  setupDayjs,
  setupIconifyOffline,
  setupLoading,
  setupNProgress
} from './plugins';
import { setupStore } from './store';
import { setupRouter } from './router';
import App from './App.vue';

/**
 * 初始化并挂载 Vue 应用：加载插件、状态、路由后挂载到 DOM。
 *
 * @returns {Promise<void>} 无返回值
 */
async function setupApp() {
  // 加载插件
  setupLoading();
  // 加载进度条
  setupNProgress();
  // 加载图标
  setupIconifyOffline();
  // 加载日期
  setupDayjs();

  // Vue 根应用实例
  const app = createApp(App);
  // 加载错误处理
  setupAppErrorHandle(app);
  // 加载状态
  setupStore(app);
  // 加载路由
  await setupRouter(app);
  // 加载版本通知
  setupAppVersionNotification();
  // 挂载应用
  app.mount('#app');
}

setupApp();
