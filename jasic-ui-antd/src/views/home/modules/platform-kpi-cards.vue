<script setup lang="ts">
/**
 * 平台超管 KPI：启用公司、角色数、通知场景配置、近7日失败操作（采样）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { createReusableTemplate } from '@vueuse/core';
import { getRecentDateRange } from '@/utils/datetime';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';

defineOptions({
  name: 'PlatformKpiCards'
});

const router = useRouter();
const { loading, kpis, operLogFailedCount } = usePlatformDashboard();

interface KpiItem {
  key: string;
  title: string;
  color: { start: string; end: string };
  icon: string;
}

const kpiList = computed<KpiItem[]>(() => [
  {
    key: 'companyEnabled',
    title: '启用公司',
    color: { start: '#5da8ff', end: '#3d7ee8' },
    icon: 'mdi:office-building-outline'
  },
  {
    key: 'role',
    title: '角色数',
    color: { start: '#8e9dff', end: '#6b7fe8' },
    icon: 'mdi:account-group-outline'
  },
  {
    key: 'notifyScene',
    title: '通知场景配置',
    color: { start: '#26deca', end: '#1aab97' },
    icon: 'mdi:bell-cog-outline'
  },
  {
    key: 'operFail',
    title: '近7日失败操作',
    color: { start: '#ec4786', end: '#b955a4' },
    icon: 'mdi:alert-circle-outline'
  }
]);

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
 * 作用：从平台看板 kpis 解析卡片展示值。
 * @param key - 卡片 key
 * @returns 展示数值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveValue(key: string) {
  const k = kpis.value;
  if (key === 'companyEnabled') return k.companyEnabled;
  if (key === 'role') return k.roleTotal;
  if (key === 'notifyScene') return k.notifySceneTotal;
  return operLogFailedCount.value;
}

/**
 * 作用：KPI 卡片点击跳转组织/角色/通知场景或失败操作日志。
 * @param key - 卡片 key
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleClick(key: string) {
  if (key === 'companyEnabled') {
    router.push({ path: '/org/company' });
    return;
  }
  if (key === 'role') {
    router.push({ path: '/system/role' });
    return;
  }
  if (key === 'notifyScene') {
    // 与后端菜单 path=notifyScene 一致，完整路由为 /system/notifyScene
    router.push({ path: '/system/notifyScene' });
    return;
  }
  const { beginDate, endDate } = getRecentDateRange(7);
  router.push({
    path: '/log/operLog',
    query: {
      status: '0',
      beginDate,
      endDate
    }
  });
}
</script>

<template>
  <ASpin :spinning="loading">
    <!-- 平台 KPI 卡片 -->
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
