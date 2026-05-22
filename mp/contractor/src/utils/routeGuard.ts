/**
 * 路由守卫：仅校验登录态与白名单。
 * 操作权限在页面内通过 `Perms` + `userStore.hasPermission` / `canAny` / `canAll` 控制按钮等。
 *
 * uni-app 无 vue-router beforeEach，使用 uni.addInterceptor。
 * 拦截器无法覆盖冷启动首屏，首屏请将登录页放在第一位；已登录用户从登录页进入业务由登录页 onShow + switchTab 处理。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

// 登录页
const LOGIN_PAGE = '/pages/login/index'

// 白名单
const WHITE_LIST = [LOGIN_PAGE]

/**
 * 将跳转 url 规范为以 / 开头的页面 path（不含 query）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizePagePath(url: string): string {
  let path = url.split('?')[0]?.trim() ?? ''
  if (!path.startsWith('/')) path = `/${path}`
  return path
}

/**
 * 是否在白名单中
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isWhiteListed(url: string | undefined): boolean {
  if (!url) return false
  const path = normalizePagePath(url)
  return WHITE_LIST.some((w) => path === w)
}

/**
 * 路由守卫
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function guard(args: { url?: string }) {
  if (isWhiteListed(args.url)) return true
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.reLaunch({ url: LOGIN_PAGE })
    return false
  }
  return true
}

/**
 * 设置路由守卫
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function setupRouteGuard() {
  const methods = ['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'] as const

  methods.forEach((method) => {
    uni.addInterceptor(method, {
      invoke(args: { url?: string }) {
        return guard(args)
      },
    })
  })
}
