import { computed, reactive, ref } from 'vue';
import { type HomeSectionVO, type HomeTrendVO, getHqDashboardHome } from '@/service/api';
import { toDashboardCount } from './dashboard-helpers';

/**
 * 总部看板共享数据：当前承接工单池、已转出、近七天事件趋势。
 * 数据来自 `/dashboard/hq/home`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const state = reactive({
  loaded: false,
  loading: false,
  title: '调度看板',
  workOrderPool: null as HomeSectionVO | null,
  transfer: null as HomeSectionVO | null,
  trend: null as HomeTrendVO | null
});

/**
 * 作用：拉取失败时重置为安全空值，避免残留旧数据。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetHqDashboardState() {
  state.title = '调度看板';
  state.workOrderPool = { title: '当前总部承接工单池', metrics: [] };
  state.transfer = { title: '已转出', metrics: [] };
  state.trend = { title: '近 7 天事件趋势', days: [], series: [] };
}

/**
 * 作用：将总部首页接口响应写入模块级共享 state。
 * @param data - getHqDashboardHome 返回的 data 字段
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyHqHomeData(data: Awaited<ReturnType<typeof getHqDashboardHome>>['data']) {
  state.title = data?.title || '调度看板';
  state.workOrderPool = data?.workOrderPool ?? null;
  state.transfer = data?.transfer ?? null;
  state.trend = data?.trend ?? null;
}

/**
 * 作用：提供总部调度看板共享状态与加载方法（单例 state）。
 * @returns 标题、工单池、已转出、趋势及 loadHqDashboard 方法
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useHqDashboard() {
  const loadError = ref(false);

  const title = computed(() => state.title);
  const workOrderPool = computed(() => state.workOrderPool);
  const transfer = computed(() => state.transfer);
  const trend = computed(() => state.trend);

  const transferMetric = computed(() => state.transfer?.metrics?.[0]);
  const transferOutCount = computed(() => toDashboardCount(transferMetric.value?.value));

  /**
   * 作用：拉取总部首页聚合数据；同一会话内默认只请求一次。
   * @param force - 为 true 时忽略 loaded 缓存（页签刷新 remount 时传 true）
   * @returns Promise
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
    title,
    workOrderPool,
    transfer,
    trend,
    transferMetric,
    transferOutCount,
    loadHqDashboard
  };
}
