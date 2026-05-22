<script setup lang="ts">
/**
 * 总部看板：网点待接单排行（横向条形，Top10）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, nextTick, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { useHqDashboard } from '../composables/use-hq-dashboard';

defineOptions({
  name: 'HqSiteBarChart'
});

const TOP_N = 10;
const BAR_COLOR = '#fcbc25';

const appStore = useAppStore();
const router = useRouter();
const { loading, sitesByWaitAccept } = useHqDashboard();

const topSites = computed(() => sitesByWaitAccept.value.slice(0, TOP_N));

const { domRef, updateOptions } = useEcharts(() => ({
  title: { text: $t('page.home.hqSiteWaitRank') },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: {
    left: '3%',
    right: '8%',
    top: 48,
    bottom: '4%',
    containLabel: true
  },
  xAxis: {
    type: 'value',
    minInterval: 1
  },
  yAxis: {
    type: 'category',
    data: [] as string[],
    axisTick: { show: false }
  },
  series: [
    {
      name: $t('page.home.hqWaitAccept'),
      type: 'bar',
      barWidth: 16,
      data: [] as Array<{ value: number; itemStyle: { color: string } }>,
      label: {
        show: true,
        position: 'right'
      },
      itemStyle: {
        borderRadius: [0, 8, 8, 0]
      }
    }
  ]
}));

const { width, height } = useElementSize(domRef);

/**
 * 作用：将网点待接单 TopN 同步到横向条形图。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function syncChart() {
  await nextTick();
  const items = topSites.value;
  const labels = items.map(item => String(item.siteCompanyName || '-'));
  const values = items.map(item => Number(item.waitAcceptCount) || 0);

  updateOptions(opts => {
    opts.title = { text: $t('page.home.hqSiteWaitRank') };
    opts.series[0].name = $t('page.home.hqWaitAccept');
    opts.yAxis = {
      type: 'category',
      data: [...labels].reverse(),
      axisTick: { show: false }
    };
    opts.xAxis = { type: 'value', minInterval: 1 };
    opts.series[0].data = [...values].reverse().map(value => ({
      value,
      itemStyle: { color: BAR_COLOR }
    }));
    return opts;
  });
}

watch(
  () => [topSites.value, width.value, height.value],
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
 * 作用：跳转工单列表（全网范围）。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function goWorkOrderPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'ALL' } });
}

onMounted(() => {
  syncChart();
});
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <!-- 总部网点柱状图 -->
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderPage">
        {{ $t('page.home.hqViewOrders') }}
      </a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
