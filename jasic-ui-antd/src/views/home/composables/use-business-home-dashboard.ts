import { computed } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useHqDashboard } from './use-hq-dashboard';
import { useServiceDashboard } from './use-service-dashboard';

/**
 * 业务首页（总部 / 服务主体）共享数据门面。
 * 供 HeaderBanner、工单卡片、趋势图、动态列表等模块按当前主体类型选用对应看板数据。
 */
export function useBusinessHomeDashboard() {
  const authStore = useAuthStore();
  const isHqAccount = computed(() => authStore.userInfo.currentSubjectType === 'HQ');

  const hqDashboard = useHqDashboard();
  const serviceDashboard = useServiceDashboard();

  const loading = computed(() => (isHqAccount.value ? hqDashboard.loading.value : serviceDashboard.loading.value));
  const loaded = computed(() => (isHqAccount.value ? hqDashboard.loaded.value : serviceDashboard.loaded.value));
  const overview = computed(() => (isHqAccount.value ? hqDashboard.overview.value : serviceDashboard.overview.value));
  const workOrderStatus = computed(() =>
    isHqAccount.value ? hqDashboard.workOrderStatus.value : serviceDashboard.workOrderStatus.value
  );
  const trend7d = computed(() => (isHqAccount.value ? hqDashboard.trend7d.value : serviceDashboard.trend7d.value));
  const latestHistoryTodos = computed(() =>
    isHqAccount.value ? hqDashboard.latestHistoryTodos.value : serviceDashboard.latestHistoryTodos.value
  );

  /** 按当前主体拉取对应首页接口 */
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
    overview,
    workOrderStatus,
    trend7d,
    latestHistoryTodos,
    loadBusinessHomeDashboard
  };
}
