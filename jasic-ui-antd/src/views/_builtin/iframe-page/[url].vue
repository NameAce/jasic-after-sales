<script setup lang="ts">
/**
 * 内嵌 iframe 页：由路由参数传入目标 URL，用于菜单外链、文档等整页嵌入（注意同源与 CSP 限制）。
 */
import { onActivated, onMounted } from 'vue';

/** iframe 内嵌地址（路由传入） */
interface Props {
  url: string;
}

defineProps<Props>();

/**
 * 作用：iframe 页面挂载时输出调试日志（便于排查加载问题）。
 * @param 无
 * @returns {void} 无
 */
onMounted(() => {
  console.log('mounted');
});

/**
 * 作用：keep-alive 激活 iframe 页时输出调试日志。
 * @param 无
 * @returns {void} 无
 */
onActivated(() => {
  console.log('activated');
});
</script>

<template>
  <div class="h-full">
    <iframe id="iframePage" class="size-full" :src="url"></iframe>
  </div>
</template>

<style scoped></style>
