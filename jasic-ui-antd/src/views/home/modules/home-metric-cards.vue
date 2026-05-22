<script setup lang="ts">
/**
 * 通用首页指标卡片：按 HomeSectionVO.metrics 渲染，点击走 routeTarget 跳转。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

// 当前分区指标列表（无 metrics 时为空数组，避免模板 v-for 报错）
const metrics = computed(() => (Array.isArray(props.section?.metrics) ? props.section!.metrics! : []));

// fillRow 且指标个数不能整除 24 时，大屏用 flex 均分（如 7 张卡）
const useFillRowFlex = computed(() => props.fillRow && metrics.value.length > 0 && 24 % metrics.value.length !== 0);

// 栅格占位：能整除 24 用 Ant Col span，否则走 flex
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

/**
 * 作用：按指标 code 生成卡片背景线性渐变 CSS。
 * @param code - 指标 code
 * @returns linear-gradient(...) 字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getGradientColor(code?: string) {
  const style = resolveMetricStyle(code, props.metricStyles);
  const { start, end } = style.color;
  const direction = style.gradientTo || 'bottom right';
  return `linear-gradient(to ${direction}, ${start}, ${end})`;
}

/**
 * 作用：按指标 code 解析卡片图标（mdi 等）。
 * @param code - 指标 code
 * @returns 图标 name
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getIcon(code?: string) {
  return resolveMetricStyle(code, props.metricStyles).icon;
}

/**
 * 作用：解析当前指标实际跳转目标（前端覆盖优先于接口 routeTarget）。
 * @param metric - 当前指标项
 * @returns HomeRouteTargetVO 或 undefined
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getMetricRouteTarget(metric: (typeof metrics.value)[number]) {
  return resolveMetricRouteTarget(metric, props.routeOverrides);
}

/**
 * 作用：判断指标卡片是否可点击跳转（需有有效 routeName）。
 * @param metric - 当前指标项
 * @returns 是否展示手型光标并响应点击
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function canNavigate(metric: (typeof metrics.value)[number]) {
  return Boolean(getMetricRouteTarget(metric)?.routeName);
}

/**
 * 作用：点击指标卡片，按解析后的 routeTarget 跳转对应业务页。
 * @param metric - 当前指标项
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openMetric(metric: (typeof metrics.value)[number]) {
  const target = getMetricRouteTarget(metric);
  if (!target?.routeName) return;
  // 统一走首页路由助手，处理 query 类型转换与工单列表 hasTransfer 补全
  navigateHomeRoute(router, target);
}
</script>

<template>
  <!-- 首页 KPI 渐变卡片：支持 fillRow 均分、routeOverrides 覆盖跳转 -->
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
