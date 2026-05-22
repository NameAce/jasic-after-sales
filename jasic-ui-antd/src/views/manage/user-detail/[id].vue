<script setup lang="ts">
/**
 * 管理端演示 — 用户详情占位：仅展示路由参数 id，可替换为真实用户画像与权限摘要。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
/** 路由参数：用户详情页 id（占位展示用） */
interface Props {
  id: string;
}

defineProps<Props>();
</script>

<template>
  <LookForward>
    <!-- 用户详情页（路由参数 id） -->
    <p>{{ id }}</p>
  </LookForward>
</template>

<style scoped></style>
