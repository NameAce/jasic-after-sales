<script setup lang="ts">
/**
 * 内嵌 iframe 页：由路由参数传入目标 URL，用于菜单外链、文档等整页嵌入（注意同源与 CSP 限制）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

/**
 * iframe 内嵌地址（路由传入）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
interface Props {
  url: string;
}

defineProps<Props>();
</script>

<template>
  <!-- 内置页：iframe-page/[url].vue -->
  <div class="h-full">
    <iframe id="iframePage" class="size-full" :src="url"></iframe>
  </div>
</template>

<style scoped></style>
