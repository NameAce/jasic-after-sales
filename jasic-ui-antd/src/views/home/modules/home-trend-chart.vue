<script setup lang="ts">
/**
 * 通用首页近七天事件趋势折线图：名称/code/values 均来自接口 HomeTrendVO，不堆叠（三条为独立日事件数）。
 * 点击某条折线/色块区域跳转工单列表，并按 series.code 回显已有筛选项后自动查询。
 */
import { computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import type { HomeTrendVO } from '@/service/api';
import { useEcharts } from '@/hooks/common/echarts';
import { alignTrendValuesToDays, toAxisDayLabel } from '../composables/dashboard-helpers';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { resolveTrendSeriesRouteTarget } from '../composables/home-trend-route';
import { HOME_TREND_LINE_GRID, buildTrendLineSeriesItem, resolveTrendLineColor } from '../composables/home-chart-theme';

defineOptions({
  name: 'HomeTrendChart'
});

const props = withDefaults(
  defineProps<{
    trend?: HomeTrendVO | null;
    loading?: boolean;
    loaded?: boolean;
    /** 右上角链接文案，为空则不展示 */
    extraLinkText?: string;
    /** 右上角链接跳转（工单列表等） */
    extraLinkRoute?: { name: string; query?: Record<string, string> };
    fillHeight?: boolean;
  }>(),
  {
    trend: null,
    loading: false,
    loaded: false,
    extraLinkText: '',
    extraLinkRoute: undefined
  }
);

const router = useRouter();

const trendPayload = computed(() => {
  const payload = props.trend;
  const days = Array.isArray(payload?.days) ? payload.days : [];
  const seriesList = Array.isArray(payload?.series) ? payload.series : [];

  return {
    title: payload?.title || '近 7 天事件趋势',
    days,
    series: seriesList.map((item, index) => {
      const code = item.code || '';
      return {
        code,
        name: item.name || code,
        color: resolveTrendLineColor(code, index),
        data: alignTrendValuesToDays(days, item.values)
      };
    })
  };
});

const { domRef, updateOptions } = useEcharts(
  () => ({
    title: { text: '' },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: { backgroundColor: '#6a7985' }
      }
    },
    legend: { data: [] as string[], top: 28, right: 16 },
    grid: { ...HOME_TREND_LINE_GRID },
    xAxis: { type: 'category', boundaryGap: false, data: [] as string[] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [] as ReturnType<typeof buildTrendLineSeriesItem>[]
  }),
  {
    onRender: chart => {
      chart.off('click');
      chart.on('click', params => {
        const clickParams = params as { componentType?: string; seriesIndex?: number };
        if (clickParams.componentType && clickParams.componentType !== 'series') return;
        const seriesIndex = Number(clickParams.seriesIndex);
        if (!Number.isFinite(seriesIndex) || seriesIndex < 0) return;
        const item = trendPayload.value.series[seriesIndex];
        const target = resolveTrendSeriesRouteTarget(item?.code, props.extraLinkRoute);
        navigateHomeRoute(router, target);
      });
    }
  }
);

const { width, height } = useElementSize(domRef);

async function applyTrendData() {
  await nextTick();
  const payload = trendPayload.value;
  updateOptions(opts => {
    opts.title.text = payload.title;
    opts.xAxis.data = payload.days.map(toAxisDayLabel);
    opts.legend.data = payload.series.map(item => item.name);
    opts.series = payload.series.map(item => ({
      ...buildTrendLineSeriesItem({ name: item.name, color: item.color, data: item.data, stacked: false }),
      cursor: 'pointer'
    }));
    return opts;
  });
}

watch(
  () => [width.value, height.value, trendPayload.value, props.loaded],
  () => {
    if (width.value > 0 && height.value > 0 && trendPayload.value.days.length) {
      applyTrendData();
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
    :class="{ 'home-chart-card--fill': fillHeight }"
    :loading="loading"
    :body-style="fillHeight ? { flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 } : undefined"
  >
    <template v-if="extraLinkText && extraLinkRoute" #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="openExtraLink">{{ extraLinkText }}</a>
    </template>
    <div
      ref="domRef"
      class="cursor-pointer overflow-hidden"
      :class="fillHeight ? 'home-chart-box--fill' : 'h-360px'"
    ></div>
  </ACard>
</template>

<style scoped>
.home-chart-card--fill {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.home-chart-box--fill {
  flex: 1;
  min-height: 240px;
}
</style>
