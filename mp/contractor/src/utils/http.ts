/**
 * 统一 HTTP 封装（与 jasic-ui/src/utils/request.js 契约对齐）
 *
 * 约定：
 * - 响应体仅 `{ code, msg, data }`；不再兼容 `result` / `message`。
 * - 成功业务码严格为 `'00000'`。
 * - 登录失效 `A0100`（或 HTTP 401）：uni.showModal 提示并回登录页；模块内互斥防止重复弹框。
 * - 无权限 `A0200`：toast 提示「没有操作权限」。
 * - 其他非 `'00000'`：toast 提示 `msg || '操作失败'`。
 * - HTTP 非 2xx：toast 取 `res.data.msg || res.data.message || '网络错误'`，与 jasic-ui 行为一致。
 * - 请求头：`Authorization: <token>`（无 `Bearer` 前缀，三端一致）。
 *
 * baseURL 归并（阶段 5.1）：业务路径不再包含 `/api` 前缀，统一由 http 层拼接，
 * 真源：[jasic-ui/src/utils/request.js](../../../../jasic-ui/src/utils/request.js) `baseURL: '/api'`。
 * 本端 `API_BASE = VITE_HTTP + '/api'`，所有 `api/*.ts` 业务 URL 与 jasic-ui 字面保持一致。
 *
 * 存储 key（阶段 5.4）：token 持久化 key 锁定为 `'token'`（历史引用过多不做破坏性改动），
 * 新增的业务缓存统一走 `jasic_*` 前缀（如 `jasic_user_info`）。
 *
 * 分支顺序契约（三端镜像，禁止调整）：
 *   success 回调内：statusCode 401 → 非 2xx → 204 → shape 校验 → handleResponseBody code 分发
 *   fail 回调内：timeout → 其他网络错误
 *
 * 与 `mp/aftersale/src/utils/http.ts` 保持逐行结构对齐（MIRROR_FILE_PAIRS.md 基准）：
 *   命名（`resolveRequestUrl` / `requestWithUni`）、分支顺序、错误提示文案均一致，
 *   如需调整请同步修改双端文件。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

import {
  API_MSG_AUTH_EXPIRED,
  API_MSG_BAD_RESPONSE,
  API_MSG_NETWORK_ERROR,
  API_MSG_NO_PERMISSION,
  API_MSG_OPERATION_FAILED,
  API_MSG_TIMEOUT,
} from '@/constants/apiMessages'

/**
 * 业务成功码
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const API_SUCCESS_CODE = '00000'
/**
 * 登录失效业务码
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const API_AUTH_EXPIRED = 'A0100'
/**
 * 无操作权限业务码
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const API_NO_PERMISSION = 'A0200'

/**
 * 统一后端响应结构（与 jasic-ui 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type ApiResponse<T> = {
  code: string
  msg: string
  data: T
}

/**
 * 模块级互斥：短时间内 A0100/401 并发只弹一次 modal
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
let authExpiredHandling = false

/**
 * 统一在错误提示前收起 loading，避免提示与 loading 叠层互相干扰。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function safeHideLoading() {
  try {
    uni.hideLoading()
  } catch {
    /* ignore */
  }
}

/**
 * 计算 API baseURL：`VITE_HTTP + '/api'`，与 jasic-ui 的 `axios.create({ baseURL: '/api' })` 对齐。
 * VITE_HTTP 不存在时回退为 `/api`，避免小程序端空字符串导致的相对路径拼接异常。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveApiBase(): string {
  const raw = String(import.meta.env.VITE_HTTP || '').replace(/\/$/, '')
  return `${raw}/api`
}

/**
 * 解析请求 URL：相对路径自动拼接 `VITE_HTTP + '/api'`
 * @param url 请求 URL（业务层约定为不含 `/api` 前缀的相对路径，如 `/system/work-order/list`）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveRequestUrl(url: string | undefined): string {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const base = resolveApiBase()
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

/**
 * 导出用于上传等非 `http()` 通道的同名拼接（api/file.ts 复用）
 * @param url 相对 URL（不含 `/api` 前缀）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolveHttpUrl(url: string): string {
  return resolveRequestUrl(url)
}

/**
 * 作用：承修方小程序（网点/总部工单处理、派工）内方法：redirectToLogin。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function redirectToLogin() {
  uni.reLaunch({ url: '/pages/login/index' })
}

/**
 * 处理授权过期：清 token → 弹窗 → reLaunch 登录页
 * 与 jasic-ui 契约一致：文案固定「登录已过期，请重新登录」，模块级互斥防止重复弹框。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleAuthExpired() {
  if (authExpiredHandling) return
  authExpiredHandling = true
  safeHideLoading()
  try {
    uni.removeStorageSync('token')
  } catch {
    /* ignore */
  }
  uni.showModal({
    title: '提示',
    content: API_MSG_AUTH_EXPIRED,
    showCancel: false,
    success: () => {
      authExpiredHandling = false
      redirectToLogin()
    },
    fail: () => {
      authExpiredHandling = false
      redirectToLogin()
    },
  })
}

