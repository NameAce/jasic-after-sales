<script setup lang="ts">
/**
 * 工单看板 KPI：有网点汇总时展示 siteSummary 全量指标，否则展示全网状态统计维度。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

/** 按接口返回维度生成 KPI 卡片（网点模式含 totalCount、completedCount） */
const kpiList = computed<KpiItem[]>(() => {
  if (hasSiteData.value) {
    return [
      {
        key: 'site',
        title: '承修网点',
        color: { start: '#5da8ff', end: '#3d7ee8' },
        icon: 'mdi:store-marker-outline'
      },
      {
        key: 'total',
        title: '工单总量',
        color: { start: '#2dcf95', end: '#1ea97a' },
        icon: 'mdi:file-document-multiple-outline'
      },
      {
        key: 'wait',
        title: '待接单合计',
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
        key: 'completed',
        title: '已完成合计',
        color: { start: '#26deca', end: '#1ab394' },
        icon: 'mdi:check-circle-outline'
      },
      {
        key: 'transfer',
        title: '转单工单',
        color: { start: '#ec4786', end: '#b955a4' },
        icon: 'mdi:swap-horizontal'
      }
    ];
  }

  return [
    {
      key: 'site',
      title: '工单总量',
      color: { start: '#5da8ff', end: '#3d7ee8' },
      icon: 'mdi:file-document-multiple-outline'
    },
    {
      key: 'wait',
      title: '待处理合计',
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
      key: 'completed',
      title: '已完成',
      color: { start: '#26deca', end: '#1ab394' },
      icon: 'mdi:check-circle-outline'
    },
    {
      key: 'transfer',
      title: '转单工单',
      color: { start: '#ec4786', end: '#b955a4' },
      icon: 'mdi:swap-horizontal'
    }
  ];
});

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

/**
 * 作用：生成 KPI 卡片背景线性渐变 CSS。
 * @param color - 起止色配置
 * @returns linear-gradient 字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getGradientColor(color: KpiItem['color']) {
  return `linear-gradient(to bottom right, ${color.start}, ${color.end})`;
}

/**
 * 作用：从 kpis 解析各卡片展示值（字段与 `/dashboard/hq/home` 对齐）。
 * @param key - 卡片业务 key
 * @returns 展示数值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveValue(key: string) {
  const k = kpis.value;
  if (key === 'site') return hasSiteData.value ? k.siteCount : k.totalCount;
  if (key === 'total') return k.totalCount;
  if (key === 'wait') return k.waitAcceptCount;
  if (key === 'progress') return k.inProgressCount;
  if (key === 'completed') return k.completedCount;
  return k.transferCount;
}

/**
 * 作用：跳转当前处理工单列表并附带筛选 query。
 * @param query - 额外 query（mainStatus、hasTransfer 等）
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openWorkOrderList(query?: Record<string, string>) {
  router.push({
    name: 'after-sales_work-order',
    query: { viewScope: 'CURRENT', ...query }
  });
}

/**
 * 作用：KPI 卡片点击分发（转单、已完成、维修中、总量等）。
 * @param key - 卡片 key
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleClick(key: string) {
  if (key === 'transfer') {
    openWorkOrderList({ hasTransfer: '1' });
    return;
  }
  if (key === 'completed') {
    openWorkOrderList({ mainStatus: 'COMPLETED' });
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
  <!-- 总部 KPI 卡片 -->
  <ASpin :spinning="loading">
    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div class="rd-8px px-16px pb-4px pt-8px text-white" :style="{ backgroundImage: gradientColor }">
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>

    <ARow :gutter="[16, 16]">
      <ACol
        v-for="item in kpiList"
        :key="item.key"
        :span="24"
        :sm="12"
        :lg="hasSiteData ? 8 : 6"
        :xl="hasSiteData ? 4 : 6"
      >
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
