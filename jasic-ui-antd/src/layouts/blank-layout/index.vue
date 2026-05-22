<script setup lang="ts">
/**
 * 空白布局：仅渲染内容区（无顶栏侧栏），用于登录、404 等独立页。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import GlobalContent from '../modules/global-content/index.vue';

defineOptions({
  name: 'BlankLayout'
});
</script>

<template>
  <!-- 空白布局：仅渲染路由子页面（登录等） -->
  <GlobalContent :show-padding="false" />
</template>

<style scoped></style>
