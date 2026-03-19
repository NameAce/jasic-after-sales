import { login, chooseCompany, getUserInfo, logout } from '@/api/auth'
import { getToken, setToken, removeToken, setCompanyId, removeCompanyId } from '@/utils/auth'

const state = {
  token: getToken(),
  userInfo: {},
  perms: [],
  companies: [],
  currentCompanyId: null,
  needChooseCompany: false
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
  },
  SET_USER_INFO(state, info) {
    state.userInfo = info
  },
  SET_PERMS(state, perms) {
    state.perms = perms
  },
  SET_COMPANIES(state, companies) {
    state.companies = companies
  },
  SET_CURRENT_COMPANY_ID(state, companyId) {
    state.currentCompanyId = companyId
  },
  SET_NEED_CHOOSE_COMPANY(state, val) {
    state.needChooseCompany = val
  }
}

const actions = {
  login({ commit }, loginForm) {
    return new Promise((resolve, reject) => {
      login(loginForm).then(res => {
        if (!res) return reject(new Error('请求失败'))
        const data = res.data
        commit('SET_TOKEN', data.token)
        setToken(data.token)
        if (data.needChooseCompany) {
          commit('SET_COMPANIES', data.companies)
          commit('SET_NEED_CHOOSE_COMPANY', true)
        } else {
          commit('SET_NEED_CHOOSE_COMPANY', false)
          if (data.userInfo) {
            commit('SET_USER_INFO', data.userInfo)
            commit('SET_PERMS', data.userInfo.perms || [])
            commit('SET_CURRENT_COMPANY_ID', data.userInfo.currentCompanyId)
            setCompanyId(data.userInfo.currentCompanyId)
          }
        }
        resolve(data)
      }).catch(error => reject(error))
    })
  },

  chooseCompany({ commit }, companyId) {
    return new Promise((resolve, reject) => {
      chooseCompany({ companyId }).then(res => {
        if (!res) return reject(new Error('请求失败'))
        const info = res.data
        commit('SET_USER_INFO', info)
        commit('SET_PERMS', info.perms || [])
        commit('SET_CURRENT_COMPANY_ID', info.currentCompanyId)
        commit('SET_NEED_CHOOSE_COMPANY', false)
        setCompanyId(info.currentCompanyId)
        resolve(info)
      }).catch(error => reject(error))
    })
  },

  getInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getUserInfo().then(res => {
        if (!res) return reject(new Error('请求失败'))
        const info = res.data
        commit('SET_USER_INFO', info)
        commit('SET_PERMS', info.perms || [])
        commit('SET_CURRENT_COMPANY_ID', info.currentCompanyId)
        commit('SET_COMPANIES', info.companies || [])
        resolve(info)
      }).catch(error => reject(error))
    })
  },

  logout({ commit }) {
    return new Promise((resolve, reject) => {
      logout().then(() => {
        commit('SET_TOKEN', '')
        commit('SET_USER_INFO', {})
        commit('SET_PERMS', [])
        commit('SET_CURRENT_COMPANY_ID', null)
        removeToken()
        removeCompanyId()
        resolve()
      }).catch(error => {
        commit('SET_TOKEN', '')
        removeToken()
        removeCompanyId()
        reject(error)
      })
    })
  },

  resetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_PERMS', [])
      removeToken()
      removeCompanyId()
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
