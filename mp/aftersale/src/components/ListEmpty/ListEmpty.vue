<template>
  <view class="list-empty">
    <view v-if="showIllustration" class="empty-icon-wrap">
      <image class="empty-list-illus" :src="emptyOrderListIcon" mode="aspectFit" />
    </view>
    <text class="empty-title">{{ title }}</text>
    <text v-if="desc" class="empty-desc">{{ desc }}</text>
  </view>
</template>

<script setup lang="ts">
  import { emptyOrderListIcon } from '@/svgs'

  withDefaults(
    defineProps<{
      title: string
      desc?: string
      showIllustration?: boolean
    }>(),
    {
      desc: '',
      showIllustration: true,
    },
  )
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  /* 与 contractor `order-pages.scss` 中 `.list-empty` 规格一致 */
  .list-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    padding: 160rpx 64rpx 120rpx;
    box-sizing: border-box;
  }

  .empty-icon-wrap {
    width: 520rpx;
    max-width: 92%;
    height: 200rpx;
    @include flex-center;
    padding: 0 24rpx;
    box-sizing: border-box;
    margin-bottom: $space-lg;
  }

  .empty-list-illus {
    width: 100%;
    height: 160rpx;
  }

  .empty-title {
    font-size: $font-lg;
    font-weight: 600;
    color: $text-slate-500;
    margin-bottom: 12rpx;
  }

  .empty-desc {
    font-size: 26rpx;
    color: $text-slate-400;
    line-height: 1.5;
    text-align: center;
  }
</style>
