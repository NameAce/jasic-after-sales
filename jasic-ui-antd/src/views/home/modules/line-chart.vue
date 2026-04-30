<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getNotifyTodoPage, listWorkOrder } from '@/service/api';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

// 应用语言与 ECharts 封装（工单与待办趋势折线）
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

/**
 * 作用：将时间字符串截断为 yyyy-MM-dd 作为统计键。
 * @param input - 任意时间表示
 * @returns 日期键或空串
 */
function toDateKey(input: unknown) {
  const text = String(input || '');
  return text.length >= 10 ? text.slice(0, 10) : '';
}

/**
 * 作用：生成向前连续若干天的日期键（yyyy-MM-dd）。
 * @param size - 天数，默认 7
 * @returns 日期键数组（从早到晚）
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

/**
 * 作用：将完整日期键格式化为坐标轴短标签（MM-DD）。
 * @param dayKey - yyyy-MM-dd
 * @returns 短标签
 */
function toAxisLabel(dayKey: string) {
  return dayKey.slice(5);
}

/**
 * 作用：统计近 7 日建单数与待办通知数并更新折线图。
 * @param 无
 * @returns 返回 Promise，图表更新后结束
 */
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

/**
 * 作用：语言切换时同步图例与系列名称文案。
 * @param 无
 * @returns {void} 无
 */
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

/**
 * 作用：挂载时加载折线数据，失败则清空坐标与系列。
 * @param 无
 * @returns 返回 Promise，初始化结束后结束
 */
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

// 语言变更时刷新图表文案
watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

/**
 * 作用：挂载后拉取折线图数据。
 * @param 无
 * @returns {void} 无
 */
onMounted(() => {
  init();
});

/**
 * 作用：跳转工单列表（全部视角）。
 * @param 无
 * @returns {void} 无
 */
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
