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
          <view class="hq-stat-card hq-stat-card--accent" @tap="emit('stat-tap', 'pending')">
            <text class="hq-stat-label">待接单</text>
            <view class="hq-stat-foot">
              <text class="hq-stat-num hq-stat-num--accent">{{ hqNetworkStats.pending }}</text>
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

      <!-- 网点负荷监控 -->
      <view class="hq-section hq-section--tail">
        <view class="hq-section-head">
          <text class="hq-section-title">网点负荷监控</text>
          <text class="hq-link" @tap="emit('view-all-branches')">查看全部</text>
        </view>
        <!-- 网点列表 -->
        <view class="hq-branch-list">
          <view
            v-for="branch in branchList"
            :key="branch.id"
            class="hq-branch-card"
            @tap="emit('branch-tap', branch)"
          >
            <!-- 网点左侧 -->
            <view class="hq-branch-left">
              <view class="hq-branch-icon">
                <image class="hq-branch-icon-img" :src="locationOnIcon" mode="aspectFit" />
              </view>
              <view class="hq-branch-text">
                <text class="hq-branch-name">{{ branch.name }}</text>
                <text class="hq-branch-sub">当前负荷: {{ branch.load }}%</text>
              </view>
            </view>
            <!-- 网点右侧 -->
            <view class="hq-branch-right">
              <!-- 负荷条 -->
              <view class="hq-mini-bar">
                <view
                  class="hq-mini-bar-inner"
                  :class="'tone-' + branch.statusClass"
                  :style="{ width: branch.load + '%' }"
                ></view>
              </view>
              <!-- 负荷状态 -->
              <text class="hq-branch-status" :class="'tone-' + branch.statusClass">{{
                branch.statusText
              }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { locationOnIcon } from '@/svgs'
  import type { HqBranchRow } from '../useIndexWorkbench'

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
      pending: number
      processing: number
      completed: number
      closed: number
    }
    hqTransferredCount: number
    branchList: HqBranchRow[]
  }>()

  const emit = defineEmits<{
    (e: 'stat-tap', tab: 'pending' | 'processing' | 'completed'): void
    (e: 'view-all-branches'): void
    (e: 'branch-tap', branch: HqBranchRow): void
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

    &--tail {
      margin-bottom: 0;
    }
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

  .hq-link {
    font-size: $font-md;
    font-weight: 500;
    color: $primary;
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
    border: 2rpx solid $surface-slate-100;
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
      background-color: $surface-slate-100;
    }
  }

  .hq-branch-list {
    @include flex-col;
    gap: $space-md;
  }

  .hq-branch-card {
    @include flex-between;
    padding: $space-lg;
    background-color: $surface-white;
    border: 2rpx solid $surface-slate-100;
    border-radius: $radius-lg;
    box-sizing: border-box;
  }

  .hq-branch-left {
    @include flex-row;
    gap: $space-md;
    flex: 1;
    min-width: 0;
  }

  .hq-branch-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: $radius-md;
    background-color: $primary-alpha-10;
    @include flex-center;
    flex-shrink: 0;
  }

  .hq-branch-icon-img {
    width: 48rpx;
    height: 48rpx;
  }

  .hq-branch-text {
    @include flex-col;
    gap: $space-xs;
    min-width: 0;
  }

  .hq-branch-name {
    font-size: 30rpx;
    font-weight: 700;
    color: $text-slate-900;
  }

  .hq-branch-sub {
    font-size: $font-sm;
    color: $text-slate-500;
  }

  .hq-branch-right {
    @include flex-col;
    align-items: flex-end;
    flex-shrink: 0;
    margin-left: $space-sm;
  }

  .hq-mini-bar {
    width: 192rpx;
    height: $space-xs;
    border-radius: $radius-pill;
    background-color: $surface-slate-100;
    overflow: hidden;
    margin-bottom: $space-xs;
  }

  .hq-mini-bar-inner {
    height: 100%;
    border-radius: $radius-pill;

    &.tone-high {
      background-color: $primary;
    }

    &.tone-normal {
      background-color: $emerald-500;
    }

    &.tone-medium {
      background-color: $amber-500;
    }
  }

  .hq-branch-status {
    font-size: 20rpx;
    font-weight: 700;

    &.tone-high {
      color: $primary;
    }

    &.tone-normal {
      color: $emerald-500;
    }

    &.tone-medium {
      color: $amber-500;
    }
  }
</style>
