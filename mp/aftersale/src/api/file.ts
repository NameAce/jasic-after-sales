import { API_SUCCESS_CODE } from '@/utils/http'

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
  result?: SysFileUploadVO
}

function resolveUploadUrl(url: string): string {
  if (/^https?:\/\//i.test(url)) return url
  const base = String(import.meta.env.VITE_HTTP || '').replace(/\/$/, '')
  if (!base) return url
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

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

export function uploadCustomerFile(filePath: string): Promise<SysFileUploadVO> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: resolveUploadUrl('/api/customer/file/upload'),
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
