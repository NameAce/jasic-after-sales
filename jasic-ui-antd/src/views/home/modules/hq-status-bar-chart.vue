<script setup lang="ts">
/**
 * 工单看板降级图：无网点汇总时用 status-count 绘制状态分布（横向条或堆叠条）。
 */
import { nextTick, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { useHqDashboard } from '../composables/use-hq-dashboard';

defineOptions({
  name: 'HqStatusBarChart'
});

const props = withDefaults(
  defineProps<{
    /** horizontal：横向条形；stack：纵向堆叠 */
    variant?: 'horizontal' | 'stack';
  }>(),
  {
    variant: 'horizontal'
  }
);

const appStore = useAppStore();
const router = useRouter();
const { loading, statusChartItems } = useHqDashboard();

const { domRef, updateOptions } = useEcharts(() => buildOptions());
const { width, height } = useElementSize(domRef);

const COLORS = ['#fcbc25', '#f68057', '#8e9dff', '#26deca', '#8c8c8c'];

function buildOptions() {
  const isStack = props.variant === 'stack';
  return {
    title: {
      text: isStack ? $t('page.home.hqStatusStack') : $t('page.home.hqStatusRank')
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: isStack ? '4%' : '8%',
      top: 48,
      bottom: isStack ? '12%' : '4%',
      containLabel: true
    },
    xAxis: isStack
      ? {
          type: 'category',
          data: [] as string[],
          axisLabel: { interval: 0, rotate: 20 }
        }
      : {
          type: 'value',
          minInterval: 1
        },
    yAxis: isStack
      ? {
          type: 'value',
          minInterval: 1
        }
      : {
          type: 'category',
          data: [] as string[],
          axisTick: { show: false }
        },
    series: [
      {
        name: $t('page.home.workOrderStatusDistribution'),
        type: 'bar',
        barWidth: isStack ? 48 : 16,
        data: [] as Array<number | { value: number; itemStyle: { color: string } }>,
        label: {
          show: true,
          position: isStack ? 'top' : 'right'
        },
        itemStyle: {
          borderRadius: isStack ? [8, 8, 0, 0] : [0, 8, 8, 0]
        }
      }
    ]
  };
}

async function syncChart() {
  await nextTick();
  const items = statusChartItems.value;
  const isStack = props.variant === 'stack';

  updateOptions(opts => {
    opts.title = {
      text: isStack ? $t('page.home.hqStatusStack') : $t('page.home.hqStatusRank')
    };

    if (isStack) {
      opts.xAxis = { type: 'category', data: items.map(i => i.label), axisLabel: { interval: 0, rotate: 20 } };
      opts.yAxis = { type: 'value', minInterval: 1 };
      opts.series[0].data = items.map((item, idx) => ({
        value: item.value,
        itemStyle: { color: COLORS[idx % COLORS.length] }
      }));
    } else {
      const labels = items.map(i => i.label);
      const values = items.map(i => i.value);
      opts.yAxis = { type: 'category', data: [...labels].reverse(), axisTick: { show: false } };
      opts.xAxis = { type: 'value', minInterval: 1 };
      opts.series[0].data = [...values].reverse().map((value, idx) => ({
        value,
        itemStyle: { color: COLORS[values.length - 1 - idx] || COLORS[0] }
      }));
    }

    return opts;
  });
}

watch(
  () => [statusChartItems.value, width.value, height.value, props.variant],
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

function goWorkOrderPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'ALL' } });
}

onMounted(() => {
  syncChart();
});
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderPage">
        {{ $t('page.home.hqViewOrders') }}
      </a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
