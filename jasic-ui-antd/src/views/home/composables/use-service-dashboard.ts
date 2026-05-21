import { computed, reactive, ref } from 'vue';
import {
  type DashboardHistoryTodoVO,
  type DashboardTrend7dVO,
  type DashboardWorkOrderStatusVO,
  type ServiceDashboardOverviewVO,
  getServiceDashboardHome
} from '@/service/api';

/**
 * 服务主体（网点等）看板共享数据。
 * 数据来自 `/dashboard/service/home`，供标准业务首页各模块复用。
 */
const state = reactive({
  loaded: false,
  loading: false,
  overview: null as ServiceDashboardOverviewVO | null,
  workOrderStatus: null as DashboardWorkOrderStatusVO | null,
  trend7d: null as DashboardTrend7dVO | null,
  latestHistoryTodos: [] as DashboardHistoryTodoVO[]
});

/** 拉取失败时重置为安全空值 */
function resetServiceDashboardState() {
  state.overview = {
    activeTodoCount: 0,
    historyTodoCount: 0,
    workOrderTotal: 0
  };
  state.workOrderStatus = {
    all: 0,
    pendingAssign: 0,
    pendingTechAccept: 0,
    inProgress: 0,
    completed: 0,
    closed: 0
  };
  state.trend7d = {
    dayKeys: [],
    createdWorkOrderCounts: [],
    activeTodoCounts: []
  };
  state.latestHistoryTodos = [];
}

/**
 * 将服务主体首页接口响应写入共享 state。
 */
function applyServiceHomeData(data: Awaited<ReturnType<typeof getServiceDashboardHome>>['data']) {
  state.overview = data?.overview ?? null;
  state.workOrderStatus = data?.workOrderStatus ?? null;
  state.trend7d = data?.trend7d ?? null;
  state.latestHistoryTodos = Array.isArray(data?.latestHistoryTodos) ? data.latestHistoryTodos : [];
}

export function useServiceDashboard() {
  const loadError = ref(false);

  const overview = computed(() => state.overview);
  const workOrderStatus = computed(() => state.workOrderStatus);
  const trend7d = computed(() => state.trend7d);
  const latestHistoryTodos = computed(() => state.latestHistoryTodos);

  /**
   * 拉取服务主体首页聚合数据；同一会话内默认只请求一次。
   * @param force 为 true 时忽略 loaded 缓存（页签栏刷新 remount 首页时须传 true）
   */
  async function loadServiceDashboard(force = false) {
    if (state.loading) return;
    if (state.loaded && !force) return;

    state.loading = true;
    loadError.value = false;

    try {
      const res = await getServiceDashboardHome();
      applyServiceHomeData(res.data);
    } catch {
      resetServiceDashboardState();
      loadError.value = true;
    } finally {
      state.loaded = true;
      state.loading = false;
    }
  }

  return {
    state,
    loadError,
    loading: computed(() => state.loading),
    loaded: computed(() => state.loaded),
    overview,
    workOrderStatus,
    trend7d,
    latestHistoryTodos,
    loadServiceDashboard
  };
}
