<template>
  <view class="page-my page-index">
    <!-- 导航栏 -->
    <view class="header-section">
      <custom-nav-bar title="我的" surface="transparent" tone="light" :show-back="false" />
      <!-- 用户信息 -->
      <view class="user-info">
        <view class="avatar-container" hover-class="avatar-container-hover" @click="openAvatarPreview">
          <view class="avatar-wrap">
            <image class="avatar-img" :src="avatarUrl" mode="aspectFill" />
          </view>
          <view class="star-badge">
            <image class="star-icon" :src="starBadgeIcon" mode="aspectFit" />
          </view>
        </view>
        <view class="user-details" @click="openProfileEdit">
          <text class="username">{{ userName }}</text>
          <view class="user-id-wrap">
            <text class="user-id">{{ phoneText }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 内容区域 -->
    <view class="content-area page-padding page-padding-no-safe">
      <!-- 工单状态卡片 -->
      <view class="card">
        <view class="card-header-simple" @click="goToOrderList">
          <text class="card-title">我的工单</text>
          <view class="card-more">
            <text>查看全部</text>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
        </view>
        <view class="grid-4">
          <view class="grid-item" @click="goToOrderList(1)">
            <view class="icon-wrap">
              <image class="assignment-late-icon" :src="assignmentLateIcon" mode="aspectFit" />
            </view>
            <text class="grid-text">待接单</text>
            <view v-if="counts.pending" class="badge">{{ counts.pending }}</view>
          </view>
          <view class="grid-item" @click="goToOrderList(2)">
            <view class="icon-wrap">
              <image class="repairing-icon" :src="repairingIcon" mode="aspectFit" />
            </view>
            <text class="grid-text">维修中</text>
            <view v-if="counts.repairing" class="badge">{{ counts.repairing }}</view>
          </view>
          <view class="grid-item" @click="goToOrderList(3)">
            <view class="icon-wrap">
              <image class="task-complete-icon" :src="taskCompleteIcon" mode="aspectFit" />
            </view>
            <text class="grid-text">已完成</text>
            <view v-if="counts.completed" class="badge">{{ counts.completed }}</view>
          </view>
          <view class="grid-item" @click="goToOrderList(4)">
            <view class="icon-wrap">
              <image
                class="cancel-presentation-icon"
                :src="cancelPresentationIcon"
                mode="aspectFit"
              />
            </view>
            <text class="grid-text">已关闭</text>
            <view v-if="counts.closed" class="badge">{{ counts.closed }}</view>
          </view>
        </view>
      </view>

      <!-- 常用功能 -->
      <view class="card">
        <view class="card-header border-b">
          <text class="card-title">常用功能</text>
        </view>
        <view class="list-group">
          <view class="list-item" @click="goToAddress">
            <view class="list-item-left">
              <image class="location-on-icon" :src="locationOnIcon" mode="aspectFit" />
              <text class="list-item-text">我的地址</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
        </view>
      </view>

      <!-- 系统设置 -->
      <view class="card">
        <view class="card-header border-b">
          <text class="card-title">系统设置</text>
        </view>
        <view class="list-group">
          <view class="list-item" @click="goToFeedback">
            <view class="list-item-left">
              <image class="headset-mic-icon" :src="headsetMicIcon" mode="aspectFit" />
              <text class="list-item-text">投诉与建议</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
          <view class="list-item" @click="goToAbout">
            <view class="list-item-left">
              <image class="info-circle-icon" :src="infoCircleIcon" mode="aspectFit" />
              <text class="list-item-text">关于我们</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
        </view>
      </view>

      <!-- 退出登录按钮 -->
      <view class="logout-btn" @click="handleLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <!-- 头像全屏预览：蒙版底部提供更换头像 / 更换信息 -->
    <view v-if="avatarPreviewVisible" class="avatar-preview-mask" @click="closeAvatarPreview">
      <view class="avatar-preview-center" @click.stop>
        <image class="avatar-preview-image" :src="avatarUrl" mode="aspectFit" />
      </view>
      <view class="avatar-preview-footer" @click.stop>
        <!-- #ifdef MP-WEIXIN -->
        <button
          class="preview-action-btn"
          open-type="chooseAvatar"
          @chooseavatar="onChooseAvatarFromPreview"
        >
          更换头像
        </button>
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <view class="preview-action-btn" @click="onChangeAvatarFallback">更换头像</view>
        <!-- #endif -->
        <view class="preview-action-btn" @click="openProfileEditFromPreview">更换信息</view>
      </view>
      <view class="avatar-preview-safe" />
    </view>

  </view>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { onHide, onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { useUserStore } from '@/stores'
  import { getUserInfo, logout, updateProfile } from '@/api/auth'
  import { uploadCustomerFile } from '@/api/file'
  import { resolveAvatarDisplayUrl, resolveAvatarUrlForSubmit } from '@/utils/profileAvatar'
  import { showApiToast } from '@/utils/uiFeedback'
  import { countCustomerWorkOrderStatus, type MyOrderCountsDTO } from '@/api/workOrder'
  import { isLoggedIn } from '@/utils/auth'
  import { themeColors } from '@/constants/theme'
  import {
    assignmentLateIcon,
    cancelPresentationIcon,
    headsetMicIcon,
    infoCircleIcon,
    locationOnIcon,
    repairingIcon,
    starBadgeIcon,
    taskCompleteIcon
  } from '@/svgs'
  import { showToastThen, switchTabThen, TAB_HOME } from '@/utils/toastNavigate'

  const PROFILE_EDIT_URL = '/pages/profile/edit'

  // 用户商店
  const userStore = useUserStore()

  // 默认头像（本地 static）
  const defaultAvatar = '/static/images/default-avatar.jpg'

  const avatarPreviewVisible = ref(false)

  /** 用户头像：优先后端保存的微信头像 URL，无则默认图 */
  const avatarUrl = computed(() =>
    resolveAvatarDisplayUrl(userStore.userInfo?.avatar, defaultAvatar),
  )

  // 用户名（后端 `CustomerUserInfoVO.nickname`，缺省用兜底文案）
  const userName = computed(() => String(userStore.userInfo?.nickname || '佳士用户'))

  // 手机号（后端 `CustomerUserInfoVO.phone`）
  const phoneText = computed(() => String(userStore.userInfo?.phone || '-'))

  // 工单统计
  const counts = ref<MyOrderCountsDTO>({
    pending: 0,
    repairing: 0,
    completed: 0,
    closed: 0
  })

  /**
   * 刷新当前登录客户资料（头像、昵称等与后端同步）
   */
  const refreshUserInfo = async () => {
    try {
      const res = await getUserInfo()
      if (res.data) userStore.setUserInfo(res.data)
    } catch {
      /* 失败时沿用本地缓存 */
    }
  }

  /**
   * 头像预览蒙版与原生 tabBar 联动：微信 tabBar 层级高于普通 view，预览时需先隐藏 tabBar。
   */
  const syncTabBarForAvatarPreview = (visible: boolean) => {
    if (visible) {
      uni.hideTabBar({ animation: false })
      return
    }
    uni.showTabBar({ animation: false })
  }

  /** 全屏预览当前头像 */
  const openAvatarPreview = () => {
    avatarPreviewVisible.value = true
    syncTabBarForAvatarPreview(true)
  }

  const closeAvatarPreview = () => {
    if (!avatarPreviewVisible.value) return
    avatarPreviewVisible.value = false
    syncTabBarForAvatarPreview(false)
  }

  /** 跳转「更换信息」页（点击昵称或预览蒙版底部按钮） */
  const goProfileEdit = () => {
    uni.navigateTo({ url: PROFILE_EDIT_URL })
  }

  const openProfileEdit = () => {
    goProfileEdit()
  }

  const openProfileEditFromPreview = () => {
    closeAvatarPreview()
    goProfileEdit()
  }

  /** 仅更新头像并同步 userStore */
  const saveAvatarOnly = async (displayPath: string) => {
    uni.showLoading({ title: '保存中', mask: true })
    try {
      const avatar = await resolveAvatarUrlForSubmit(
        displayPath,
        userStore.userInfo?.avatar,
        uploadCustomerFile,
        defaultAvatar,
      )
      if (avatar === undefined) return
      const res = await updateProfile({ avatar })
      userStore.setUserInfo(res.data)
      void showApiToast('头像已更新')
    } catch {
      /* http 层已提示 */
    } finally {
      uni.hideLoading()
    }
  }

  const onChooseAvatarFromPreview = async (e: { detail?: { avatarUrl?: string } }) => {
    const url = String(e.detail?.avatarUrl ?? '').trim()
    if (!url) return
    closeAvatarPreview()
    await saveAvatarOnly(url)
  }

  const onChangeAvatarFallback = () => {
    closeAvatarPreview()
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const path = res.tempFilePaths?.[0]
        if (path) await saveAvatarOnly(path)
      },
    })
  }

  /**
   * 加载工单统计
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const loadCounts = async () => {
    try {
      const res = await countCustomerWorkOrderStatus()
      counts.value = {
        pending: res.data?.waitAcceptCount ?? 0,
        repairing: res.data?.inProgressCount ?? 0,
        completed: res.data?.completedCount ?? 0,
        closed: res.data?.closedCount ?? 0
      }
    } catch {
      counts.value = { pending: 0, repairing: 0, completed: 0, closed: 0 }
    }
  }

  /**
   * 页面显示
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  onShow(() => {
    if (!isLoggedIn()) {
      switchTabThen(TAB_HOME, () => {
        uni.navigateTo({ url: '/pages/login/index' })
      })
      return
    }
    void refreshUserInfo()
    loadCounts()
  })

  /** 离开页面时若预览仍打开，恢复 tabBar，避免其它 tab 页底部栏消失 */
  onHide(() => {
    if (!avatarPreviewVisible.value) return
    avatarPreviewVisible.value = false
    syncTabBarForAvatarPreview(false)
  })

  /**
   * 跳转工单列表
   * @param tabIndex - 工单状态索引
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToOrderList = (tabIndex = 0) => {
    const q = tabIndex > 0 ? `?tab=${tabIndex}` : ''
    uni.navigateTo({ url: `/pages/order/list${q}` })
  }

  /**
   * 跳转收货地址
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToAddress = () => {
    uni.navigateTo({ url: '/pages/address/index' })
  }

  /** 跳转投诉与建议 */
  const goToFeedback = () => {
    uni.navigateTo({ url: '/pages/feedback/index' })
  }

  /**
   * 跳转关于我们
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToAbout = () => {
    uni.navigateTo({ url: '/pages/about/index' })
  }

  /**
   * 退出登录
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleLogout = () => {
    uni.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: async (res) => {
        if (!res.confirm) return
        let logoutMsg = '已退出登录'
        try {
          // http.ts 已根据 POST 自动显示 loading；这里不再手动管理
          const res = await logout()
          logoutMsg = res.msg || logoutMsg
        } catch {
          /* http 层已提示；仍清理本地，避免无法退出 */
        }
        userStore.clearUserInfo()
        uni.removeStorageSync('token')
        showToastThen('/pages/login/index', {
          title: logoutMsg,
          duration: 1500,
          navigateType: 'reLaunch'
        })
      }
    })
  }
