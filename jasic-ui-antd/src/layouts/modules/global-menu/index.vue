<script setup lang="ts">
/**
 * 菜单根组件：按主题布局 mode 动态选择竖/横/混合等具体菜单实现，并注入选中项背景 CSS 变量。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import type { Component } from 'vue';
import { transformColorWithOpacity } from '@sa/utils';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import VerticalMenu from './modules/vertical-menu.vue';
import VerticalMixMenu from './modules/vertical-mix-menu.vue';
import HorizontalMenu from './modules/horizontal-menu.vue';
import HorizontalMixMenu from './modules/horizontal-mix-menu.vue';
import ReversedHorizontalMixMenu from './modules/reversed-horizontal-mix-menu.vue';

defineOptions({
  name: 'GlobalMenu'
});

const appStore = useAppStore();
const themeStore = useThemeStore();

// 按 layout mode 选择具体菜单组件（含水平混合是否反转）
const activeMenu = computed(() => {
  const menuMap: Record<UnionKey.ThemeLayoutMode, Component> = {
    vertical: VerticalMenu,
    'vertical-mix': VerticalMixMenu,
    horizontal: HorizontalMenu,
    'horizontal-mix': themeStore.layout.reverseHorizontalMix ? ReversedHorizontalMixMenu : HorizontalMixMenu
  };

  return menuMap[themeStore.layout.mode];
});

// 移动端竖向布局下强制重挂载菜单以修正宽度样式
const reRenderVertical = computed(() => themeStore.layout.mode === 'vertical' && appStore.isMobile);

// 菜单选中项背景：亮/暗下对主色做不同透明度叠加
const selectedBgColor = computed(() => {
  const { darkMode, themeColor } = themeStore;

  const light = transformColorWithOpacity(themeColor, 0.1, '#ffffff');
  const dark = transformColorWithOpacity(themeColor, 0.3, '#000000');

  return darkMode ? dark : light;
});
</script>

<template>
  <div>
    <!-- 全局菜单容器（按布局模式切换子菜单） -->
    <component :is="activeMenu" :key="reRenderVertical" />
  </div>
</template>

<style>
@import './index.scss';

.select-menu {
  --selected-bg-color: v-bind(selectedBgColor);
}
</style>
