import { useUserStore } from '@/stores/modules/user'

/** 接口成功业务码 */
export const API_SUCCESS_CODE = '00000'

type Data<T> = {
  code: string
  msg: string
  result: T
}

/** 部分接口用 data 承载业务体，与 result 等价，统一归一成 result */
type RawBody<T> = {
  code: string
  msg: string
  result?: T
  data?: T
}

/** 
 * 解析请求 URL
 * @param url 请求 URL
 * @returns 解析后的请求 URL
 */
function resolveRequestUrl(url: string | undefined): string {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const base = String(import.meta.env.VITE_HTTP || '').replace(/\/$/, '')
  if (!base) return url
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

/** 
 * 处理授权过期
 * @returns void
 */
function handleAuthExpired() {
  const userStore = useUserStore()
  userStore.clearUserInfo()
  try {
    uni.removeStorageSync('token')
  } catch {
    /* ignore */
  }
  uni.reLaunch({ url: '/pages/login/index' })
}

/** 
 * 提取 HTTP 错误消息
 * @param data 错误数据
 * @returns 错误消息
 */
function pickHttpErrorMsg(data: unknown): string {
  if (data && typeof data === 'object') {
    const d = data as Record<string, unknown>
    const m = d.msg ?? d.message
    if (typeof m === 'string' && m) return m
  }
  return '网络错误'
}

/**
 * 按业务码处理响应（与后端约定：00000 成功，A0100 登录失效，A0200 无权限）
 * @param body 响应数据
 * @param resolve 成功回调
 * @param reject 失败回调
 * @returns void
 */
function handleResponseBody<T>(body: Data<T>, resolve: (v: Data<T>) => void, reject: (v: Data<T>) => void) {
  if (body.code === API_SUCCESS_CODE) {
    resolve(body)
    return
  }
  if (body.code === 'A0100') {
    handleAuthExpired()
    reject(body)
    return
  }
  if (body.code === 'A0200') {
    uni.showToast({ icon: 'none', title: body.msg || '没有操作权限', duration: 1500 })
    reject(body)
    return
  }
  uni.showToast({ icon: 'none', title: body.msg || '操作失败', duration: 1500 })
  reject(body)
}

function requestWithUni<T>(options: UniApp.RequestOptions): Promise<Data<T>> {
  return new Promise<Data<T>>((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''

    uni.request({
      ...options,
      url: resolveRequestUrl(options.url),
      header: {
        ...options.header,
        ...(token ? { Authorization: token } : {}),
      },
      success(res) {
        if (res.statusCode === 401) {
          handleAuthExpired()
          reject(res.data as Data<T>)
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          const msg = pickHttpErrorMsg(res.data)
          uni.showToast({ icon: 'none', title: msg, duration: 1500 })
          reject(res.data as Data<T>)
          return
        }
        /** DELETE 等接口可能返回 204 无响应体 */
        if (res.statusCode === 204) {
          resolve({
            code: API_SUCCESS_CODE,
            msg: '',
            result: null as T,
          })
          return
        }
        const raw = res.data as RawBody<T>
        if (!raw || typeof raw !== 'object' || typeof raw.code !== 'string') {
          uni.showToast({ icon: 'none', title: '响应格式错误', duration: 1500 })
          reject(res.data as Data<T>)
          return
        }
        const body: Data<T> = {
          code: raw.code,
          msg: raw.msg,
          result: (raw.result !== undefined ? raw.result : raw.data) as T,
        }
        handleResponseBody(body, resolve, reject)
      },
      fail(err) {
        const msg = err.errMsg?.includes('timeout') ? '请求超时' : '网络错误，换个网络试试'
        uni.showToast({
          icon: 'none',
          title: msg,
          duration: 1500,
        })
        reject(err)
      },
    })
  })
}

/** 请求接口：相对路径会拼上 `.env` 的 `VITE_HTTP`（测试域） */
export const http = requestWithUni
