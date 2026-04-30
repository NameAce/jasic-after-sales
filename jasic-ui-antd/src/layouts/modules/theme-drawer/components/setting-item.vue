<script setup lang="ts">
/**
 * 主题抽屉内单行设置：左侧标签（可选 suffix 插槽）+ 右侧控件插槽。
 */
defineOptions({
  name: 'SettingItem'
});

interface Props {
  /** Label */
  label: string;
}

defineProps<Props>();
</script>

<template>
  <div class="w-full flex-y-center justify-between">
    <div>
      <span class="pr-8px text-base-text">{{ label }}</span>
      <slot name="suffix"></slot>
    </div>
    <slot></slot>
  </div>
</template>

<style scoped></style>
