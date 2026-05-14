/**
 * 请求层共享：Authorization 头、refreshToken 串行刷新、错误消息去重等。
 */
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { fetchRefreshToken } from '../api';
import type { RequestInstanceState } from './type';

/**
 * 作用：从本地存储读取 token 并格式化为请求头 Authorization 值（与 Sa-Token 约定一致，不加 Bearer）。
 * @returns {string | null} 鉴权头内容，未登录为 null
 */
export function getAuthorization() {
  const token = localStg.get('token');
  // 与 jasic-ui / Sa-Token（token-name: Authorization，无 token-prefix）一致：请求头为原始 token，不加 Bearer 前缀
  const Authorization = token ? String(token) : null;

  return Authorization;
}

/**
 * 作用：是否启用 refreshToken 刷新流程（由环境变量控制）。
 * @returns {boolean}
 */
function isAuthRefreshTokenEnabled() {
  return import.meta.env.VITE_AUTH_REFRESH_TOKEN === 'Y';
}

/**
 * 作用：调用刷新接口更新 token；未开启刷新或无 refreshToken 时清空登录态。
 * @returns {Promise<boolean>} 刷新成功为 true，否则 false
 */
async function handleRefreshToken() {
  const { resetStore } = useAuthStore();

  if (!isAuthRefreshTokenEnabled()) {
    resetStore();
    return false;
  }

  const rToken = localStg.get('refreshToken') || '';
  if (!rToken) {
    resetStore();
    return false;
  }

  const { error, data } = await fetchRefreshToken(rToken);
  if (!error) {
    localStg.set('token', data.token);
    if (data.refreshToken !== undefined && data.refreshToken !== null && data.refreshToken !== '') {
      localStg.set('refreshToken', data.refreshToken);
    }
    return true;
  }

  resetStore();

  return false;
}

/**
 * 作用：401/过期时串行刷新 token，避免并发重复刷新（通过 state.refreshTokenFn 复用同一 Promise）。
 * @param state 请求实例状态
 * @returns {Promise<boolean>} 是否刷新成功
 */
export async function handleExpiredRequest(state: RequestInstanceState) {
  if (!state.refreshTokenFn) {
    state.refreshTokenFn = handleRefreshToken();
  }

  const success = await state.refreshTokenFn;

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
 * 作用：从 Axios 响应体中解析后端 msg/message 字段。
 * @param response 原始响应对象
 * @param fallback 无文案时的默认值
 * @returns {string} 后端消息或 fallback
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
