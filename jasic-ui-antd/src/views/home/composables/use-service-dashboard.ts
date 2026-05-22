import { computed, reactive, ref } from 'vue';
import { type HomeEntryVO, type HomeSectionVO, type HomeTrendVO, getServiceDashboardHome } from '@/service/api';
import { toDashboardCount } from './dashboard-helpers';

/**
 * 服务主体（网点等）看板共享数据。
 * 数据来自 `/dashboard/service/home`，供服务工作台首页各模块复用。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const state = reactive({
  loaded: false,
  loading: false,
  title: '服务工作台',
  currentPool: null as HomeSectionVO | null,
  transfer: null as HomeSectionVO | null,
  historyEntry: null as HomeEntryVO | null,
  trend: null as HomeTrendVO | null
});

/**
 * 作用：拉取失败或异常时重置为安全空值，避免页面残留上一次成功请求的数据。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetServiceDashboardState() {
  state.title = '服务工作台';
  state.currentPool = { title: '当前服务公司承接工单', metrics: [] };
  state.transfer = { title: '已转出', metrics: [] };
  state.historyEntry = null;
  state.trend = { title: '近 7 天事件趋势', days: [], series: [] };
}

/**
 * 作用：将服务主体首页接口响应写入模块级共享 state。
 * @param data - getServiceDashboardHome 返回的 data 字段
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyServiceHomeData(data: Awaited<ReturnType<typeof getServiceDashboardHome>>['data']) {
  state.title = data?.title || '服务工作台';
  state.currentPool = data?.currentPool ?? null;
  state.transfer = data?.transfer ?? null;
  state.historyEntry = data?.historyEntry ?? null;
  state.trend = data?.trend ?? null;
}

/**
 * 作用：提供服务主体首页看板共享状态与加载方法（单例 state，多组件复用同一份数据）。
 * @returns 标题、各分区、趋势、加载状态及 loadServiceDashboard 方法
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useServiceDashboard() {
  const loadError = ref(false);

  const title = computed(() => state.title);
  const currentPool = computed(() => state.currentPool);
  const transfer = computed(() => state.transfer);
  const historyEntry = computed(() => state.historyEntry);
  const trend = computed(() => state.trend);

  // 已转出数量：取 transfer 分区首个指标，供横幅等轻量展示
  const transferMetric = computed(() => state.transfer?.metrics?.[0]);
  const transferOutCount = computed(() => toDashboardCount(transferMetric.value?.value));

  /**
   * 作用：拉取服务主体首页聚合数据；同一会话内默认只请求一次。
   * @param force - 为 true 时忽略 loaded 缓存（页签栏刷新 remount 首页时须传 true）
   * @returns Promise，请求结束后更新 state
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
    title,
    currentPool,
    transfer,
    historyEntry,
    trend,
    transferMetric,
    transferOutCount,
    loadServiceDashboard
  };
}
