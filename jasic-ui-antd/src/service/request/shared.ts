/**
 * 请求层共享：Authorization 头、refreshToken 串行刷新、错误消息去重等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { fetchRefreshToken } from '../api';
import type { RequestInstanceState } from './type';

/**
 * 作用：判断值是否为 null 或 undefined（扁平请求结果解析共用）。
 * @param value - 待判断值
 * @returns 是否为 null/undefined
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isNil(value: unknown): value is null | undefined {
  return value === null || value === undefined;
}

/**
 * 作用：从本地存储读取 token 并格式化为请求头 Authorization 值（与 Sa-Token 约定一致，不加 Bearer）。
 * @returns 鉴权头内容，未登录为 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getAuthorization() {
  // 从本地存储读取登录 token
  const token = localStg.get('token');
  // 与 jasic-ui / Sa-Token（token-name: Authorization，无 token-prefix）一致：请求头为原始 token，不加 Bearer 前缀
  const Authorization = token ? String(token) : null;

  return Authorization;
}

/**
 * 作用：是否启用 refreshToken 刷新流程（由环境变量控制）。
 * @returns 是否启用
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isAuthRefreshTokenEnabled() {
  return import.meta.env.VITE_AUTH_REFRESH_TOKEN === 'Y';
}

/**
 * 作用：调用刷新接口更新 token；未开启刷新或无 refreshToken 时清空登录态。
 * @returns 刷新成功为 true，否则 false
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleRefreshToken() {
  // 取鉴权 Store 的 resetStore，用于刷新失败或未开启刷新时清空登录态
  const { resetStore } = useAuthStore();

  if (!isAuthRefreshTokenEnabled()) {
    // 未开启刷新：直接清空会话
    resetStore();
    return false;
  }

  // 本地 refreshToken，缺失则无法续期
  const rToken = localStg.get('refreshToken') || '';
  if (!rToken) {
    resetStore();
    return false;
  }

  // 调用后端刷新接口换取新 token
  const { error, data } = await fetchRefreshToken(rToken);
  if (!error) {
    localStg.set('token', data.token);
    if (data.refreshToken !== undefined && data.refreshToken !== null && data.refreshToken !== '') {
      localStg.set('refreshToken', data.refreshToken);
    }
    return true;
  }

  // 刷新接口失败：登出
  resetStore();

  return false;
}

/**
 * 作用：401/过期时串行刷新 token，避免并发重复刷新（通过 state.refreshTokenFn 复用同一 Promise）。
 * @param state - 请求实例状态
 * @returns 是否刷新成功
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function handleExpiredRequest(state: RequestInstanceState) {
  if (!state.refreshTokenFn) {
    // 复用同一 Promise，避免并发 401 触发多次 refresh
    state.refreshTokenFn = handleRefreshToken();
  }

  const success = await state.refreshTokenFn;

  // 延迟清空，给短时间内其它挂起请求复用刷新结果的机会
  setTimeout(() => {
    state.refreshTokenFn = null;
  }, 1000);

  return success;
}

/**
 * 作用：防抖式全局 error 消息提示，同一状态下相同文案不重复弹出。
 * @param state - 请求实例状态（维护 toastErrMsgStack）
 * @param message - 展示给用户的消息
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function showErrorMsg(state: RequestInstanceState, message: string) {
  if (!state.toastErrMsgStack?.length) {
    state.toastErrMsgStack = [];
  }

  const isExist = state.toastErrMsgStack.includes(message);

  if (!isExist) {
    state.toastErrMsgStack.push(message);

    window.$message?.error(message, 1.5, () => {
      state.toastErrMsgStack = state.toastErrMsgStack.filter(msg => msg !== message);
    });
  }
}

/**
 * 作用：从 Axios 响应体中解析后端提示文案（**优先 `msg`**，其次 `message`），无则返回 fallback。
 * @param response - 原始响应对象
 * @param fallback - 无文案时的默认值
 * @returns 后端消息或 fallback
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getResponseMsg(response: unknown, fallback = '') {
  if (!response || typeof response !== 'object') {
    return fallback;
  }

  const responseData = (response as { data?: Record<string, unknown> }).data;
  if (!responseData || typeof responseData !== 'object') {
    return fallback;
  }

  const msg = responseData.msg;
  if (typeof msg === 'string' && msg.trim()) {
    return msg.trim();
  }

  const message = responseData.message;
  if (typeof message === 'string' && message.trim()) {
    return message.trim();
  }

  return fallback;
}

/**
 * 作用：从 `createFlatRequest` 返回的 `{ data, error, response }` 等结果上取提示（优先走 `response` 中的 `msg`）。
 * @param flatResult - 扁平请求返回值或含 `response` / `msg` 的对象
 * @param fallback 无后端文案时的兜底
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getFlatResponseMsg(flatResult: unknown, fallback = ''): string {
  if (isNil(flatResult)) return fallback;
  if (typeof flatResult === 'string') {
    const t = flatResult.trim();
    return t || fallback;
  }
  if (typeof flatResult !== 'object') return fallback;

  const o = flatResult as Record<string, unknown>;
  /** 扁平请求失败时不再从 response 取文案，避免业务层误把错误 msg 当成功提示
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  if ('error' in o && !isNil(o.error) && o.error !== false) {
    return fallback;
  }
  if (o.response) {
    const fromResponse = getResponseMsg(o.response, '');
    if (fromResponse) return fromResponse;
  }
  for (const key of ['msg', 'message', 'successMessage'] as const) {
    const v = o[key];
    if (typeof v === 'string' && v.trim()) return v.trim();
  }
  if (o.data && typeof o.data === 'object') {
    const d = o.data as Record<string, unknown>;
    const nested = [d.msg, d.message].find(x => typeof x === 'string' && String(x).trim());
    if (typeof nested === 'string') return nested.trim();
  }
  return fallback;
}

/**
 * 作用：从扁平请求**失败**结果上取后端提示（优先 `response.data.msg` / `message`）。
 * @param flatResult - `await request(...)` 在业务失败时的返回值
 * @param fallback - 无文案时的兜底
 * @returns {string} 错误提示文案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getFlatErrorMsg(flatResult: unknown, fallback = ''): string {
  if (isNil(flatResult) || typeof flatResult !== 'object') return fallback;
  const o = flatResult as Record<string, unknown>;
  if (!isNil(o.response)) {
    const fromResponse = getResponseMsg(o.response, '');
    if (fromResponse) return fromResponse;
  }
  for (const key of ['msg', 'message'] as const) {
    const v = o[key];
    if (typeof v === 'string' && v.trim()) return v.trim();
  }
  if (!isNil(o.data) && typeof o.data === 'object') {
    const d = o.data as Record<string, unknown>;
    const nested = [d.msg, d.message].find(x => typeof x === 'string' && String(x).trim());
    if (typeof nested === 'string') return nested.trim();
  }
  return fallback;
}

/**
 * 作用：判断 `createFlatRequest` 返回值是否为业务/网络失败。
 * @param flatResult - `await request(...)` 返回值
 * @returns 是否存在 error 字段且为真
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function isFlatRequestFailed(flatResult: unknown): boolean {
  if (isNil(flatResult) || typeof flatResult !== 'object') {
    return false;
  }
  const r = flatResult as { error?: unknown };
  return !isNil(r.error) && r.error !== false;
}

/**
 * 作用：解析环境变量中的逗号分隔业务码列表（与 index.ts parseCodeList 语义一致）。
 * @param raw - 环境变量原始字符串
 * @returns 去空白后的码列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function parseEnvCodeList(raw: string | undefined) {
  return (raw?.split(',') || []).map(c => c.trim()).filter(Boolean);
}

/**
 * 作用：失败结果是否会由请求层跳转 403/404/500 异常页（此类不关弹窗也可，由路由接管）。
 * @param flatResult - `await request(...)` 失败返回值
 * @returns 是否会走异常页路由
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function isFlatRequestExceptionPageError(flatResult: unknown): boolean {
  if (!isFlatRequestFailed(flatResult)) {
    return false;
  }
  const o = flatResult as { response?: { data?: Record<string, unknown>; status?: number } };
  const responseData = o.response?.data;
  const businessCode = responseData && typeof responseData === 'object' ? String(responseData.code ?? '') : '';
  const forbiddenCodes = parseEnvCodeList(import.meta.env.VITE_SERVICE_FORBIDDEN_CODES ?? 'A0200');
  const serverErrorCodes = parseEnvCodeList(import.meta.env.VITE_SERVICE_SERVER_ERROR_CODES ?? 'A0500');
  if (forbiddenCodes.includes(businessCode) || serverErrorCodes.includes(businessCode)) {
    return true;
  }
  const httpStatus = o.response?.status;
  if (httpStatus === 403 || httpStatus === 404) {
    return true;
  }
  return typeof httpStatus === 'number' && httpStatus >= 500;
}

/**
 * 作用：操作弹窗/抽屉提交结果处理：成功则 `success` 并返回 true；失败返回 false（**不关弹窗**）。
 * 非 403/404/500 的错误文案由 `onBackendFail` / `onError` 全局 `showErrorMsg` 提示。
 *
 * @param result - `await request(...)` 的返回值 `{ data, error, response }`
 * @param fallback - 接口未带文案时的兜底成功提示
 * @returns 是否成功（失败返回 false，不关弹窗）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function notifyOnceSuccessFromFlatResult(result: unknown, fallback: string): boolean {
  if (isFlatRequestFailed(result)) {
    return false;
  }
  if (!isNil(result) && typeof result === 'object') {
    const r = result as { response?: unknown };
    if (!isNil(r.response) && typeof r.response === 'object') {
      const text = getResponseMsg(r.response, '').trim();
      window.$message?.success?.(text || fallback);
      return true;
    }
  }
  window.$message?.success?.(fallback);
  return true;
}
