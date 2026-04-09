<template>
  <view :id="anchorId" class="form-field-anchor" />
</template>

<script setup lang="ts">
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
