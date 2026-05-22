<script setup lang="ts">
/**
 * 主题配置抽屉：集中展示暗色/布局/主题色/页面功能等子模块，显隐由 appStore.themeDrawerVisible 控制。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { SimpleScrollbar } from '@sa/materials';
import { useAppStore } from '@/store/modules/app';
import { $t } from '@/locales';
import DarkMode from './modules/dark-mode.vue';
import LayoutMode from './modules/layout-mode.vue';
import ThemeColor from './modules/theme-color.vue';
import PageFun from './modules/page-fun.vue';
import ConfigOperation from './modules/config-operation.vue';

defineOptions({
  name: 'ThemeDrawer'
});

// 全局应用状态（含抽屉开关）
const appStore = useAppStore();
</script>

<template>
  <ADrawer
    :open="appStore.themeDrawerVisible"
    :title="$t('theme.themeDrawerTitle')"
    :closable="false"
    :body-style="{ padding: '0px' }"
    @close="appStore.closeThemeDrawer"
  >
    <!-- 主题配置抽屉入口 -->
    <template #extra>
      <ButtonIcon icon="ant-design:close-outlined" class="h-28px" @click="appStore.closeThemeDrawer" />
    </template>
    <SimpleScrollbar>
      <div class="px-24px pb-24px pt-8px">
        <DarkMode />
        <LayoutMode />
        <ThemeColor />
        <PageFun />
      </div>
    </SimpleScrollbar>
    <template #footer>
      <ConfigOperation />
    </template>
  </ADrawer>
</template>

<style scoped></style>
