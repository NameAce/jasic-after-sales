/**
 * 媒体文件上传占用标记（如 MediaUploadField），用于在上传未完成时拦截同页其它操作。
 * @修改人 黄碧莲
 * @修改时间 2026-05-26
 */

import { showApiToast } from '@/utils/uiFeedback'

let mediaUploadDepth = 0

/**
 * 作用：承修方小程序（网点/总部工单处理、派工）内方法：beginMediaUpload。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function beginMediaUpload(): void {
  mediaUploadDepth++
}

/**
 * 作用：承修方小程序（网点/总部工单处理、派工）内方法：endMediaUpload。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function endMediaUpload(): void {
  mediaUploadDepth = Math.max(0, mediaUploadDepth - 1)
}

/**
 * 作用：判断：isMediaUploading。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function isMediaUploading(): boolean {
  return mediaUploadDepth > 0
}

/**
 * 正在上传时弹出提示并返回 true，表示应终止当前交互
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function toastIfMediaUploading(): boolean {
  if (!isMediaUploading()) return false
  void showApiToast('当前正在上传数据，请稍等', { duration: 2000 })
  return true
}
