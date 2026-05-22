<script setup lang="ts">
/**
 * 通用首页分区饼图：由 HomeSectionVO 指标聚合（可排除总量类指标）。
 * 工单状态分布建议开启 showZeroInLegend：接口会返回待接单等全量状态，值为 0 时仍展示图例项，扇区仅渲染 value > 0。
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import type { HomeSectionVO } from '@/service/api';
import { useEcharts } from '@/hooks/common/echarts';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { buildWorkOrderPoolPieLegendItems, toDashboardCount } from '../composables/dashboard-helpers';
import {
  buildHomePieChartInlineTitle,
  resolvePieCenter,
  resolvePieLegendLayout,
  resolvePieRadius
} from '../composables/home-chart-theme';
import { HOME_PIE_COLORS } from '../composables/home-metric-styles';

defineOptions({
  name: 'HomeSectionPieChart'
});

/** 默认扇区色板：与组织治理饼图 git 四色环图一致 */
const PIE_COLORS = [...HOME_PIE_COLORS];

const props = withDefaults(
  defineProps<{
    section?: HomeSectionVO | null;
    loading?: boolean;
    loaded?: boolean;
    /** 图表标题文案 */
    chartTitle?: string;
    /** 是否在 ACard 标题位展示（false 时标题写入 ECharts 绘图区） */
    showCardTitle?: boolean;
    /** 排除不参与饼图的指标 code */
    excludeCodes?: string[];
    extraLinkText?: string;
    extraLinkRoute?: { name: string; query?: Record<string, string> };
    colors?: string[];
    /** 按指标 code 指定扇区颜色 */
    colorByCode?: Record<string, string>;
    /** 值为 0 是否仍展示在图例（平台组织治理建议开启） */
    showZeroInLegend?: boolean;
    /** 承接池工单状态分布：固定五种主状态图例（含待接单 0 值占位） */
    workOrderPoolPie?: boolean;
    /** 绘图区固定高度 class（与趋势图 h-360px 对齐时可传 h-360px） */
    chartBodyClass?: string;
    fillHeight?: boolean;
  }>(),
  {
    section: null,
    loading: false,
    loaded: false,
    chartTitle: '状态分布',
    showCardTitle: false,
    excludeCodes: () => ['CURRENT_TOTAL'],
    extraLinkText: '',
    extraLinkRoute: undefined,
    colors: () => [...HOME_PIE_COLORS],
    colorByCode: () => ({}),
    showZeroInLegend: false,
    chartBodyClass: 'h-320px'
  }
);

const router = useRouter();

/** 图例是否展示 0 值项（承接池饼图固定为 true） */
const showZeroLegend = computed(() => props.workOrderPoolPie || props.showZeroInLegend);

/** 参与分区的全部指标（含 0 值，用于图例） */
const legendItems = computed(() => {
  if (props.workOrderPoolPie) {
    return buildWorkOrderPoolPieLegendItems(
      props.section,
      props.colorByCode,
      props.colors?.length ? props.colors : PIE_COLORS
    );
  }
  const metrics = props.section?.metrics || [];
  return metrics
    .filter(item => !props.excludeCodes.includes(item.code || ''))
    .map((item, index) => {
      const code = item.code || '';
      const color =
        props.colorByCode?.[code] ||
        props.colors?.[index % (props.colors?.length || PIE_COLORS.length)] ||
        PIE_COLORS[index % PIE_COLORS.length];
      return {
        code,
        name: item.title || code,
        value: toDashboardCount(item.value),
        color,
        routeTarget: item.routeTarget
      };
    });
});

/** 实际渲染扇区：仅 value > 0 */
const chartData = computed(() => legendItems.value.filter(item => item.value > 0));

const hasRenderableData = computed(() => chartData.value.length > 0);
const hasLegendOnlyZero = computed(
  () => showZeroLegend.value && legendItems.value.length > 0 && !hasRenderableData.value
);

const seriesColors = computed(() => (props.colors?.length ? props.colors : PIE_COLORS));

function handleSliceClick(dataIndex: number) {
  const item = chartData.value[dataIndex];
  navigateHomeRoute(router, item?.routeTarget);
}

const { domRef, updateOptions } = useEcharts(
  () => ({
    title: { text: '' },
    tooltip: { trigger: 'item' },
    legend: { bottom: '2%', left: 'center', itemStyle: { borderWidth: 0 } },
    series: [
      {
        color: PIE_COLORS,
        name: props.chartTitle,
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 500 },
          scale: true,
          scaleSize: 6
        },
        labelLine: { show: false },
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
  const w = width.value;
  updateOptions(opts => {
    opts.title = props.showCardTitle ? { text: '' } : buildHomePieChartInlineTitle(props.chartTitle);
    opts.legend = {
      ...resolvePieLegendLayout(w),
      itemStyle: { borderWidth: 0 },
      data: showZeroLegend.value
        ? legendItems.value.map(item => ({
            name: item.name,
            itemStyle: { color: item.color }
          }))
        : chartData.value.map(item => item.name)
    };
    opts.series[0].center = resolvePieCenter(w);
    opts.series[0].radius = resolvePieRadius(w);
    opts.series[0].color = seriesColors.value;
    opts.series[0].data = chartData.value.map(item => ({
      name: item.name,
      value: item.value,
      itemStyle: item.color ? { color: item.color } : undefined
    }));
    return opts;
  });
}

watch(
  () => [
    width.value,
    height.value,
    legendItems.value,
    chartData.value,
    props.loaded,
    props.chartTitle,
    props.showCardTitle,
    props.colorByCode,
    props.colors,
    props.showZeroInLegend,
    props.workOrderPoolPie,
    showZeroLegend.value
  ],
  () => {
    if (width.value > 0 && height.value > 0 && (hasRenderableData.value || hasLegendOnlyZero.value)) {
      applyChartData();
    }
  },
  { flush: 'post', deep: true }
);

function openExtraLink() {
  if (!props.extraLinkRoute?.name) return;
  router.push({
    name: props.extraLinkRoute.name,
    query: props.extraLinkRoute.query
  });
}
</script>

<template>
  <ACard
    :bordered="false"
    class="card-wrapper"
    :class="{
      'home-section-chart-card': showCardTitle,
      'home-chart-card--fill': fillHeight
    }"
    :loading="loading"
    :title="showCardTitle ? chartTitle : undefined"
    :body-style="
      fillHeight
        ? {
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            paddingTop: showCardTitle ? '8px' : undefined
          }
        : showCardTitle
          ? { paddingTop: '8px' }
          : undefined
    "
  >
    <template v-if="extraLinkText && extraLinkRoute" #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="openExtraLink">{{ extraLinkText }}</a>
    </template>
    <div
      v-if="hasRenderableData || hasLegendOnlyZero"
      ref="domRef"
      class="overflow-hidden"
      :class="fillHeight ? 'home-chart-box--fill' : chartBodyClass"
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
