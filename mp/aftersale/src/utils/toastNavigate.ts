/** tabBar 首页（与 pages.json 中 tabBar.list 一致） */
export const TAB_HOME = '/pages/index/index'
/** tabBar 我的 */
export const TAB_MY = '/pages/my/index'

/**
 * 切换到 tabBar 页面（统一入口，便于替换与检索）。
 */
export function switchToTab(url: string) {
  uni.switchTab({ url })
}

/**
 * 先切到 tabBar 页，再在延迟后执行回调（如未登录时先回首页再打开登录页）。
 */
export function switchTabThen(url: string, then: () => void, delayMs = 100) {
  switchToTab(url)
  setTimeout(then, delayMs)
}

/**
 * 展示 toast 后在相同时长后跳转，避免 duration 与 setTimeout 不一致。
 * 默认使用 switchTab（tabBar）；非 tabBar 页面用 navigateTo；需清栈（如退出登录）用 reLaunch。
 */
export function showToastThen(
  url: string,
  options?: {
    title?: string
    /** 与 uni.showToast 的 duration 一致，默认 1500 */
    duration?: number
    icon?: UniApp.ShowToastOptions['icon']
    /** 默认 switchTab；普通页面用 navigateTo；清栈打开用 reLaunch */
    navigateType?: 'switchTab' | 'navigateTo' | 'reLaunch'
  },
) {
  const duration = options?.duration ?? 1500
  const title = options?.title ?? '操作成功'
  const icon = options?.icon ?? 'success'
  const navigateType = options?.navigateType ?? 'switchTab'
  uni.showToast({ title, icon, duration })
  setTimeout(() => {
    if (navigateType === 'navigateTo') {
      uni.navigateTo({ url })
    } else if (navigateType === 'reLaunch') {
      uni.reLaunch({ url })
    } else {
      switchToTab(url)
    }
  }, duration)
}
