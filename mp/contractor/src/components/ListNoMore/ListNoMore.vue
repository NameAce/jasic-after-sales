<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）组件 ListNoMore -->
  <view class="list-no-more">
    <text class="list-no-more-text">{{ text }}</text>
  </view>
</template>

<script setup lang="ts">
/**
 * 承修方小程序（网点/总部工单处理、派工）：ListNoMore。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  withDefaults(
    defineProps<{
      text?: string
    }>(),
    {
      text: '没有更多了'
    }
  )
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .list-no-more {
    padding-bottom: $space-lg;
    @include flex-center;

    .list-no-more-text {
      font-size: 24rpx;
      color: $text-slate-400;
    }
  }
</style>
