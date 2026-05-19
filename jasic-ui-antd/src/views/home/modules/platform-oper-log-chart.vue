<script setup lang="ts">
/**
 * 平台超管：近 7 日操作日志量折线图（基于 oper-log 分页采样）。
 */
import { nextTick, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';

defineOptions({
  name: 'PlatformOperLogChart'
});

const appStore = useAppStore();
const router = useRouter();
const { loading, operLogDayKeys, operLogDailyCounts } = usePlatformDashboard();

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.home.platformOperLogTrend')
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'cross' }
  },
  grid: {
    left: '3%',
    right: '4%',
    top: 48,
    bottom: '8%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [] as string[]
  },
  yAxis: {
    type: 'value',
    minInterval: 1
  },
  series: [
    {
      color: '#5da8ff',
      name: $t('page.home.platformOperLogCount'),
      type: 'line',
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0.25, color: '#5da8ff' },
            { offset: 1, color: '#fff' }
          ]
        }
      },
      data: [] as number[]
    }
  ]
}));

const { width, height } = useElementSize(domRef);

function toAxisLabel(dayKey: string) {
  return dayKey.slice(5);
}

async function syncChart() {
  await nextTick();
  const keys = operLogDayKeys.value;
  const values = operLogDailyCounts.value;
  if (!keys.length) return;

  updateOptions(opts => {
    opts.title = { text: $t('page.home.platformOperLogTrend') };
    opts.series[0].name = $t('page.home.platformOperLogCount');
    opts.xAxis.data = keys.map(toAxisLabel);
    opts.series[0].data = values;
    return opts;
  });
}

watch(
  () => [operLogDayKeys.value, operLogDailyCounts.value, width.value, height.value],
  () => {
    syncChart();
  },
  { flush: 'post', deep: true }
);

watch(
  () => appStore.locale,
  () => {
    syncChart();
  }
);

function goLogPage() {
  router.push({ path: '/log' });
}

onMounted(() => {
  syncChart();
});
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goLogPage">{{ $t('page.home.platformViewLog') }}</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden" />
  </ACard>
</template>

<style scoped></style>
