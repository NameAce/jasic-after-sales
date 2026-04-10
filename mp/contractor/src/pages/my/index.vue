<template>
  <view class="my-page">
    <!-- 头部区域 -->
    <view class="header-section">
      <!-- 用户信息 -->
      <view class="profile-info">
        <view class="avatar-container">
          <view class="avatar-wrap">
            <image class="avatar-img" mode="aspectFill" :src="profileData.avatar" />
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

    <!-- 统计数据 -->
    <view class="stats-row">
      <view
        v-for="(stat, idx) in statsData"
        :key="idx"
        class="stat-card"
        @click="onOrderClick(stat.status)"
      >
        <text class="stat-label">{{ stat.label }}</text>
        <text :class="['stat-value', { 'primary-text': stat.highlight }]">{{ stat.value }}</text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-list-wrap">
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
  import { computed, ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { useUserStore } from '@/stores/modules/user'
  import { useAppStore } from '@/stores/modules/app'
  import { Perms } from '@/utils/permissions'
  import { buildUserProfile, DEFAULT_MY_MENU } from '@/models/user'
  import { fetchWorkOrderStatusCount, mapMainStatusToOrderStatus } from '@/api/order'
  import type { OrderStatus } from '@/models/order'
  import { verifiedIcon, postAddIcon, menuInfoIcon, logoutIcon } from '@/svgs'
  // 菜单图标映射
  const MENU_ICON_MAP: Record<string, string> = {
    post_add: postAddIcon,
    info: menuInfoIcon
  }

  // 用户商店
  const userStore = useUserStore()
  const appStore = useAppStore()

  type StatusCounts = Record<OrderStatus, number>
  const statusCounts = ref<StatusCounts>({
    pending: 0,
    processing: 0,
    completed: 0,
    closed: 0
  })

  async function refreshStatusCounts() {
    try {
      const list = await fetchWorkOrderStatusCount({
        viewScope: 'CURRENT'
      })
      const next: StatusCounts = { pending: 0, processing: 0, completed: 0, closed: 0 }
      list.forEach((it) => {
        const s = mapMainStatusToOrderStatus(it.mainStatus)
        const n = Number(it.countNum ?? 0)
        next[s] += Number.isFinite(n) ? n : 0
      })
      statusCounts.value = next
    } catch {
      // http.ts 已做 toast；这里保留上次值/默认值即可
    }
  }

  onShow(() => {
    refreshStatusCounts()
  })

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

  /**
   * 统计数据
   * @returns 统计数据
   */
  const statsData = computed(() => {
    const c = statusCounts.value
    return [
      {
        label: userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '今日派单' : '今日接单',
        value: c.pending,
        status: 'pending',
        highlight: false
      },
      { label: '维修中', value: c.processing, status: 'processing', highlight: true },
      {
        label: '月度完成',
        value: c.completed + c.closed,
        status: 'completed',
        highlight: false
      }
    ]
  })

  /** 功能菜单（入口均展示；具体操作权限在各页按钮上控制） */
  const menuItems = computed(() => DEFAULT_MY_MENU)

  /**
   * 菜单项点击
   * @param item 菜单项
   * @returns void
   */
  const onMenuItemTap = (item: { link?: string }) => {
    if (item.link) uni.navigateTo({ url: item.link })
  }

  /**
   * 跳转到工单库：一级tab为"总部处理/未转单"，二级tab为对应状态
   * @param status 状态
   * @returns void
   */
  const onOrderClick = (status: string) => {
    appStore.setOrderListNavTarget({
      primaryTab: 'untransferred',
      secondaryTab: status as 'pending' | 'processing' | 'completed'
    })
    uni.switchTab({ url: '/pages/order/list' })
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
    background: $my-header-gradient;
    padding: 0 48rpx 106rpx;
    padding-top: calc(var(--status-bar-height) + 100rpx);
    border-bottom-left-radius: 64rpx;
    border-bottom-right-radius: 64rpx;

    .profile-info {
      display: flex;
      align-items: center;
      gap: 40rpx;

      .avatar-container {
        position: relative;

        .avatar-wrap {
          width: 140rpx;
          height: 140rpx;
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
          font-size: 26rpx;
          font-weight: 500;

          .id-label {
            opacity: 0.7;
            font-weight: normal;
          }
        }
      }
    }
  }

  .stats-row {
    margin-top: -80rpx;
    padding: 0 40rpx;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 32rpx;
    position: relative;
    z-index: 10;

    .stat-card {
      background-color: $surface-white;
      padding: 20rpx 0;
      border-radius: 48rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow:
        0 20rpx 50rpx -10rpx rgba(0, 0, 0, 0.05),
        0 16rpx 20rpx -12rpx rgba(0, 0, 0, 0.05);

      .stat-label {
        color: $text-slate-400;
        font-size: 24rpx;
        font-weight: bold;
        text-transform: uppercase;
        letter-spacing: 2rpx;
        margin-bottom: 16rpx;
      }

      .stat-value {
        color: $text-slate-900;
        font-size: 48rpx;
        font-weight: 900;

        &.primary-text {
          color: $primary;
        }
      }
    }
  }

  .menu-list-wrap {
    margin-top: 32rpx;
    padding: 0 40rpx;
    display: flex;
    flex-direction: column;
    gap: 24rpx;

    .menu-list {
      background-color: $surface-white;
      border-radius: 24rpx;
      overflow: hidden;
      box-shadow:
        0 20rpx 50rpx -10rpx rgba(0, 0, 0, 0.05),
        0 16rpx 20rpx -12rpx rgba(0, 0, 0, 0.05);

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
            color: $text-slate-700;
            font-weight: 600;
            font-size: 26rpx;
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
        font-weight: bold;
        font-size: 28rpx;
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
