<template>
  <!-- 售后客户端小程序（报修、工单、地址）组件 ServicePointFormItem -->
  <uni-forms-item label="选择附近网点" name="centerId" required>
    <FormItemAnchor name="centerId" />
    <view @click="goToServicePoint">
      <uni-easyinput
        :model-value="displayText"
        placeholder="请选择距离您最近的维修中心"
        prefix-icon="location-filled"
        style="pointer-events: none"
      />
    </view>
  </uni-forms-item>
</template>

<script setup lang="ts">
/**
 * 售后客户端小程序（报修、工单、地址）：ServicePointFormItem。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'

  defineProps<{
    displayText: string
  }>()

  const goToServicePoint = () => {
    uni.navigateTo({ url: '/pages/servicePoint/index' })
  }
</script>
