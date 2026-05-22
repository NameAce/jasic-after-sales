import { createSSRApp } from 'vue'
import pinia from './stores'

import App from './App.vue'

/**
 * 作用：提交/变更：createApp。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function createApp() {
  const app = createSSRApp(App)

  app.use(pinia)
  return {
    app,
    pinia,
  }
}
