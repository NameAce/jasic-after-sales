import { computed, reactive, ref } from 'vue';
import { getPlatformDashboardHome } from '@/service/api';
import { toDashboardCount } from './dashboard-helpers';

/**
 * 平台超管看板共享数据：组织规模、主体类型分布、操作日志趋势。
 * 数据来自 `/dashboard/platform/home`，不再由前端拼装分页列表二次统计。
 */
const state = reactive({
  loaded: false,
  loading: false,
  companyTotal: 0,
  companyEnabled: 0,
  userTotal: 0,
  roleTotal: 0,
  notifySceneTotal: 0,
  subjectCounts: { PLATFORM: 0, HQ: 0, SERVICE: 0 } as Record<string, number>,
  operLogDayKeys: [] as string[],
  operLogDailyCounts: [] as number[],
  operLogFailedCount: 0
});

/** 平台看板拉取失败时重置组织类指标 */
function resetPlatformOrgMetrics() {
  state.companyTotal = 0;
  state.companyEnabled = 0;
  state.userTotal = 0;
  state.roleTotal = 0;
  state.notifySceneTotal = 0;
  state.subjectCounts = { PLATFORM: 0, HQ: 0, SERVICE: 0 };
}

/** 操作日志趋势拉取失败时清空图表数据 */
function resetPlatformOperLogMetrics() {
  state.operLogDayKeys = [];
  state.operLogDailyCounts = [];
  state.operLogFailedCount = 0;
}

/**
 * 将平台首页接口响应写入共享 state。
 */
function applyPlatformHomeData(data: Awaited<ReturnType<typeof getPlatformDashboardHome>>['data']) {
  const overview = data?.overview;
  state.companyTotal = toDashboardCount(overview?.companyTotal);
  state.companyEnabled = toDashboardCount(overview?.enabledCompanyTotal);
  state.userTotal = toDashboardCount(overview?.userTotal);
  state.roleTotal = toDashboardCount(overview?.roleTotal);
  state.notifySceneTotal = toDashboardCount(overview?.notifySceneTotal);

  const distribution = data?.subjectTypeDistribution;
  state.subjectCounts = {
    PLATFORM: toDashboardCount(distribution?.platformCount),
    HQ: toDashboardCount(distribution?.hqCount),
    SERVICE: toDashboardCount(distribution?.serviceCount)
  };

  const operTrend = data?.operLogTrend7d;
  const dayKeys = Array.isArray(operTrend?.dayKeys) ? operTrend.dayKeys : [];
  const counts = Array.isArray(operTrend?.operLogCounts) ? operTrend.operLogCounts : [];
  state.operLogDayKeys = dayKeys;
  state.operLogDailyCounts = dayKeys.map((_, index) => toDashboardCount(counts[index]));
  state.operLogFailedCount = toDashboardCount(operTrend?.failedCount);
}

export function usePlatformDashboard() {
  const loadError = ref(false);

  const showDashboard = computed(() => state.loaded);

  const kpis = computed(() => ({
    companyTotal: state.companyTotal,
    companyEnabled: state.companyEnabled,
    userTotal: state.userTotal,
    roleTotal: state.roleTotal,
    notifySceneTotal: state.notifySceneTotal
  }));

  const subjectChartItems = computed(() => {
    const labels: Record<string, string> = {
      PLATFORM: '平台',
      HQ: '总部',
      SERVICE: '服务网点'
    };
    return (['PLATFORM', 'HQ', 'SERVICE'] as const)
      .map(key => ({
        key,
        label: labels[key] || key,
        value: state.subjectCounts[key] || 0
      }))
      .filter(item => item.value > 0);
  });

  /**
   * 拉取平台首页聚合数据；同一会话内默认只请求一次，force 可强制刷新。
   */
  async function loadPlatformDashboard(force = false) {
    if (state.loading) return;
    if (state.loaded && !force) return;

    state.loading = true;
    loadError.value = false;

    try {
      const res = await getPlatformDashboardHome();
      applyPlatformHomeData(res.data);
    } catch {
      resetPlatformOrgMetrics();
      resetPlatformOperLogMetrics();
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
    kpis,
    subjectChartItems,
    operLogDayKeys: computed(() => state.operLogDayKeys),
    operLogDailyCounts: computed(() => state.operLogDailyCounts),
    operLogFailedCount: computed(() => state.operLogFailedCount),
    loadPlatformDashboard
  };
}
