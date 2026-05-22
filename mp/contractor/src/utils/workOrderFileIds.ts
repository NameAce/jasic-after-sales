import { resolvePreviewableUrl } from '@/utils/mediaPreview'

/**
 * 从上传组件回显对象上解析服务端文件 ID（上传接口回填 fileId / id 后生效）
 * @param item 上传组件回显对象
 * @returns 文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function parseWorkOrderFileId(item: unknown): number | null {
  if (!item || typeof item !== 'object') return null
  const o = item as Record<string, unknown>
  for (const k of ['fileId', 'file_id', 'id']) {
    const v = o[k]
    if (typeof v === 'number' && Number.isFinite(v)) return v
    if (typeof v === 'string' && /^\d+$/.test(v.trim())) return Number(v.trim())
  }
  return null
}

/**
 * 判断是否为视频媒体文件
 * @param item 上传组件回显对象
 * @returns 是否为视频媒体文件
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function isVideoMediaItem(item: unknown): boolean {
  if (!item || typeof item !== 'object') return false
  const o = item as Record<string, unknown>
  if (o.fileType === 'video') return true
  const url = String(o.url ?? '')
  return /\.(mp4|mov|avi|webm|mkv)(\?|$)/i.test(url)
}

/**
 * 分割故障媒体文件ID
 * @param items 上传组件回显对象列表
 * @returns 故障媒体文件ID列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function partitionFaultMediaFileIds(
  items: unknown[],
): { faultImageFileIds: number[]; faultVideoFileIds: number[] } {
  const faultImageFileIds: number[] = []
  const faultVideoFileIds: number[] = []
  for (const item of items) {
    const id = parseWorkOrderFileId(item)
    if (id == null) continue
    if (isVideoMediaItem(item)) faultVideoFileIds.push(id)
    else faultImageFileIds.push(id)
  }
  return { faultImageFileIds, faultVideoFileIds }
}

/**
 * 收集寄件凭证文件ID
 * @param items 上传组件回显对象列表
 * @returns 寄件凭证文件ID列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function collectVoucherFileIds(items: unknown[]): number[] {
  const ids: number[] = []
  for (const item of items) {
    const id = parseWorkOrderFileId(item)
    if (id != null) ids.push(id)
  }
  return ids
}

/**
 * 将未知对象转换为数组
 * @param v 未知对象
 * @returns 数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function asUnknownArray(v: unknown): unknown[] {
  return Array.isArray(v) ? v : []
}

/**
 * 收集语音文件ID
 * @param list 上传组件回显对象列表
 * @returns 语音文件ID列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function collectVoiceFileIds(list: unknown[]): number[] {
  return list
    .map((it) => parseWorkOrderFileId(it))
    .filter((n): n is number => n != null)
}

/**
 * 是否仍有未上传完成的媒体（仅有本地路径、无 fileId / 无可用 http(s) url）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function hasUnuploadedMediaItems(items: unknown[]): boolean {
  for (const item of asUnknownArray(items)) {
    if (!item || typeof item !== 'object') continue
    if (parseWorkOrderFileId(item) != null) continue
    const o = item as Record<string, unknown>
    const url = String(o.url ?? o.previewUrl ?? '').trim()
    const local = String(o.tempFilePath ?? o.path ?? '').trim()
    // 统一约束：提交接口仅允许文件 ID，只有 URL（即使是 http）也视为未完成可提交态
    if (url || local) return true
  }
  return false
}

/**
 * 维修登记提交：收集已上传图片的可访问 URL（逗号拼接前用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function collectMediaUrlsForRepairSubmit(items: unknown[]): string[] {
  const out: string[] = []
  for (const item of asUnknownArray(items)) {
    if (!item || typeof item !== 'object') continue
    const o = item as Record<string, unknown>
    if (parseWorkOrderFileId(item) == null) {
      const urlOnly = String(o.url ?? o.previewUrl ?? '').trim()
      if (!/^https?:\/\//i.test(urlOnly)) continue
    }
    const raw = o.url ?? o.previewUrl ?? o.fileUrl
    const u = resolvePreviewableUrl(raw).trim()
    if (u) out.push(u)
  }
  return out
}
