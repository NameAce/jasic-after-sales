<template>
  <!-- 售后客户端小程序（报修、工单、地址）：BaseButton。 @修改人 黄碧莲 @修改时间 2026-05-22 -->
  <!-- 售后客户端小程序（报修、工单、地址）组件 BaseButton -->
  <view class="base-btn">
    <view class="base-btn-content">
      <slot />
      <slot name="submit" />
    </view>
  </view>
</template>

<style scoped lang="scss">
  .base-btn {
    @include fixed-btn;
    box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.08);
    padding-bottom: calc(#{$space-md} + constant(safe-area-inset-bottom));
    padding-bottom: calc(#{$space-md} + env(safe-area-inset-bottom));

    .base-btn-content {
      @include flex-center;
      height: 80rpx;
    }
  }
</style>
