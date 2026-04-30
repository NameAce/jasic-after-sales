<script setup lang="ts">
/**
 * 默认用户头像展示：静态图片，用于登录页等占位。
 */
defineOptions({
  name: 'JasicUIAvatar'
});
</script>

<template>
  <div class="size-72px overflow-hidden rd-1/2">
    <img src="@/assets/imgs/avatar.jpg" class="size-full" />
  </div>
</template>

<style scoped></style>
