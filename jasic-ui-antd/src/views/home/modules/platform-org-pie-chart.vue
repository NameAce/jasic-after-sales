<script setup lang="ts">
/**
 * 平台超管：按主体类型（平台/总部/网点）的公司数量分布饼图。
 */
import { nextTick, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useElementSize } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';

defineOptions({
  name: 'PlatformOrgPieChart'
});

const appStore = useAppStore();
const router = useRouter();
const { loading, subjectChartItems } = usePlatformDashboard();

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.home.platformOrgDistribution')
  },
  tooltip: {
    trigger: 'item'
  },
  legend: {
    bottom: '1%',
    left: 'center'
  },
  series: [
    {
      color: ['#5da8ff', '#fcbc25', '#26deca'],
      name: $t('page.home.platformOrgDistribution'),
      type: 'pie',
      radius: ['45%', '75%'],
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 1
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 12 }
      },
      data: [] as { name: string; value: number }[]
    }
  ]
}));

const { width, height } = useElementSize(domRef);

async function syncChart() {
  await nextTick();
  const items = subjectChartItems.value;
  updateOptions(opts => {
    opts.title = { text: $t('page.home.platformOrgDistribution') };
    opts.series[0].data = items.map(item => ({ name: item.label, value: item.value }));
    return opts;
  });
}

watch(
  () => [subjectChartItems.value, width.value, height.value],
  () => {
    syncChart();
  },
  { flush: 'post', deep: true }
);

watch(
  () => appStore.locale,
  () => {
    syncChart();
  }
);

function goOrgPage() {
  router.push({ path: '/org/company' });
}

onMounted(() => {
  syncChart();
});
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goOrgPage">{{ $t('page.home.platformViewOrg') }}</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden" />
  </ACard>
</template>

<style scoped></style>
