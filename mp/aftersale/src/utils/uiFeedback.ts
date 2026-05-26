/**
 * 统一的 UI 反馈封装：请求级 loading + 接口提示 toast
 *
 * 设计目标：
 * - 写接口（POST/PUT/DELETE/PATCH）自动显示 loading，请求结束自动关闭，业务侧不必再手写；
 * - 接口提示（成功 / 失败）统一带 mask 显示 1500ms，必须等提示完成才能继续操作，
 *   避免「toast 还没看清就跳页 / 刷新数据」的体验问题；
 * - 跳转 / 刷新等后续动作可统一在 toast 完成后再执行，时序由本工具保证。
 *
 * 并发处理：
 * - loading 通过引用计数管理：并发请求只显示一次 loading，最后一个请求结束才真正关闭，
 *   避免「上一个请求关 loading 时下一个还在跑，loading 提早消失」。
 * - loading 显示延迟 200ms：极快接口（<200ms）不会闪屏，仅在请求确实有耗时才显示。
 *
 * 与 mp/contractor/src/utils/uiFeedback.ts 双端 1:1 对齐，逐行对应，调整需双端同步。
 *
 * @修改人 黄碧莲
 * @修改时间 2026-05-26
 */

/** 默认 loading 文案，与原业务侧 uni.showLoading('加载中...') 保持一致 */
const DEFAULT_LOADING_TITLE = '加载中...'

/** 默认接口提示 mask + 阻塞时长（ms）；与 jasic-ui 业务 toast duration 对齐 */
const DEFAULT_TOAST_DURATION_MS = 1500

/** loading 显示延迟（ms）：在此时长内若请求已结束则不显示 loading，避免快接口闪屏 */
const LOADING_SHOW_DELAY_MS = 200

/** 当前活跃请求计数：>0 时屏幕上有 loading；归零时真正调 uni.hideLoading */
let activeLoadingCount = 0

/**
 * 延迟显示 loading 的定时器句柄：
 * - 在 LOADING_SHOW_DELAY_MS 内若计数归零，可直接清理，不显示 loading；
 * - 兼容并发场景：仅第一个进来的请求负责安排 timer，后续请求只是 count++。
 */
let pendingShowTimer: ReturnType<typeof setTimeout> | null = null

/** loading 是否已经真正下发到端（已 uni.showLoading），用于决定 hide 时要不要调 hideLoading */
let loadingShown = false

/**
 * 安全调用 uni.hideLoading
 *
 * 端 API 在某些环境（小程序基础库版本不一致 / loading 未显示时直接 hide）可能抛错；
 * 业务侧不关心异常，统一吞掉，不影响主流程。
 */
function safeUniHideLoading() {
  try {
    uni.hideLoading()
  } catch {
    /* ignore */
  }
}

/**
 * 申请显示一次 loading：把活跃计数 +1
 *
 * 行为：
 * - 第一个请求进来时，安排一个 200ms 的延迟显示 timer；若期间所有请求结束（计数归零），timer 自动失效；
 * - 后续并发请求只是 count++，不重复显示 loading。
 *
 * @param title loading 文案，默认 '加载中...'，调用方一般不需要传
 */
export function showRequestLoading(title: string = DEFAULT_LOADING_TITLE) {
  activeLoadingCount++
  if (activeLoadingCount !== 1) return
  // 仅第一个请求负责安排延迟显示，避免重复定时器
  pendingShowTimer = setTimeout(() => {
    pendingShowTimer = null
    if (activeLoadingCount <= 0) return
    try {
      uni.showLoading({ title, mask: true })
      loadingShown = true
    } catch {
      /* ignore */
    }
  }, LOADING_SHOW_DELAY_MS)
}

/**
 * 释放一次 loading：把活跃计数 -1；归零时真正关闭
 *
 * 行为：
 * - 若 loading 因延迟尚未真正显示（count 在 200ms 内归零），直接清掉 timer，不调 hideLoading；
 * - 若已真正显示，归零时调 uni.hideLoading 真正关闭；
 * - 计数永远不会小于 0，避免外部误调多次 hide 把后续合法 loading 关掉。
 */
