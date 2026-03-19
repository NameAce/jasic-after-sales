import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import permission from './modules/permission'

Vue.use(Vuex)

const getters = {
  token: state => state.user.token,
  userInfo: state => state.user.userInfo,
  perms: state => state.user.perms,
  companies: state => state.user.companies,
  currentCompanyId: state => state.user.currentCompanyId,
  routes: state => state.permission.routes,
  addRoutes: state => state.permission.addRoutes
}

export default new Vuex.Store({
  modules: { user, permission },
  getters
})
