<script setup lang="ts">
/**
 * 通用首页分区条形图：将 HomeSectionVO 各指标 value 横向展示（平台治理等无趋势接口时使用）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import type { HomeSectionVO } from '@/service/api';
import { useEcharts } from '@/hooks/common/echarts';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { toDashboardCount } from '../composables/dashboard-helpers';
import { HOME_SECTION_BAR_COLORS, HOME_SECTION_CHART_GRID } from '../composables/home-chart-theme';

defineOptions({
  name: 'HomeSectionBarChart'
});

/** 条形图柱色：与 Soybean 首页色板同源，按指标顺序循环 */
const BAR_COLORS = [...HOME_SECTION_BAR_COLORS];

const props = withDefaults(
  defineProps<{
    section?: HomeSectionVO | null;
    loading?: boolean;
    loaded?: boolean;
    chartTitle?: string;
    /** 按指标 code 指定柱条颜色 */
    colorByCode?: Record<string, string>;
    /** 是否按数值从大到小排序（便于对比） */
    sortByValue?: boolean;
    fillHeight?: boolean;
  }>(),
  {
    section: null,
    loading: false,
    loaded: false,
    chartTitle: '',
    colorByCode: () => ({}),
    sortByValue: true
  }
);

const router = useRouter();

const chartItems = computed(() => {
  const metrics = props.section?.metrics || [];
  const items = metrics
    .filter(item => toDashboardCount(item.value) >= 0)
    .map((item, index) => {
      const code = item.code || '';
      return {
        code,
        name: item.title || code,
        value: toDashboardCount(item.value),
        color: props.colorByCode?.[code] || BAR_COLORS[index % BAR_COLORS.length],
        routeTarget: item.routeTarget
      };
    });
  if (props.sortByValue) {
    return [...items].sort((a, b) => a.value - b.value);
  }
  return items;
});

const hasData = computed(() => chartItems.value.length > 0);

const { domRef, updateOptions } = useEcharts(
  () => ({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { ...HOME_SECTION_CHART_GRID, left: 88, right: 40 },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }
    },
    yAxis: {
      type: 'category',
      data: [] as string[],
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#666', fontSize: 12 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        barMaxWidth: 20,
        data: [] as Array<{ value: number; itemStyle: { color: string } }>,
        label: {
          show: true,
          position: 'right',
          color: '#666',
          fontSize: 12,
          formatter: (params: { value?: number }) => {
            const v = Number(params.value);
            return v > 0 ? String(v) : '';
          }
        },
        itemStyle: { borderRadius: [0, 8, 8, 0] },
        emphasis: { focus: 'series' }
      }
    ]
  }),
  {
    onRender: chart => {
      chart.on('click', params => {
        const dataIndex = Number((params as { dataIndex?: number }).dataIndex);
        if (Number.isFinite(dataIndex) && dataIndex >= 0) {
          navigateHomeRoute(router, chartItems.value[dataIndex]?.routeTarget);
        }
      });
    }
  }
);

const { width, height } = useElementSize(domRef);

/**
 * 作用：将 chartItems 同步到横向条形图 yAxis 与 series 数据。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function applyChartData() {
  await nextTick();
  updateOptions(opts => {
    opts.yAxis.data = chartItems.value.map(item => item.name);
    opts.series[0].data = chartItems.value.map(item => ({
      value: item.value,
      itemStyle: { color: item.color }
    }));
    return opts;
  });
}

watch(
  () => [width.value, height.value, chartItems.value, props.loaded, props.section, props.colorByCode],
  () => {
    if (width.value > 0 && height.value > 0 && hasData.value) {
      applyChartData();
    }
  },
  { flush: 'post', deep: true }
);
</script>

<template>
  <ACard
    :bordered="false"
    class="home-section-chart-card card-wrapper"
    :class="{ 'home-chart-card--fill': fillHeight }"
    :loading="loading"
    :title="chartTitle || section?.title || ''"
    :body-style="
      fillHeight
        ? {
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            paddingTop: '8px'
          }
        : { paddingTop: '8px' }
    "
  >
    <!-- 首页区块柱状图 -->
    <div
      v-if="hasData"
      ref="domRef"
      class="overflow-hidden"
      :class="fillHeight ? 'home-chart-box--fill' : 'h-320px'"
    ></div>
    <AEmpty v-else-if="loaded && !loading" class="py-48px" description="暂无分布数据" />
  </ACard>
</template>

<style scoped>
.home-section-chart-card :deep(.ant-card-head) {
  min-height: 42px;
  padding-inline: 16px;
}

.home-section-chart-card :deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 600;
}

.home-chart-card--fill {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.home-chart-box--fill {
  flex: 1;
  min-height: 220px;
}
</style>
