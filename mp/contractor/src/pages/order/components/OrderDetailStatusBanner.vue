<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）页面 order / components / OrderDetailStatusBanner -->
  <view class="od-top-section">
    <view class="status-banner">
      <view class="status-top">
        <view class="status-text-wrap">
          <text class="status-title">{{ orderStatusText }}</text>
          <text class="status-desc">{{ statusDesc }}</text>
        </view>
        <image v-if="statusIconSrc" class="status-icon" :src="statusIconSrc" mode="aspectFit" />
      </view>

      <view class="stepper-wrap">
        <view class="stepper-line"></view>
        <view v-for="(step, index) in steps" :key="index" class="step-item">
          <view :class="['step-dot', index <= stepIndex ? 'active' : '']"></view>
          <text :class="['step-text', index <= stepIndex ? 'active' : '']">{{ step }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { WorkOrderMainStatus } from '@/models/order'
  import { ORDER_STATUS_TEXT_MAP, getStatusDesc, getStepIndex } from '@/utils/orderStatus'
  import {
    statusBuildCircleIcon,
    statusCheckCircleIcon,
    statusPendingActionsIcon,
    statusTaskAltIcon
  } from '@/svgs'

  const props = defineProps<{
    status: WorkOrderMainStatus
  }>()

  /**
 * 与主状态枚举一致：待派单 / 待接单 拆成两步（对齐列表侧 PENDING_ASSIGN / PENDING_TECH_ACCEPT）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const steps = ['待派单', '待接单', '维修中', '已完成', '已关闭'] as const

  const orderStatusText = computed(() => ORDER_STATUS_TEXT_MAP[props.status])

  const statusDesc = computed(() => getStatusDesc(props.status))

  const statusIconSrc = computed(() => {
    switch (props.status) {
      case 'PENDING_ASSIGN':
      case 'PENDING_TECH_ACCEPT':
        return statusPendingActionsIcon
      case 'IN_PROGRESS':
        return statusBuildCircleIcon
      case 'COMPLETED':
        return statusCheckCircleIcon
      case 'CLOSED':
        return statusTaskAltIcon
      default:
        return ''
    }
  })

  const stepIndex = computed(() => getStepIndex(props.status))
</script>

<style lang="scss" scoped>
  .od-top-section {
    position: relative;
    z-index: 0;
    background-color: $primary;
    color: $text-bg;
    padding-bottom: 64rpx;
  }

  .status-banner {
    padding: 0 $space-xl $space-lg;

    .status-top {
      @include flex-between;
      margin-bottom: $space-lg;

      .status-text-wrap {
        @include flex-col;

        .status-title {
          font-size: 40rpx;
          font-weight: bold;
        }

        .status-desc {
          font-size: $font-sm;
          color: rgba(255, 255, 255, 0.8);
          margin-top: $space-xs;
        }
      }

      .status-icon {
        width: 96rpx;
        height: 96rpx;
        opacity: 0.8;
      }
    }

    .stepper-wrap {
      position: relative;
      display: flex;
      justify-content: space-between;
      padding: 0 $space-sm;

      .stepper-line {
        position: absolute;
        top: 12rpx;
        left: $space-sm;
        right: $space-sm;
        height: 4rpx;
        background-color: rgba(255, 255, 255, 0.3);
        z-index: 1;
      }

      .step-item {
        position: relative;
        z-index: 10;
        @include flex-col;
        align-items: center;
        gap: $space-sm;

        .step-dot {
          width: $space-md;
          height: $space-md;
          border-radius: 50%;
          background-color: rgba(255, 255, 255, 0.5);
          box-shadow: 0 0 0 8rpx rgba(255, 255, 255, 0.1);

          &.active {
            background-color: $bg-card;
            box-shadow: 0 0 0 8rpx rgba(255, 255, 255, 0.2);
          }
        }

        .step-text {
          font-size: $font-sm;
          font-weight: 500;
          color: rgba(255, 255, 255, 0.7);

          &.active {
            font-weight: bold;
            color: $text-bg;
          }
        }
      }
    }
  }
</style>
