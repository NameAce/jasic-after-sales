<script setup lang="ts">
/**
 * 根组件：注入 Ant Design Vue 主题与中文、AppProvider、路由出口及可选全屏水印。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { ConfigProvider } from 'ant-design-vue';
import type { WatermarkProps } from 'ant-design-vue';
import { useThemeStore } from './store/modules/theme';
import { antdLocales } from './locales/antd';

defineOptions({
  name: 'App'
});

// 主题商店
const themeStore = useThemeStore();

// Ant Design Vue 组件库使用的语言包（固定中文）
const antdLocale = computed(() => antdLocales['zh-CN']);

// 全屏水印组件绑定属性（内容、样式、层级等）
const watermarkProps = computed(() => {
  const props: WatermarkProps = {
    content: themeStore.watermark.text,
    width: 120,
    height: 120,
    font: {
      fontSize: 16
    },
    offset: [12, 60],
    rotate: -15,
    zIndex: 9999
  };

  return props;
});
</script>

<template>
  <!-- 应用根组件：ConfigProvider 与路由出口 -->
  <ConfigProvider :theme="themeStore.antdTheme" :locale="antdLocale" :form="{ requiredMark: true }">
    <AppProvider>
      <RouterView class="bg-layout" />
      <AWatermark
        v-if="themeStore.watermark.visible"
        v-bind="watermarkProps"
        class="pointer-events-none size-full absolute-lt!"
      />
    </AppProvider>
  </ConfigProvider>
</template>

<style scoped></style>
