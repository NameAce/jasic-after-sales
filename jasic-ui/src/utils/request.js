import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import { getToken } from '@/utils/auth'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

let reloginPromptVisible = false

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

function buildLoginHash() {
  const currentHash = window.location.hash || '#/'
  const currentPath = currentHash.replace(/^#/, '')
  if (!currentPath || currentPath === '/' || currentPath.startsWith('/login')) {
    return '#/login'
  }
  return `#/login?redirect=${encodeURIComponent(currentPath)}`
}

function redirectToLogin() {
  const loginHash = buildLoginHash()
  if (window.location.hash === loginHash) {
    window.location.reload()
    return
  }
  window.location.replace(loginHash)
}

function handleAuthExpired() {
  if (reloginPromptVisible) {
    return
  }
  reloginPromptVisible = true
  const store = require('@/store').default
  MessageBox.confirm('登录已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .catch(() => null)
    .then(() => store.dispatch('user/resetToken'))
    .finally(() => {
      reloginPromptVisible = false
      redirectToLogin()
    })
}

service.interceptors.request.use(
  config => {
    NProgress.start()
    const token = getToken()
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  error => {
    NProgress.done()
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    NProgress.done()
    const res = response.data

    if (res.code !== '00000') {
      if (res.code === 'A0100') {
        handleAuthExpired()
        return null
      }

      const msg = res.code === 'A0200' ? '没有操作权限' : (res.msg || '操作失败')
      const type = res.code === 'A0200' ? 'warning' : 'error'
      Message({ message: msg, type })
      return null
    }

    return res
  },
  error => {
    NProgress.done()
    let msg = error.message || '网络错误'
    if (error.response && error.response.data) {
      msg = error.response.data.msg || error.response.data.message || msg
    }
    Message({ message: msg, type: 'error' })
    return null
  }
)

export default service
