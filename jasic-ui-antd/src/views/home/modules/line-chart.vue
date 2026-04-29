<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getNotifyTodoPage, listWorkOrder } from '@/service/api';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

defineOptions({
  name: 'LineChart'
});

const appStore = useAppStore();
const router = useRouter();

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.home.workOrderTrend')
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      label: {
        backgroundColor: '#6a7985'
      }
    }
  },
  legend: {
    data: [$t('page.home.createdWorkOrder'), $t('page.home.todoNotice')],
    top: 28,
    right: 16
  },
  grid: {
    left: '3%',
    right: '4%',
    top: 88,
    bottom: '8%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [] as string[]
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      color: '#8e9dff',
      name: $t('page.home.createdWorkOrder'),
      type: 'line',
      smooth: true,
      stack: 'Total',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            {
              offset: 0.25,
              color: '#8e9dff'
            },
            {
              offset: 1,
              color: '#fff'
            }
          ]
        }
      },
      emphasis: {
        focus: 'series'
      },
      data: [] as number[]
    },
    {
      color: '#26deca',
      name: $t('page.home.todoNotice'),
      type: 'line',
      smooth: true,
      stack: 'Total',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            {
              offset: 0.25,
              color: '#26deca'
            },
            {
              offset: 1,
              color: '#fff'
            }
          ]
        }
      },
      emphasis: {
        focus: 'series'
      },
      data: []
    }
  ]
}));

function toDateKey(input: unknown) {
  const text = String(input || '');
  return text.length >= 10 ? text.slice(0, 10) : '';
}

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

function toAxisLabel(dayKey: string) {
  return dayKey.slice(5);
}

async function loadRealData() {
  const dayKeys = buildDayKeys(7);
  const orderCountMap: Record<string, number> = {};
  const todoCountMap: Record<string, number> = {};

  for (const key of dayKeys) {
    orderCountMap[key] = 0;
    todoCountMap[key] = 0;
  }

  const [orderRes, todoRes] = await Promise.all([
    listWorkOrder({ pageNum: 1, pageSize: 200, viewScope: 'ALL' }),
    getNotifyTodoPage({ box: 'TODO', pageNum: 1, pageSize: 200 })
  ]);

  const orderRows = Array.isArray(orderRes.data?.records) ? orderRes.data.records : [];
  for (const row of orderRows) {
    const key = toDateKey(row?.createTime);
    if (key in orderCountMap) orderCountMap[key] += 1;
  }

  const todoRows = Array.isArray(todoRes.data?.records) ? todoRes.data.records : [];
  for (const row of todoRows) {
    const key = toDateKey(row?.createTime);
    if (key in todoCountMap) todoCountMap[key] += 1;
  }

  updateOptions(opts => {
    opts.xAxis.data = dayKeys.map(toAxisLabel);
    opts.series[0].data = dayKeys.map(key => orderCountMap[key] || 0);
    opts.series[1].data = dayKeys.map(key => todoCountMap[key] || 0);

    return opts;
  });
}

function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();
    opts.title = originOpts.title;
    opts.legend.data = originOpts.legend.data;
    opts.series[0].name = originOpts.series[0].name;
    opts.series[1].name = originOpts.series[1].name;

    return opts;
  });
}

async function init() {
  try {
    await loadRealData();
  } catch {
    updateOptions(opts => {
      opts.xAxis.data = [];
      opts.series[0].data = [];
      opts.series[1].data = [];
      return opts;
    });
  }
}

watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

onMounted(() => {
  init();
});

function goWorkOrderPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'ALL' } });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderPage">工单列表</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
