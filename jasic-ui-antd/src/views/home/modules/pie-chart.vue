<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { countWorkOrderStatus } from '@/service/api';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

defineOptions({
  name: 'PieChart'
});

const appStore = useAppStore();
const router = useRouter();

const { domRef, updateOptions } = useEcharts(() => ({
  title: {
    text: $t('page.home.workOrderStatusDistribution')
  },
  tooltip: {
    trigger: 'item'
  },
  legend: {
    bottom: '1%',
    left: 'center',
    itemStyle: {
      borderWidth: 0
    }
  },
  series: [
    {
      color: ['#5da8ff', '#8e9dff', '#fedc69', '#26deca'],
      name: $t('page.home.workOrderStatusDistribution'),
      type: 'pie',
      radius: ['45%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 1
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '12'
        }
      },
      labelLine: {
        show: false
      },
      data: [] as { name: string; value: number }[]
    }
  ]
}));

const STATUS_LABEL_MAP: Record<string, string> = {
  PENDING_ASSIGN: '待派单',
  PENDING_TECH_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
};

async function loadRealData() {
  const res = await countWorkOrderStatus({ viewScope: 'CURRENT' });
  const rows = Array.isArray(res.data) ? res.data : [];
  const data = rows
    .filter(item => item?.mainStatus && item.mainStatus !== 'ALL')
    .map(item => ({
      name: item.displayStatus || STATUS_LABEL_MAP[item.mainStatus] || item.mainStatus,
      value: Number(item.countNum || 0)
    }));

  updateOptions(opts => {
    opts.series[0].data = data;

    return opts;
  });
}

function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();
    opts.title = originOpts.title;
    opts.series[0].name = originOpts.series[0].name;

    return opts;
  });
}

async function init() {
  try {
    await loadRealData();
  } catch {
    updateOptions(opts => {
      opts.series[0].data = [];
      return opts;
    });
  }
}

watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

onMounted(() => {
  init();
});

function goWorkOrderStatusPage() {
  router.push({ name: 'after-sales_work-order', query: { viewScope: 'CURRENT' } });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper">
    <template #extra>
      <a class="text-primary" href="javascript:;" @click.prevent="goWorkOrderStatusPage">状态明细</a>
    </template>
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </ACard>
</template>

<style scoped></style>
