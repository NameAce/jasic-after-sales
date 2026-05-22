/**
 * 按钮级权限：根据 `authStore.userInfo.buttons` 判断当前用户是否具备指定权限码。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { useAuthStore } from '@/store/modules/auth';

/**
 * 作用：基于 `userInfo.buttons` 判断当前用户是否拥有指定按钮权限码。
 * @returns {{ hasAuth }} 校验函数
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useAuth() {
  const authStore = useAuthStore();
  // 判断当前用户是否拥有指定按钮权限码
  function hasAuth(codes: string | string[]) {
    if (!authStore.isLogin) {
      return false;
    }

    if (typeof codes === 'string') {
      return authStore.userInfo.buttons.includes(codes);
    }

    return codes.some(code => authStore.userInfo.buttons.includes(code));
  }

  /**
   * 作用：判断是否至少拥有一个按钮权限码（用于表格操作列是否展示等「任一即可」场景）。
   * @param codes - 单个权限码或权限码数组
   * @returns 拥有任一权限时返回 true
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  function hasAnyAuth(codes: string | string[]) {
    return hasAuth(codes);
  }

  return {
    hasAuth,
    hasAnyAuth
  };
}
