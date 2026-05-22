<script setup lang="ts">
/**
 * 遗留业务首页双折线趋势（近 7 天建单数 + 待办通知，堆叠面积图）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { toAxisDayLabel, toDashboardCount } from '../composables/dashboard-helpers';
import {
  HOME_SOYBEAN_DUAL_LINE_COLORS,
  HOME_TREND_LINE_GRID,
  buildTrendLineSeriesItem
} from '../composables/home-chart-theme';
import { useBusinessHomeDashboard } from '../composables/use-business-home-dashboard';

defineOptions({
  name: 'LineChart'
});

const appStore = useAppStore();
const router = useRouter();
const { loading, loaded, trend7d } = useBusinessHomeDashboard();

const trendPayload = computed(() => {
  const trend = trend7d.value;
  const dayKeys = Array.isArray(trend?.dayKeys) ? trend.dayKeys : [];
  const orderCounts = Array.isArray(trend?.createdWorkOrderCounts) ? trend.createdWorkOrderCounts : [];
  const todoCounts = Array.isArray(trend?.activeTodoCounts) ? trend.activeTodoCounts : [];

  return {
    dayKeys,
    orderData: dayKeys.map((_, index) => toDashboardCount(orderCounts[index])),
    todoData: dayKeys.map((_, index) => toDashboardCount(todoCounts[index]))
  };
});

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.home.workOrderTrend')
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      label: { backgroundColor: '#6a7985' }
    }
  },
  legend: {
    data: [$t('page.home.createdWorkOrder'), $t('page.home.todoNotice')],
    top: 28,
    right: 16
  },
  grid: { ...HOME_TREND_LINE_GRID },
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
      ...buildTrendLineSeriesItem({
        name: $t('page.home.createdWorkOrder'),
        color: HOME_SOYBEAN_DUAL_LINE_COLORS[0],
        data: [],
        stacked: true
      }),
      data: [] as number[]
    },
    {
      ...buildTrendLineSeriesItem({
        name: $t('page.home.todoNotice'),
        color: HOME_SOYBEAN_DUAL_LINE_COLORS[1],
        data: [],
        stacked: true
      }),
      data: [] as number[]
    }
  ]
}));

const { width, height } = useElementSize(domRef);

/**
 * 作用：将 trend7d 数据同步到 ECharts 折线图。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function applyTrendData() {
  await nextTick();
  const { dayKeys, orderData, todoData } = trendPayload.value;
  updateOptions(opts => {
    opts.xAxis.data = dayKeys.map(toAxisDayLabel);
    opts.series[0] = buildTrendLineSeriesItem({
      name: $t('page.home.createdWorkOrder'),
      color: HOME_SOYBEAN_DUAL_LINE_COLORS[0],
      data: orderData,
      stacked: true
    });
    opts.series[1] = buildTrendLineSeriesItem({
      name: $t('page.home.todoNotice'),
      color: HOME_SOYBEAN_DUAL_LINE_COLORS[1],
      data: todoData,
      stacked: true
    });
    return opts;
  });
}

/**
 * 作用：语言切换后刷新图表标题、图例与序列名称。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

watch(
  () => [width.value, height.value, trendPayload.value, loaded.value],
  () => {
    if (width.value > 0 && height.value > 0 && trendPayload.value.dayKeys.length) {
      applyTrendData();
    }
  },
  { flush: 'post', deep: true }
);

/**
 * 作用：跳转工单列表（全网视角）。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function goWorkOrderPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'ALL' } });
}
</script>

<template>
  <!-- 折线图封装 -->
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderPage">工单列表</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
