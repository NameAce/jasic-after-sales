/**
 * 媒体文件上传占用标记（如 MediaUploadField），用于在上传未完成时拦截同页其它操作。
 */

let mediaUploadDepth = 0

export function beginMediaUpload(): void {
  mediaUploadDepth++
}

export function endMediaUpload(): void {
  mediaUploadDepth = Math.max(0, mediaUploadDepth - 1)
}

export function isMediaUploading(): boolean {
  return mediaUploadDepth > 0
}

/** 正在上传时弹出提示并返回 true，表示应终止当前交互 */
export function toastIfMediaUploading(): boolean {
  if (!isMediaUploading()) return false
  uni.showToast({
    title: '当前正在上传数据，请稍等',
    icon: 'none',
    duration: 2000
  })
  return true
}
