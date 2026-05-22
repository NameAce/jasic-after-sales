<template>
  <!-- 售后客户端小程序（报修、工单、地址）组件 FormItemAnchor -->
  <view :id="anchorId" class="form-field-anchor" />
</template>

<script setup lang="ts">
/**
 * 售后客户端小程序（报修、工单、地址）：FormItemAnchor。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { computed } from 'vue'
  import { formFieldAnchorId } from '@/utils/formFieldScrollFocus'

  const props = defineProps<{ name: string }>()
  const anchorId = computed(() => formFieldAnchorId(props.name))
</script>

<style lang="scss" scoped>
  /* 不占布局流：父级需 position: relative（表单项已统一处理） */
  .form-field-anchor {
    position: absolute;
    top: 0;
    left: 0;
    width: 1px;
    height: 1px;
    overflow: hidden;
    pointer-events: none;
  }
</style>