</script>

<style lang="scss">
  .page-my.page-index {
    gap: 0;
  }

  .header-section {
    background-color: $primary;
    color: $primary-contrast;
    padding: $space-lg;
    padding-bottom: 96rpx;
    border-bottom-left-radius: $radius-xxl;
    border-bottom-right-radius: $radius-xxl;
    box-shadow: 0 20rpx 30rpx -6rpx rgba(0, 0, 0, 0.1);

    .user-info {
      @include flex-row;
      gap: $space-lg;
    }

    .avatar-container {
      position: relative;

      &-hover {
        opacity: 0.92;
      }

      .avatar-wrap {
        width: 140rpx;
        height: 140rpx;
        border-radius: 50%;
        background-color: $primary-contrast;
        overflow: hidden;

        .avatar-img {
          width: 100%;
          height: 100%;
        }
      }

      .star-badge {
        position: absolute;
        bottom: 0;
        right: 0;
        background-color: $vip-badge-bg;
        border-radius: 50%;
        padding: $space-xs;
        border: 4rpx solid $primary;
        @include flex-center;

        .star-icon {
          width: 24rpx;
          height: 24rpx;
          display: block;
        }
      }
    }

    .user-details {
      @include flex-column;

      .username {
        font-size: $font-title;
        font-weight: bold;
        line-height: 1.2;
      }

      .user-id-wrap {
        @include flex-row;
        margin-top: $space-xs;
        opacity: 0.9;

        .user-id {
          font-size: $font-md;
        }
      }
    }
  }

  .content-area.page-padding {
    margin-top: -48rpx;
    @include flex-column;
    gap: $space-lg;
  }

  .page-my .card {
    @include white-card($radius-lg, $space-lg);
    box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.05);
    overflow: hidden;

    .card-header-simple {
      @include flex-between;
      padding-bottom: $space-sm;

      .card-title {
        color: $text-dark;
        font-weight: bold;
        font-size: $font-lg;
      }

      .card-more {
        @include flex-row;
        color: $text-muted;
        font-size: $font-md;
      }
    }

    .card-header {
      padding-bottom: -sm;

      &.border-b {
        border-bottom: 2rpx solid $border-lighter;
      }

      .card-title {
        color: $text-dark;
        font-weight: bold;
        font-size: $font-lg;
      }
    }

    .grid-4 {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: $space-sm;

      .grid-item {
        @include flex-column-center;
        gap: 12rpx;
        padding: $space-sm 0;
        position: relative;

        .icon-wrap {
          width: 96rpx;
          height: 96rpx;
          border-radius: 50%;
          background-color: rgba($primary, 0.1);
          @include flex-center;
          color: $primary;

          .assignment-late-icon {
            width: 56rpx;
            height: 56rpx;
            display: block;
          }

          .repairing-icon {
            width: 56rpx;
            height: 56rpx;
            display: block;
          }

          .task-complete-icon {
            width: 56rpx;
            height: 56rpx;
            display: block;
          }

          .cancel-presentation-icon {
            width: 56rpx;
            height: 56rpx;
            display: block;
          }
        }

        .grid-text {
          color: $text-body;
          font-size: $font-sm;
          font-weight: 500;
        }

        .badge {
          position: absolute;
          top: $space-xs;
          right: 24rpx;
          background-color: $danger-emphasis;
          color: $primary-contrast;
          font-size: $font-xs;
          width: 32rpx;
          height: 32rpx;
          @include flex-center;
          border-radius: 50%;
        }
      }
    }

    .list-group {
      @include flex-column;

      .list-item {
        @include flex-between;
        padding: $space-lg 0;
        transition: background-color 0.2s;

        &:active {
          background-color: $bg-light;
        }

        .list-item-left {
          @include flex-row;
          gap: $space-md;
        }

        .list-item-text {
          color: $text-body;
          font-weight: 500;
          font-size: 30rpx;
        }
      }
    }
  }

  .page-my .location-on-icon {
    width: 38rpx;
    height: 38rpx;
    display: block;
    flex-shrink: 0;
  }

  .page-my .info-circle-icon,
  .page-my .headset-mic-icon {
    width: 38rpx;
    height: 38rpx;
    display: block;
    flex-shrink: 0;
  }

  .logout-btn {
    @include white-card($radius-lg, $space-lg);
    @include flex-center;
    box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.05);
    transition: background-color 0.2s;

    &:active {
      background-color: $bg-hover;
    }

    .logout-text {
      color: $danger;
      font-weight: 600;
      font-size: $font-lg;
    }
  }

  .avatar-preview-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100vw;
    height: 100vh;
    z-index: 99999;
    background-color: rgba(0, 0, 0, 0.88);
    display: flex;
    flex-direction: column;
  }

  .avatar-preview-center {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 48rpx;
    box-sizing: border-box;
  }

  .avatar-preview-image {
    width: 100%;
    max-height: 70vh;
  }

  .avatar-preview-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 32rpx;
    padding: 32rpx 48rpx calc(32rpx + env(safe-area-inset-bottom));
    background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.45) 100%);
  }

  .preview-action-btn {
    min-width: 240rpx;
    padding: 24rpx 40rpx;
    border-radius: 48rpx;
    background-color: rgba(255, 255, 255, 0.16);
    color: #fff;
    font-size: 30rpx;
    font-weight: 600;
    text-align: center;
    line-height: 1.4;
    border: 2rpx solid rgba(255, 255, 255, 0.35);
    box-sizing: border-box;

    &::after {
      border: none;
    }
  }

  button.preview-action-btn {
    margin: 0;
    padding: 24rpx 40rpx;
  }

  .avatar-preview-safe {
    height: 0;
  }
</style>
