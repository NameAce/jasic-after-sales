<script setup lang="ts">
/**
 * 全局侧栏容器：Logo + 菜单挂载点（菜单组件通过 Teleport 写入 #GLOBAL_SIDER_MENU_ID）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import GlobalLogo from '../global-logo/index.vue';

defineOptions({
  name: 'GlobalSider'
});

const appStore = useAppStore();
const themeStore = useThemeStore();

// 当前是否为纵向混合布局
const isVerticalMix = computed(() => themeStore.layout.mode === 'vertical-mix');
// 当前是否为水平混合布局
const isHorizontalMix = computed(() => themeStore.layout.mode === 'horizontal-mix');
// 非暗色全局且非水平混合时，侧栏菜单可用 inverted 深色皮肤
const darkMenu = computed(() => !themeStore.darkMode && !isHorizontalMix.value && themeStore.sider.inverted);
// 混合布局时 Logo 由别处承载，此处不再重复
const showLogo = computed(() => !isVerticalMix.value && !isHorizontalMix.value);
const menuWrapperClass = computed(() => (showLogo.value ? 'flex-1-hidden' : 'h-full'));
</script>

<template>
  <!-- 侧栏容器：菜单与折叠 -->
  <DarkModeContainer class="size-full flex-col-stretch shadow-sider" :inverted="darkMenu">
    <GlobalLogo
      v-if="showLogo"
      :show-title="!appStore.siderCollapse"
      :style="{ height: themeStore.header.height + 'px' }"
    />
    <div :id="GLOBAL_SIDER_MENU_ID" :class="menuWrapperClass"></div>
  </DarkModeContainer>
</template>

<style scoped></style>
