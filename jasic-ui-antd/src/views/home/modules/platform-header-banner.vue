<script setup lang="ts">
/**
 * 平台超管首页顶部：问候语 + 组织类指标（不含工单业务指标与个人通知待办）。
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';
import { usePlatformDashboard } from '../composables/use-platform-dashboard';

defineOptions({
  name: 'PlatformHeaderBanner'
});

const authStore = useAuthStore();
const router = useRouter();
const { loading, kpis } = usePlatformDashboard();

interface StatisticData {
  key: 'company' | 'user';
  title: string;
  value: string;
}

const statisticData = computed<StatisticData[]>(() => [
  {
    key: 'company',
    title: $t('page.home.summaryCompany'),
    value: String(kpis.value.companyTotal)
  },
  {
    key: 'user',
    title: $t('page.home.summaryUser'),
    value: String(kpis.value.userTotal)
  }
]);

function handleStatisticClick(key: StatisticData['key']) {
  if (key === 'company') {
    router.push({ path: '/org/company' });
    return;
  }
  router.push({ path: '/system/user' });
}

function openUserCenter() {
  router.push({ path: '/user-center' });
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper" :loading="loading">
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
              {{ $t('page.home.platformWeatherDesc') }}
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
