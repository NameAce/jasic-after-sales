<script setup lang="ts">
/**
 * 普通业务账号首页（网点等，非总部、非平台）：横幅、工单状态卡片、趋势图与通知动态。
 * 不含总部网点汇总看板（HqDashboardSection / HqSiteBarChart）。
 */
import { computed, onMounted } from 'vue';
import { useAuth } from '@/hooks/business/auth';
import { useServiceDashboard } from './composables/use-service-dashboard';
import HeaderBanner from './modules/header-banner.vue';
import CardData from './modules/card-data.vue';
import LineChart from './modules/line-chart.vue';
import PieChart from './modules/pie-chart.vue';
import ProjectNews from './modules/project-news.vue';
import CreativityBanner from './modules/creativity-banner.vue';

defineOptions({
  name: 'StandardHomeIndex'
});

const { hasAuth } = useAuth();
const { loadServiceDashboard } = useServiceDashboard();
/** 是否具备工单列表权限（控制统计卡片与图表区域） */
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

/** 进入服务主体首页时拉取 `/dashboard/service/home` */
onMounted(() => {
  loadServiceDashboard();
});
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <HeaderBanner />
    <CardData v-if="canViewWorkOrder" />
    <ARow v-if="canViewWorkOrder" :gutter="[16, 16]">
      <ACol :span="24" :lg="14">
        <LineChart />
      </ACol>
      <ACol :span="24" :lg="10">
        <PieChart />
      </ACol>
    </ARow>
    <ARow :gutter="[16, 16]">
      <ACol :span="24" :lg="14">
        <ProjectNews />
      </ACol>
      <ACol :span="24" :lg="10">
        <CreativityBanner />
      </ACol>
    </ARow>
  </ASpace>
</template>

<style scoped></style>
