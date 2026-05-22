import { computed, reactive, ref } from 'vue';
import { getPlatformDashboardHome } from '@/service/api';
import type { HomeSectionVO } from '@/service/api';

/**
 * 平台超管看板共享数据：组织治理、账号治理、基础配置三块分区。
 * 数据来自 `/dashboard/platform/home`，不含工单与操作日志趋势。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const state = reactive({
  loaded: false,
  loading: false,
  title: '治理看板',
  organization: null as HomeSectionVO | null,
  account: null as HomeSectionVO | null,
  basicConfig: null as HomeSectionVO | null
});

/**
 * 作用：拉取失败时重置为安全空值。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetPlatformDashboardState() {
  state.title = '治理看板';
  state.organization = { title: '组织治理', metrics: [] };
  state.account = { title: '账号治理', metrics: [] };
  state.basicConfig = { title: '基础配置', metrics: [] };
}

/**
 * 作用：将平台治理首页接口响应写入模块级共享 state。
 * @param data - getPlatformDashboardHome 返回的 data 字段
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyPlatformHomeData(data: Awaited<ReturnType<typeof getPlatformDashboardHome>>['data']) {
  state.title = data?.title || '治理看板';
  state.organization = data?.organization ?? null;
  state.account = data?.account ?? null;
  state.basicConfig = data?.basicConfig ?? null;
}

/**
 * 作用：提供平台超管治理看板共享状态与加载方法（单例 state）。
 * @returns 组织/账号/基础配置分区及 loadPlatformDashboard 方法
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function usePlatformDashboard() {
  const loadError = ref(false);

  const title = computed(() => state.title);
  const organization = computed(() => state.organization);
  const account = computed(() => state.account);
  const basicConfig = computed(() => state.basicConfig);

  /** 横幅快捷统计：取组织治理前两项指标 */
  const bannerMetrics = computed(() => {
    const metrics = state.organization?.metrics || [];
    return metrics.slice(0, 2);
  });

  /**
   * 作用：拉取平台治理首页聚合数据；同一会话内默认只请求一次。
   * @param force - 为 true 时忽略 loaded 缓存
   * @returns Promise
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
      resetPlatformDashboardState();
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
    organization,
    account,
    basicConfig,
    bannerMetrics,
    loadPlatformDashboard
  };
}
