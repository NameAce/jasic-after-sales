<script setup lang="ts">
/**
 * 水平布局顶栏菜单：Teleport 到顶栏 `#GLOBAL_HEADER_MENU_ID`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface';
import type { RouteKey } from '@elegant-router/types';
import { GLOBAL_HEADER_MENU_ID } from '@/constants/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { useMenu } from '../../../context';

defineOptions({
  name: 'HorizontalMenu'
});

const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { selectedKey } = useMenu();

/**
 * 菜单项点击跳转
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleClickMenu(menuInfo: MenuInfo) {
  const key = menuInfo.key as RouteKey;

  routerPushByKeyWithMetaQuery(key);
}
</script>

<template>
  <!-- 布局子模块：horizontal-menu -->
  <Teleport :to="`#${GLOBAL_HEADER_MENU_ID}`">
    <AMenu
      mode="horizontal"
      :selected-keys="[selectedKey]"
      :items="routeStore.menus"
      class="horizontal-menu size-full transition-300 border-0!"
      :class="{ 'bg-container!': themeStore.darkMode }"
      :style="{ lineHeight: themeStore.header.height + 'px' }"
      @click="handleClickMenu"
    />
  </Teleport>
</template>

<style lang="scss" scoped></style>
