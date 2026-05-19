<script setup lang="ts">
/**
 * 工单看板 KPI：有网点汇总时展示网点维度，否则展示全网状态统计维度。
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { createReusableTemplate } from '@vueuse/core';
import { useHqDashboard } from '../composables/use-hq-dashboard';

defineOptions({
  name: 'HqKpiCards'
});

const router = useRouter();
const { loading, kpis, hasSiteData } = useHqDashboard();

interface KpiItem {
  key: string;
  title: string;
  color: { start: string; end: string };
  icon: string;
}

const kpiList = computed<KpiItem[]>(() => [
  {
    key: 'site',
    title: hasSiteData.value ? '承修网点' : '工单总量',
    color: { start: '#5da8ff', end: '#3d7ee8' },
    icon: hasSiteData.value ? 'mdi:store-marker-outline' : 'mdi:file-document-multiple-outline'
  },
  {
    key: 'wait',
    title: hasSiteData.value ? '待接单合计' : '待处理合计',
    color: { start: '#fcbc25', end: '#f68057' },
    icon: 'mdi:clipboard-clock-outline'
  },
  {
    key: 'progress',
    title: '维修中合计',
    color: { start: '#8e9dff', end: '#6b7fe8' },
    icon: 'mdi:tools'
  },
  {
    key: 'transfer',
    title: '转单工单',
    color: { start: '#ec4786', end: '#b955a4' },
    icon: 'mdi:swap-horizontal'
  }
]);

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

function getGradientColor(color: KpiItem['color']) {
  return `linear-gradient(to bottom right, ${color.start}, ${color.end})`;
}

function resolveValue(key: string) {
  const k = kpis.value;
  if (key === 'site') return hasSiteData.value ? k.siteCount : k.totalCount;
  if (key === 'wait') return k.waitAcceptCount;
  if (key === 'progress') return k.inProgressCount;
  return k.transferCount;
}

function openWorkOrderList(query?: Record<string, string>) {
  router.push({
    name: 'after-sales_work-order',
    query: { viewScope: 'CURRENT', ...query }
  });
}

function handleClick(key: string) {
  if (key === 'transfer') {
    openWorkOrderList({ hasTransfer: '1' });
    return;
  }
  if (key === 'progress') {
    openWorkOrderList({ mainStatus: 'IN_PROGRESS' });
    return;
  }
  if (key === 'site' && !hasSiteData.value) {
    router.push({
      name: 'after-sales_work-order',
      query: { viewScope: 'ALL' }
    });
    return;
  }
  openWorkOrderList();
}
</script>

<template>
  <ASpin :spinning="loading">
    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div class="rd-8px px-16px pb-4px pt-8px text-white" :style="{ backgroundImage: gradientColor }">
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>

    <ARow :gutter="[16, 16]">
      <ACol v-for="item in kpiList" :key="item.key" :span="24" :sm="12" :lg="6">
        <GradientBg
          :gradient-color="getGradientColor(item.color)"
          class="flex-1 cursor-pointer"
          @click="handleClick(item.key)"
        >
          <h3 class="text-16px">{{ item.title }}</h3>
          <div class="flex justify-between pt-12px">
            <SvgIcon :icon="item.icon" class="text-32px" />
            <CountTo :start-value="0" :end-value="resolveValue(item.key)" class="text-30px text-white dark:text-dark" />
          </div>
        </GradientBg>
      </ACol>
    </ARow>
  </ASpin>
</template>

<style scoped></style>
