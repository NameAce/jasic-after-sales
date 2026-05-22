<script setup lang="ts">
/**
 * 布局区背景容器：默认容器色；`inverted` 时用于侧栏等反色底（与全局暗色模式区分）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
defineOptions({ name: 'DarkModeContainer' });

interface Props {
  inverted?: boolean;
}

defineProps<Props>();
</script>

<template>
  <div class="bg-container text-base-text transition-300" :class="{ 'bg-inverted text-#1f1f1f': inverted }">
    <!-- 通用组件：dark-mode-container -->
    <slot></slot>
  </div>
</template>

<style scoped></style>
