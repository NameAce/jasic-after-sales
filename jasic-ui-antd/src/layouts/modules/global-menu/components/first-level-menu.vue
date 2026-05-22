<script setup lang="ts">
/**
 * 混合布局共用的一级菜单列：图标 + 文案（可折叠为仅图标），底部折叠按钮。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { createReusableTemplate } from '@vueuse/core';
import { SimpleScrollbar } from '@sa/materials';
import { transformColorWithOpacity } from '@sa/color';

defineOptions({
  name: 'FirstLevelMenu'
});

interface Props {
  menus: App.Global.Menu[];
  activeMenuKey?: string;
  inverted?: boolean;
  siderCollapse?: boolean;
  darkMode?: boolean;
  themeColor: string;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'select', menu: App.Global.Menu): boolean;
  (e: 'toggleSiderCollapse'): void;
}

const emit = defineEmits<Emits>();

interface MixMenuItemProps {
  /**
   * Menu item label
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  label: App.Global.Menu['label'];
  /**
   * Menu item icon
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  icon: App.Global.Menu['icon'];
  /**
   * Active menu item
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  active: boolean;
  /**
   * Mini size
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  isMini?: boolean;
}
const [DefineMixMenuItem, MixMenuItem] = createReusableTemplate<MixMenuItemProps>();

// 一级菜单项选中背景：主色加透明度，亮暗底不同
const selectedBgColor = computed(() => {
  const { darkMode, themeColor } = props;

  const light = transformColorWithOpacity(themeColor, 0.1, '#ffffff');
  const dark = transformColorWithOpacity(themeColor, 0.3, '#000000');

  return darkMode ? dark : light;
});

/**
 * 点击某一一级菜单项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleClickMixMenu(menu: App.Global.Menu) {
  emit('select', menu);
}

/**
 * 底部箭头：切换侧栏折叠（由父级处理 appStore）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function toggleSiderCollapse() {
  emit('toggleSiderCollapse');
}
</script>

<template>
  <!-- define component: MixMenuItem -->
  <DefineMixMenuItem v-slot="{ label, icon, active, isMini }">
    <div
      class="mx-4px mb-6px flex-col-center cursor-pointer rounded-8px bg-transparent px-4px py-8px transition-300 hover:bg-[rgb(0,0,0,0.08)]"
      :class="{
        'text-primary selected-mix-menu': active,
        'text-white:65 hover:text-white': inverted,
        '!text-white !bg-primary': active && inverted
      }"
    >
      <component :is="icon" :class="[isMini ? 'text-icon-small' : 'text-icon-large']" />
      <p
        class="w-full ellipsis-text text-center text-12px transition-height-300"
        :class="[isMini ? 'h-0 pt-0' : 'h-20px pt-4px']"
      >
        {{ label }}
      </p>
    </div>
  </DefineMixMenuItem>
  <!-- define component end: MixMenuItem -->

  <div class="h-full flex-col-stretch flex-1-hidden">
    <slot></slot>
    <SimpleScrollbar>
      <MixMenuItem
        v-for="menu in menus"
        :key="menu.key"
        :label="menu.label"
        :icon="menu.icon"
        :active="menu.key === activeMenuKey"
        :is-mini="siderCollapse"
        @click="handleClickMixMenu(menu)"
      />
    </SimpleScrollbar>
    <MenuToggler
      arrow-icon
      :collapsed="siderCollapse"
      :z-index="99"
      :class="{ 'text-white:88 !hover:text-white': inverted }"
      @click="toggleSiderCollapse"
    />
  </div>
</template>

<style scoped>
.selected-mix-menu {
  background-color: v-bind(selectedBgColor);
}
</style>
