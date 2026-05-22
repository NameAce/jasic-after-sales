<script setup lang="ts">
/**
 * 平台超管（subjectType=PLATFORM）首页「治理看板」：
 * - 基础配置 KPI 卡片置顶（组织/账号图表之上）；
 * - 组织治理饼图（左）+ 账号治理柱状图（右），图表区占满剩余高度。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { useHomeDashboardOnMount } from './composables/use-home-dashboard-on-mount';
import { usePlatformDashboard } from './composables/use-platform-dashboard';
import { PLATFORM_BASIC_CONFIG_ROUTE_OVERRIDES } from './composables/platform-basic-config-routes';
import { PLATFORM_BASIC_CONFIG_METRIC_STYLES, PLATFORM_ORG_PIE_COLOR_BY_CODE } from './composables/home-metric-styles';
import PlatformHeaderBanner from './modules/platform-header-banner.vue';
import HomeMetricCards from './modules/home-metric-cards.vue';
import HomeSectionBarChart from './modules/home-section-bar-chart.vue';
import HomeSectionPieChart from './modules/home-section-pie-chart.vue';

defineOptions({
  name: 'PlatformHomeIndex'
});

const { loaded, loading, loadPlatformDashboard, organization, account, basicConfig } = usePlatformDashboard();

useHomeDashboardOnMount(loadPlatformDashboard, loaded);
</script>

<template>
  <!-- 平台治理看板：横幅 + 基础配置四卡 + 组织饼图 / 账号条形图 -->
  <div class="home-dashboard">
    <PlatformHeaderBanner class="home-dashboard__shrink" />
    <HomeMetricCards
      class="home-dashboard__shrink"
      :section="basicConfig"
      :loading="loading"
      :metric-styles="PLATFORM_BASIC_CONFIG_METRIC_STYLES"
      :route-overrides="PLATFORM_BASIC_CONFIG_ROUTE_OVERRIDES"
      :show-section-title="false"
      fill-row
      compact
    />
    <ARow class="home-dashboard__charts" :gutter="[16, 16]">
      <ACol :span="24" :lg="12" class="home-dashboard__chart-col">
        <HomeSectionPieChart
          :section="organization"
          :loading="loading"
          :loaded="loaded"
          chart-title="组织治理分布"
          :exclude-codes="[]"
          :color-by-code="PLATFORM_ORG_PIE_COLOR_BY_CODE"
          show-zero-in-legend
          fill-height
        />
      </ACol>
      <ACol :span="24" :lg="12" class="home-dashboard__chart-col">
        <HomeSectionBarChart
          :section="account"
          :loading="loading"
          :loaded="loaded"
          chart-title="账号治理分布"
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
