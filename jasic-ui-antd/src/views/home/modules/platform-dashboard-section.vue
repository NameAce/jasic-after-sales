<script setup lang="ts">
/**
 * 平台超管看板区块：组织 KPI + 图表数据加载（与总部工单看板隔离）。
 */
import { useHomeDashboardOnMount } from '../composables/use-home-dashboard-on-mount';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';
import PlatformKpiCards from './platform-kpi-cards.vue';

defineOptions({
  name: 'PlatformDashboardSection'
});

const { showDashboard, loading, loaded, loadPlatformDashboard } = usePlatformDashboard();

/** 与 platform-home-index 共用看板 state；页签刷新 remount 时需 force 重拉 */
useHomeDashboardOnMount(loadPlatformDashboard, loaded);
</script>

<template>
  <ASpin :spinning="loading && !loaded">
    <PlatformKpiCards v-if="showDashboard" />
  </ASpin>
</template>
