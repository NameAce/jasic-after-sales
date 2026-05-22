<script setup lang="ts">
/**
 * 工单类首页顶部横幅：接口标题 + 用户问候 + 可选已转出快捷统计。
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { HomeMetricVO } from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { toDashboardCount } from '../composables/dashboard-helpers';

defineOptions({
  name: 'HomeWorkbenchHeader'
});

const props = withDefaults(
  defineProps<{
    title?: string;
    loading?: boolean;
    /** 已转出指标（总部/服务主体） */
    transferMetric?: HomeMetricVO | null;
  }>(),
  {
    title: '',
    loading: false,
    transferMetric: null
  }
);

const authStore = useAuthStore();
const router = useRouter();

const transferCount = computed(() => toDashboardCount(props.transferMetric?.value));

function openUserCenter() {
  router.push({ path: '/user-center' });
}

function openTransferList() {
  navigateHomeRoute(router, props.transferMetric?.routeTarget);
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <ARow :gutter="[16, 16]">
      <ACol :span="24" :md="transferMetric ? 18 : 24">
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
            <p class="text-#999 leading-30px">{{ title }}</p>
          </div>
        </div>
      </ACol>
      <ACol v-if="transferMetric" :span="24" :md="6">
        <ASpace class="w-full justify-end" :size="24">
          <ATooltip :title="transferMetric.statCondition">
            <AStatistic
              class="cursor-pointer whitespace-nowrap"
              :title="transferMetric.title || '已转出'"
              :value="transferCount"
              @click="openTransferList"
            />
          </ATooltip>
        </ASpace>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped></style>
