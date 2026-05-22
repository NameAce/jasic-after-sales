<script setup lang="ts">
/**
 * 四种布局模式的缩略示意图卡片，点击切换 `themeStore.layout.mode`（可整体禁用，如移动端）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import type { TooltipPlacement } from 'ant-design-vue/es/tooltip';
import { themeLayoutModeRecord } from '@/constants/app';
import { $t } from '@/locales';

defineOptions({
  name: 'LayoutModeCard'
});

interface Props {
  /**
   * Layout mode
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  mode: UnionKey.ThemeLayoutMode;
  /**
   * Disabled
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  disabled?: boolean;
}

const props = defineProps<Props>();

interface Emits {
  /**
   * Layout mode change
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  (e: 'update:mode', mode: UnionKey.ThemeLayoutMode): void;
}

const emit = defineEmits<Emits>();

/**
 * 各布局示意图在 Tooltip、flex 分区上的展示配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
type LayoutConfig = Record<
  UnionKey.ThemeLayoutMode,
  {
    placement: TooltipPlacement;
    headerClass: string;
    menuClass: string;
    mainClass: string;
  }
>;

const layoutConfig: LayoutConfig = {
  vertical: {
    placement: 'bottom',
    headerClass: '',
    menuClass: 'w-1/3 h-full',
    mainClass: 'w-2/3 h-3/4'
  },
  'vertical-mix': {
    placement: 'bottom',
    headerClass: '',
    menuClass: 'w-1/4 h-full',
    mainClass: 'w-2/3 h-3/4'
  },
  horizontal: {
    placement: 'bottom',
    headerClass: '',
    menuClass: 'w-full h-1/4',
    mainClass: 'w-full h-3/4'
  },
  'horizontal-mix': {
    placement: 'bottom',
    headerClass: '',
    menuClass: 'w-full h-1/4',
    mainClass: 'w-2/3 h-3/4'
  }
};

/**
 * 选中某一布局模式并向上 v-model:mode 同步
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleChangeMode(mode: UnionKey.ThemeLayoutMode) {
  if (props.disabled) return;

  emit('update:mode', mode);
}
</script>

<template>
  <div class="flex-center flex-wrap gap-x-32px gap-y-16px">
    <!-- 布局子模块：layout-mode-card -->
    <div
      v-for="(item, key) in layoutConfig"
      :key="key"
      class="flex cursor-pointer border-2px rounded-6px hover:border-primary"
      :class="[mode === key ? 'border-primary' : 'border-transparent']"
      @click="handleChangeMode(key)"
    >
      <ATooltip :placement="item.placement" :title="$t(themeLayoutModeRecord[key])">
        <div
          class="h-64px w-96px gap-6px rd-4px p-6px shadow dark:shadow-coolGray-5"
          :class="[key.includes('vertical') ? 'flex' : 'flex-col']"
        >
          <slot :name="key"></slot>
        </div>
      </ATooltip>
    </div>
  </div>
</template>

<style scoped></style>
