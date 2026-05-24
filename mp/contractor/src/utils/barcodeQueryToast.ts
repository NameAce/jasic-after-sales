/**
 * 佳士报修「商品查询」结果 toast：按文案长度延长；iOS 失败提示额外加长（无 success 图标时体感偏短）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

export type BarcodeQueryToastKind = 'success' | 'fail'

let cachedIsIOS: boolean | null = null

/**
 * 是否 iOS 端（用于失败 toast 加长）
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
 * @param text - 提示文案
 * @param kind - success 保持原节奏；fail 在 iOS 上额外加长
 */
export function barcodeQueryToastDuration(
  text: string,
  kind: BarcodeQueryToastKind = 'success'
): number {
  const len = String(text ?? '').trim().length
  const base =
    kind === 'success'
      ? Math.max(2800, 2000 + len * 45)
      : Math.max(3200, 2400 + len * 50)
  let ms = Math.min(5000, base)
  if (kind === 'fail' && isIOSPlatform()) {
    ms = Math.min(8000, Math.max(ms + 1500, 4200 + len * 55))
  }
  return ms
}

/**
 * 展示条码查询结果 toast；iOS 失败时在 hideLoading 后延迟一帧，避免被系统打断
 */
export function showBarcodeQueryToast(options: {
  title: string
  kind?: BarcodeQueryToastKind
  icon?: UniApp.ShowToastOptions['icon']
}): void {
  const kind = options.kind ?? 'success'
  const title = String(options.title ?? '').trim() || (kind === 'success' ? '查询成功' : '查询失败')
  const duration = barcodeQueryToastDuration(title, kind)
  const icon = options.icon ?? (kind === 'success' ? 'success' : 'none')
  const run = () => {
    uni.showToast({ title, icon, duration })
  }
  if (kind === 'fail' && isIOSPlatform()) {
    setTimeout(run, 80)
    return
  }
  run()
}
