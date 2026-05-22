import { router } from '@/router';

/**
 * 命中 `VITE_SERVICE_MODAL_LOGOUT_CODES`（如 A0100）时，对这些请求 URL 不弹「登录已过期」全屏 Modal。
 *
 * 典型原因：后端在登录失败、主动登出、刷新 token 等场景也会返回与「未登录」相同的业务码，
 * 若仍弹不可点遮罩的 Modal，会与登录页/登出跳转叠层，表现为整页卡住。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：判断当前请求 URL 是否应跳过「登录已过期」全屏 Modal（登出/登录/刷新等接口）。
 * @param requestUrl - axios `config.url`（可能为相对路径）
 * @returns 为 true 时不弹会话过期 Modal，改由 toast 或调用方处理
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function shouldSkipSessionExpiredModalForUrl(requestUrl: string): boolean {
  const u = requestUrl.toLowerCase();
  return SESSION_EXPIRED_MODAL_SKIP_URL_PARTS.some(part => u.includes(part));
}

/**
 * 作用：登录路由上返回会话失效码时，仅用表单/Toast 提示，不弹全屏重新登录 Modal。
 * @returns 当前是否在 login 路由
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function shouldSkipSessionExpiredModalOnLoginRoute(): boolean {
  return String(router.currentRoute.value.name || '') === 'login';
}
