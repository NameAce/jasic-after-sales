<script setup lang="ts">
/**
 * 通用首页指标卡片：按 HomeSectionVO.metrics 渲染，点击走 routeTarget 跳转。
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { createReusableTemplate } from '@vueuse/core';
import type { HomeRouteTargetVO, HomeSectionVO } from '@/service/api';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { resolveMetricRouteTarget } from '../composables/platform-basic-config-routes';
import { type HomeMetricCardStyle, resolveMetricStyle } from '../composables/home-metric-styles';
import { toDashboardCount } from '../composables/dashboard-helpers';

defineOptions({
  name: 'HomeMetricCards'
});

const props = withDefaults(
  defineProps<{
    /** 首页分区数据 */
    section?: HomeSectionVO | null;
    /** 卡片 loading */
    loading?: boolean;
    /** 指标 code → 样式映射 */
    metricStyles?: Record<string, HomeMetricCardStyle>;
    /** 是否展示分区标题（平台基础配置等场景可关闭） */
    showSectionTitle?: boolean;
    /** 指标在同一行均分占满 24 栅格（如平台基础配置四项） */
    fillRow?: boolean;
    /** 按指标 code 覆盖跳转目标（平台基础配置等，不改后端） */
    routeOverrides?: Record<string, HomeRouteTargetVO>;
    /** 紧凑布局：减小卡片内边距（平台四卡一行） */
    compact?: boolean;
  }>(),
  {
    section: null,
    loading: false,
    metricStyles: () => ({}),
    showSectionTitle: true,
    fillRow: false,
    routeOverrides: () => ({}),
    compact: false
  }
);

const router = useRouter();

const metrics = computed(() => (Array.isArray(props.section?.metrics) ? props.section!.metrics! : []));

/** fillRow 时大屏用 flex 均分（如 7 张卡无法整除 24 栅格） */
const useFillRowFlex = computed(() => props.fillRow && metrics.value.length > 0 && 24 % metrics.value.length !== 0);

/** 栅格占位：fillRow 且能整除 24 时用栅格，否则大屏走 flex */
const metricColSpan = computed(() => {
  const count = metrics.value.length;
  if (props.fillRow && count > 0 && !useFillRowFlex.value) {
    const lg = Math.floor(24 / count);
    return { span: 24, md: count <= 2 ? 12 : lg, lg, xl: lg };
  }
  return {
    span: 24,
    md: 12,
    lg: count <= 2 ? 12 : 8,
    xl: count <= 2 ? 12 : 4
  };
});

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

function getGradientColor(code?: string) {
  const style = resolveMetricStyle(code, props.metricStyles);
  const { start, end } = style.color;
  const direction = style.gradientTo || 'bottom right';
  return `linear-gradient(to ${direction}, ${start}, ${end})`;
}

function getIcon(code?: string) {
  return resolveMetricStyle(code, props.metricStyles).icon;
}

/** 解析当前指标实际跳转目标 */
function getMetricRouteTarget(metric: (typeof metrics.value)[number]) {
  return resolveMetricRouteTarget(metric, props.routeOverrides);
}

/** 是否可点击跳转 */
function canNavigate(metric: (typeof metrics.value)[number]) {
  return Boolean(getMetricRouteTarget(metric)?.routeName);
}

/** 点击指标卡片，按解析后的 routeTarget 跳转对应业务页 */
function openMetric(metric: (typeof metrics.value)[number]) {
  const target = getMetricRouteTarget(metric);
  if (!target?.routeName) return;
  navigateHomeRoute(router, target);
}
</script>

<template>
  <ACard
    :bordered="false"
    size="small"
    class="card-wrapper"
    :class="{
      'home-metric-cards--compact': compact,
      'home-metric-cards--no-title': !showSectionTitle
    }"
    :loading="loading"
    :body-style="compact ? { padding: '12px 16px' } : undefined"
  >
    <template v-if="showSectionTitle && section?.title" #title>
      {{ section.title }}
    </template>

    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div
        class="home-metric-card-inner rd-8px px-16px pb-8px pt-10px text-white shadow-sm"
        :style="{ backgroundImage: gradientColor }"
      >
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>

    <ARow :gutter="[16, 16]" :class="{ 'home-metric-cards__row--fill': useFillRowFlex }">
      <ACol
        v-for="metric in metrics"
        :key="metric.code || metric.title"
        :span="useFillRowFlex ? 24 : metricColSpan.span"
        :md="useFillRowFlex ? (metrics.length <= 2 ? 12 : 8) : metricColSpan.md"
        :lg="useFillRowFlex ? undefined : metricColSpan.lg"
        :xl="useFillRowFlex ? undefined : metricColSpan.xl"
        :flex="useFillRowFlex ? 1 : undefined"
      >
        <GradientBg
          :gradient-color="getGradientColor(metric.code)"
          class="flex-1"
          :class="{ 'cursor-pointer': canNavigate(metric) }"
          @click="openMetric(metric)"
        >
          <h3 class="text-15px font-medium opacity-95">{{ metric.title }}</h3>
          <div class="flex items-end justify-between pt-14px">
            <SvgIcon :icon="getIcon(metric.code)" class="text-36px opacity-85" />
            <CountTo
              :start-value="0"
              :end-value="toDashboardCount(metric.value)"
              class="text-28px text-white font-semibold leading-none dark:text-dark"
            />
          </div>
        </GradientBg>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped>
.home-metric-cards--compact :deep(.ant-card-head),
.home-metric-cards--no-title :deep(.ant-card-head) {
  display: none;
}

/** 7 等分等无法整除 24 时，大屏 flex 占满一行 */
@media (min-width: 992px) {
  .home-metric-cards__row--fill {
    flex-wrap: nowrap !important;
  }

  .home-metric-cards__row--fill > :deep(.ant-col) {
    flex: 1 1 0 !important;
    min-width: 0;
    max-width: none;
  }
}

/** 悬停时微微放大，移开平滑恢复（transform 不影响布局占位） */
.home-metric-card-inner {
  transition: transform 0.22s ease;
}

.home-metric-card-inner:hover {
  transform: scale(1.04);
}
</style>
