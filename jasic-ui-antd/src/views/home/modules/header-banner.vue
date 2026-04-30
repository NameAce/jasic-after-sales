<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  type WorkOrderStatusCountVO,
  countWorkOrderStatus,
  getNotifyTodoCount,
  getNotifyTodoPage
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

// 顶部问候区：认证信息、指标与路由跳转
defineOptions({
  name: 'HeaderBanner'
});

const authStore = useAuthStore();
const router = useRouter();
// 顶部指标区骨架加载
const loading = ref(false);
const { hasAuth } = useAuth();
// 是否具备工单列表权限（控制「项目数」指标）
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

// 待办/消息/工单汇总指标
const metrics = ref({
  projectCount: 0,
  todoCount: 0,
  messageCount: 0
});

interface StatisticData {
  key: 'project' | 'todo' | 'message';
  title: string;
  value: string;
}

// 右上角统计卡片数据源
const statisticData = computed<StatisticData[]>(() => {
  const items: StatisticData[] = [
    {
      key: 'todo',
      title: $t('page.home.todo'),
      value: String(metrics.value.todoCount)
    },
    {
      key: 'message',
      title: $t('page.home.message'),
      value: String(metrics.value.messageCount)
    }
  ];

  if (canViewWorkOrder.value) {
    items.unshift({
      key: 'project',
      title: $t('page.home.projectCount'),
      value: String(metrics.value.projectCount)
    });
  }

  return items;
});

/**
 * 作用：从状态分布列表中取指定 mainStatus 的数量。
 * @param rows - 接口列表
 * @param mainStatus - 主状态编码
 * @returns 数量
 */
function getStatusCount(rows: WorkOrderStatusCountVO[] | null | undefined, mainStatus: string) {
  if (!Array.isArray(rows)) return 0;
  const target = rows.find(item => item?.mainStatus === mainStatus);
  return Number(target?.countNum || 0);
}

/**
 * 作用：拉取待办数、历史消息数及（若有权限）工单总数。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function loadHeaderMetrics() {
  loading.value = true;
  try {
    const [todoRes, messageRes] = await Promise.all([
      getNotifyTodoCount(),
      getNotifyTodoPage({ box: 'HISTORY', pageNum: 1, pageSize: 1 })
    ]);
    metrics.value.todoCount = Number(todoRes.data?.count || 0);
    metrics.value.messageCount = Number(messageRes.data?.total || 0);
    if (canViewWorkOrder.value) {
      const statusRes = await countWorkOrderStatus({ viewScope: 'CURRENT' });
      metrics.value.projectCount = getStatusCount(statusRes.data, 'ALL');
    } else {
      metrics.value.projectCount = 0;
    }
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：点击统计项跳转对应业务页。
 * @param key - 统计项 key
 * @returns {void} 无
 */
function handleStatisticClick(key: StatisticData['key']) {
  if (key === 'project') {
    router.push({ name: 'after-sales_work-order', query: { viewScope: 'CURRENT' } });
    return;
  }
  if (key === 'todo') {
    router.push({ path: '/notify', query: { box: 'TODO' } });
    return;
  }
  router.push({ path: '/notify', query: { box: 'HISTORY' } });
}

/**
 * 作用：跳转个人中心页。
 * @param 无
 * @returns {void} 无
 */
function openUserCenter() {
  router.push({ path: '/user-center' });
}

/**
 * 作用：挂载后加载顶部指标。
 * @param 无
 * @returns {void} 无
 */
onMounted(() => {
  loadHeaderMetrics();
});
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <ARow :gutter="[16, 16]">
      <ACol :span="24" :md="18">
        <div class="flex-y-center cursor-pointer" @click="openUserCenter">
          <div class="size-72px shrink-0 overflow-hidden rd-1/2">
            <img
              src="/@fs/D:/companyProject/售后/jasic-after-sales/mp/aftersale/static/images/default-avatar.jpg"
              class="size-full object-cover"
            />
          </div>
          <div class="pl-12px">
            <h3 class="text-18px font-semibold">
              {{ $t('page.home.greeting', { userName: authStore.userInfo.userName }) }}
            </h3>
            <p class="text-#999 leading-30px">{{ $t('page.home.weatherDesc') }}</p>
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
