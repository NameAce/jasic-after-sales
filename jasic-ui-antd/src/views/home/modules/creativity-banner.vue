<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getNotifyTodoCount, getNotifyTodoPage, listRole, listUser } from '@/service/api';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

defineOptions({
  name: 'CreativityBanner'
});

const router = useRouter();
const appStore = useAppStore();
const { hasAuth } = useAuth();
// 可按用户列表权限查看用户汇总
const canViewUser = computed(() => hasAuth('system:user:list'));
// 可按角色列表权限查看角色汇总
const canViewRole = computed(() => hasAuth('system:role:list'));
// 是否展示组织维度的用户/角色汇总（否则展示待办/消息）
const useOrgSummary = computed(() => canViewUser.value || canViewRole.value);

// 汇总条形图所用的数值（部分为占位 '--'）
const summary = reactive({
  userTotal: '--' as number | string,
  roleTotal: '--' as number | string,
  todoTotal: '--' as number | string,
  messageTotal: '--' as number | string
});

// 横向条形图各柱数据
const chartItems = computed(() => {
  if (useOrgSummary.value) {
    return [
      { key: 'user', label: $t('page.home.summaryUser'), value: Number(summary.userTotal) || 0, color: '#5da8ff' },
      { key: 'role', label: $t('page.home.summaryRole'), value: Number(summary.roleTotal) || 0, color: '#8e9dff' }
    ];
  }

  return [
    { key: 'todo', label: $t('page.home.todo'), value: Number(summary.todoTotal) || 0, color: '#5da8ff' },
    { key: 'message', label: $t('page.home.message'), value: Number(summary.messageTotal) || 0, color: '#8e9dff' }
  ];
});

/**
 * 作用：在允许时请求接口并用 extractor 取数，失败返回 '--'。
 * @param enabled - 是否请求
 * @param requester - 请求函数
 * @param extractor - 从响应取 total/count
 * @returns 数字或 '--'
 */
async function safeGetTotal(enabled: boolean, requester: () => Promise<any>, extractor: (res: any) => number) {
  if (!enabled) return '--';
  try {
    const res = await requester();
    return Number(extractor(res) || 0);
  } catch {
    return '--';
  }
}

/**
 * 作用：根据权限加载用户/角色或待办/消息汇总并刷新图表。
 */
async function loadSummary() {
  try {
    if (useOrgSummary.value) {
      const [userTotal, roleTotal] = await Promise.all([
        safeGetTotal(
          canViewUser.value,
          () => listUser({ pageNum: 1, pageSize: 1 }),
          res => res.data?.total
        ),
        safeGetTotal(
          canViewRole.value,
          () => listRole({ pageNum: 1, pageSize: 1 }),
          res => res.data?.total
        )
      ]);
      summary.userTotal = userTotal;
      summary.roleTotal = roleTotal;
    } else {
      const [todoTotal, messageTotal] = await Promise.all([
        safeGetTotal(
          true,
          () => getNotifyTodoCount(),
          res => res.data?.count
        ),
        safeGetTotal(
          true,
          () => getNotifyTodoPage({ box: 'HISTORY', pageNum: 1, pageSize: 1 }),
          res => res.data?.total
        )
      ]);
      summary.todoTotal = todoTotal;
      summary.messageTotal = messageTotal;
    }
    updateChart();
  } catch {
    summary.userTotal = '--';
    summary.roleTotal = '--';
    summary.todoTotal = '--';
    summary.messageTotal = '--';
    updateChart();
  }
}

onMounted(() => {
  loadSummary();
});

/**
 * 作用：点击柱状图条目跳转对应管理页或消息中心。
 */
function openSummaryPage(key: string) {
  if (key === 'user') {
    router.push({ path: '/system/user' });
    return;
  }
  if (key === 'role') {
    router.push({ path: '/system/role' });
    return;
  }
  if (key === 'todo') {
    router.push({ path: '/notify', query: { box: 'TODO' } });
    return;
  }
  router.push({ path: '/notify', query: { box: 'HISTORY' } });
}

// 横向汇总条形图 ECharts 实例
const { domRef, updateOptions } = useEcharts(
  () => ({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: {
      top: 24,
      right: 12,
      bottom: 18,
      left: 70,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      splitLine: {
        lineStyle: { color: '#f0f0f0' }
      }
    },
    yAxis: {
      type: 'category',
      data: [] as string[],
      axisTick: { show: false }
    },
    series: [
      {
        name: $t('page.home.summaryTitle'),
        type: 'bar',
        barWidth: 18,
        data: [] as Array<{ value: number; itemStyle: { color: string } }>,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}'
        },
        itemStyle: {
          borderRadius: [0, 8, 8, 0]
        }
      }
    ]
  }),
  {
    onRender: chart => {
      chart.on('click', params => {
        const name = String((params as any)?.name || '');
        if (name === $t('page.home.summaryUser')) {
          openSummaryPage('user');
          return;
        }
        if (name === $t('page.home.summaryRole')) {
          openSummaryPage('role');
          return;
        }
        if (name === $t('page.home.todo')) {
          openSummaryPage('todo');
          return;
        }
        if (name === $t('page.home.message')) {
          openSummaryPage('message');
        }
      });
    }
  }
);

/**
 * 作用：将 summary/chartItems 同步到图表配置。
 */
function updateChart() {
  updateOptions(opts => {
    opts.series[0].name = $t('page.home.summaryTitle');
    opts.yAxis.data = chartItems.value.map(item => item.label);
    opts.series[0].data = chartItems.value.map(item => ({
      value: item.value,
      itemStyle: { color: item.color }
    }));
    return opts;
  });
}

// 语言切换后重绘汇总图
watch(
  () => appStore.locale,
  () => {
    updateChart();
  }
);
</script>

<template>
  <ACard
    :title="$t('page.home.summaryTitle')"
    :bordered="false"
    size="small"
    class="h-full flex-col-stretch card-wrapper"
    :body-style="{ flex: 1, overflow: 'hidden' }"
  >
    <div class="h-full">
      <div ref="domRef" class="h-230px"></div>
    </div>
  </ACard>
</template>

<style scoped></style>
