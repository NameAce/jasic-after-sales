import { createSSRApp } from 'vue'
import pinia from './stores'
import { setupRouteGuard } from './utils/routeGuard'

import App from './App.vue'

setupRouteGuard()

export function createApp() {
  const app = createSSRApp(App)
  app.use(pinia)
  return {
    app,
    pinia,
  }
}
