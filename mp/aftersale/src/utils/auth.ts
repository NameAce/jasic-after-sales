/**
 * 登录状态检查与跳转工具
 */

export function isLoggedIn(): boolean {
  return !!uni.getStorageSync('token')
}

/**
 * 检查登录状态，未登录则跳转登录页。
 * 返回 true 表示已登录，false 表示未登录（已触发跳转）。
 */
export function requireLogin(): boolean {
  if (isLoggedIn()) return true
  uni.navigateTo({ url: '/pages/login/index' })
  return false
}