export function hideRequestLoading() {
  activeLoadingCount = Math.max(0, activeLoadingCount - 1)
  if (activeLoadingCount > 0) return
  if (pendingShowTimer) {
    clearTimeout(pendingShowTimer)
    pendingShowTimer = null
  }
  if (loadingShown) {
    loadingShown = false
    safeUniHideLoading()
  }
}

/**
 * 兜底强制关闭 loading：清计数 + 清 timer + 调 hideLoading
 *
 * 适用场景：
 * - 接口异常需要 toast 但 loading 还在显示，先关 loading 再 toast，避免视觉叠层；
 * - 登录失效等需要立即弹 modal 的全局事件，先把所有 loading 清光；
 * - 调试 / 异常兜底，保证不出现"动画永久转圈"。
 *
 * 注意：此函数会把所有 in-flight 请求的 loading 状态一起清光；之后即便其他请求还在跑，
 * 也不会重新显示 loading。仅在确实需要"立即把屏幕让给提示"时调用。
 */
export function forceHideRequestLoading() {
  activeLoadingCount = 0
  if (pendingShowTimer) {
    clearTimeout(pendingShowTimer)
    pendingShowTimer = null
  }
  if (loadingShown) {
    loadingShown = false
  }
  safeUniHideLoading()
}

/**
 * 展示带 mask 的接口提示并在时长结束后 resolve
 *
 * 行为约定：
 * - 默认 1500ms 全屏 mask，期间用户无法点击任何区域，保证提示能完整看到；
 * - 显示 toast 前先强关 loading，避免 loading 转圈与 toast 视觉叠层；
 * - 返回 Promise 在 duration 时长后 resolve，调用方可 await 后再做跳转 / 刷新，
 *   避免「提示还在显示就跳页 / 数据被刷新冲掉」。
 *
 * @param title 提示文案
 * @param opts.icon  toast 图标，默认 'none'（业务多为文字提示，不强制 success / error）
 * @param opts.duration  提示时长，默认 1500ms
 * @param opts.mask  是否阻塞操作，默认 true（满足「过了时间才能继续操作」需求）
 * @returns Promise<void> 在 duration 时长后 resolve
 */
export function showApiToast(
  title: string,
  opts?: {
    icon?: UniApp.ShowToastOptions['icon']
    duration?: number
    mask?: boolean
  },
): Promise<void> {
  const duration = opts?.duration ?? DEFAULT_TOAST_DURATION_MS
  const icon = opts?.icon ?? 'none'
  const mask = opts?.mask ?? true
  // 先关 loading：loading 转圈与 toast 同屏会互相覆盖，强关 loading 后 toast 才能完整可见
  forceHideRequestLoading()
  try {
    uni.showToast({ title, icon, duration, mask })
  } catch {
    /* ignore */
  }
  return new Promise((resolve) => setTimeout(resolve, duration))
}

/**
 * 展示提示并在提示完成后执行后续动作（常见组合：弹提示 → 跳转 / 刷新 / 关弹窗）
 *
 * 行为：
 * - 先弹 toast 并 await 它的 1500ms 阻塞期，再执行 after；
 * - after 抛错不向外传，保证调用方主流程不被打断；
 * - 不传 after 时退化为 showApiToast。
 *
 * @param title  提示文案
 * @param after  提示完成后要执行的动作（同步或异步均可）
 * @param opts   透传给 showApiToast
 */
export async function showApiToastThen(
  title: string,
  after?: () => void | Promise<void>,
  opts?: Parameters<typeof showApiToast>[1],
) {
  await showApiToast(title, opts)
  if (!after) return
  try {
    await after()
  } catch (e) {
    // 后续动作失败不影响 toast 流程，仅记录，避免业务报"未捕获的 Promise 异常"
    console.error('[showApiToastThen] after action failed', e)
  }
}
