<script setup lang="ts">
/**
 * 服务主体首页顶部横幅：接口标题 + 用户问候 + 已转出快捷统计。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { useServiceDashboard } from '../composables/use-service-dashboard';

defineOptions({
  name: 'ServiceHeaderBanner'
});

const authStore = useAuthStore();
const router = useRouter();
const { hasAuth } = useAuth();
const { loading, title, transfer, transferOutCount } = useServiceDashboard();

const canViewWorkOrder = computed(() => hasAuth('workorder:list'));

/** 已转出指标（用于右上角统计与点击跳转） */
const transferMetric = computed(() => transfer.value?.metrics?.[0]);

/**
 * 作用：跳转个人中心。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openUserCenter() {
  router.push({ path: '/user-center' });
}

/**
 * 作用：点击已转出统计跳转工单列表（使用后端 routeTarget，含 hasTransfer 补全）。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openTransferList() {
  navigateHomeRoute(router, transferMetric.value?.routeTarget);
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <!-- 服务商横幅 -->
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
            <p class="text-#999 leading-30px">{{ title }}</p>
          </div>
        </div>
      </ACol>
      <ACol v-if="canViewWorkOrder && transferMetric" :span="24" :md="6">
        <ASpace class="w-full justify-end" :size="24">
          <ATooltip :title="transferMetric.statCondition">
            <AStatistic
              class="cursor-pointer whitespace-nowrap"
              :title="transferMetric.title || '已转出'"
              :value="transferOutCount"
              :suffix="transferMetric.unit || '单'"
              @click="openTransferList"
            />
          </ATooltip>
        </ASpace>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped></style>
