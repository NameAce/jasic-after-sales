import { computed, reactive, ref } from 'vue';
import {
  type DashboardHistoryTodoVO,
  type DashboardSiteRankVO,
  type DashboardSiteSummaryVO,
  type DashboardTrend7dVO,
  type DashboardWorkOrderStatusVO,
  type HqDashboardOverviewVO,
  getHqDashboardHome
} from '@/service/api';
import { buildStatusChartItems, toDashboardCount } from './dashboard-helpers';

/**
 * 总部看板共享数据：网点汇总、状态统计、趋势与动态。
 * 数据来自 `/dashboard/hq/home`，不再由前端拼装多个旧接口。
 */
const state = reactive({
  loaded: false,
  loading: false,
  overview: null as HqDashboardOverviewVO | null,
  workOrderStatus: null as DashboardWorkOrderStatusVO | null,
  trend7d: null as DashboardTrend7dVO | null,
  siteSummary: null as DashboardSiteSummaryVO | null,
  siteWaitAcceptRank: [] as DashboardSiteRankVO[],
  latestHistoryTodos: [] as DashboardHistoryTodoVO[]
});

/** 拉取失败时重置为安全空值 */
function resetHqDashboardState() {
  state.overview = {
    activeTodoCount: 0,
    historyTodoCount: 0,
    workOrderTotal: 0,
    transferCount: 0
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
  state.siteSummary = {
    siteCount: 0,
    totalCount: 0,
    waitAcceptCount: 0,
    inProgressCount: 0,
    completedCount: 0
  };
  state.siteWaitAcceptRank = [];
  state.latestHistoryTodos = [];
}

/**
 * 将总部首页接口响应写入共享 state，并对缺失节点做安全兜底。
 */
function applyHqHomeData(data: Awaited<ReturnType<typeof getHqDashboardHome>>['data']) {
  state.overview = data?.overview ?? null;
  state.workOrderStatus = data?.workOrderStatus ?? null;
  state.trend7d = data?.trend7d ?? null;
  state.siteSummary = data?.siteSummary ?? null;
  state.siteWaitAcceptRank = Array.isArray(data?.siteWaitAcceptRank) ? data.siteWaitAcceptRank : [];
  state.latestHistoryTodos = Array.isArray(data?.latestHistoryTodos) ? data.latestHistoryTodos : [];
}

export function useHqDashboard() {
  const loadError = ref(false);

  const overview = computed(() => state.overview);
  const workOrderStatus = computed(() => state.workOrderStatus);
  const trend7d = computed(() => state.trend7d);
  const siteSummary = computed(() => state.siteSummary);
  const latestHistoryTodos = computed(() => state.latestHistoryTodos);

  const hasSiteData = computed(() => toDashboardCount(state.siteSummary?.siteCount) > 0);

  /** 有网点汇总或工单状态数据即可展示看板区 */
  const showDashboard = computed(() => {
    if (!state.loaded) return false;
    return hasSiteData.value || toDashboardCount(state.workOrderStatus?.all) > 0;
  });

  const kpis = computed(() => {
    if (hasSiteData.value && state.siteSummary) {
      return {
        mode: 'site' as const,
        siteCount: toDashboardCount(state.siteSummary.siteCount),
        totalCount: toDashboardCount(state.siteSummary.totalCount),
        waitAcceptCount: toDashboardCount(state.siteSummary.waitAcceptCount),
        inProgressCount: toDashboardCount(state.siteSummary.inProgressCount),
        completedCount: toDashboardCount(state.siteSummary.completedCount),
        transferCount: toDashboardCount(state.overview?.transferCount)
      };
    }

    const status = state.workOrderStatus;
    return {
      mode: 'status' as const,
      siteCount: 0,
      totalCount: toDashboardCount(status?.all),
      waitAcceptCount: toDashboardCount(status?.pendingAssign) + toDashboardCount(status?.pendingTechAccept),
      inProgressCount: toDashboardCount(status?.inProgress),
      completedCount: toDashboardCount(status?.completed),
      transferCount: toDashboardCount(state.overview?.transferCount)
    };
  });

  /** 网点待接单排行（接口已按待接单数排序） */
  const sitesByWaitAccept = computed(() => [...state.siteWaitAcceptRank]);

  const sitesByTotal = computed(() =>
    [...state.siteWaitAcceptRank].sort((a, b) => toDashboardCount(b.totalCount) - toDashboardCount(a.totalCount))
  );

  /** 无网点汇总时降级展示的状态分布图数据 */
  const statusChartItems = computed(() => buildStatusChartItems(state.workOrderStatus));

  /**
   * 拉取总部首页聚合数据；同一会话内默认只请求一次，force 可强制刷新。
   */
  async function loadHqDashboard(force = false) {
    if (state.loading) return;
    if (state.loaded && !force) return;

    state.loading = true;
    loadError.value = false;

    try {
      const res = await getHqDashboardHome();
      applyHqHomeData(res.data);
    } catch {
      resetHqDashboardState();
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
    showDashboard,
    hasSiteData,
    overview,
    workOrderStatus,
    trend7d,
    siteSummary,
    latestHistoryTodos,
    kpis,
    statusChartItems,
    sitesByWaitAccept,
    sitesByTotal,
    loadHqDashboard
  };
}
