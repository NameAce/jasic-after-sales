<script setup lang="ts">
/**
 * 总部（subjectType=HQ）首页「调度看板」：顶部 KPI 不展示待接单/已关闭（饼图保留），无分区标题、单行占满。
 */
import { computed } from 'vue';
import type { HomeSectionVO } from '@/service/api';
import { useAuth } from '@/hooks/business/auth';
import { filterHomeKpiPoolMetrics } from './composables/dashboard-helpers';
import { useHomeDashboardOnMount } from './composables/use-home-dashboard-on-mount';
import { useHqDashboard } from './composables/use-hq-dashboard';
import { WORK_ORDER_METRIC_STYLES, WORK_ORDER_PIE_COLOR_BY_CODE } from './composables/home-metric-styles';
import HomeWorkbenchHeader from './modules/home-workbench-header.vue';
import HomeMetricCards from './modules/home-metric-cards.vue';
import HomeTrendChart from './modules/home-trend-chart.vue';
import HomeSectionPieChart from './modules/home-section-pie-chart.vue';

defineOptions({
  name: 'HqHomeIndex'
});

const { hasAuth } = useAuth();
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

const { loaded, loading, loadHqDashboard, title, workOrderPool, transfer, trend, transferMetric } = useHqDashboard();

useHomeDashboardOnMount(loadHqDashboard, loaded);

const workOrderRoute = { name: 'after-sales_work-order', query: { viewScope: 'CURRENT' } };

/** 承接池（已过滤）仅用于顶部 KPI 卡片 */
const filteredWorkOrderPool = computed(() => filterHomeKpiPoolMetrics(workOrderPool.value));

/**
 * 承接工单池展示分区：在池内状态指标之后追加「已转出」，与承接卡同一行排列。
 * 饼图仍使用原始 workOrderPool（含待接单、已关闭），避免转出数混入状态分布。
 */
const workOrderPoolWithTransfer = computed((): HomeSectionVO | null => {
  const pool = filteredWorkOrderPool.value;
  if (!pool) return null;
  const transferMetrics = transfer.value?.metrics;
  if (!Array.isArray(transferMetrics) || transferMetrics.length === 0) {
    return pool;
  }
  return {
    ...pool,
    metrics: [...(pool.metrics ?? []), ...transferMetrics]
  };
});
</script>

<template>
  <div class="home-dashboard">
    <HomeWorkbenchHeader
      class="home-dashboard__shrink"
      :title="title"
      :loading="loading"
      :transfer-metric="canViewWorkOrder ? transferMetric : null"
    />
    <HomeMetricCards
      v-if="canViewWorkOrder"
      class="home-dashboard__shrink"
      :section="workOrderPoolWithTransfer"
      :loading="loading"
      :metric-styles="WORK_ORDER_METRIC_STYLES"
      :show-section-title="false"
      fill-row
      compact
    />
    <ARow v-if="canViewWorkOrder" class="home-dashboard__charts" :gutter="[16, 16]">
      <ACol :span="24" :lg="14" class="home-dashboard__chart-col">
        <HomeTrendChart
          :trend="trend"
          :loading="loading"
          :loaded="loaded"
          extra-link-text="工单列表"
          :extra-link-route="workOrderRoute"
          fill-height
        />
      </ACol>
      <ACol :span="24" :lg="10" class="home-dashboard__chart-col">
        <HomeSectionPieChart
          :section="workOrderPool"
          :loading="loading"
          :loaded="loaded"
          chart-title="当前承接工单状态分布"
          :show-card-title="false"
          work-order-pool-pie
          :color-by-code="WORK_ORDER_PIE_COLOR_BY_CODE"
          extra-link-text="当前工单"
          :extra-link-route="workOrderRoute"
          fill-height
        />
      </ACol>
    </ARow>
  </div>
</template>

<style scoped>
.home-dashboard {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  gap: 16px;
}

.home-dashboard__shrink {
  flex-shrink: 0;
}

.home-dashboard__charts {
  flex: 1;
  min-height: 280px;
}

.home-dashboard__chart-col {
  height: 100%;
  min-height: 240px;
}
</style>
