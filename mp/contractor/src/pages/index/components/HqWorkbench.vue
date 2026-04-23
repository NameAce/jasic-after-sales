<template>
  <!-- 使用页面级滚动：避免 scroll-view + flex 在部分端上高度为 0 或无法滚动 -->
  <view class="hq-workbench-root">
    <view class="hq-main">
      <!-- 全网实时统计 -->
      <view class="hq-section">
        <view class="hq-section-head">
          <text class="hq-section-title">全网实时统计</text>
          <text class="hq-section-meta">更新于 {{ hqUpdatedAt }}</text>
        </view>
        <!-- 统计卡片 -->
        <view class="hq-stats-grid">
          <!-- 待接单 -->
          <view class="hq-stat-card hq-stat-card--accent" @tap="emit('stat-tap', 'pending_accept')">
            <text class="hq-stat-label">待接单</text>
            <view class="hq-stat-foot">
              <text class="hq-stat-num hq-stat-num--accent">{{
                hqNetworkStats.pendingTechAccept
              }}</text>
              <text
                class="hq-pill"
                :class="'hq-pill--' + mockHqRealtimeStatBadges.pending.variant"
                >{{ mockHqRealtimeStatBadges.pending.text }}</text
              >
            </view>
          </view>
          <!-- 维修中 -->
          <view class="hq-stat-card" @tap="emit('stat-tap', 'processing')">
            <text class="hq-stat-label">维修中</text>
            <view class="hq-stat-foot">
              <text class="hq-stat-num">{{ hqNetworkStats.processing }}</text>
              <text
                class="hq-pill"
                :class="'hq-pill--' + mockHqRealtimeStatBadges.processing.variant"
                >{{ mockHqRealtimeStatBadges.processing.text }}</text
              >
            </view>
          </view>
          <!-- 已完成 -->
          <view class="hq-stat-card" @tap="emit('stat-tap', 'completed')">
            <text class="hq-stat-label">已完成</text>
            <view class="hq-stat-foot">
              <text class="hq-stat-num">{{ hqNetworkStats.completed }}</text>
              <text
                class="hq-pill"
                :class="'hq-pill--' + mockHqRealtimeStatBadges.completed.variant"
                >{{ mockHqRealtimeStatBadges.completed.text }}</text
              >
            </view>
          </view>
          <!-- 转单至总部 -->
          <view class="hq-stat-card">
            <text class="hq-stat-label">转单至总部</text>
            <view class="hq-stat-foot">
              <text class="hq-stat-num">{{ hqTransferredCount }}</text>
              <text
                class="hq-pill"
                :class="'hq-pill--' + mockHqRealtimeStatBadges.transferred.variant"
                >{{ mockHqRealtimeStatBadges.transferred.text }}</text
              >
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  const mockHqRealtimeStatBadges = {
    pending: { text: '-', variant: 'neutral' as const },
    processing: { text: '-', variant: 'neutral' as const },
    completed: { text: '-', variant: 'neutral' as const },
    transferred: { text: '-', variant: 'neutral' as const }
  }

  /** 总部更新时间 */
  defineProps<{
    hqUpdatedAt: string
    /** 全网实时统计 */
    hqNetworkStats: {
      pendingTechAccept: number
      processing: number
      completed: number
      closed: number
    }
    hqTransferredCount: number
  }>()

  const emit = defineEmits<{
    (e: 'stat-tap', tab: 'pending_accept' | 'processing' | 'completed'): void
  }>()
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .hq-workbench-root {
    width: 100%;
    box-sizing: border-box;
    padding-bottom: calc(env(safe-area-inset-bottom, 0px) + #{$space-md});
  }

  .hq-main {
    padding: $space-lg;
    @include flex-col;
    gap: $space-lg;
  }

  .hq-section {
    @include sheet-white($space-lg);
  }

  .hq-section-head {
    @include flex-between;
    margin-bottom: $space-lg;
  }

  .hq-section-title {
    font-size: $font-lg;
    font-weight: 700;
    color: $text-slate-900;
  }

  .hq-section-meta {
    font-size: $font-sm;
    color: $text-slate-500;
  }

  .hq-stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $space-md;
  }

  .hq-stat-card {
    padding: $space-lg;
    border-radius: $radius-lg;
    @include surface-muted;
    border: 2rpx solid $bg-hover;
    box-sizing: border-box;

    &--accent {
      background-color: $primary-alpha-06;
      border-color: $primary-alpha-12;
    }
  }

  .hq-stat-label {
    display: block;
    font-size: $font-md;
    color: $text-slate-500;
    margin-bottom: $space-xs;
  }

  .hq-stat-foot {
    @include flex-between-end;
  }

  .hq-stat-num {
    font-size: $font-xl;
    font-weight: 700;
    color: $text-slate-900;
    line-height: 1.1;

    &--accent {
      color: $primary;
    }
  }

  .hq-pill {
    font-size: $font-sm;
    font-weight: 600;
    padding: 4rpx 12rpx;
    border-radius: $radius-sm;

    &--accent {
      color: $primary;
      background-color: $primary-alpha-12;
    }

    &--down {
      color: $rose-500;
      background-color: $rose-50;
    }

    &--up {
      color: $emerald-500;
      background-color: $emerald-50;
    }

    &--neutral {
      color: $text-slate-500;
      background-color: $bg-hover;
    }
  }
</style>
