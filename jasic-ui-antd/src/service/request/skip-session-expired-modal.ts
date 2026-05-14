import { router } from '@/router';

/**
 * 命中 `VITE_SERVICE_MODAL_LOGOUT_CODES`（如 A0100）时，对这些请求 URL 不弹「登录已过期」全屏 Modal。
 *
 * 典型原因：后端在登录失败、主动登出、刷新 token 等场景也会返回与「未登录」相同的业务码，
 * 若仍弹不可点遮罩的 Modal，会与登录页/登出跳转叠层，表现为整页卡住。
 */
const SESSION_EXPIRED_MODAL_SKIP_URL_PARTS = [
  'auth/logout',
  'auth/login',
  'auth/refresh-token',
  'auth/mp-login',
  'auth/mp-bind-login',
  'auth/mp-bind-confirm'
] as const;

/**
 * @param requestUrl - axios `config.url`（可能为相对路径）
 * @returns 为 true 时不应弹会话过期 Modal，改由后续逻辑（如 toast）或调用方自行处理
 */
export function shouldSkipSessionExpiredModalForUrl(requestUrl: string): boolean {
  const u = requestUrl.toLowerCase();
  return SESSION_EXPIRED_MODAL_SKIP_URL_PARTS.some(part => u.includes(part));
}

/**
 * 登录路由上任意接口返回「会话失效」码时，只应用表单/Toast 等提示，不弹全屏重新登录 Modal。
 */
export function shouldSkipSessionExpiredModalOnLoginRoute(): boolean {
  return String(router.currentRoute.value.name || '') === 'login';
}
