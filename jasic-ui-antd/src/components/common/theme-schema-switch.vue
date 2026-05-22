<script setup lang="ts">
/**
 * 顶栏一键切换亮/暗/跟随系统：向外 emit `switch`，由父级调用 themeStore。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import type { TooltipPlacement } from 'ant-design-vue/es/tooltip';
import { $t } from '@/locales';

defineOptions({ name: 'ThemeSchemaSwitch' });

interface Props {
  /**
   * Theme schema
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  themeSchema: UnionKey.ThemeScheme;
  /**
   * Show tooltip
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  showTooltip?: boolean;
  /**
   * Tooltip placement
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  tooltipPlacement?: TooltipPlacement;
}

const props = withDefaults(defineProps<Props>(), {
  showTooltip: true,
  tooltipPlacement: 'bottom'
});

interface Emits {
  (e: 'switch'): void;
}

const emit = defineEmits<Emits>();

/**
 * 点击后通知父级切换主题方案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleSwitch() {
  emit('switch');
}

const icons: Record<UnionKey.ThemeScheme, string> = {
  light: 'material-symbols:sunny',
  dark: 'material-symbols:nightlight-rounded',
  auto: 'material-symbols:hdr-auto'
};

// 当前主题方案对应的太阳/月亮/HDR 图标名
const icon = computed(() => icons[props.themeSchema]);

// 关闭 tooltip 时返回空串，避免无谓的浮层
const tooltipContent = computed(() => {
  if (!props.showTooltip) return '';

  return $t('icon.themeSchema');
});
</script>

<template>
  <ButtonIcon
    :icon="icon"
    :tooltip-content="tooltipContent"
    :tooltip-placement="tooltipPlacement"
    @click="handleSwitch"
  />
  <!-- 通用组件：theme-schema-switch -->
</template>

<style scoped></style>
