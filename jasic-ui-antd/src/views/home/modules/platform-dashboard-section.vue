<script setup lang="ts">
/**
 * 平台超管看板区块：组织 KPI + 图表数据加载（与总部工单看板隔离）。
 */
import { onMounted } from 'vue';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';
import PlatformKpiCards from './platform-kpi-cards.vue';

defineOptions({
  name: 'PlatformDashboardSection'
});

const { showDashboard, loading, loaded, loadPlatformDashboard } = usePlatformDashboard();

onMounted(() => {
  loadPlatformDashboard();
});
</script>

<template>
  <ASpin :spinning="loading && !loaded">
    <PlatformKpiCards v-if="showDashboard" />
  </ASpin>
</template>
