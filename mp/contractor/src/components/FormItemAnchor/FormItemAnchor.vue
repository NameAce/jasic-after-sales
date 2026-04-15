<template>
  <view :id="name" class="form-item-anchor" aria-hidden="true" />
</template>

<script setup lang="ts">
  defineProps<{
    /** 与 scroll-view 的 scroll-into-view、triggerScrollIntoView 的 fieldKey 一致 */
    name: string
  }>()
</script>

<style scoped lang="scss">
  .form-item-anchor {
    position: absolute;
    left: 0;
    top: 0;
    width: 1px;
    height: 1px;
    overflow: hidden;
    pointer-events: none;
  }
</style>
