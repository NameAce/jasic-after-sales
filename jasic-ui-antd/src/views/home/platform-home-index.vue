<script setup lang="ts">
/**
 * 平台超级管理员（subjectType=PLATFORM）专属首页：组织治理与运维监控，不含工单业务看板。
 */
import { onMounted } from 'vue';
import PlatformDashboardSection from './modules/platform-dashboard-section.vue';
import PlatformHeaderBanner from './modules/platform-header-banner.vue';
import PlatformOperLogChart from './modules/platform-oper-log-chart.vue';
import PlatformOrgPieChart from './modules/platform-org-pie-chart.vue';
import { usePlatformDashboard } from './composables/use-platform-dashboard';

defineOptions({
  name: 'PlatformHomeIndex'
});

const { showDashboard, loadPlatformDashboard } = usePlatformDashboard();

/** 进入平台首页时拉取聚合接口，供顶部横幅与看板区共用 */
onMounted(() => {
  loadPlatformDashboard();
});
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <PlatformHeaderBanner />
    <PlatformDashboardSection />
    <ARow v-if="showDashboard" :gutter="[16, 16]">
      <ACol :span="24" :lg="14">
        <PlatformOperLogChart />
      </ACol>
      <ACol :span="24" :lg="10">
        <PlatformOrgPieChart />
      </ACol>
    </ARow>
  </ASpace>
</template>

<style scoped></style>
