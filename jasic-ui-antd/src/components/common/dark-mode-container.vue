<script setup lang="ts">
/**
 * 布局区背景容器：默认容器色；`inverted` 时用于侧栏等反色底（与全局暗色模式区分）。
 */
defineOptions({ name: 'DarkModeContainer' });

interface Props {
  inverted?: boolean;
}

defineProps<Props>();
</script>

<template>
  <div class="bg-container text-base-text transition-300" :class="{ 'bg-inverted text-#1f1f1f': inverted }">
    <slot></slot>
  </div>
</template>

<style scoped></style>
