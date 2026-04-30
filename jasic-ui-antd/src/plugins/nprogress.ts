/**
 * 路由切换顶部进度条：配置 NProgress 并挂到 window 供守卫调用。
 */
import NProgress from 'nprogress';

/**
 * 作用：初始化顶部路由进度条 NProgress，并挂到 window 供路由守卫调用。
 * @returns {void}
 */
export function setupNProgress() {
  NProgress.configure({ easing: 'ease', speed: 500 });

  // 挂到 window，供 router guard 中 start/done
  window.NProgress = NProgress;
}
