<script setup lang="ts">
/**
 * 工作台入口：按登录主体类型分流，三套首页互不影响。
 * - PLATFORM：平台超管运维首页
 * - HQ：总部运营看板（网点汇总等）
 * - 其它：普通业务账号首页（工单卡片 + 趋势图 + 通知动态，与改造前一致）
 */
import { computed } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import HqHomeIndex from './hq-home-index.vue';
import PlatformHomeIndex from './platform-home-index.vue';
import StandardHomeIndex from './standard-home-index.vue';

defineOptions({
  name: 'HomeIndex'
});

const authStore = useAuthStore();

const isPlatformAdmin = computed(() => authStore.userInfo.currentSubjectType === 'PLATFORM');
const isHqAccount = computed(() => authStore.userInfo.currentSubjectType === 'HQ');
</script>

<template>
  <PlatformHomeIndex v-if="isPlatformAdmin" />
  <HqHomeIndex v-else-if="isHqAccount" />
  <StandardHomeIndex v-else />
</template>

<style scoped></style>
