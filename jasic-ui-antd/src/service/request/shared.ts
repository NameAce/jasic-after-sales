import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { fetchRefreshToken } from '../api';
import type { RequestInstanceState } from './type';

export function getAuthorization() {
  const token = localStg.get('token');
  // 与 jasic-ui / Sa-Token（token-name: Authorization，无 token-prefix）一致：请求头为原始 token，不加 Bearer 前缀
  const Authorization = token ? String(token) : null;

  return Authorization;
}

function isAuthRefreshTokenEnabled() {
  return import.meta.env.VITE_AUTH_REFRESH_TOKEN === 'Y';
}

/** refresh token（受 VITE_AUTH_REFRESH_TOKEN 开关约束，与 jasic 无 refresh 接口时对齐） */
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

export function showErrorMsg(state: RequestInstanceState, message: string) {
  if (!state.errMsgStack?.length) {
    state.errMsgStack = [];
  }

  const isExist = state.errMsgStack.includes(message);

  if (!isExist) {
    state.errMsgStack.push(message);

    window.$message?.error(message, 1.5, () => {
      state.errMsgStack = state.errMsgStack.filter(msg => msg !== message);

      setTimeout(() => {
        state.errMsgStack = [];
      }, 5000);
    });
  }
}

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
