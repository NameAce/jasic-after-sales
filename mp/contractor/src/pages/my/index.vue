<template>
  <view class="my-page">
    <!-- 头部区域 -->
    <view class="header-section">
      <custom-nav-bar title="我的" surface="transparent" tone="light" :show-back="false" />
      <!-- 用户信息 -->
      <view class="profile-info">
        <view class="avatar-container">
          <view class="avatar-wrap">
            <image
              class="avatar-img"
              mode="aspectFill"
              :src="profileData.avatar || defaultAvatar"
            />
          </view>
          <view class="verified-badge">
            <image class="icon-verified" :src="verifiedIcon" mode="aspectFit" />
          </view>
        </view>
        <!-- 用户信息 -->
        <view class="user-details">
          <view class="name-row">
            <text class="name">{{ profileData.name }}</text>
            <text class="title-tag">{{ profileData.titleTag }}</text>
          </view>
          <view class="id-row">
            <text class="id-value">{{ profileData.phone }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-list-wrap">
      <view class="common-feature-card">
        <text class="common-feature-title">常用功能</text>

        <view class="menu-list">
          <view
            v-for="(item, idx) in menuItems"
            :key="idx"
            class="menu-item"
            hover-class="menu-item-hover"
            @tap="onMenuItemTap(item)"
          >
            <view class="menu-item-left">
              <view class="icon-wrap">
                <image
                  v-if="MENU_ICON_MAP[item.icon]"
                  class="menu-icon-img"
                  :src="MENU_ICON_MAP[item.icon]"
                  mode="aspectFit"
                />
              </view>
              <text class="menu-text">{{ item.label }}</text>
            </view>
            <uni-icons type="right" size="18" color="#cbd5e1"></uni-icons>
          </view>
        </view>
      </view>
      <!-- 退出当前账号 -->
      <view class="logout-wrap">
        <button class="btn-logout" hover-class="btn-logout-hover" @tap="onLogout">
          <image class="logout-icon" :src="logoutIcon" mode="aspectFit" />
          <text>退出登录</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { useUserStore } from '@/stores/modules/user'
  import { Perms } from '@/utils/permissions'
  import { buildUserProfile, DEFAULT_MY_MENU } from '@/models/user'
  import { verifiedIcon, postAddIcon, menuInfoIcon, logoutIcon, locationOnIcon } from '@/svgs'
  import defaultAvatar from '@/static/images/default-avatar.jpg'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  // 菜单图标映射
  const MENU_ICON_MAP: Record<string, string> = {
    post_add: postAddIcon,
    address: locationOnIcon,
    info: menuInfoIcon
  }

  // 用户商店
  const userStore = useUserStore()

  /**
   * 用户信息
   * @returns 用户信息
   */
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

  /** 功能菜单（建维修单需工单新增权限；其余入口展示，具体操作权限在各页控制） */
  const menuItems = computed(() => {
    const withoutRepairCreate = DEFAULT_MY_MENU.filter(
      (item) => item.link !== '/pages/jasicRepair/index'
    )
    if (userStore.hasPermission(Perms.WORKORDER_ADD)) return DEFAULT_MY_MENU
    return withoutRepairCreate
  })

  /**
   * 菜单项点击
   * @param item 菜单项
   * @returns void
   */
  const onMenuItemTap = (item: { link?: string }) => {
    if (item.link) uni.navigateTo({ url: item.link })
  }

  /**
   * 退出当前账号
   * @returns void
   */
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

<style lang="scss" scoped>
  .my-page {
    min-height: 100vh;
    background-color: $surface-slate-50;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
  }

  .header-section {
    background: $primary;
    padding: 0 48rpx 96rpx;
    border-bottom-left-radius: 48rpx;
    border-bottom-right-radius: 48rpx;
    box-shadow: 0 20rpx 30rpx -6rpx rgba(0, 0, 0, 0.1);

    .profile-info {
      display: flex;
      align-items: center;
      gap: 32rpx;

      .avatar-container {
        position: relative;

        .avatar-wrap {
          width: 160rpx;
          height: 160rpx;
          border-radius: 50%;
          border: 6rpx solid rgba(255, 255, 255, 0.3);
          background-color: rgba(255, 255, 255, 0.1);
          overflow: hidden;
          box-shadow: 0 50rpx 100rpx -24rpx rgba(0, 0, 0, 0.25);

          .avatar-img {
            width: 100%;
            height: 100%;
          }
        }

        .verified-badge {
          position: absolute;
          bottom: 4rpx;
          right: 8rpx;
          background-color: $surface-white;
          border-radius: 50%;
          padding: 8rpx;
          box-shadow: 0 8rpx 12rpx -2rpx rgba(0, 0, 0, 0.1);
          display: flex;
          align-items: center;
          justify-content: center;

          .icon-verified {
            width: 36rpx;
            height: 36rpx;
            display: block;
          }
        }
      }

      .user-details {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .name-row {
          display: flex;
          align-items: center;
          gap: 20rpx;

          .name {
            color: $surface-white;
            font-size: 48rpx;
            font-weight: bold;
            line-height: 1.2;
            letter-spacing: -1rpx;
          }

          .title-tag {
            background-color: rgba(255, 255, 255, 0.2);
            backdrop-filter: blur(24rpx);
            padding: 4rpx 16rpx;
            border-radius: 8rpx;
            font-size: 20rpx;
            color: $surface-white;
            font-weight: bold;
            border: 2rpx solid rgba(255, 255, 255, 0.2);
          }
        }

        .id-row {
          display: flex;
          align-items: center;
          gap: 12rpx;
          color: rgba(255, 255, 255, 0.9);
          font-size: 28rpx;
          font-weight: 500;

          .id-label {
            opacity: 0.7;
            font-weight: normal;
          }
        }
      }
    }
  }

  .menu-list-wrap {
    margin-top: -48rpx;
    padding: 0 40rpx;
    display: flex;
    flex-direction: column;
    gap: 24rpx;

    .common-feature-card {
      background-color: $surface-white;
      border-radius: 24rpx;
      padding: 32rpx;
      box-shadow:
        0 20rpx 50rpx -10rpx rgba(0, 0, 0, 0.05),
        0 16rpx 20rpx -12rpx rgba(0, 0, 0, 0.05);

      .common-feature-title {
        display: block;
        color: #0f172a;
        font-size: 34rpx;
        font-weight: 600;
        margin-bottom: 20rpx;
      }

      .common-feature-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14rpx 0;
        transition: background-color 0.2s;

        &-hover {
          background-color: $surface-slate-50;
        }

        .common-feature-left {
          display: flex;
          align-items: center;
          gap: 18rpx;
        }

        .common-feature-icon-wrap {
          width: 40rpx;
          height: 40rpx;
          border-radius: 50%;
          background-color: rgba(251, 146, 60, 0.16);
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .common-feature-icon {
          width: 26rpx;
          height: 26rpx;
          display: block;
        }

        .common-feature-text {
          color: #334155;
          font-size: 30rpx;
          font-weight: 500;
        }
      }
    }

    .menu-list {
      .menu-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 20rpx;
        transition: background-color 0.2s;

        &-hover {
          background-color: $surface-slate-50;
        }

        .menu-item-left {
          display: flex;
          align-items: center;
          gap: 32rpx;

          .icon-wrap {
            width: 70rpx;
            height: 70rpx;
            border-radius: 32rpx;
            background-color: $tag-brand-bg;
            display: flex;
            align-items: center;
            justify-content: center;

            .menu-icon-img {
              width: 48rpx;
              height: 48rpx;
              display: block;
            }
          }

          .menu-text {
            color: #334155;
            font-weight: 500;
            font-size: 30rpx;
          }
        }
      }
    }

    .logout-wrap {
      padding-top: 16rpx;

      .btn-logout {
        width: 100%;
        background-color: $surface-white;
        color: $red-500;
        font-weight: 600;
        font-size: 32rpx;
        padding: 40rpx 0;
        border-radius: 48rpx;
        box-shadow:
          0 20rpx 50rpx -10rpx rgba(0, 0, 0, 0.05),
          0 16rpx 20rpx -12rpx rgba(0, 0, 0, 0.05);
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 16rpx;
        border: none;
        line-height: 1;

        &::after {
          border: none;
        }

        &-hover {
          background-color: $red-50;
        }

        .logout-icon {
          width: 40rpx;
          height: 40rpx;
          flex-shrink: 0;
        }
      }
    }
  }
</style>
