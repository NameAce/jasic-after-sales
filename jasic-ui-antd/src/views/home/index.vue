<script setup lang="ts">
/**
 * 工作台入口：按登录主体类型分流，三套首页互不影响。
 * 外层容器撑满主内容区；内容超出时纵向滚动，图表保持固定高度不随视口压缩。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
  <!-- 按 currentSubjectType 分流：平台治理 / 总部调度 / 网点服务工作台 -->
  <div class="home-page">
    <PlatformHomeIndex v-if="isPlatformAdmin" />
    <HqHomeIndex v-else-if="isHqAccount" />
    <StandardHomeIndex v-else />
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