/**
 * 提取 HTTP 非 2xx 时的错误消息（兼容旧后端只回 message 的情况）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickHttpErrorMsg(data: unknown): string {
  if (data && typeof data === 'object') {
    const d = data as Record<string, unknown>
    const m = d.msg ?? (d as { message?: unknown }).message
    if (typeof m === 'string' && m) return m
  }
  return API_MSG_NETWORK_ERROR
}

/**
 * 扩展 uni.request 选项：允许调用方在 hideLoading 后自行展示接口 msg（避免与 loading 叠层）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type HttpRequestOptions = UniApp.RequestOptions & {
  skipErrorToast?: boolean
}

/**
 * 按业务码处理响应：00000 → resolve；A0100 → 强登；A0200 → 无权限 toast；其他 → 失败 toast
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleResponseBody<T>(
  body: ApiResponse<T>,
  resolve: (v: ApiResponse<T>) => void,
  reject: (v: ApiResponse<T>) => void,
  skipErrorToast?: boolean,
) {
  if (body.code === API_SUCCESS_CODE) {
    resolve(body)
    return
  }
  if (body.code === API_AUTH_EXPIRED) {
    handleAuthExpired()
    reject(body)
    return
  }
  if (body.code === API_NO_PERMISSION) {
    safeHideLoading()
    uni.showToast({ icon: 'none', title: API_MSG_NO_PERMISSION, duration: 1500 })
    reject(body)
    return
  }
  safeHideLoading()
  if (!skipErrorToast) {
    uni.showToast({ icon: 'none', title: body.msg || API_MSG_OPERATION_FAILED, duration: 1500 })
  }
  reject(body)
}

function requestWithUni<T>(options: HttpRequestOptions): Promise<ApiResponse<T>> {
  const skipErrorToast = options.skipErrorToast === true
  return new Promise<ApiResponse<T>>((resolve, reject) => {
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
          reject(res.data as ApiResponse<T>)
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          const msg = pickHttpErrorMsg(res.data)
          safeHideLoading()
          if (!skipErrorToast) {
            uni.showToast({ icon: 'none', title: msg, duration: 1500 })
          }
          reject(res.data as ApiResponse<T>)
          return
        }
        /**
 * DELETE 等接口可能返回 204 无响应体
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
        if (res.statusCode === 204) {
          resolve({
            code: API_SUCCESS_CODE,
            msg: '',
            data: null as T,
          })
          return
        }
        const raw = res.data as Partial<ApiResponse<T>> | null | undefined
        if (!raw || typeof raw !== 'object' || typeof raw.code !== 'string') {
          safeHideLoading()
          if (!skipErrorToast) {
            uni.showToast({ icon: 'none', title: API_MSG_BAD_RESPONSE, duration: 1500 })
          }
          reject(res.data as ApiResponse<T>)
          return
        }
        handleResponseBody<T>(
          {
            code: raw.code,
            msg: raw.msg ?? '',
            data: raw.data as T,
          },
          resolve,
          reject,
          skipErrorToast,
        )
      },
      fail(err) {
        const msg = err.errMsg?.includes('timeout') ? API_MSG_TIMEOUT : API_MSG_NETWORK_ERROR
        safeHideLoading()
        if (!skipErrorToast) {
          uni.showToast({
            icon: 'none',
            title: msg,
            duration: 1500,
          })
        }
        reject(err)
      },
    })
  })
}

/**
 * 请求接口：相对路径会拼上 `.env` 的 `VITE_HTTP`（测试域）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const http = requestWithUni

/**
 * 提取接口提示文案，统一优先使用后端返回。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getApiMessage<T>(res: ApiResponse<T> | null | undefined, fallback = ''): string {
  const msg = String(res?.msg ?? '').trim()
  return msg || fallback
}
