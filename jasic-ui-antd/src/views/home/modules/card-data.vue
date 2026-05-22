<script setup lang="ts">
/**
 * 遗留业务首页工单状态 KPI 卡片（六态 + 全部，点击带 mainStatus 筛选）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { createReusableTemplate } from '@vueuse/core';
import { buildStatusCountMap } from '../composables/dashboard-helpers';
import { useBusinessHomeDashboard } from '../composables/use-business-home-dashboard';

defineOptions({
  name: 'CardData'
});

interface CardData {
  key: 'ALL' | 'PENDING_ASSIGN' | 'PENDING_TECH_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';
  title: string;
  value: number;
  color: {
    start: string;
    end: string;
  };
  icon: string;
}

const router = useRouter();
const { loading, workOrderStatus } = useBusinessHomeDashboard();

/** 首页工单状态卡片列表（含样式与图标，数值来自首页接口 workOrderStatus） */
const cardData = computed<CardData[]>(() => {
  const statusCountMap = buildStatusCountMap(workOrderStatus.value);

  return [
    {
      key: 'ALL',
      title: '全部工单',
      value: statusCountMap.ALL,
      color: { start: '#ec4786', end: '#b955a4' },
      icon: 'mdi:file-document-multiple-outline'
    },
    {
      key: 'PENDING_ASSIGN',
      title: '待派单',
      value: statusCountMap.PENDING_ASSIGN,
      color: { start: '#865ec0', end: '#5144b4' },
      icon: 'mdi:clipboard-clock-outline'
    },
    {
      key: 'PENDING_TECH_ACCEPT',
      title: '待接单',
      value: statusCountMap.PENDING_TECH_ACCEPT,
      color: { start: '#56cdf3', end: '#719de3' },
      icon: 'mdi:account-clock-outline'
    },
    {
      key: 'IN_PROGRESS',
      title: '维修中',
      value: statusCountMap.IN_PROGRESS,
      color: { start: '#fcbc25', end: '#f68057' },
      icon: 'mdi:tools'
    },
    {
      key: 'COMPLETED',
      title: '已完成',
      value: statusCountMap.COMPLETED,
      color: { start: '#2dcf95', end: '#1ea97a' },
      icon: 'mdi:check-circle-outline'
    },
    {
      key: 'CLOSED',
      title: '已关闭',
      value: statusCountMap.CLOSED,
      color: { start: '#8c8c8c', end: '#595959' },
      icon: 'mdi:archive-lock-outline'
    }
  ];
});

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

/**
 * 作用：生成卡片背景线性渐变 CSS。
 * @param color - 起止色配置
 * @returns linear-gradient 字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getGradientColor(color: CardData['color']) {
  return `linear-gradient(to bottom right, ${color.start}, ${color.end})`;
}

/**
 * 作用：按主状态跳转当前处理工单列表。
 * @param status - 主状态或 ALL
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openWorkOrderPage(status: CardData['key']) {
  const query = status === 'ALL' ? { viewScope: 'CURRENT' } : { viewScope: 'CURRENT', mainStatus: status };
  router.push({ name: 'after-sales_work-order', query });
}
</script>

<template>
  <!-- 卡片数据展示 -->
  <ACard :bordered="false" size="small" class="card-wrapper" :loading="loading">
    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div class="rd-8px px-16px pb-4px pt-8px text-white" :style="{ backgroundImage: gradientColor }">
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>

    <ARow :gutter="[16, 16]">
      <ACol v-for="item in cardData" :key="item.key" :span="24" :md="12" :lg="8" :xl="4">
        <GradientBg
          :gradient-color="getGradientColor(item.color)"
          class="flex-1 cursor-pointer"
          @click="openWorkOrderPage(item.key)"
        >
          <h3 class="text-16px">{{ item.title }}</h3>
          <div class="flex justify-between pt-12px">
            <SvgIcon :icon="item.icon" class="text-32px" />
            <CountTo :start-value="0" :end-value="item.value" class="text-30px text-white dark:text-dark" />
          </div>
        </GradientBg>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped></style>
