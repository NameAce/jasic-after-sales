import router from './index'
import store from '@/store'
import { getToken } from '@/utils/auth'
import NProgress from 'nprogress'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/choose-company']

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const hasToken = getToken()

  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      const hasRoutes = store.getters.addRoutes && store.getters.addRoutes.length > 0
      if (hasRoutes) {
        next()
      } else {
        try {
          await store.dispatch('user/getInfo')
          const asyncRoutes = await store.dispatch('permission/generateRoutes')
          router.addRoutes(asyncRoutes)
          next({ ...to, replace: true })
        } catch (error) {
          await store.dispatch('user/resetToken')
          next(`/login?redirect=${to.path}`)
          NProgress.done()
        }
      }
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
