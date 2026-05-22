<script setup lang="ts">
/**
 * 通用首页分区折线图：将 HomeSectionVO 各指标按类目展示为折线（平台账号治理等）。
 * 折线/面积主色与 platform-oper-log-chart 一致；数据点颜色可沿用组织治理饼图色板。
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
import { HOME_LINE_PRIMARY_COLOR, HOME_SINGLE_LINE_GRID, buildLineAreaGradient } from '../composables/home-chart-theme';

defineOptions({
  name: 'HomeSectionLineChart'
});

const props = withDefaults(
  defineProps<{
    section?: HomeSectionVO | null;
    loading?: boolean;
    loaded?: boolean;
    chartTitle?: string;
    excludeCodes?: string[];
    colorByCode?: Record<string, string>;
    extraLinkText?: string;
    extraLinkRoute?: { name: string; query?: Record<string, string> };
  }>(),
  {
    section: null,
    loading: false,
    loaded: false,
    chartTitle: '',
    excludeCodes: () => [],
    colorByCode: () => ({}),
    extraLinkText: '',
    extraLinkRoute: undefined
  }
);

const router = useRouter();

const chartItems = computed(() => {
  const metrics = props.section?.metrics || [];
  return metrics
    .filter(item => !props.excludeCodes.includes(item.code || '') && toDashboardCount(item.value) >= 0)
    .map(item => {
      const code = item.code || '';
      return {
        code,
        name: item.title || code,
        value: toDashboardCount(item.value),
        pointColor: props.colorByCode?.[code],
        routeTarget: item.routeTarget
      };
    });
});

/**
 * 作用：折线图数据点点击后按指标 routeTarget 跳转。
 * @param dataIndex - ECharts 点击 dataIndex
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handlePointClick(dataIndex: number) {
  const item = chartItems.value[dataIndex];
  navigateHomeRoute(router, item?.routeTarget);
}

const { domRef, updateOptions } = useEcharts(
  () => ({
    title: { text: props.chartTitle },
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
      data: [] as string[],
      axisLabel: { interval: 0, rotate: 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: props.chartTitle,
        type: 'line',
        smooth: true,
        color: HOME_LINE_PRIMARY_COLOR,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 2, color: HOME_LINE_PRIMARY_COLOR },
        areaStyle: { color: buildLineAreaGradient(HOME_LINE_PRIMARY_COLOR) },
        data: [] as Array<number | { value: number; itemStyle?: { color: string } }>
      }
    ]
  }),
  {
    onRender: chart => {
      chart.on('click', params => {
        const dataIndex = Number((params as { dataIndex?: number }).dataIndex);
        if (Number.isFinite(dataIndex) && dataIndex >= 0) {
          handlePointClick(dataIndex);
        }
      });
    }
  }
);

const { width, height } = useElementSize(domRef);

/**
 * 作用：将 chartItems 同步到 ECharts 单线面积图。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function applyChartData() {
  await nextTick();
  updateOptions(opts => {
    opts.title.text = props.chartTitle;
    opts.xAxis.data = chartItems.value.map(item => item.name);
    opts.series[0].name = props.chartTitle;
    opts.series[0].data = chartItems.value.map(item => {
      if (item.pointColor) {
        return {
          value: item.value,
          itemStyle: { color: item.pointColor, borderColor: '#fff', borderWidth: 2 }
        };
      }
      return item.value;
    });
    return opts;
  });
}

watch(
  () => [width.value, height.value, chartItems.value, props.loaded, props.chartTitle, props.colorByCode],
  () => {
    if (width.value > 0 && height.value > 0 && chartItems.value.length) {
      applyChartData();
    }
  },
  { flush: 'post', deep: true }
);

/**
 * 作用：点击卡片右上角额外链接跳转。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openExtraLink() {
  if (!props.extraLinkRoute?.name) return;
  router.push({
    name: props.extraLinkRoute.name,
    query: props.extraLinkRoute.query
  });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <!-- 首页区块折线图 -->
    <template v-if="extraLinkText && extraLinkRoute" #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="openExtraLink">{{ extraLinkText }}</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
