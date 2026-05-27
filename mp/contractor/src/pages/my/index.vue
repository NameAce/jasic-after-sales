<template>
  <view class="page-my page-index">
    <view class="header-section">
      <custom-nav-bar title="我的" surface="transparent" tone="light" :show-back="false" />
      <view class="user-info">
        <view
          class="avatar-container"
          hover-class="avatar-container-hover"
          @click="openAvatarPreview"
        >
          <view class="avatar-wrap">
            <image class="avatar-img" :src="avatarDisplayUrl" mode="aspectFill" />
          </view>
          <view class="verified-badge">
            <image class="icon-verified" :src="verifiedIcon" mode="aspectFit" />
          </view>
        </view>
        <view class="user-details" @click="openProfileEdit">
          <text class="username">{{ profileData.name }}</text>
          <view class="user-id-wrap">
            <text class="user-id">{{ profileData.phone }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content-area page-padding page-padding-no-safe">
      <view class="card">
        <view class="card-header border-b">
          <text class="card-title">常用功能</text>
        </view>
        <view class="list-group">
          <view
            v-if="canCreateRepairOrder"
            class="list-item"
            @click="goToPage('/pages/jasicRepair/index')"
          >
            <view class="list-item-left">
              <image class="post-add-icon" :src="postAddIcon" mode="aspectFit" />
              <text class="list-item-text">建维修订单</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
          <view class="list-item" @click="goToPage('/pages/address/index')">
            <view class="list-item-left">
              <image class="location-on-icon" :src="locationOnIcon" mode="aspectFit" />
              <text class="list-item-text">地址管理</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-header border-b">
          <text class="card-title">系统设置</text>
        </view>
        <view class="list-group">
          <view class="list-item" @click="goToPage('/pages/feedback/index')">
            <view class="list-item-left">
              <image class="headset-mic-icon" :src="headsetMicIcon" mode="aspectFit" />
              <text class="list-item-text">投诉与建议</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
          <view class="list-item" @click="goToPage('/pages/about/index')">
            <view class="list-item-left">
              <image class="info-circle-icon" :src="infoCircleIcon" mode="aspectFit" />
              <text class="list-item-text">关于我们</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColors.textMuted"></uni-icons>
          </view>
        </view>
      </view>

      <view class="logout-btn" @click="onLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <view v-if="avatarPreviewVisible" class="avatar-preview-mask" @click="closeAvatarPreview">
      <view class="avatar-preview-center" @click.stop>
        <image class="avatar-preview-image" :src="avatarDisplayUrl" mode="aspectFit" />
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
  import { useUserStore } from '@/stores/modules/user'
  import { Perms } from '@/utils/permissions'
  import { buildUserProfile } from '@/models/user'
  import { verifiedIcon, postAddIcon, infoCircleIcon, locationOnIcon, headsetMicIcon } from '@/svgs'
  import { themeColors } from '@/theme/colors'
  import { ASSET_IMAGES } from '@/constants/assets'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { getUserInfo, updateMpProfile } from '@/api/auth'
  import { uploadSystemFile } from '@/api/file'
  import { resolveAvatarDisplayUrl, resolveAvatarUrlForSubmit } from '@/utils/profileAvatar'
  import { showApiToast } from '@/utils/uiFeedback'

  const PROFILE_EDIT_URL = '/pages/profile/edit'

  const defaultAvatar = ASSET_IMAGES.defaultAvatar

  const userStore = useUserStore()

  /** 是否展示「建维修订单」（与原先 DEFAULT_MY_MENU 权限逻辑一致） */
  const canCreateRepairOrder = computed(() => userStore.hasPermission(Perms.WORKORDER_ADD))

  const avatarPreviewVisible = ref(false)

  const avatarDisplayUrl = computed(() =>
    resolveAvatarDisplayUrl(userStore.userInfo?.avatar, defaultAvatar)
  )

  const profileData = computed(() => {
    const u = userStore.userInfo
    if (u) return buildUserProfile(u)
    return {
      name: '',
      titleTag: '',
      idLabel: '员工ID:',
      idValue: '',
      phone: '',
      avatar: ''
    }
  })

  onShow(() => {
    void refreshUserInfo()
  })

  onHide(() => {
    if (!avatarPreviewVisible.value) return
    avatarPreviewVisible.value = false
    syncTabBarForAvatarPreview(false)
  })

  const refreshUserInfo = async () => {
    if (!userStore.isLoggedIn) return
    try {
      const res = await getUserInfo()
      if (res.data) userStore.setUserInfo(res.data)
    } catch {
      /* 失败时沿用本地缓存 */
    }
  }

  const syncTabBarForAvatarPreview = (visible: boolean) => {
    if (visible) {
      uni.hideTabBar({ animation: false })
      return
    }
    uni.showTabBar({ animation: false })
  }

  const openAvatarPreview = () => {
    avatarPreviewVisible.value = true
    syncTabBarForAvatarPreview(true)
  }

  const closeAvatarPreview = () => {
    if (!avatarPreviewVisible.value) return
    avatarPreviewVisible.value = false
    syncTabBarForAvatarPreview(false)
  }

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

  const saveAvatarOnly = async (displayPath: string) => {
    uni.showLoading({ title: '保存中', mask: true })
    try {
      const avatar = await resolveAvatarUrlForSubmit(
        displayPath,
        userStore.userInfo?.avatar,
        uploadSystemFile,
        defaultAvatar
      )
      if (avatar === undefined) return
      const res = await updateMpProfile({ avatar })
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
      }
    })
  }

  const goToPage = (url: string) => {
    uni.navigateTo({ url })
  }

  const onLogout = () => {
    uni.showModal({
      title: '提示',
      content: '确定退出当前账号吗？',
      success: (res) => {
        if (res.confirm) {
          userStore.logout()
        }
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

      .verified-badge {
        position: absolute;
        bottom: 0;
        right: 0;
        background-color: $bg-card;
        border-radius: 50%;
        padding: $space-xs;
        border: 4rpx solid $primary;
        @include flex-center;

        .icon-verified {
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
        color: $primary-contrast;
      }

      .user-id-wrap {
        @include flex-row;
        margin-top: $space-xs;
        opacity: 0.9;

        .user-id {
          font-size: $font-md;
          color: $primary-contrast;
        }
      }
    }
  }

  .content-area.page-padding {
    margin-top: -48rpx;
    /* 使用 flex-col，避免 flex-column 的 justify-content:center 把整块内容垂直居中 */
    @include flex-col;
    gap: $space-lg;
  }

  .page-my .card {
    @include white-card($radius-lg, $space-lg);
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    align-items: stretch;
    gap: 0;
    border: none;
    box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.05);
    overflow: hidden;

    .card-header {
      padding-bottom: $space-sm;

      &.border-b {
        border-bottom: 2rpx solid $border-lighter;
      }

      .card-title {
        color: $text-dark;
        font-weight: bold;
        font-size: $font-lg;
      }
    }

    .list-group {
      @include flex-col;

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

  .page-my .post-add-icon,
  .page-my .location-on-icon,
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
