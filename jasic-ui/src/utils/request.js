import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import { getToken } from '@/utils/auth'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  config => {
    NProgress.start()
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = token
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
      // 未登录或Token过期 —— 需要中断流程，仍然 reject
      if (res.code === 'A0100') {
        MessageBox.confirm('登录已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          require('@/store').default.dispatch('user/logout').then(() => {
            location.reload()
          })
        })
        return Promise.reject(res)
      }

      // 其他业务错误：Message 提示后 resolve null，调用方用 if (!res) return 守卫
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
