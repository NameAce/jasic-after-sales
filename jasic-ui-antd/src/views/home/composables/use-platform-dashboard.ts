import { computed, reactive, ref } from 'vue';
import { getPlatformDashboardHome } from '@/service/api';
import type { HomeSectionVO } from '@/service/api';

/**
 * 平台超管看板共享数据：组织治理、账号治理、基础配置三块分区。
 * 数据来自 `/dashboard/platform/home`，不含工单与操作日志趋势。
 */
const state = reactive({
  loaded: false,
  loading: false,
  title: '治理看板',
  organization: null as HomeSectionVO | null,
  account: null as HomeSectionVO | null,
  basicConfig: null as HomeSectionVO | null
});

function resetPlatformDashboardState() {
  state.title = '治理看板';
  state.organization = { title: '组织治理', metrics: [] };
  state.account = { title: '账号治理', metrics: [] };
  state.basicConfig = { title: '基础配置', metrics: [] };
}

function applyPlatformHomeData(data: Awaited<ReturnType<typeof getPlatformDashboardHome>>['data']) {
  state.title = data?.title || '治理看板';
  state.organization = data?.organization ?? null;
  state.account = data?.account ?? null;
  state.basicConfig = data?.basicConfig ?? null;
}

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
