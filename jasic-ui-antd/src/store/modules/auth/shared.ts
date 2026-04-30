/**
 * 鉴权 store 共享：token 读取与登出时清理本地凭证。
 */
import { localStg } from '@/utils/storage';

/**
 * 作用：读取当前登录 token 字符串（无则空串）。
 * @returns {string}
 */
export function getToken() {
  return localStg.get('token') || '';
}

/**
 * 作用：清除本地 token 与 refreshToken，与登出流程对齐。
 * @returns {void}
 */
export function clearAuthStorage() {
  localStg.remove('token');
  localStg.remove('refreshToken');
}
