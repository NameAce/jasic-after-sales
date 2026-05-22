import { computed } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useHqDashboard } from './use-hq-dashboard';
import { useServiceDashboard } from './use-service-dashboard';

/**
 * 业务首页（总部 / 服务主体）共享数据门面（遗留模块兼容）。
 * 新首页请直接使用 useHqDashboard / useServiceDashboard。
 */
export function useBusinessHomeDashboard() {
  const authStore = useAuthStore();
  const isHqAccount = computed(() => authStore.userInfo.currentSubjectType === 'HQ');

  const hqDashboard = useHqDashboard();
  const serviceDashboard = useServiceDashboard();

  const loading = computed(() => (isHqAccount.value ? hqDashboard.loading.value : serviceDashboard.loading.value));
  const loaded = computed(() => (isHqAccount.value ? hqDashboard.loaded.value : serviceDashboard.loaded.value));

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
