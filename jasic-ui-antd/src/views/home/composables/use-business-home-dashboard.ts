import { computed } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useHqDashboard } from './use-hq-dashboard';
import { useServiceDashboard } from './use-service-dashboard';

/**
 * 作用：业务首页（总部 / 服务主体）共享数据门面，按当前主体类型选择对应看板 composable（遗留模块兼容）。
 * 新首页请直接使用 useHqDashboard / useServiceDashboard。
 * @returns 是否总部账号、加载状态、统一 load 方法及两套看板引用
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useBusinessHomeDashboard() {
  const authStore = useAuthStore();
  const isHqAccount = computed(() => authStore.userInfo.currentSubjectType === 'HQ');

  const hqDashboard = useHqDashboard();
  const serviceDashboard = useServiceDashboard();

  const loading = computed(() => (isHqAccount.value ? hqDashboard.loading.value : serviceDashboard.loading.value));
  const loaded = computed(() => (isHqAccount.value ? hqDashboard.loaded.value : serviceDashboard.loaded.value));

  /**
   * 作用：按主体类型拉取对应首页聚合数据。
   * @param force - 是否强制刷新（忽略 loaded 缓存）
   * @returns Promise
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  function loadBusinessHomeDashboard(force = false) {
    if (isHqAccount.value) {
      return hqDashboard.loadHqDashboard(force);
    }
    return serviceDashboard.loadServiceDashboard(force);
  }

  return {
    isHqAccount,
    loading,
    loaded,
    loadBusinessHomeDashboard,
    hqDashboard,
    serviceDashboard
  };
}
