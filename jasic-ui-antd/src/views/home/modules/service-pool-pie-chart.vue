<script setup lang="ts">
/**
 * 服务主体首页工单状态分布饼图：由 currentPool 各状态指标聚合（排除总量卡片）。
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useEcharts } from '@/hooks/common/echarts';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { toDashboardCount } from '../composables/dashboard-helpers';
import { useServiceDashboard } from '../composables/use-service-dashboard';
import { HOME_PIE_COLORS } from '../composables/home-metric-styles';

defineOptions({
  name: 'ServicePoolPieChart'
});

const PIE_COLORS = [...HOME_PIE_COLORS];

const router = useRouter();
const { loading, loaded, currentPool } = useServiceDashboard();

/** 饼图数据：排除 CURRENT_TOTAL，仅展示有数量的状态指标 */
const chartData = computed(() => {
  const metrics = currentPool.value?.metrics || [];
  return metrics
    .filter(item => item.code !== 'CURRENT_TOTAL' && toDashboardCount(item.value) > 0)
    .map(item => ({
      name: item.title || item.code || '',
      value: toDashboardCount(item.value),
      routeTarget: item.routeTarget
    }));
});

/** 点击扇区时按指标 routeTarget 跳转 */
function handleSliceClick(dataIndex: number) {
  const item = chartData.value[dataIndex];
  navigateHomeRoute(router, item?.routeTarget);
}

const { domRef, updateOptions } = useEcharts(
  () => ({
    title: {
      text: '当前承接工单状态分布'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: '1%',
      left: 'center',
      itemStyle: {
        borderWidth: 0
      }
    },
    series: [
      {
        color: PIE_COLORS,
        name: '当前承接工单状态分布',
        type: 'pie',
        radius: ['45%', '75%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 1
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '12'
          }
        },
        labelLine: {
          show: false
        },
        data: [] as { name: string; value: number }[]
      }
    ]
  }),
  {
    onRender: chart => {
      chart.on('click', params => {
        const dataIndex = Number((params as { dataIndex?: number }).dataIndex);
        if (Number.isFinite(dataIndex) && dataIndex >= 0) {
          handleSliceClick(dataIndex);
        }
      });
    }
  }
);

const { width, height } = useElementSize(domRef);

async function applyChartData() {
  await nextTick();
  updateOptions(opts => {
    opts.series[0].data = chartData.value.map(item => ({
      name: item.name,
      value: item.value
    }));
    return opts;
  });
}

watch(
  () => [width.value, height.value, chartData.value, loaded.value],
  () => {
    if (width.value > 0 && height.value > 0 && chartData.value.length) {
      applyChartData();
    }
  },
  { flush: 'post', deep: true }
);

function goCurrentPoolPage() {
  router.push({
    name: 'after-sales_work-order',
    query: { viewScope: 'CURRENT' }
  });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goCurrentPoolPage">当前工单</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
