<script setup lang="ts">
/**
 * 服务主体首页指标卡片：按 HomeSectionVO.metrics 渲染，点击走 routeTarget 跳转。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { createReusableTemplate } from '@vueuse/core';
import type { HomeSectionVO } from '@/service/api';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { DEFAULT_SERVICE_METRIC_STYLE, SERVICE_METRIC_CARD_STYLES } from '../composables/service-metric-styles';
import { toDashboardCount } from '../composables/dashboard-helpers';
import { useServiceDashboard } from '../composables/use-service-dashboard';

defineOptions({
  name: 'ServiceMetricCards'
});

const props = defineProps<{
  /** 首页分区数据（承接工单池或已转出） */
  section?: HomeSectionVO | null;
}>();

const router = useRouter();
const { loading } = useServiceDashboard();

const metrics = computed(() => (Array.isArray(props.section?.metrics) ? props.section!.metrics! : []));

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

/**
 * 作用：按指标 code 生成卡片背景线性渐变 CSS。
 * @param code - 指标 code
 * @returns linear-gradient 字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getGradientColor(code?: string) {
  const style = (code && SERVICE_METRIC_CARD_STYLES[code]) || DEFAULT_SERVICE_METRIC_STYLE;
  const { start, end } = style.color;
  return `linear-gradient(to bottom right, ${start}, ${end})`;
}

/**
 * 作用：按指标 code 解析卡片图标。
 * @param code - 指标 code
 * @returns mdi 图标名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getIcon(code?: string) {
  const style = (code && SERVICE_METRIC_CARD_STYLES[code]) || DEFAULT_SERVICE_METRIC_STYLE;
  return style.icon;
}

/**
 * 作用：点击指标卡片，按后端 routeTarget 进入工单列表。
 * @param metric - 当前指标项
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openMetric(metric: (typeof metrics.value)[number]) {
  navigateHomeRoute(router, metric?.routeTarget);
}
</script>

<template>
  <!-- 服务商指标卡片 -->
  <ACard :bordered="false" size="small" class="card-wrapper" :loading="loading">
    <template v-if="section?.title" #title>
      {{ section.title }}
    </template>

    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div class="rd-8px px-16px pb-4px pt-8px text-white" :style="{ backgroundImage: gradientColor }">
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>

    <ARow :gutter="[16, 16]">
      <ACol
        v-for="metric in metrics"
        :key="metric.code || metric.title"
        :span="24"
        :md="12"
        :lg="metrics.length <= 2 ? 12 : 8"
        :xl="metrics.length <= 2 ? 12 : 4"
      >
        <ATooltip :title="metric.statCondition">
          <GradientBg
            :gradient-color="getGradientColor(metric.code)"
            class="flex-1 cursor-pointer"
            @click="openMetric(metric)"
          >
            <h3 class="text-16px">{{ metric.title }}</h3>
            <div class="flex justify-between pt-12px">
              <SvgIcon :icon="getIcon(metric.code)" class="text-32px" />
              <div class="text-30px text-white dark:text-dark">
                <CountTo :start-value="0" :end-value="toDashboardCount(metric.value)" />
                <span v-if="metric.unit" class="ml-4px text-16px">{{ metric.unit }}</span>
              </div>
            </div>
          </GradientBg>
        </ATooltip>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped></style>
