import { computed, reactive, ref } from 'vue';
import {
  type SysCompanyType,
  listCompany,
  listCompanyType,
  listNotifyScene,
  listOperLog,
  listRole,
  listUser
} from '@/service/api';

/**
 * 平台超管看板共享数据：组织规模、操作日志趋势。
 * 仅用于 subjectType=PLATFORM 的首页，与总部工单看板 composable 隔离。
 */
const state = reactive({
  loaded: false,
  loading: false,
  companyTotal: 0,
  companyEnabled: 0,
  userTotal: 0,
  roleTotal: 0,
  /** 通知场景配置总数 */
  notifySceneTotal: 0,
  subjectCounts: { PLATFORM: 0, HQ: 0, SERVICE: 0 } as Record<string, number>,
  operLogDayKeys: [] as string[],
  operLogDailyCounts: [] as number[],
  operLogFailedCount: 0
});

function toCount(value: unknown) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
}

/**
 * 生成向前连续若干天的日期键（yyyy-MM-dd）。
 */
function buildDayKeys(size = 7) {
  const result: string[] = [];
  const now = new Date();
  for (let i = size - 1; i >= 0; i -= 1) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    const y = d.getFullYear();
    const m = `${d.getMonth() + 1}`.padStart(2, '0');
    const day = `${d.getDate()}`.padStart(2, '0');
    result.push(`${y}-${m}-${day}`);
  }
  return result;
}

function toDateKey(input: unknown) {
  const text = String(input || '');
  return text.length >= 10 ? text.slice(0, 10) : '';
}

function formatRangeEnd(dayKey: string) {
  return `${dayKey} 23:59:59`;
}

/** 组织规模等指标拉取失败时重置为 0 */
function resetPlatformOrgMetrics() {
  state.companyTotal = 0;
  state.companyEnabled = 0;
  state.userTotal = 0;
  state.roleTotal = 0;
  state.notifySceneTotal = 0;
  state.subjectCounts = { PLATFORM: 0, HQ: 0, SERVICE: 0 };
}

/**
 * 拉取并写入平台看板组织规模相关指标。
 */
async function fetchPlatformOrgMetrics() {
  const [companyTypeRes, companyRes, userRes, roleRes, notifySceneRes] = await Promise.all([
    listCompanyType(),
    listCompany({ pageNum: 1, pageSize: 500 }),
    listUser({ pageNum: 1, pageSize: 1 }),
    listRole({ pageNum: 1, pageSize: 1 }),
    listNotifyScene({ pageNum: 1, pageSize: 1 })
  ]);

  const typeRows = Array.isArray(companyTypeRes.data) ? companyTypeRes.data : [];
  const typeSubjectMap: Record<string, string> = {};
  for (const row of typeRows as SysCompanyType[]) {
    if (row?.typeCode && row.subjectType) {
      typeSubjectMap[row.typeCode] = row.subjectType;
    }
  }

  const companyRows = Array.isArray(companyRes.data?.records) ? companyRes.data.records : [];
  state.companyTotal = toCount(companyRes.data?.total) || companyRows.length;
  state.companyEnabled = companyRows.filter(row => Number(row?.status) === 1).length;

  const subjectCounts: Record<string, number> = { PLATFORM: 0, HQ: 0, SERVICE: 0 };
  for (const row of companyRows) {
    const subject = typeSubjectMap[String(row?.typeCode || '')] || 'SERVICE';
    if (subject in subjectCounts) {
      subjectCounts[subject] += 1;
    }
  }
  state.subjectCounts = subjectCounts;

  state.userTotal = toCount(userRes.data?.total);
  state.roleTotal = toCount(roleRes.data?.total);
  state.notifySceneTotal = toCount(notifySceneRes.data?.total);
}

/**
 * 拉取近 7 日操作日志并写入趋势与失败数。
 */
async function fetchPlatformOperLogMetrics(dayKeys: string[]) {
  const operCountMap: Record<string, number> = {};
  for (const key of dayKeys) {
    operCountMap[key] = 0;
  }

  const beginTime = `${dayKeys[0]} 00:00:00`;
  const endTime = formatRangeEnd(dayKeys[dayKeys.length - 1]);
  const operRes = await listOperLog({ pageNum: 1, pageSize: 500, beginTime, endTime });
  const operRows = Array.isArray(operRes.data?.records) ? operRes.data.records : [];
  let failed = 0;
  for (const row of operRows) {
    const key = toDateKey(row?.operTime);
    if (key in operCountMap) operCountMap[key] += 1;
    if (Number(row?.status) === 0) failed += 1;
  }
  state.operLogDayKeys = dayKeys;
  state.operLogDailyCounts = dayKeys.map(key => operCountMap[key] || 0);
  state.operLogFailedCount = failed;
}

/** 操作日志拉取失败时仅清空趋势数据 */
function resetPlatformOperLogMetrics(dayKeys: string[]) {
  state.operLogDayKeys = dayKeys;
  state.operLogDailyCounts = dayKeys.map(() => 0);
  state.operLogFailedCount = 0;
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

  async function loadPlatformDashboard(force = false) {
    if (state.loading) return;
    if (state.loaded && !force) return;

    state.loading = true;
    loadError.value = false;

    const dayKeys = buildDayKeys(7);

    try {
      await fetchPlatformOrgMetrics();
    } catch {
      resetPlatformOrgMetrics();
      loadError.value = true;
    }

    try {
      await fetchPlatformOperLogMetrics(dayKeys);
    } catch {
      resetPlatformOperLogMetrics(dayKeys);
    }

    state.loaded = true;
    state.loading = false;
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
