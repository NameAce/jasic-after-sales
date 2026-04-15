<template>
  <view class="list-no-more">
    <text class="list-no-more-text">{{ text }}</text>
  </view>
</template>

<script setup lang="ts">
  withDefaults(
    defineProps<{
      text?: string
    }>(),
    {
      text: '没有更多了',
    },
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
      color: $text-muted;
    }
  }
</style>
