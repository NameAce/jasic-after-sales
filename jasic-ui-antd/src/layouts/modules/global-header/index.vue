<script setup lang="ts">
/**
 * 全局顶栏：Logo、菜单/面包屑占位、全屏与主题切换、用户区；具体菜单由 Teleport 注入对应 DOM 节点。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { useFullscreen } from '@vueuse/core';
import { GLOBAL_HEADER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import GlobalLogo from '../global-logo/index.vue';
import GlobalBreadcrumb from '../global-breadcrumb/index.vue';
import ThemeButton from './components/theme-button.vue';
import UserAvatar from './components/user-avatar.vue';

defineOptions({
  name: 'GlobalHeader'
});

interface Props {
  /**
   * Whether to show the logo
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  showLogo?: App.Global.HeaderProps['showLogo'];
  /**
   * Whether to show the menu toggler
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  showMenuToggler?: App.Global.HeaderProps['showMenuToggler'];
  /**
   * Whether to show the menu
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  showMenu?: App.Global.HeaderProps['showMenu'];
}

defineProps<Props>();

const appStore = useAppStore();
const themeStore = useThemeStore();
// 浏览器全屏（顶栏按钮）
const { isFullscreen, toggle } = useFullscreen();
</script>

<template>
  <!-- 顶栏：Logo、面包屑、主题与用户菜单 -->
  <DarkModeContainer class="h-full flex-y-center px-12px shadow-header">
    <GlobalLogo v-if="showLogo" class="h-full" :style="{ width: themeStore.sider.width + 'px' }" />
    <MenuToggler v-if="showMenuToggler" :collapsed="appStore.siderCollapse" @click="appStore.toggleSiderCollapse" />
    <div v-if="showMenu" :id="GLOBAL_HEADER_MENU_ID" class="h-full flex-y-center flex-1-hidden pb-1px"></div>
    <div v-else class="h-full flex-y-center flex-1-hidden">
      <GlobalBreadcrumb v-if="!appStore.isMobile" class="ml-12px" />
    </div>
    <div class="h-full flex-y-center justify-end">
      <FullScreen v-if="!appStore.isMobile" :full="isFullscreen" @click="toggle" />
      <ThemeSchemaSwitch
        :theme-schema="themeStore.themeScheme"
        :is-dark="themeStore.darkMode"
        @switch="themeStore.toggleThemeScheme"
      />
      <ThemeButton />
      <UserAvatar />
    </div>
  </DarkModeContainer>
</template>

<style scoped></style>
