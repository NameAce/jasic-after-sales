<script setup lang="ts">
/**
 * 全屏切换图标按钮：`full` 为 true 时展示退出全屏文案与图标。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { $t } from '@/locales';

defineOptions({
  name: 'FullScreen'
});

interface Props {
  full?: boolean;
}

defineProps<Props>();
</script>

<template>
  <!-- 通用组件：full-screen -->
  <ButtonIcon :key="String(full)" :tooltip-content="full ? $t('icon.fullscreenExit') : $t('icon.fullscreen')">
    <icon-gridicons-fullscreen-exit v-if="full" />
    <icon-gridicons-fullscreen v-else />
  </ButtonIcon>
</template>

<style scoped></style>
