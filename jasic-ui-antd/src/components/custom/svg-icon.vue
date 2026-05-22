<script setup lang="ts">
/**
 * 图标组件：优先渲染本地 SVG 雪碧（`localIcon`），否则走 Iconify 在线/离线图标。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, useAttrs } from 'vue';
import { Icon } from '@iconify/vue';

defineOptions({ name: 'SvgIcon', inheritAttrs: false });

/**
 * Props
 *
 * - Support iconify and local svg icon
 * - If icon and localIcon are passed at the same time, localIcon will be rendered first
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
interface Props {
  /**
   * Iconify icon name
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  icon?: string;
  /**
   * Local svg icon name
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  localIcon?: string;
}

const props = defineProps<Props>();

const attrs = useAttrs();

const bindAttrs = computed<{ class: string; style: string }>(() => ({
  class: (attrs.class as string) || '',
  style: (attrs.style as string) || ''
}));

// svg use 引用的 symbol id（与 vite 雪碧前缀一致）
const symbolId = computed(() => {
  const { VITE_ICON_LOCAL_PREFIX: prefix } = import.meta.env;

  const defaultLocalIcon = 'no-icon';

  const icon = props.localIcon || defaultLocalIcon;

  return `#${prefix}-${icon}`;
});

// 有 localIcon 或仅有本地占位时走雪碧图，否则走 Iconify
const renderLocalIcon = computed(() => props.localIcon || !props.icon);
</script>

<template>
  <template v-if="renderLocalIcon">
    <!-- 通用组件：svg-icon -->
    <svg aria-hidden="true" width="1em" height="1em" v-bind="bindAttrs">
      <use :xlink:href="symbolId" fill="currentColor" />
    </svg>
  </template>
  <template v-else>
    <Icon v-if="icon" :icon="icon" v-bind="bindAttrs" />
  </template>
</template>

<style scoped></style>
