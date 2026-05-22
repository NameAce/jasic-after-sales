import { computed, reactive, ref } from 'vue';
import { type HomeSectionVO, type HomeTrendVO, getHqDashboardHome } from '@/service/api';
import { toDashboardCount } from './dashboard-helpers';

/**
 * 总部看板共享数据：当前承接工单池、已转出、近七天事件趋势。
 * 数据来自 `/dashboard/hq/home`。
 */
const state = reactive({
  loaded: false,
  loading: false,
  title: '调度看板',
  workOrderPool: null as HomeSectionVO | null,
  transfer: null as HomeSectionVO | null,
  trend: null as HomeTrendVO | null
});

function resetHqDashboardState() {
  state.title = '调度看板';
  state.workOrderPool = { title: '当前总部承接工单池', metrics: [] };
  state.transfer = { title: '已转出', metrics: [] };
  state.trend = { title: '近 7 天事件趋势', days: [], series: [] };
}

function applyHqHomeData(data: Awaited<ReturnType<typeof getHqDashboardHome>>['data']) {
  state.title = data?.title || '调度看板';
  state.workOrderPool = data?.workOrderPool ?? null;
  state.transfer = data?.transfer ?? null;
  state.trend = data?.trend ?? null;
}

export function useHqDashboard() {
  const loadError = ref(false);

  const title = computed(() => state.title);
  const workOrderPool = computed(() => state.workOrderPool);
  const transfer = computed(() => state.transfer);
  const trend = computed(() => state.trend);

  const transferMetric = computed(() => state.transfer?.metrics?.[0]);
  const transferOutCount = computed(() => toDashboardCount(transferMetric.value?.value));

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
    title,
    workOrderPool,
    transfer,
    trend,
    transferMetric,
    transferOutCount,
    loadHqDashboard
  };
}
