<script setup lang="ts">
/**
 * 系统 Logo + 标题：点击回首页，标题优先展示当前公司名称。
 */
import { computed } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineOptions({
  name: 'GlobalLogo'
});

interface Props {
  /** Whether to show the title */
  showTitle?: boolean;
}

withDefaults(defineProps<Props>(), {
  showTitle: true
});

const authStore = useAuthStore();
// 侧栏/顶栏标题：优先展示当前公司名称
const systemTitle = computed(() => authStore.userInfo.currentCompanyName || $t('system.title'));
</script>

<template>
  <RouterLink to="/" class="w-full flex-center nowrap-hidden">
    <SystemLogo class="size-32px" />
    <h2 v-show="showTitle" class="pl-8px text-16px text-primary font-bold transition duration-300 ease-in-out">
      {{ systemTitle }}
    </h2>
  </RouterLink>
</template>

<style scoped></style>
