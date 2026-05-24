/**
 * 佳士报修「商品查询」结果 toast：按文案长度延长展示时长。
 * 微信小程序 loading / toast 共用通道，须在 hideLoading 完成后再延迟弹出。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

export type BarcodeQueryToastKind = 'success' | 'fail'

/** 默认：hideLoading 完成后再弹 toast 的间隔 */
const TOAST_AFTER_LOADING_DELAY_MS = 150
/** iOS 成功提示：扫码返回常触发 onShow + 表单重绘，需更长间隔避免被 hideLoading 吞掉 */
const TOAST_AFTER_LOADING_DELAY_IOS_SUCCESS_MS = 520
const TOAST_AFTER_LOADING_DELAY_IOS_FAIL_MS = 280

let cachedIsIOS: boolean | null = null

/**
 * 是否 iOS 端
 */
function isIOSPlatform(): boolean {
  if (cachedIsIOS !== null) return cachedIsIOS
  try {
    const platform = String(uni.getSystemInfoSync().platform || '').toLowerCase()
    cachedIsIOS = platform === 'ios'
  } catch {
    cachedIsIOS = false
  }
  return cachedIsIOS
}

/**
 * 计算条码查询结果 toast 展示时长（毫秒）
 */
export function barcodeQueryToastDuration(
  text: string,
  kind: BarcodeQueryToastKind = 'success'
): number {
  const len = String(text ?? '').trim().length
  const base =
    kind === 'success'
      ? Math.max(4000, 2600 + len * 55)
      : Math.max(3200, 2400 + len * 50)
  let ms = Math.min(kind === 'success' ? 7000 : 5000, base)
  if (kind === 'success' && isIOSPlatform()) {
    ms = Math.min(8000, Math.max(ms + 1000, 5000 + len * 55))
  }
  if (kind === 'fail' && isIOSPlatform()) {
    ms = Math.min(8000, Math.max(ms + 1500, 4200 + len * 55))
  }
  return ms
}

function toastDelayAfterHideLoading(kind: BarcodeQueryToastKind): number {
  if (!isIOSPlatform()) return TOAST_AFTER_LOADING_DELAY_MS
  return kind === 'success'
    ? TOAST_AFTER_LOADING_DELAY_IOS_SUCCESS_MS
    : TOAST_AFTER_LOADING_DELAY_IOS_FAIL_MS
}

/**
 * 展示条码查询结果 toast
 */
export function showBarcodeQueryToast(options: {
  title: string
  kind?: BarcodeQueryToastKind
  icon?: UniApp.ShowToastOptions['icon']
}): void {
  const kind = options.kind ?? 'success'
  const title = String(options.title ?? '').trim() || (kind === 'success' ? '查询成功' : '查询失败')
  const duration = barcodeQueryToastDuration(title, kind)
  const icon: UniApp.ShowToastOptions['icon'] =
    kind === 'success' ? 'none' : (options.icon ?? 'none')
  const delay = toastDelayAfterHideLoading(kind)
  setTimeout(() => {
    uni.showToast({ title, icon, duration })
  }, delay)
}

/**
 * 先 hideLoading（等 complete）再展示 toast，避免 iOS 上成功提示被第二次 hideLoading 关掉
 */
export function hideLoadingThenShowBarcodeQueryToast(options: {
  title: string
  kind?: BarcodeQueryToastKind
  icon?: UniApp.ShowToastOptions['icon']
}): void {
  try {
    uni.hideLoading()
  } catch {
    /* 无 loading 时忽略 */
  }
  showBarcodeQueryToast(options)
}
