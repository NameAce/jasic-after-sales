<script setup lang="ts">
/**
 * 默认用户头像展示：静态图片，用于登录页等占位。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
defineOptions({
  name: 'JasicUIAvatar'
});
</script>

<template>
  <div class="size-72px overflow-hidden rd-1/2">
    <!-- 通用组件：soybean-avatar -->
    <img
      src="https://jasic-after.oss-cn-shenzhen.aliyuncs.com/uniapp/contractor/default-avatar.jpg"
      class="size-full"
    />
  </div>
</template>

<style scoped></style>
