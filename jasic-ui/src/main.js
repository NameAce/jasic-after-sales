import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import './router/permission'
import hasPerms from './directive/permission/hasPerms'

Vue.use(ElementUI, { size: 'medium' })
Vue.directive('hasPerms', hasPerms)

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
