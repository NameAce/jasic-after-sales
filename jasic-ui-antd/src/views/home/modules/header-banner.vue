<script setup lang="ts">
/**
 * 业务首页顶部横幅：问候语 + 待办/消息/工单汇总（数据来自首页聚合接口）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import { toDashboardCount } from '../composables/dashboard-helpers';
import { useBusinessHomeDashboard } from '../composables/use-business-home-dashboard';

defineOptions({
  name: 'HeaderBanner'
});

const authStore = useAuthStore();
const router = useRouter();
const { hasAuth } = useAuth();
const { loading, overview } = useBusinessHomeDashboard();

/** 是否具备工单列表权限（控制「项目数」指标） */
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

interface StatisticData {
  key: 'project' | 'todo' | 'message';
  title: string;
  value: string;
}

/** 右上角统计卡片数据源（字段与后端 overview 对齐） */
const statisticData = computed<StatisticData[]>(() => {
  const ov = overview.value;
  const items: StatisticData[] = [
    {
      key: 'todo',
      title: $t('page.home.todo'),
      value: String(toDashboardCount(ov?.activeTodoCount))
    },
    {
      key: 'message',
      title: $t('page.home.message'),
      value: String(toDashboardCount(ov?.historyTodoCount))
    }
  ];

  if (canViewWorkOrder.value) {
    items.unshift({
      key: 'project',
      title: $t('page.home.projectCount'),
      value: String(toDashboardCount(ov?.workOrderTotal))
    });
  }

  return items;
});

/**
 * 作用：点击右上角统计项跳转工单列表或消息中心。
 * @param key - project / todo / message
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleStatisticClick(key: StatisticData['key']) {
  if (key === 'project') {
    router.push({
      name: 'after-sales_work-order',
      query: { viewScope: 'CURRENT' }
    });
    return;
  }
  if (key === 'todo') {
    router.push({ path: '/notify', query: { box: 'TODO' } });
    return;
  }
  router.push({ path: '/notify', query: { box: 'HISTORY' } });
}

/**
 * 作用：跳转个人中心。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openUserCenter() {
  router.push({ path: '/user-center' });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <!-- 遗留业务首页横幅：问候 + 工单/待办/消息统计（useBusinessHomeDashboard） -->
    <ARow :gutter="[16, 16]">
      <ACol :span="24" :md="18">
        <div class="flex-y-center cursor-pointer" @click="openUserCenter">
          <div class="size-72px shrink-0 overflow-hidden rd-1/2">
            <img
              src="https://jasic-after.oss-cn-shenzhen.aliyuncs.com/uniapp/contractor/default-avatar.jpg"
              class="size-full object-cover"
            />
          </div>
          <div class="pl-12px">
            <h3 class="text-18px font-semibold">
              {{
                $t('page.home.greeting', {
                  userName: authStore.userInfo.userName
                })
              }}
            </h3>
            <p class="text-#999 leading-30px">
              {{ $t('page.home.weatherDesc') }}
            </p>
          </div>
        </div>
      </ACol>
      <ACol :span="24" :md="6">
        <ASpace class="w-full justify-end" :size="24">
          <AStatistic
            v-for="item in statisticData"
            :key="item.key"
            class="cursor-pointer whitespace-nowrap"
            :title="item.title"
            :value="item.value"
            @click="handleStatisticClick(item.key)"
          />
        </ASpace>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped></style>
