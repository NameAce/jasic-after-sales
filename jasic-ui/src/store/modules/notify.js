import { getNotifyTodoCount } from '@/api/notify'

function buildDefaultState() {
  return {
    todoCount: 0
  }
}

const state = buildDefaultState()

const mutations = {
  SET_TODO_COUNT(state, count) {
    state.todoCount = Number(count) || 0
  },
  RESET_STATE(state) {
    Object.assign(state, buildDefaultState())
  }
}

const actions = {
  fetchTodoCount({ commit }) {
    return new Promise(resolve => {
      getNotifyTodoCount().then(res => {
        if (!res) {
          resolve(0)
          return
        }
        const count = (res.data && res.data.count) || 0
        commit('SET_TODO_COUNT', count)
        resolve(count)
      }).catch(() => {
        resolve(0)
      })
    })
  },
  resetState({ commit }) {
    commit('RESET_STATE')
    return Promise.resolve()
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
