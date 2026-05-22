<template>
  <custom-nav-bar title="首页" surface="sticky" :show-back="false" :shadow="false" />
  <view class="page-index">
    <view class="home-body page-padding page-padding-no-safe">
      <!-- 欢迎语 -->
      <view class="custom-header">
        <view class="header-left">
          <view class="welcome-section">
            <text class="welcome-title">
              您好，
              <text class="text-primary">{{ userName }}</text>
            </text>
            <text class="welcome-subtitle">{{ welcomeSubtitle }}</text>
          </view>
        </view>
        <view class="header-right">
          <view class="icon-btn" @click="goToClosedOrderList">
            <image class="header-svg-icon" :src="notificationsIcon" mode="aspectFit" />
          </view>
        </view>
      </view>

      <!-- 主要操作 -->
      <view class="main-actions">
        <view class="action-card card-jasic" @click="goToJasicRepair">
          <image class="card-bg-img" :src="cardJasicBg" mode="aspectFill" />
          <view class="card-bg-mask card-bg-mask-jasic"></view>
          <view class="card-inner">
            <view class="card-icon-wrap">
              <image class="jasic-svg-icon" :src="jasicScanIcon" mode="aspectFit" />
            </view>
            <view class="card-text-wrap">
              <text class="card-title">佳士品牌报修</text>
              <view class="card-subtitle-wrap">
                <text class="card-subtitle">扫码报修 · 官方直营</text>
                <image class="arrow-icon" :src="arrowForwardIcon" mode="aspectFit" />
              </view>
            </view>
          </view>
        </view>

        <view class="action-card card-other" @click="goToOtherRepair">
          <image class="card-bg-img" :src="cardOtherBg" mode="aspectFill" />
          <view class="card-bg-mask card-bg-mask-other"></view>
          <view class="card-inner">
            <view class="card-icon-wrap">
              <image class="other-repair-svg-icon" :src="otherRepairIcon" mode="aspectFit" />
            </view>
            <view class="card-text-wrap">
              <text class="card-title">非佳士报修</text>
              <view class="card-subtitle-wrap">
                <text class="card-subtitle">通用维修 · 极速响应</text>
                <image class="arrow-icon" :src="arrowForwardIcon" mode="aspectFit" />
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 报修指南 -->
      <view class="guide-section">
        <view class="section-header">
          <text class="section-title">报修指南</text>
        </view>
        <view class="guide-card" @click="goToRepairGuide">
          <view class="guide-line"></view>
          <view class="guide-step">
            <view class="step-icon active">
              <image class="step-svg-icon" :src="editDocumentIcon" mode="aspectFit" />
            </view>
            <text class="step-text">1. 提交申请</text>
          </view>
          <view class="guide-step">
            <view class="step-icon">
              <image class="step-svg-icon" :src="engineeringIcon" mode="aspectFit" />
            </view>
            <text class="step-text">2. 派单处理</text>
          </view>
          <view class="guide-step">
            <view class="step-icon">
              <image class="step-svg-icon" :src="homeRepairServiceIcon" mode="aspectFit" />
            </view>
            <text class="step-text">3. 进行维修</text>
          </view>
          <view class="guide-step">
            <view class="step-icon">
              <image class="step-svg-icon" :src="taskCompleteIcon" mode="aspectFit" />
            </view>
            <text class="step-text">4. 服务评价</text>
          </view>
        </view>
      </view>

      <!-- 报修进度（有最新工单时才展示） -->
      <view v-if="hasLatestOrder" class="status-section">
        <view class="section-header">
          <text class="section-title">报修进度</text>
          <view class="header-right-action" @click="goToOrderList">
            <text class="action-text">全部订单</text>
            <uni-icons type="right" size="12" :color="themeColor.primary"></uni-icons>
          </view>
        </view>
        <view class="status-card" @click="goToOrderDetail">
          <view class="status-top">
            <view class="status-info">
              <view class="status-icon-bg">
                <image class="icon-tv" :src="tvGenIcon" mode="aspectFit" />
              </view>
              <view class="status-desc">
                <text class="status-name">{{ latestOrder.description || '-' }}</text>
                <text class="status-id">{{ latestOrder.orderNo || '-' }}</text>
              </view>
            </view>
          </view>
          <view class="status-timeline">
            <view class="timeline-line"></view>
            <view class="timeline-dot"></view>
            <text class="timeline-title">{{ latestOrder.timelineTitle || '-' }}</text>
            <view class="timeline-sub">
              <image class="icon-time" :src="scheduleIcon" mode="aspectFit" />
              <text class="time-text">{{ latestOrder.timelineSub || '-' }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { useUserStore } from '@/stores'
  import { getCustomerWorkOrderLatestSummary, type LatestOrderDTO } from '@/api/workOrder'
  import { themeColor } from '@/constants/theme'
  import { isLoggedIn, requireLogin } from '@/utils/auth'
  import {
    arrowForwardIcon,
    editDocumentIcon,
    engineeringIcon,
    homeRepairServiceIcon,
    jasicScanIcon,
    notificationsIcon,
    otherRepairIcon,
    scheduleIcon,
    taskCompleteIcon,
    tvGenIcon
  } from '@/svgs'
  /**
 * 首页报修卡片背景（本地 static，小程序用 image 更稳）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const cardJasicBg = '/static/images/card-jasic-bg.jpg'
  const cardOtherBg = '/static/images/card-other-bg.jpg'

  // 用户商店
  const userStore = useUserStore()
  // 用户名（后端 `CustomerUserInfoVO.nickname`）
  const userName = computed(() => String(userStore.userInfo?.nickname || '佳士用户'))
  // 欢迎语（C 端无角色分支，统一展示客户文案）
  const welcomeSubtitle = computed(() => '专业维修，安心保障')

  // 最新工单
  const latestOrder = ref<LatestOrderDTO>({
    id: '',
    description: '',
    orderNo: '',
    statusText: '',
    timelineTitle: '',
    timelineSub: '',
    status: ''
  })

  // 是否有最新工单
  const hasLatestOrder = computed(() => !!latestOrder.value.id)

  /**
   * 空工单
   * @returns LatestOrderDTO
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const emptyOrder: LatestOrderDTO = {
    id: '',
    description: '',
    orderNo: '',
    statusText: '',
    timelineTitle: '暂无报修工单',
    timelineSub: '提交报修后将在此显示最新进度',
    status: ''
  }

  /**
   * 加载最新工单
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const loadLatest = async () => {
    if (!isLoggedIn()) {
      latestOrder.value = emptyOrder
      return
    }
    try {
      const res = await getCustomerWorkOrderLatestSummary()
      latestOrder.value = res.data
    } catch {
      latestOrder.value = emptyOrder
    }
  }

  /**
   * 页面显示
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  onShow(() => {
    loadLatest()
  })

  /**
   * 跳转到佳士报修
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToJasicRepair = () => {
    if (!requireLogin()) return
    uni.navigateTo({ url: '/pages/jasicRepair/index' })
  }

  /**
   * 跳转到非佳士报修
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToOtherRepair = () => {
    if (!requireLogin()) return
    uni.navigateTo({ url: '/pages/otherRepair/index' })
  }

  /**
   * 跳转到报修指南
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToRepairGuide = () => {
    if (!requireLogin()) return
    uni.navigateTo({ url: '/pages/repairGuide/index' })
  }

  /**
   * 跳转到工单列表
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToOrderList = () => {
    if (!requireLogin()) return
    uni.navigateTo({ url: '/pages/order/list' })
  }

  /**
   * 跳转到已关闭工单列表
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToClosedOrderList = () => {
    if (!requireLogin()) return
    uni.navigateTo({ url: '/pages/order/list?tab=4' })
  }

  /**
   * 跳转到工单详情
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToOrderDetail = () => {
    if (!requireLogin()) return
    const id = latestOrder.value.id
    if (!id) return
    const status = latestOrder.value.status || latestOrder.value.statusText
    uni.navigateTo({
      url: `/pages/order/detail?id=${encodeURIComponent(id)}&status=${encodeURIComponent(String(status || ''))}`
    })
  }
</script>

<style lang="scss" scoped>
  .home-body {
    @include flex-column-gap;
    flex: 1;
    min-height: 0;
  }

  .custom-header {
    @include flex-between;
    background-color: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    margin: 0 -32rpx;
    padding: $space-md $space-lg;

    .header-left {
      @include flex-row;
      gap: $space-sm;
    }

    .header-right {
      @include flex-row;
      gap: $space-md;

      .icon-btn {
        @include flex-center;
        color: $text-secondary;
        transition: color 0.3s;

        &:active {
          color: $primary;
        }

        .header-svg-icon {
          width: $font-title;
          height: $font-title;
        }
      }
    }
  }

  .welcome-section {
    padding: 0 $space-lg;

    .welcome-title {
      color: $text-main;
      font-size: $font-title;
      font-weight: 800;
      letter-spacing: -0.5rpx;

      .text-primary {
        color: $primary;
      }
    }

    .welcome-subtitle {
      display: block;
      color: $text-secondary;
      font-size: $font-md;
      margin-top: 12rpx;
      font-weight: 500;
    }
  }

  // 主操作卡片（佳士 / 非佳士）
  .main-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 58rpx;

    .action-card {
      position: relative;
      overflow: hidden;
      border-radius: $radius-xxl;
      height: 410rpx;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
      transition: transform 0.2s;
      box-sizing: border-box;

      .card-bg-img {
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        z-index: 0;
      }

      .card-bg-mask {
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        z-index: 1;
      }

      .card-bg-mask-jasic {
        background: rgba($primary, 0.85);
      }

      .card-bg-mask-other {
        background: rgba(0, 0, 0, 0.5);
      }

      .card-inner {
        position: relative;
        z-index: 2;
        height: 100%;
        box-sizing: border-box;
        padding: $space-lg;
        @include flex-column;
        justify-content: space-between;
      }

      &:active {
        transform: scale(0.95);
      }

      .card-title {
        color: $primary-contrast;
        font-size: $font-xl;
        font-weight: bold;
        line-height: 1.2;
      }

      .card-subtitle-wrap {
        @include flex-row;
        gap: $space-xs;
        margin-top: 12rpx;

        .card-subtitle,
        .arrow-icon {
          font-size: 22rpx;
          color: rgba($primary-contrast, 0.8);
        }
        .arrow-icon {
          width: 20rpx;
          height: 20rpx;
          opacity: 0.8;
          display: block;
          flex-shrink: 0;
        }
      }
    }

    .card-jasic {
      box-shadow: 0 10rpx 30rpx rgba($primary, 0.2);

      .card-icon-wrap {
        background: rgba($primary-contrast, 0.2);
        backdrop-filter: blur(8px);
        width: 96rpx;
        height: 96rpx;
        border-radius: $radius-lg;
        @include flex-center;

        .jasic-svg-icon {
          width: 60rpx;
          height: 60rpx;
          display: block;
        }
      }
    }

    .card-other {
      .card-icon-wrap {
        background: $bg-light;
        width: 96rpx;
        height: 96rpx;
        border-radius: $radius-lg;
        @include flex-center;

        .other-repair-svg-icon {
          width: 60rpx;
          height: 60rpx;
          display: block;
          color: $text-body;
        }
      }

      .card-subtitle-wrap .card-subtitle,
      .card-subtitle-wrap .arrow-icon {
        opacity: 0.9;
      }
    }
  }

  // 区块标题
  .section-header {
    @include flex-between;
    margin-bottom: $space-lg;

    .section-title {
      color: $text-main;
      font-size: $font-lg;
      font-weight: bold;
    }

    .header-right-action {
      @include flex-row;
      gap: 4rpx;
      color: $primary;

      .action-text {
        font-size: $font-sm;
        font-weight: bold;
        line-height: 1;
      }

      .uni-icons {
        display: flex;
        align-items: center;
        line-height: 1;
      }
    }
  }

  // 报修指南
  .guide-section .guide-card {
    @include white-card($radius-xxl, $space-lg);
    @include flex-between;
    align-items: flex-start;
    position: relative;

    .guide-line {
      position: absolute;
      top: 70rpx;
      left: 64rpx;
      right: 64rpx;
      height: 2rpx;
      background: $border-color;
      z-index: 0;
    }

    .guide-step {
      @include flex-column-center;
      gap: $space-sm;
      position: relative;
      z-index: 10;
      width: 25%;

      .step-icon {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        background: $bg-card;
        border: 4rpx solid $primary;
        color: $primary;
        box-sizing: border-box;
        @include flex-center;

        &.active {
          background: $primary;
          color: $primary-contrast;
          box-shadow: 0 4rpx 12rpx rgba($primary, 0.3);
          border: none;
        }

        .step-svg-icon {
          width: $font-xxl;
          height: $font-xxl;
          display: block;
        }
      }

      .step-text {
        font-size: $font-xs;
        font-weight: bold;
        color: $text-main;
        text-align: center;
      }
    }
  }

  // 报修进度
  .status-section {
    padding-bottom: $space-md;

    .status-card {
      @include white-card($radius-xxl, $space-xl);

      .status-top {
        @include flex-between;
        align-items: flex-start;
        margin-bottom: $space-xl;

        .status-info {
          @include flex-row;
          gap: $space-md;

          .status-icon-bg {
            background: rgba($primary, 0.05);
            width: 96rpx;
            height: 96rpx;
            border-radius: $radius-lg;
            @include flex-center;

            .icon-tv {
              width: 56rpx;
              height: 56rpx;
            }
          }

          .status-desc {
            @include flex-column;

            .status-name {
              font-size: $font-md;
              font-weight: bold;
              color: $text-main;
              line-height: 1.4;
            }

            .status-id {
              font-size: $font-xs;
              color: $text-placeholder;
              margin-top: $space-xs;
              font-weight: 500;
              letter-spacing: 1rpx;
            }
          }
        }
      }

      .status-timeline {
        position: relative;
        padding-left: 56rpx;
        padding-top: 4rpx;
        padding-bottom: 4rpx;

        .timeline-line {
          position: absolute;
          left: 0;
          top: 0;
          bottom: 0;
          width: 2rpx;
          background: $border-color;
        }

        .timeline-dot {
          position: absolute;
          left: -12rpx;
          top: 0;
          width: 24rpx;
          height: 24rpx;
          border-radius: 50%;
          background: $primary;
          border: 6rpx solid $primary-contrast;
          box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
          box-sizing: border-box;
        }

        .timeline-title {
          display: block;
          font-size: $font-sm;
          font-weight: bold;
          color: $text-main;
        }

        .timeline-sub {
          @include flex-row;
          gap: 12rpx;
          margin-top: $space-xs;

          .icon-time {
            width: 26rpx;
            height: 26rpx;
          }
          .time-text {
            font-size: 22rpx;
            color: $text-secondary;
          }
        }
      }
    }
  }
</style>
