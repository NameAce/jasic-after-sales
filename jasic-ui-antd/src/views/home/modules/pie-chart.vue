<script setup lang="ts">
/**
 * 遗留业务首页工单状态分布环图（workOrderStatus，ALL 视角）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { WORK_ORDER_STATUS_LABELS, buildStatusChartItems } from '../composables/dashboard-helpers';
import { HOME_PIE_CHART_COLORS } from '../composables/home-chart-theme';
import { useBusinessHomeDashboard } from '../composables/use-business-home-dashboard';

defineOptions({
  name: 'PieChart'
});

const appStore = useAppStore();
const router = useRouter();
const { loading, loaded, workOrderStatus } = useBusinessHomeDashboard();

/** 饼图数据：使用首页接口 workOrderStatus（权限范围内 ALL 视角） */
const chartData = computed(() =>
  buildStatusChartItems(workOrderStatus.value).map(item => ({
    name: WORK_ORDER_STATUS_LABELS[item.key] || item.label,
    value: item.value
  }))
);

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.home.workOrderStatusDistribution')
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
      color: [...HOME_PIE_CHART_COLORS.slice(0, 4)],
      name: $t('page.home.workOrderStatusDistribution'),
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
}));

const { width, height } = useElementSize(domRef);

/**
 * 作用：将 chartData 同步到 ECharts 饼图 series。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function applyChartData() {
  await nextTick();
  updateOptions(opts => {
    opts.series[0].data = chartData.value;
    return opts;
  });
}

/**
 * 作用：语言切换后刷新图表标题与 series 名称文案。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();
    opts.title = originOpts.title;
    opts.series[0].name = originOpts.series[0].name;
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
  () => [width.value, height.value, chartData.value, loaded.value],
  () => {
    if (width.value > 0 && height.value > 0 && chartData.value.length) {
      applyChartData();
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
function goWorkOrderStatusPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'ALL' } });
}
</script>

<template>
  <!-- 饼图封装 -->
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderStatusPage">状态明细</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
