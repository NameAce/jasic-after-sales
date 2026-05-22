<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）页面 order / components / OrderDetailEvaluateSection -->
  <view class="od-eval-section">
    <view class="od-eval-section-header">
      <view class="section-mark"></view>
      <text class="section-title">客户评价</text>
    </view>
    <view v-if="evaluate && hasContent" class="od-eval-list">
      <view v-if="(evaluate?.timeliness ?? 0) > 0" class="od-eval-item">
        <text class="od-eval-label">服务时效</text>
        <view class="od-eval-stars">
          <text
            v-for="i in 5"
            :key="'t' + i"
            :class="['od-star-char', i <= (evaluate?.timeliness || 0) ? 'active' : '']"
            >★</text
          >
        </view>
      </view>
      <view v-if="(evaluate?.quality ?? 0) > 0" class="od-eval-item">
        <text class="od-eval-label">维修质量</text>
        <view class="od-eval-stars">
          <text
            v-for="i in 5"
            :key="'q' + i"
            :class="['od-star-char', i <= (evaluate?.quality || 0) ? 'active' : '']"
            >★</text
          >
        </view>
      </view>
      <view v-if="(evaluate?.satisfaction ?? 0) > 0" class="od-eval-item">
        <text class="od-eval-label">服务满意度</text>
        <view class="od-eval-stars">
          <text
            v-for="i in 5"
            :key="'s' + i"
            :class="['od-star-char', i <= (evaluate?.satisfaction || 0) ? 'active' : '']"
            >★</text
          >
        </view>
      </view>
      <view v-if="hasStr(evaluate?.comment)" class="od-eval-comment">
        <text class="od-eval-comment-label">评价内容</text>
        <view class="od-eval-comment-box">
          <text class="od-eval-comment-text">{{ evaluate?.comment }}</text>
        </view>
      </view>
    </view>
    <view v-else class="od-eval-empty">
      <text class="od-eval-empty-text">暂无评价信息</text>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 承修方小程序（网点/总部工单处理、派工） 组件：OrderDetailEvaluateSection。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { hasStr } from '@/utils/value'

  const props = defineProps<{
    evaluate?: OrderDetail['evaluate']
  }>()

  const hasContent = computed(() => {
    const e = props.evaluate
    if (!e) return false
    return (
      (e.timeliness ?? 0) > 0 ||
      (e.quality ?? 0) > 0 ||
      (e.satisfaction ?? 0) > 0 ||
      hasStr(e.comment)
    )
  })
</script>

<style lang="scss" scoped>
  .od-eval-section {
    padding: $space-lg;
  }

  .od-eval-section-header {
    @include section-title-bar;
  }

  .od-eval-list {
    @include flex-col;
    gap: $space-lg;
    padding: 0 $space-md;
  }

  .od-eval-item {
    @include flex-between;
    align-items: center;
    gap: $space-md;

    .od-eval-label {
      flex-shrink: 0;
      font-size: $font-sm;
      color: $text-slate-500;
    }

    .od-eval-stars {
      @include flex-row;
      gap: 4rpx;

      .od-star-char {
        font-size: 32rpx;
        color: $border-slate;

        &.active {
          color: #f59e0b;
        }
      }
    }
  }

  .od-eval-comment {
    @include flex-col;
    gap: $space-sm;

    .od-eval-comment-label {
      font-size: $font-sm;
      color: $text-slate-500;
    }

    .od-eval-comment-box {
      @include surface-muted-box;
      padding: $space-md;
      border-radius: $radius-md;
    }

    .od-eval-comment-text {
      font-size: $font-sm;
      line-height: 1.6;
      color: $text-slate-700;
    }
  }

  .od-eval-empty {
    padding: 64rpx 0;
    text-align: center;

    .od-eval-empty-text {
      font-size: $font-md;
      color: $text-slate-400;
    }
  }
</style>
