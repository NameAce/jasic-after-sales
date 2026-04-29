<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createReusableTemplate } from '@vueuse/core';
import { type WorkOrderStatusCountVO, countWorkOrderStatus } from '@/service/api';

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
const loading = ref(false);
const statusCountMap = ref<Record<CardData['key'], number>>({
  ALL: 0,
  PENDING_ASSIGN: 0,
  PENDING_TECH_ACCEPT: 0,
  IN_PROGRESS: 0,
  COMPLETED: 0,
  CLOSED: 0
});

const cardData = computed<CardData[]>(() => [
  {
    key: 'ALL',
    title: '全部工单',
    value: statusCountMap.value.ALL,
    color: {
      start: '#ec4786',
      end: '#b955a4'
    },
    icon: 'mdi:file-document-multiple-outline'
  },
  {
    key: 'PENDING_ASSIGN',
    title: '待派单',
    value: statusCountMap.value.PENDING_ASSIGN,
    color: {
      start: '#865ec0',
      end: '#5144b4'
    },
    icon: 'mdi:clipboard-clock-outline'
  },
  {
    key: 'PENDING_TECH_ACCEPT',
    title: '待接单',
    value: statusCountMap.value.PENDING_TECH_ACCEPT,
    color: {
      start: '#56cdf3',
      end: '#719de3'
    },
    icon: 'mdi:account-clock-outline'
  },
  {
    key: 'IN_PROGRESS',
    title: '维修中',
    value: statusCountMap.value.IN_PROGRESS,
    color: {
      start: '#fcbc25',
      end: '#f68057'
    },
    icon: 'mdi:tools'
  },
  {
    key: 'COMPLETED',
    title: '已完成',
    value: statusCountMap.value.COMPLETED,
    color: {
      start: '#2dcf95',
      end: '#1ea97a'
    },
    icon: 'mdi:check-circle-outline'
  },
  {
    key: 'CLOSED',
    title: '已关闭',
    value: statusCountMap.value.CLOSED,
    color: {
      start: '#8c8c8c',
      end: '#595959'
    },
    icon: 'mdi:archive-lock-outline'
  }
]);

interface GradientBgProps {
  gradientColor: string;
}

const [DefineGradientBg, GradientBg] = createReusableTemplate<GradientBgProps>();

function getGradientColor(color: CardData['color']) {
  return `linear-gradient(to bottom right, ${color.start}, ${color.end})`;
}

function syncStatusCountMap(rows: WorkOrderStatusCountVO[] | null | undefined) {
  const next: Record<CardData['key'], number> = {
    ALL: 0,
    PENDING_ASSIGN: 0,
    PENDING_TECH_ACCEPT: 0,
    IN_PROGRESS: 0,
    COMPLETED: 0,
    CLOSED: 0
  };
  if (Array.isArray(rows)) {
    for (const item of rows) {
      const key = item?.mainStatus as CardData['key'] | undefined;
      if (key && key in next) {
        next[key] = Number(item.countNum || 0);
      }
    }
  }
  statusCountMap.value = next;
}

async function loadStatusCount() {
  loading.value = true;
  try {
    const res = await countWorkOrderStatus({ viewScope: 'CURRENT' });
    syncStatusCountMap(res.data);
  } finally {
    loading.value = false;
  }
}

function openWorkOrderPage(status: CardData['key']) {
  const query = status === 'ALL' ? { viewScope: 'CURRENT' } : { viewScope: 'CURRENT', mainStatus: status };
  router.push({ name: 'after-sales_work-order', query });
}

onMounted(() => {
  loadStatusCount();
});
</script>

<template>
  <ACard :bordered="false" size="small" class="card-wrapper" :loading="loading">
    <!-- define component start: GradientBg -->
    <DefineGradientBg v-slot="{ $slots, gradientColor }">
      <div class="rd-8px px-16px pb-4px pt-8px text-white" :style="{ backgroundImage: gradientColor }">
        <component :is="$slots.default" />
      </div>
    </DefineGradientBg>
    <!-- define component end: GradientBg -->

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
