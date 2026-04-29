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

defineOptions({
  name: 'HeaderBanner'
});

const authStore = useAuthStore();
const router = useRouter();
const loading = ref(false);
const { hasAuth } = useAuth();
const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

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

function getStatusCount(rows: WorkOrderStatusCountVO[] | null | undefined, mainStatus: string) {
  if (!Array.isArray(rows)) return 0;
  const target = rows.find(item => item?.mainStatus === mainStatus);
  return Number(target?.countNum || 0);
}

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

function openUserCenter() {
  router.push({ path: '/user-center' });
}

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
