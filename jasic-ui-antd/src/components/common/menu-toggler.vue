<script lang="ts" setup>
/**
 * 侧栏折叠切换：支持普通折叠图标或双箭头样式（混合菜单底部等场景）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { $t } from '@/locales';

defineOptions({ name: 'MenuToggler' });

interface Props {
  /**
   * Show collapsed icon
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  collapsed?: boolean;
  /**
   * Arrow style icon
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  arrowIcon?: boolean;
}

const props = defineProps<Props>();

type NumberBool = 0 | 1;

// 根据折叠态与箭头样式从二维表取 Iconify 名
const icon = computed(() => {
  const icons: Record<NumberBool, Record<NumberBool, string>> = {
    0: {
      0: 'line-md:menu-fold-left',
      1: 'line-md:menu-fold-right'
    },
    1: {
      0: 'ph-caret-double-left-bold',
      1: 'ph-caret-double-right-bold'
    }
  };

  const arrowIcon = Number(props.arrowIcon || false) as NumberBool;

  const collapsed = Number(props.collapsed || false) as NumberBool;

  return icons[arrowIcon][collapsed];
});
</script>

<template>
  <!-- 通用组件：menu-toggler -->
  <ButtonIcon
    :key="String(collapsed)"
    :tooltip-content="collapsed ? $t('icon.expand') : $t('icon.collapse')"
    tooltip-placement="bottomLeft"
  >
    <SvgIcon :icon="icon" />
  </ButtonIcon>
</template>

<style scoped></style>
