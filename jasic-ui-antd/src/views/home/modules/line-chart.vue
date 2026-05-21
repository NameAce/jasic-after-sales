<script setup lang="ts">
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { toAxisDayLabel, toDashboardCount } from '../composables/dashboard-helpers';
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
            { offset: 0.25, color: '#8e9dff' },
            { offset: 1, color: '#fff' }
          ]
        }
      },
      emphasis: { focus: 'series' },
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
            { offset: 0.25, color: '#26deca' },
            { offset: 1, color: '#fff' }
          ]
        }
      },
      emphasis: { focus: 'series' },
      data: [] as number[]
    }
  ]
}));

const { width, height } = useElementSize(domRef);

async function applyTrendData() {
  await nextTick();
  const { dayKeys, orderData, todoData } = trendPayload.value;
  updateOptions(opts => {
    opts.xAxis.data = dayKeys.map(toAxisDayLabel);
    opts.series[0].data = orderData;
    opts.series[1].data = todoData;
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

function goWorkOrderPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'ALL' } });
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
