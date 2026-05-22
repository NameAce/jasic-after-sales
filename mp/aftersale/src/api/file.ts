import { API_SUCCESS_CODE, resolveHttpUrl } from '@/utils/http'

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
  code?: string
  msg?: string
  data?: SysFileUploadVO
}

/**
 * 作用：接口封装：resolveUploadUrl。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveUploadUrl(url: string): string {
  return resolveHttpUrl(url)
}

/**
 * 作用：转换/构造：parseUploadResponse。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：接口封装：uploadCustomerFile。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function uploadCustomerFile(filePath: string): Promise<SysFileUploadVO> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: resolveUploadUrl('/customer/file/upload'),
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: token } : {}),
      },
      success(res) {
        const body = parseUploadResponse(res.data as string | UploadBody)
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(body.msg || '上传失败'))
          return
        }
        if (body.code !== API_SUCCESS_CODE) {
          reject(new Error(body.msg || '上传失败'))
          return
        }
        const data = body.data
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
