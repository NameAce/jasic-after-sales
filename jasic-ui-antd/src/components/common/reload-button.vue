<script setup lang="ts">
/**
 * 页签栏等处使用的刷新按钮：`loading` 时图标旋转表示正在重载。
 */
import { $t } from '@/locales';

defineOptions({
  name: 'ReloadButton'
});

interface Props {
  loading?: boolean;
}

defineProps<Props>();
</script>

<template>
  <ButtonIcon :tooltip-content="$t('icon.reload')">
    <icon-ant-design-reload-outlined :class="{ 'animate-spin animate-duration-750': loading }" />
  </ButtonIcon>
</template>

<style scoped></style>
