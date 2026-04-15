/** 系统侧上传返回（与 SysFileUploadVO 一致） */
export interface SysFileUploadVO {
  contentType?: string
  fileExt?: string
  fileHash?: string
  fileId?: number
  fileSize?: number
  originalName?: string
  previewUrl?: string
}

type UploadBody = {
  code?: string | number
  msg?: string
  data?: SysFileUploadVO
  result?: SysFileUploadVO
}

/**
 * 解析上传URL
 * @param url - 上传URL
 * @returns 解析后的上传URL
 */
function resolveUploadUrl(url: string): string {
  if (/^https?:\/\//i.test(url)) return url
  const base = String(import.meta.env.VITE_HTTP || '').trim().replace(/\/$/, '')
  if (!base) return url
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

/**
 * 判断上传是否成功
 * @param code - 上传返回码
 * @returns 是否成功
 */
function isUploadSuccess(code: unknown): boolean {
  const s = String(code)
  return s === '00000' || s === '200' || s === '0'
}

/**
 * 解析上传响应
 * @param raw - 上传响应
 * @returns 解析后的上传响应
 */
function parseUploadResponse(raw: string | UploadBody): UploadBody {
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as UploadBody
    } catch {
      return { code: '', msg: '上传响应格式错误' }
    }
  }
  return raw ?? {}
}

/**
 * 承包商端系统文件上传（工单等 B 端场景）
 * @param filePath - 文件路径
 * @returns 上传后的文件信息
 * POST `/api/system/file/upload`
 */
export function uploadSystemFile(filePath: string): Promise<SysFileUploadVO> {
  /**
   * 上传文件
   * @param filePath - 文件路径
   * @returns 上传后的文件信息
   */
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: resolveUploadUrl('/api/system/file/upload'),
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: token } : {}),
      },
      success(res) {
        const body = parseUploadResponse(res.data as string | UploadBody)
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(String(body.msg || '上传失败')))
          return
        }
        if (!isUploadSuccess(body.code)) {
          reject(new Error(String(body.msg || '上传失败')))
          return
        }
        const data = body.data ?? body.result
        if (!data) {
          reject(new Error('上传返回为空'))
          return
        }
        resolve(data)
      },
      fail(err) {
        const e = err as { errMsg?: string }
        reject(new Error(e?.errMsg || '网络错误，上传失败'))
      },
    })
  })
}
