<script setup lang="ts">
/**
 * 纵向布局侧栏菜单：Teleport 到侧栏容器，内联子菜单 + 选中路径 openKeys。
 */
import { computed } from 'vue';
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface';
import type { RouteKey } from '@elegant-router/types';
import { SimpleScrollbar } from '@sa/materials';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { useMenu } from '../../../context';

defineOptions({
  name: 'VerticalMenu'
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { selectedKey } = useMenu();

// 侧栏 inverted 时 AMenu 使用 dark 主题
const darkTheme = computed(() => !themeStore.darkMode && themeStore.sider.inverted);

// AMenu 的 theme 属性：dark | light
const menuTheme = computed(() => (darkTheme.value ? 'dark' : 'light'));

// 同 vertical-mix：根据选中菜单推导应展开的父级 key
const openKeys = computed(() => {
  if (appStore.siderCollapse || !selectedKey.value) return [];

  if (!selectedKey.value) return [];

  return routeStore.getSelectedMenuKeyPath(selectedKey.value);
});

/** 菜单项点击：携带 meta.query 等按路由 key 跳转 */
function handleClickMenu(menuInfo: MenuInfo) {
  const key = menuInfo.key as RouteKey;

  routerPushByKeyWithMetaQuery(key);
}
</script>

<template>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <SimpleScrollbar class="menu-wrapper" :class="{ 'select-menu': !darkTheme }">
      <AMenu
        mode="inline"
        :theme="menuTheme"
        :items="routeStore.menus"
        :selected-keys="[selectedKey]"
        :open-keys="openKeys"
        :inline-collapsed="appStore.siderCollapse"
        :inline-indent="18"
        class="size-full transition-300 border-0!"
        :class="{ 'bg-container!': !darkTheme }"
        @click="handleClickMenu"
      />
    </SimpleScrollbar>
  </Teleport>
</template>

<style lang="scss" scoped></style>
