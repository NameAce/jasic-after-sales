import { computed, reactive, ref } from 'vue';
import {
  type WorkOrderHqSiteSummaryVO,
  type WorkOrderStatusCountVO,
  countWorkOrderStatus,
  listHqSiteSummary
} from '@/service/api';

/**
 * 总部看板共享数据：网点汇总、状态统计与转单量。
 * 总部账号优先用 hq-site-summary；否则或接口不可用时降级为 status-count。
 */
const state = reactive({
  loaded: false,
  loading: false,
  sites: [] as WorkOrderHqSiteSummaryVO[],
  statusRows: [] as WorkOrderStatusCountVO[],
  transferCount: 0,
  /** 是否成功拉取网点汇总（总部且接口未因非总部被拒） */
  hasSiteSummary: false
});

function toCount(value: unknown) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
}

function buildStatusMap(rows: WorkOrderStatusCountVO[]) {
  const map: Record<string, number> = {};
  for (const row of rows) {
    if (row?.mainStatus) {
      map[row.mainStatus] = toCount(row.countNum);
    }
  }
  return map;
}

function getStatusCount(map: Record<string, number>, key: string) {
  return map[key] ?? 0;
}

export function useHqDashboard() {
  const loadError = ref(false);

  const statusMap = computed(() => buildStatusMap(state.statusRows));

  const hasSiteData = computed(() => state.hasSiteSummary && state.sites.length > 0);

  /** 有网点数据或状态统计即可展示看板区 */
  const showDashboard = computed(() => {
    if (!state.loaded) return false;
    return hasSiteData.value || state.statusRows.length > 0;
  });

  const kpis = computed(() => {
    if (hasSiteData.value) {
      let waitAcceptCount = 0;
      let inProgressCount = 0;
      let totalCount = 0;

      for (const site of state.sites) {
        totalCount += toCount(site.totalCount);
        waitAcceptCount += toCount(site.waitAcceptCount);
        inProgressCount += toCount(site.inProgressCount);
      }

      return {
        mode: 'site' as const,
        siteCount: state.sites.length,
        totalCount,
        waitAcceptCount,
        inProgressCount,
        transferCount: state.transferCount
      };
    }

    const map = statusMap.value;
    return {
      mode: 'status' as const,
      siteCount: 0,
      totalCount: getStatusCount(map, 'ALL'),
      waitAcceptCount: getStatusCount(map, 'PENDING_ASSIGN') + getStatusCount(map, 'PENDING_TECH_ACCEPT'),
      inProgressCount: getStatusCount(map, 'IN_PROGRESS'),
      transferCount: state.transferCount
    };
  });

  const sitesByWaitAccept = computed(() =>
    [...state.sites].sort((a, b) => toCount(b.waitAcceptCount) - toCount(a.waitAcceptCount))
  );

  const sitesByTotal = computed(() => [...state.sites].sort((a, b) => toCount(b.totalCount) - toCount(a.totalCount)));

  /** 供降级柱状图使用的状态分布（排除 ALL） */
  const statusChartItems = computed(() => {
    const labels: Record<string, string> = {
      PENDING_ASSIGN: '待派单',
      PENDING_TECH_ACCEPT: '待接单',
      IN_PROGRESS: '维修中',
      COMPLETED: '已完成',
      CLOSED: '已关闭'
    };
    const order = ['PENDING_ASSIGN', 'PENDING_TECH_ACCEPT', 'IN_PROGRESS', 'COMPLETED', 'CLOSED'];
    const map = statusMap.value;

    return order
      .map(key => ({
        key,
        label: labels[key] || key,
        value: getStatusCount(map, key)
      }))
      .filter(item => item.value > 0);
  });

  async function loadHqDashboard(force = false) {
    if (state.loading) return;
    if (state.loaded && !force) return;

    state.loading = true;
    loadError.value = false;

    try {
      const [statusRes, transferRes] = await Promise.all([
        countWorkOrderStatus({ viewScope: 'ALL' }),
        countWorkOrderStatus({ viewScope: 'CURRENT', hasTransfer: 1 })
      ]);

      state.statusRows = Array.isArray(statusRes.data) ? statusRes.data : [];

      const transferRows = Array.isArray(transferRes.data) ? transferRes.data : [];
      state.transferCount = transferRows.reduce((sum, row) => {
        if (row?.mainStatus === 'ALL') return sum + toCount(row.countNum);
        return sum;
      }, 0);
    } catch {
      state.statusRows = [];
      state.transferCount = 0;
      loadError.value = true;
    }

    try {
      const siteRes = await listHqSiteSummary();
      state.sites = Array.isArray(siteRes.data) ? siteRes.data : [];
      state.hasSiteSummary = state.sites.length > 0;
    } catch {
      state.sites = [];
      state.hasSiteSummary = false;
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
    kpis,
    statusMap,
    statusChartItems,
    sitesByWaitAccept,
    sitesByTotal,
    loadHqDashboard
  };
}
