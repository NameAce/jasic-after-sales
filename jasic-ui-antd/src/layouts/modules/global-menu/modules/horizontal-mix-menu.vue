<script setup lang="ts">
/**
 * 水平混合菜单：顶栏展示当前一级下的子级横向菜单，侧栏为一级图标列表。
 */
import { computed } from 'vue';
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface';
import type { RouteKey } from '@elegant-router/types';
import { GLOBAL_HEADER_MENU_ID, GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouterPush } from '@/hooks/common/router';
import FirstLevelMenu from '../components/first-level-menu.vue';
import { useMenu, useMixMenuContext } from '../../../context';

defineOptions({
  name: 'HorizontalMixMenu'
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { allMenus, childLevelMenus, activeFirstLevelMenuKey, setActiveFirstLevelMenuKey } = useMixMenuContext();
const { selectedKey } = useMenu();

// 顶栏下菜单区若启用 inverted，仅在非全局暗色时生效
const inverted = computed(() => !themeStore.darkMode && themeStore.sider.inverted);

/** 顶栏子菜单项点击 */
function handleClickMenu(menuInfo: MenuInfo) {
  const key = menuInfo.key as RouteKey;

  routerPushByKeyWithMetaQuery(key);
}

/** 侧栏一级选中：仅叶子一级直接跳转 */
function handleSelectMixMenu(menu: App.Global.Menu) {
  setActiveFirstLevelMenuKey(menu.key);

  if (!menu.children?.length) {
    routerPushByKeyWithMetaQuery(menu.routeKey);
  }
}
</script>

<template>
  <Teleport :to="`#${GLOBAL_HEADER_MENU_ID}`">
    <AMenu
      mode="horizontal"
      :selected-keys="[selectedKey]"
      :items="childLevelMenus"
      class="horizontal-menu size-full transition-300 border-0!"
      :class="{ 'bg-container!': themeStore.darkMode }"
      :style="{ lineHeight: themeStore.header.height + 'px' }"
      @click="handleClickMenu"
    />
  </Teleport>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <FirstLevelMenu
      :menus="allMenus"
      :active-menu-key="activeFirstLevelMenuKey"
      :inverted="inverted"
      :sider-collapse="appStore.siderCollapse"
      :dark-mode="themeStore.darkMode"
      :theme-color="themeStore.themeColor"
      @select="handleSelectMixMenu"
      @toggle-sider-collapse="appStore.toggleSiderCollapse"
    />
  </Teleport>
</template>

<style lang="scss" scoped></style>
