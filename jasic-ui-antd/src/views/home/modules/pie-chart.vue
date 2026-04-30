<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { countWorkOrderStatus } from '@/service/api';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { $t } from '@/locales';

// 应用语言、路由与 ECharts 封装（工单状态饼图）
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

// 工单主状态枚举 → 中文展示（接口无 displayStatus 时兜底）
const STATUS_LABEL_MAP: Record<string, string> = {
  PENDING_ASSIGN: '待派单',
  PENDING_TECH_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
};

/**
 * 作用：请求当前视角工单状态分布并更新饼图数据。
 * @param 无
 * @returns 返回 Promise，更新图表后结束
 */
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

/**
 * 作用：语言切换时刷新图表标题与系列名。
 * @param 无
 * @returns {void} 无
 */
function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();
    opts.title = originOpts.title;
    opts.series[0].name = originOpts.series[0].name;

    return opts;
  });
}

/**
 * 作用：首次挂载时加载数据，失败则清空系列。
 * @param 无
 * @returns 返回 Promise，初始化结束后结束
 */
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

// 切换语言时更新图表文案
watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

/**
 * 作用：挂载后初始化图表数据。
 * @param 无
 * @returns {void} 无
 */
onMounted(() => {
  init();
});

/**
 * 作用：跳转工单列表并按当前视角筛选。
 * @param 无
 * @returns {void} 无
 */
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
