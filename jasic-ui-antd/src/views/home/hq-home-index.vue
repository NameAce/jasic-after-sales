<script setup lang="ts">
/**
 * 总部（subjectType=HQ）工作台首页：网点汇总、待接单排行等总部运营看板。
 * 工单统计卡片与图表仅在具备 `workorder:list` 权限时展示。
 */
import { computed } from 'vue';
import { useAuth } from '@/hooks/business/auth';
import { useBusinessHomeDashboard } from './composables/use-business-home-dashboard';
import { useHomeDashboardOnMount } from './composables/use-home-dashboard-on-mount';
import { useHqDashboard } from './composables/use-hq-dashboard';
import HeaderBanner from './modules/header-banner.vue';
import HqDashboardSection from './modules/hq-dashboard-section.vue';
import HqSiteBarChart from './modules/hq-site-bar-chart.vue';
import LineChart from './modules/line-chart.vue';
import PieChart from './modules/pie-chart.vue';
import ProjectNews from './modules/project-news.vue';

defineOptions({
  name: 'HqHomeIndex'
});

// 权限判断（首页卡片与图表仅工单权限可见）
const { hasAuth } = useAuth();
// 是否具备工单列表权限（控制统计卡片与图表区域）
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

const { hasSiteData } = useHqDashboard();
const { loaded, loadBusinessHomeDashboard } = useBusinessHomeDashboard();

/** 进入总部首页时拉取 `/dashboard/hq/home`；页签刷新时 force 重拉 */
useHomeDashboardOnMount(loadBusinessHomeDashboard, loaded);
// 是否展示网点待接单图（与动态并排，占左侧列）
const showSiteWaitChart = computed(() => canViewWorkOrder.value && hasSiteData.value);
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <HeaderBanner />
    <HqDashboardSection v-if="canViewWorkOrder" />
    <ARow v-if="canViewWorkOrder" :gutter="[16, 16]">
      <ACol :span="24" :lg="14">
        <LineChart />
      </ACol>
      <ACol :span="24" :lg="10">
        <PieChart />
      </ACol>
    </ARow>
    <ARow :gutter="[16, 16]">
      <ACol v-if="showSiteWaitChart" :span="24" :lg="14">
        <HqSiteBarChart />
      </ACol>
      <ACol :span="24" :lg="showSiteWaitChart ? 10 : 24">
        <ProjectNews />
      </ACol>
    </ARow>
  </ASpace>
</template>

<style scoped></style>
