<script setup lang="ts">
/**
 * 顶栏主题配置入口：打开主题抽屉。
 */
import { useAppStore } from '@/store/modules/app';
import { $t } from '@/locales';

defineOptions({
  name: 'ThemeButton'
});

// 控制主题抽屉显隐
const appStore = useAppStore();
</script>

<template>
  <ButtonIcon
    icon="majesticons:color-swatch-line"
    :tooltip-content="$t('icon.themeConfig')"
    trigger-parent
    @click="appStore.openThemeDrawer"
  />
</template>

<style scoped></style>
