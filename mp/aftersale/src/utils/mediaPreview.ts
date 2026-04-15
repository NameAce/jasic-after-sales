/** 预览用 URL（可在此拼接鉴权参数等） */
export function resolvePreviewableUrl(url: string): string {
  return String(url || '').trim()
}

export function previewImages(urls: string[], current: number) {
  if (!urls.length) return
  const i = Math.max(0, Math.min(current, urls.length - 1))
  uni.previewImage({ urls, current: i })
}
