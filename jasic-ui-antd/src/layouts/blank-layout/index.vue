<script setup lang="ts">
/**
 * 空白布局：仅渲染内容区（无顶栏侧栏），用于登录、404 等独立页。
 */
import GlobalContent from '../modules/global-content/index.vue';

defineOptions({
  name: 'BlankLayout'
});
</script>

<template>
  <GlobalContent :show-padding="false" />
</template>

<style scoped></style>
