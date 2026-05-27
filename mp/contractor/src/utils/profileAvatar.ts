/**
 * 头像展示与上传辅助（B 端「我的」资料编辑）
 *
 * 微信 `chooseAvatar` 返回本地临时路径，需上传至系统文件服务后再把 URL 写入用户资料。
 */

const HTTP_URL_RE = /^https?:\/\//i

/**
 * 解析用于 `<image>` 展示的头像地址：优先用户已保存 URL，否则默认图。
 */
export function resolveAvatarDisplayUrl(avatar: string | undefined, defaultAvatar: string): string {
  const saved = String(avatar ?? '').trim()
  if (saved) return saved
  return defaultAvatar
}

/**
 * 判断是否为需上传的本地临时路径（非 http(s) 即视为本地）。
 */
export function isLocalAvatarPath(path: string): boolean {
  const s = String(path ?? '').trim()
  if (!s) return false
  return !HTTP_URL_RE.test(s)
}

/**
 * 将展示用头像转为提交后端的 URL：本地路径走上传，已是 URL 则原样返回。
 */
export async function resolveAvatarUrlForSubmit(
  displayPath: string,
  savedUrl: string | undefined,
  uploadFile: (localPath: string) => Promise<{ previewUrl?: string }>,
  defaultAvatar?: string,
): Promise<string | undefined> {
  const draft = String(displayPath ?? '').trim()
  const previous = String(savedUrl ?? '').trim()
  if (!draft || draft === previous) {
    return previous || undefined
  }
  const fallback = String(defaultAvatar ?? '').trim()
  if (!previous && fallback && draft === fallback) {
    return undefined
  }
  if (!isLocalAvatarPath(draft)) {
    return draft
  }
  const uploaded = await uploadFile(draft)
  return String(uploaded.previewUrl ?? '').trim() || undefined
}
