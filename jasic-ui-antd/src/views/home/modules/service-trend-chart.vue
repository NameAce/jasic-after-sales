<script setup lang="ts">
/**
 * 服务主体首页近七天事件趋势图：接单 / 完成 / 转出（数据来自 trend.series）。
 * 折线样式与 git line-chart.vue 一致。
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useEcharts } from '@/hooks/common/echarts';
import { alignTrendValuesToDays, toAxisDayLabel } from '../composables/dashboard-helpers';
import { HOME_TREND_LINE_GRID, buildTrendLineSeriesItem, resolveTrendLineColor } from '../composables/home-chart-theme';
import { useServiceDashboard } from '../composables/use-service-dashboard';

defineOptions({
  name: 'ServiceTrendChart'
});

const router = useRouter();
const { loading, loaded, trend } = useServiceDashboard();

const trendPayload = computed(() => {
  const payload = trend.value;
  const days = Array.isArray(payload?.days) ? payload.days : [];
  const seriesList = Array.isArray(payload?.series) ? payload.series : [];

  return {
    title: payload?.title || '近 7 天事件趋势',
    days,
    series: seriesList.map((item, index) => {
      const code = item.code || '';
      return {
        code,
        name: item.name || code,
        color: resolveTrendLineColor(code, index),
        data: alignTrendValuesToDays(days, item.values)
      };
    })
  };
});

const { domRef, updateOptions } = useEcharts(() => ({
  title: { text: '' },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      label: { backgroundColor: '#6a7985' }
    }
  },
  legend: { data: [] as string[], top: 28, right: 16 },
  grid: { ...HOME_TREND_LINE_GRID },
  xAxis: { type: 'category', boundaryGap: false, data: [] as string[] },
  yAxis: { type: 'value', minInterval: 1 },
  series: [] as ReturnType<typeof buildTrendLineSeriesItem>[]
}));

const { width, height } = useElementSize(domRef);

async function applyTrendData() {
  await nextTick();
  const payload = trendPayload.value;
  updateOptions(opts => {
    opts.title.text = payload.title;
    opts.xAxis.data = payload.days.map(toAxisDayLabel);
    opts.legend.data = payload.series.map(item => item.name);
    opts.series = payload.series.map(item =>
      buildTrendLineSeriesItem({ name: item.name, color: item.color, data: item.data, stacked: false })
    );
    return opts;
  });
}

watch(
  () => [width.value, height.value, trendPayload.value, loaded.value],
  () => {
    if (width.value > 0 && height.value > 0 && trendPayload.value.days.length) {
      applyTrendData();
    }
  },
  { flush: 'post', deep: true }
);

function goWorkOrderPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'CURRENT' } });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderPage">工单列表</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
