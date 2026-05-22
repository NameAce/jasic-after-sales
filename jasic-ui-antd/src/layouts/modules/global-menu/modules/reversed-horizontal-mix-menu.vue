<script setup lang="ts">
/**
 * 水平混合（反转）：顶栏为一级菜单，侧栏为当前一级下的子菜单（与默认 horizontal-mix 区域对调）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface';
import type { RouteKey } from '@elegant-router/types';
import { SimpleScrollbar } from '@sa/materials';
import { GLOBAL_HEADER_MENU_ID, GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { useMenu, useMixMenuContext } from '../../../context';

defineOptions({
  name: 'ReversedHorizontalMixMenu'
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const {
  firstLevelMenus,
  childLevelMenus,
  activeFirstLevelMenuKey,
  setActiveFirstLevelMenuKey,
  isActiveFirstLevelMenuHasChildren
} = useMixMenuContext();
const { selectedKey } = useMenu();

/**
 * 顶栏一级点击：更新激活一级；无子级时直接跳转该路由
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleSelectMixMenu(menuInfo: MenuInfo) {
  const key = menuInfo.key as RouteKey;

  setActiveFirstLevelMenuKey(key);

  if (!isActiveFirstLevelMenuHasChildren.value) {
    routerPushByKeyWithMetaQuery(key);
  }
}

// 水平反转混合布局下侧栏子菜单的 openKeys（与 vertical 逻辑相同）
const openKeys = computed(() => {
  if (appStore.siderCollapse || !selectedKey.value) return [];

  if (!selectedKey.value) return [];

  return routeStore.getSelectedMenuKeyPath(selectedKey.value);
});

/**
 * 侧栏子菜单项点击跳转
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleClickMenu(menuInfo: MenuInfo) {
  const key = menuInfo.key as RouteKey;

  routerPushByKeyWithMetaQuery(key);
}
</script>

<template>
  <Teleport :to="`#${GLOBAL_HEADER_MENU_ID}`">
    <!-- 布局子模块：reversed-horizontal-mix-menu -->
    <AMenu
      mode="horizontal"
      :selected-keys="[activeFirstLevelMenuKey]"
      :items="firstLevelMenus"
      class="horizontal-menu size-full transition-300 border-0!"
      :class="{ 'bg-container!': themeStore.darkMode }"
      :style="{ lineHeight: themeStore.header.height + 'px' }"
      @click="handleSelectMixMenu"
    />
  </Teleport>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <SimpleScrollbar>
      <AMenu
        mode="inline"
        :items="childLevelMenus"
        :selected-keys="[selectedKey]"
        :open-keys="openKeys"
        :inline-collapsed="appStore.siderCollapse"
        :inline-indent="18"
        class="size-full transition-300 border-0!"
        :class="{ 'bg-container!': themeStore.darkMode }"
        @click="handleClickMenu"
      />
    </SimpleScrollbar>
  </Teleport>
</template>

<style scoped></style>
