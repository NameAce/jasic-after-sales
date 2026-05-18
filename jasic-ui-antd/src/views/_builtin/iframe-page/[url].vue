<script setup lang="ts">
/**
 * 内嵌 iframe 页：由路由参数传入目标 URL，用于菜单外链、文档等整页嵌入（注意同源与 CSP 限制）。
 */

/** iframe 内嵌地址（路由传入） */
interface Props {
  url: string;
}

defineProps<Props>();
</script>

<template>
  <div class="h-full">
    <iframe id="iframePage" class="size-full" :src="url"></iframe>
  </div>
</template>

<style scoped></style>
