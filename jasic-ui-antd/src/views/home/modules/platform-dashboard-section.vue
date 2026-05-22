<script setup lang="ts">
/**
 * 平台超管看板区块：组织 KPI + 图表数据加载（与总部工单看板隔离）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
    <!-- 平台看板区块 -->
    <PlatformKpiCards v-if="showDashboard" />
  </ASpin>
</template>
