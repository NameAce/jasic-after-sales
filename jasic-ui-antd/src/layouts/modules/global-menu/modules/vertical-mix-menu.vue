<script setup lang="ts">
/**
 * 纵向混合菜单：左侧一级 + 可选固定/抽屉式子菜单，与 mix-menu 上下文联动。
 */
import { computed } from 'vue';
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface';
import type { RouteKey } from '@elegant-router/types';
import { SimpleScrollbar } from '@sa/materials';
import { useBoolean } from '@sa/hooks';
import { GLOBAL_SIDER_MENU_ID } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';
import { useMenu, useMixMenuContext } from '../../../context';
import FirstLevelMenu from '../components/first-level-menu.vue';
import GlobalLogo from '../../global-logo/index.vue';

defineOptions({
  name: 'VerticalMixMenu'
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const authStore = useAuthStore();
const { routerPushByKeyWithMetaQuery } = useRouterPush();
const { bool: drawerVisible, setBool: setDrawerVisible } = useBoolean();
const { allMenus, childLevelMenus, activeFirstLevelMenuKey, setActiveFirstLevelMenuKey, getActiveFirstLevelMenuKey } =
  useMixMenuContext();
const { selectedKey } = useMenu();

// 侧栏菜单暗色皮肤：全局非暗色且开启 sider.inverted 时
const inverted = computed(() => !themeStore.darkMode && themeStore.sider.inverted);

const menuTheme = computed(() => (inverted.value ? 'dark' : 'light'));

// 当前一级下是否存在可展示子菜单
const hasChildMenus = computed(() => childLevelMenus.value.length > 0);

// 有子菜单且抽屉已开或开启固定混合子栏时展示抽屉
const showDrawer = computed(() => hasChildMenus.value && (drawerVisible.value || appStore.mixSiderFixed));
const systemTitle = computed(() => authStore.userInfo.currentCompanyName || $t('system.title'));

/** 点击一级：有子级则开抽屉，无子级则直接跳转 */
function handleSelectMixMenu(menu: App.Global.Menu) {
  setActiveFirstLevelMenuKey(menu.key);

  if (menu.children?.length) {
    setDrawerVisible(true);
  } else {
    routerPushByKeyWithMetaQuery(menu.routeKey);
  }
}

/** 鼠标离开混合区：关抽屉并在非固定模式下重算一级选中 */
function handleResetActiveMenu() {
  setDrawerVisible(false);

  if (!appStore.mixSiderFixed) {
    getActiveFirstLevelMenuKey();
  }
}

// 侧栏未折叠时展开至当前选中项的各级 key（用于 SubMenu openKeys）
const openKeys = computed(() => {
  if (appStore.siderCollapse || !selectedKey.value) return [];

  if (!selectedKey.value) return [];

  return routeStore.getSelectedMenuKeyPath(selectedKey.value);
});

/** 子菜单项点击跳转 */
function handleClickMenu(menuInfo: MenuInfo) {
  const key = menuInfo.key as RouteKey;

  routerPushByKeyWithMetaQuery(key);
}
</script>

<template>
  <Teleport :to="`#${GLOBAL_SIDER_MENU_ID}`">
    <div class="h-full flex" @mouseleave="handleResetActiveMenu">
      <FirstLevelMenu
        :menus="allMenus"
        :active-menu-key="activeFirstLevelMenuKey"
        :inverted="inverted"
        :sider-collapse="appStore.siderCollapse"
        :dark-mode="themeStore.darkMode"
        :theme-color="themeStore.themeColor"
        @select="handleSelectMixMenu"
        @toggle-sider-collapse="appStore.toggleSiderCollapse"
      >
        <GlobalLogo :show-title="false" :style="{ height: themeStore.header.height + 'px' }" />
      </FirstLevelMenu>
      <div
        class="relative h-full transition-width-300"
        :style="{ width: appStore.mixSiderFixed && hasChildMenus ? themeStore.sider.mixChildMenuWidth + 'px' : '0px' }"
      >
        <DarkModeContainer
          class="absolute-lt h-full flex-col-stretch nowrap-hidden shadow-sm transition-all-300"
          :inverted="inverted"
          :style="{ width: showDrawer ? themeStore.sider.mixChildMenuWidth + 'px' : '0px' }"
        >
          <header class="flex-y-center justify-between px-12px" :style="{ height: themeStore.header.height + 'px' }">
            <h2 class="text-16px text-primary font-bold">{{ systemTitle }}</h2>
            <PinToggler
              :pin="appStore.mixSiderFixed"
              :class="{ 'text-white:88 !hover:text-white': inverted }"
              @click="appStore.toggleMixSiderFixed"
            />
          </header>
          <SimpleScrollbar class="menu-wrapper" :class="{ 'select-menu': !inverted }">
            <AMenu
              mode="inline"
              :theme="menuTheme"
              :items="childLevelMenus"
              :selected-keys="[selectedKey]"
              :open-keys="openKeys"
              class="size-full transition-300 border-0!"
              :class="{ 'bg-container!': !inverted }"
              @click="handleClickMenu"
            />
          </SimpleScrollbar>
        </DarkModeContainer>
      </div>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped></style>
