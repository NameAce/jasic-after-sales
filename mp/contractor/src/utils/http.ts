/**
 * 统一后端返回格式（与 aftersale 对齐）。
 *
 * 兼容字段：
 * - `data`：常见后端返回字段
 * - `result`：历史字段（当前项目早期 mock 使用）
 */
export type ApiResponse<T> = {
  code: number | string
  msg?: string
  message?: string
  data?: T
  result?: T
}

// Vite/uni-app 环境变量（对应根目录 `.env` 内的 VITE_*）
const API_BASE = import.meta.env.VITE_HTTP as string | undefined

function withBaseUrl(url: string) {
  if (!url) return url
  if (/^https?:\/\//i.test(url)) return url
  if (!API_BASE) return url
  return `${API_BASE.replace(/\/+$/, '')}/${url.replace(/^\/+/, '')}`
}

/**
 * 请求函数
 * @param options 请求选项
 * @returns 请求结果
 */
export const http = <T>(options: UniApp.RequestOptions) => {
  return new Promise<ApiResponse<T>>((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''

    uni.request({
      ...options,
      url: withBaseUrl(String(options.url || '')),
      header: {
        ...options.header,
        ...(token ? { Authorization: token } : {}),
      },
      success(res) {
        const apiRes = (res.data || {}) as ApiResponse<T>
        if (res.statusCode >= 200 && res.statusCode < 300) {
          // 业务约定：登录态失效等，统一提示并回登录页
          if (String(apiRes.code) === 'A0100') {
            uni.removeStorageSync('token')
            const msg = getApiMessage(apiRes, '请重新登录')
            uni.showModal({
              title: '提示',
              content: msg,
              showCancel: false,
              success: () => {
                uni.reLaunch({ url: '/pages/login/index' })
              },
            })
            reject(apiRes)
            return
          }
          resolve(apiRes)
        } else if (res.statusCode === 401) {
          // 避免 http 层依赖 store，打断循环引用链
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/index' })
          reject(res)
        } else if (res.statusCode === 403) {
          uni.showToast({
            icon: 'none',
            title: getApiMessage(apiRes, '暂无权限'),
          })
          reject(res)
        } else {
          uni.showToast({
            icon: 'none',
            title: getApiMessage(apiRes, '请求错误'),
          })
          reject(res)
        }
      },
      fail(err) {
        const errMsg = String((err as { errMsg?: string })?.errMsg || '').trim()
        uni.showToast({
          icon: 'none',
          title: errMsg || '网络错误，换个网络试试',
        })
        reject(err)
      },
    })
  })
}

/** 从 ApiResponse 中提取业务数据（兼容 data/result）。 */
export function unwrap<T>(res: ApiResponse<T>): T {
  return (res.data ?? res.result) as T
}

/** 提取接口提示文案，统一优先使用后端返回。 */
export function getApiMessage<T>(res: ApiResponse<T> | null | undefined, fallback = ''): string {
  const msg = String(res?.msg ?? res?.message ?? '').trim()
  return msg || fallback
}
