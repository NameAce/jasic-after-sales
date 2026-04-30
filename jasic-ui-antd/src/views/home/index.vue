<script setup lang="ts">
/**
 * 工作台首页：横幅、项目动态与创意区；工单统计卡片与图表仅在具备 `workorder:list` 权限时展示。
 */
import { computed } from 'vue';
import { useAuth } from '@/hooks/business/auth';
import HeaderBanner from './modules/header-banner.vue';
import CardData from './modules/card-data.vue';
import LineChart from './modules/line-chart.vue';
import PieChart from './modules/pie-chart.vue';
import ProjectNews from './modules/project-news.vue';
import CreativityBanner from './modules/creativity-banner.vue';

// 权限判断（首页卡片与图表仅工单权限可见）
const { hasAuth } = useAuth();
// 是否具备工单列表权限（控制统计卡片与图表区域）
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));
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
