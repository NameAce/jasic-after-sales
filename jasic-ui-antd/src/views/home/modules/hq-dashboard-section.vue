<script setup lang="ts">
/**
 * 工单看板区块：KPI 卡片；无网点汇总时降级展示状态分布图（网点待接单图在首页与动态并排）。
 * 数据由父级 `hq-home-index` 统一拉取 `/dashboard/hq/home`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { useHqDashboard } from '../composables/use-hq-dashboard';
import HqKpiCards from './hq-kpi-cards.vue';
import HqStatusBarChart from './hq-status-bar-chart.vue';

defineOptions({
  name: 'HqDashboardSection'
});

const { showDashboard, hasSiteData, loading, loaded } = useHqDashboard();
</script>

<template>
  <!-- 总部看板区块 -->
  <ASpin :spinning="loading && !loaded">
    <ASpace v-if="showDashboard" direction="vertical" :size="16" class="w-full">
      <HqKpiCards />
      <ARow v-if="!hasSiteData" :gutter="[16, 16]">
        <ACol :span="24">
          <HqStatusBarChart />
        </ACol>
      </ARow>
    </ASpace>
  </ASpin>
</template>
