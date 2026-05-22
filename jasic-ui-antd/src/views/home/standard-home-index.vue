<script setup lang="ts">
/**
 * 服务主体（网点管理员）首页「服务工作台」：KPI + 已转出、无分区标题、单行占满（不展示待接单/已完成/已关闭），
 * 近七天事件趋势与状态分布图在上，历史参与入口置于图表区下方。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import type { HomeSectionVO } from '@/service/api';
import { useAuth } from '@/hooks/business/auth';
import { useHomeDashboardOnMount } from './composables/use-home-dashboard-on-mount';
import { filterHomeKpiPoolMetrics } from './composables/dashboard-helpers';
import { useServiceDashboard } from './composables/use-service-dashboard';
import { WORK_ORDER_METRIC_STYLES, WORK_ORDER_PIE_COLOR_BY_CODE } from './composables/home-metric-styles';
import HomeWorkbenchHeader from './modules/home-workbench-header.vue';
import HomeMetricCards from './modules/home-metric-cards.vue';
import HomeTrendChart from './modules/home-trend-chart.vue';
import HomeSectionPieChart from './modules/home-section-pie-chart.vue';
import ServiceHistoryEntryCard from './modules/service-history-entry-card.vue';

defineOptions({
  name: 'StandardHomeIndex'
});

const { hasAuth } = useAuth();
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

const { loaded, loading, loadServiceDashboard, title, currentPool, transfer, trend, transferMetric } =
  useServiceDashboard();

useHomeDashboardOnMount(loadServiceDashboard, loaded);

const workOrderRoute = { name: 'after-sales_work-order', query: { viewScope: 'CURRENT' } };

/** 承接池（已过滤）仅用于顶部 KPI 卡片 */
const filteredCurrentPool = computed(() => filterHomeKpiPoolMetrics(currentPool.value));

/**
 * 作用：承接工单池展示分区，在池内状态指标之后追加「已转出」，与承接卡同一行排列。
 * 说明：饼图仍用原始 currentPool（含待接单、已关闭），避免转出数混入状态分布。
 * @returns 合并后的 HomeSectionVO 或 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const currentPoolWithTransfer = computed((): HomeSectionVO | null => {
  const pool = filteredCurrentPool.value;
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
    <!-- 服务工作台：横幅 + KPI（含已转出）+ 趋势/饼图 + 历史参与入口 -->
    <HomeWorkbenchHeader
      class="home-dashboard__shrink"
      :title="title"
      :loading="loading"
      :transfer-metric="canViewWorkOrder ? transferMetric : null"
    />
    <HomeMetricCards
      v-if="canViewWorkOrder"
      class="home-dashboard__shrink"
      :section="currentPoolWithTransfer"
      :loading="loading"
      :metric-styles="WORK_ORDER_METRIC_STYLES"
      :show-section-title="false"
      fill-row
      compact
    />
    <ARow v-if="canViewWorkOrder" class="home-dashboard__chart-row" :gutter="[16, 16]">
      <ACol :span="24" :lg="14" class="home-dashboard__chart-col">
        <HomeTrendChart
          :trend="trend"
          :loading="loading"
          :loaded="loaded"
          extra-link-text="工单列表"
          :extra-link-route="workOrderRoute"
        />
      </ACol>
      <ACol :span="24" :lg="10" class="home-dashboard__chart-col">
        <HomeSectionPieChart
          :section="currentPool"
          :loading="loading"
          :loaded="loaded"
          chart-title="当前承接工单状态分布"
          :show-card-title="false"
          work-order-pool-pie
          chart-body-class="h-360px"
          :color-by-code="WORK_ORDER_PIE_COLOR_BY_CODE"
          extra-link-text="当前工单"
          :extra-link-route="workOrderRoute"
        />
      </ACol>
    </ARow>
    <div class="home-dashboard__shrink">
      <ServiceHistoryEntryCard />
    </div>
  </div>
</template>

<style scoped>
/** 纵向堆叠；图表区双卡同高（标题 + 360px 绘图区），历史参与独占一行 */
.home-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.home-dashboard__shrink {
  flex-shrink: 0;
}

.home-dashboard__chart-row {
  align-items: stretch;
}

.home-dashboard__chart-col {
  display: flex;
}

.home-dashboard__chart-col > * {
  flex: 1;
  width: 100%;
}
</style>
