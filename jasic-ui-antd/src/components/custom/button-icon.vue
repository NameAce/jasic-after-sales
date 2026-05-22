<script setup lang="ts">
/**
 * 带 Tooltip 的文本按钮：可仅展示 Iconify 图标或默认插槽自定义内容；支持 tooltip 挂到父节点避免裁剪。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import type { TooltipPlacement } from 'ant-design-vue/es/tooltip';
import { twMerge } from 'tailwind-merge';
defineOptions({
  name: 'ButtonIcon',
  inheritAttrs: false
});

interface Props {
  /**
   * Button class
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  class?: string;
  /**
   * Iconify icon name
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  icon?: string;
  /**
   * Tooltip content
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  tooltipContent?: string;
  /**
   * Tooltip placement
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  tooltipPlacement?: TooltipPlacement;
  /**
   * Trigger tooltip on parent
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  triggerParent?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  class: 'h-36px text-icon',
  icon: '',
  tooltipContent: '',
  tooltipPlacement: 'bottom',
  triggerParent: false
});

/**
 * Tooltip 挂载容器：`triggerParent` 时挂到触发按钮父级，否则挂 body
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getPopupContainer(triggerNode: HTMLElement) {
  return props.triggerParent ? triggerNode.parentElement! : document.body;
}

const DEFAULT_CLASS = 'h-[36px] text-icon';
</script>

<template>
  <!-- 通用组件：button-icon -->
  <ATooltip :placement="tooltipPlacement" :get-popup-container="getPopupContainer" :title="tooltipContent">
    <AButton type="text" :class="twMerge(DEFAULT_CLASS, props.class)" v-bind="$attrs">
      <div class="flex-center gap-8px">
        <slot>
          <SvgIcon :icon="icon" />
        </slot>
      </div>
    </AButton>
  </ATooltip>
</template>

<style scoped></style>
