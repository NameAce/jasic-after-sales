<script setup lang="ts">
/**
 * 平台超管：近 7 日操作日志量折线图（基于 oper-log 分页采样）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { nextTick, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';
import { HOME_LINE_PRIMARY_COLOR, HOME_SINGLE_LINE_GRID, buildLineAreaGradient } from '../composables/home-chart-theme';

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
    axisPointer: {
      type: 'cross',
      label: { backgroundColor: '#6a7985' }
    }
  },
  grid: { ...HOME_SINGLE_LINE_GRID },
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
      color: HOME_LINE_PRIMARY_COLOR,
      name: $t('page.home.platformOperLogCount'),
      type: 'line',
      smooth: true,
      lineStyle: { width: 2, color: HOME_LINE_PRIMARY_COLOR },
      areaStyle: { color: buildLineAreaGradient(HOME_LINE_PRIMARY_COLOR) },
      data: [] as number[]
    }
  ]
}));

const { width, height } = useElementSize(domRef);

/**
 * 作用：将 yyyy-MM-dd 格式化为 MM-dd 横轴标签。
 * @param dayKey - 日期键
 * @returns 轴标签字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function toAxisLabel(dayKey: string) {
  return dayKey.slice(5);
}

/**
 * 作用：将近 7 日操作日志采样数据同步到折线图。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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

/**
 * 作用：跳转操作日志列表页。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function goLogPage() {
  router.push({ path: '/log' });
}

onMounted(() => {
  syncChart();
});
</script>

<template>
  <!-- 平台操作日志图 -->
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goLogPage">{{ $t('page.home.platformViewLog') }}</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden" />
  </ACard>
</template>

<style scoped></style>
