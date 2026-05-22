/**
 * 将接口或本地路径转为可预览的完整 URL（与 MediaUploadField 中逻辑一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolvePreviewableUrl(url: unknown): string {
  const raw = String(url ?? '').trim()
  if (!raw) return ''
  if (/^(data:|blob:|file:|wxfile:|http:\/\/tmp\/|https:\/\/tmp\/)/i.test(raw)) return raw
  if (/^\/?(tmp|storage|var|private|android|sdcard)\//i.test(raw)) return raw
  if (/^https?:\/\//i.test(raw)) return raw
  const base = String(import.meta.env.VITE_HTTP || '')
    .trim()
    .replace(/\/$/, '')
  if (!base) return raw
  return `${base}${raw.startsWith('/') ? raw : `/${raw}`}`
}

/**
 * 预览图片列表；current 为下标或当前图原始地址（会与 urls 同样规则解析后匹配）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function previewImages(urls: string[], current?: number | string): void {
  const list = urls.map((u) => resolvePreviewableUrl(u)).filter(Boolean)
  if (!list.length) return
  let cur = list[0]!
  if (typeof current === 'number' && current >= 0 && current < list.length) {
    cur = list[current]!
  } else if (typeof current === 'string') {
    const resolved = resolvePreviewableUrl(current)
    cur = list.find((u) => u === resolved) ?? list[0]!
  }
  uni.previewImage({
    urls: list,
    current: cur
  })
}

/**
 * 全屏预览单个视频（小程序等支持 uni.previewMedia 的环境）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function previewVideo(url: unknown): void {
  const src = resolvePreviewableUrl(url)
  if (!src) return
  if (typeof uni.previewMedia === 'function') {
    uni.previewMedia({
      sources: [{ url: src, type: 'video' }],
      fail: () => {
        uni.showToast({ title: '无法预览视频', icon: 'none', duration: 1500 })
      }
    })
  } else {
    uni.showToast({ title: '当前环境不支持视频预览', icon: 'none', duration: 1500 })
  }
}
