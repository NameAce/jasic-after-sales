/**
 * 请求层共享：Authorization 头、refreshToken 串行刷新、错误消息去重等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { fetchRefreshToken } from '../api';
import type { RequestInstanceState } from './type';

/**
 * 作用：从本地存储读取 token 并格式化为请求头 Authorization 值（与 Sa-Token 约定一致，不加 Bearer）。
 * @returns {string | null} 鉴权头内容，未登录为 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
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
 * @returns {boolean}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function isAuthRefreshTokenEnabled() {
  return import.meta.env.VITE_AUTH_REFRESH_TOKEN === 'Y';
}

/**
 * 作用：调用刷新接口更新 token；未开启刷新或无 refreshToken 时清空登录态。
 * @returns {Promise<boolean>} 刷新成功为 true，否则 false
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
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
 * @param state 请求实例状态
 * @returns {Promise<boolean>} 是否刷新成功
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
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
 * @param state 请求实例状态（维护 toastErrMsgStack）
 * @param message 展示给用户的消息
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
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
 * @param response 原始响应对象
 * @param fallback 无文案时的默认值
 * @returns {string} 后端消息或 fallback
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
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
 * @修改时间 2026-05-14
 */
export function getFlatResponseMsg(flatResult: unknown, fallback = ''): string {
  if (flatResult == null) return fallback;
  if (typeof flatResult === 'string') {
    const t = flatResult.trim();
    return t || fallback;
  }
  if (typeof flatResult !== 'object') return fallback;

  const o = flatResult as Record<string, unknown>;
  /** 扁平请求失败时不再从 response 取文案，避免业务层误把错误 msg 当成功提示
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  if ('error' in o && o.error != null && o.error !== false) {
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
 * @修改时间 2026-05-14
 */
export function getFlatErrorMsg(flatResult: unknown, fallback = ''): string {
  if (flatResult == null || typeof flatResult !== 'object') return fallback;
  const o = flatResult as Record<string, unknown>;
  if (o.response != null) {
    const fromResponse = getResponseMsg(o.response, '');
    if (fromResponse) return fromResponse;
  }
  for (const key of ['msg', 'message'] as const) {
    const v = o[key];
    if (typeof v === 'string' && v.trim()) return v.trim();
  }
  if (o.data != null && typeof o.data === 'object') {
    const d = o.data as Record<string, unknown>;
    const nested = [d.msg, d.message].find(x => typeof x === 'string' && String(x).trim());
    if (typeof nested === 'string') return nested.trim();
  }
  return fallback;
}

/**
 * 作用：扁平请求 `request()` 成功后弹一次 `success`，**优先接口返回的 `msg` / `message`**；失败时不弹（错误已在 `onBackendFail` 中通过 `showErrorMsg` 提示）。
 *
 * @param result - `await request(...)` 的返回值 `{ data, error, response }`
 * @param fallback - 接口未带文案时的兜底
 * @returns {boolean} 是否成功（无 `error`）；可用于失败后不再关抽屉/刷新
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function notifyOnceSuccessFromFlatResult(result: unknown, fallback: string): boolean {
  if (result != null && typeof result === 'object') {
    const r = result as { error?: unknown; response?: unknown };
    if (r.error != null && r.error !== false) {
      return false;
    }
    if (r.response != null && typeof r.response === 'object') {
      const text = getResponseMsg(r.response, '').trim();
      window.$message?.success?.(text || fallback);
      return true;
    }
  }
  window.$message?.success?.(fallback);
  return true;
}
