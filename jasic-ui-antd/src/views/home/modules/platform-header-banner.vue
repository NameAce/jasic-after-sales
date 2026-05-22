<script setup lang="ts">
/**
 * 平台超管首页顶部：问候语 + 接口标题 + 组织治理快捷指标（来自 organization 分区）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { toDashboardCount } from '../composables/dashboard-helpers';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';

defineOptions({
  name: 'PlatformHeaderBanner'
});

const authStore = useAuthStore();
const router = useRouter();
const { loading, title, bannerMetrics } = usePlatformDashboard();

const statisticData = computed(() =>
  bannerMetrics.value.map(metric => ({
    key: metric.code || metric.title || '',
    title: metric.title || '',
    value: toDashboardCount(metric.value),
    routeTarget: metric.routeTarget
  }))
);

/**
 * 作用：点击组织治理快捷指标，按 routeTarget 跳转业务页。
 * @param item - 统计项（含 routeTarget）
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleStatisticClick(item: (typeof statisticData.value)[number]) {
  navigateHomeRoute(router, item.routeTarget);
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
  <!-- 平台横幅 -->
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
    <ARow :gutter="[16, 16]">
      <ACol :span="24" :md="statisticData.length ? 18 : 24">
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
      <ACol v-if="statisticData.length" :span="24" :md="6">
        <ASpace class="w-full justify-end" :size="32">
          <ATooltip v-for="item in statisticData" :key="item.key" :title="item.title">
            <AStatistic
              class="home-banner-stat cursor-pointer whitespace-nowrap"
              :title="item.title"
              :value="item.value"
              @click="handleStatisticClick(item)"
            />
          </ATooltip>
        </ASpace>
      </ACol>
    </ARow>
  </ACard>
</template>

<style scoped>
.home-banner-stat :deep(.ant-statistic-title) {
  color: #8c8c8c;
  font-size: 13px;
}

.home-banner-stat :deep(.ant-statistic-content-value) {
  color: #1677ff;
  font-size: 22px;
  font-weight: 600;
}
</style>
