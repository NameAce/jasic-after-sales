<template>
  <view class="addr-page-root">
    <custom-nav-bar :title="navTitle" surface="sticky" />
    <scroll-view
      class="addr-scroll"
      scroll-y
      lower-threshold="120"
      refresher-enabled
      :refresher-triggered="refresherTriggered"
      @refresherrefresh="onRefresherRefresh"
      @scrolltolower="loadMoreAddresses"
    >
      <view class="page-index page-padding section">
        <view class="action-row">
          <view class="action-card wechat" @click="importFromWeChat">
            <image class="action-icon" :src="wechatChatIcon" mode="aspectFit" />
            <view class="action-texts">
              <text class="action-title">从微信导入</text>
              <text class="action-desc">使用微信收货地址快速填写</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColor.textMuted"></uni-icons>
          </view>
          <view class="action-card manual" @click="goManualAdd">
            <image class="action-icon" :src="addressManualIcon" mode="aspectFit" />
            <view class="action-texts">
              <text class="action-title">手动填写</text>
              <text class="action-desc">自行输入收件人、电话与详细地址</text>
            </view>
            <uni-icons type="right" size="12" :color="themeColor.textMuted"></uni-icons>
          </view>
        </view>

        <!-- 已保存地址 -->
        <view class="section-label">{{ isSelectMode ? '请选择寄件信息' : '已保存地址' }}</view>

        <!-- 暂无收货地址 -->
        <view v-if="addresses.length === 0" class="empty-hint">
          <text class="empty-title">{{ isSelectMode ? '暂无可选地址' : '暂无收货地址' }}</text>
          <text class="empty-desc">可通过上方「从微信导入」或「手动填写」添加</text>
        </view>

        <view v-if="isSelectMode && addresses.length > 0">
          <view
            v-for="item in visibleAddresses"
            :key="item.id"
            class="addr-swipe-item"
            @click="selectAddressForRepair(item)"
          >
            <view class="addr-card">
              <view class="addr-top">
                <view class="addr-top-left">
                  <text class="addr-name">{{ item.name }}</text>
                  <text class="addr-phone">{{ item.phone }}</text>
                </view>
                <view class="addr-top-right">
                  <text v-if="item.isDefault === 1" class="default-badge">默认</text>
                </view>
              </view>
              <text class="addr-line">{{ fullAddress(item) }}</text>
            </view>
          </view>
        </view>

        <!-- 地址卡片：左滑露出删除 -->
        <uni-swipe-action v-if="!isSelectMode && addresses.length > 0">
          <uni-swipe-action-item
            v-for="item in visibleAddresses"
            :key="item.id"
            class="addr-swipe-item"
            :right-options="swipeDeleteOptions"
            @click="onSwipeItemClick($event, item.id)"
          >
            <view class="addr-card" @click="goEdit(item.id)">
              <view class="addr-top">
                <view class="addr-top-left">
                  <text class="addr-name">{{ item.name }}</text>
                  <text class="addr-phone">{{ item.phone }}</text>
                </view>
                <view class="addr-top-right">
                  <text v-if="item.isDefault === 1" class="default-badge">默认</text>
                </view>
              </view>
              <view class="addr-top">
                <text class="addr-line addr-top-left">{{ fullAddress(item) }}</text>
                <view class="addr-edit-btn addr-top-right" @click.stop="goEdit(item.id)">
                  <uni-icons type="compose" size="20" :color="themeColor.textLabel" />
                </view>
              </view>
              <view v-if="item.isDefault !== 1" class="addr-actions" @click.stop>
                <text class="link-primary" @click="setAsDefault(item.id)">设为默认</text>
              </view>
            </view>
          </uni-swipe-action-item>
        </uni-swipe-action>
        <ListNoMore v-if="visibleAddresses.length > 0 && hasLoadedAllAddresses" />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { onLoad, onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import {
    addCustomerAddress,
    customerAddressVOToSavedAddress,
    deleteCustomerAddress,
    listCustomerAddress,
    setDefaultCustomerAddress
  } from '@/api/customerAddress'
  import {
    loadAddresses,
    saveAddresses,
    saveSelectedShippingAddress,
    type SavedAddress
  } from '@/utils/addressStorage'
  import { themeColor } from '@/constants/theme'
  import { addressManualIcon, wechatChatIcon } from '@/svgs'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'

  // 保存地址列表
  const addresses = ref<SavedAddress[]>([])
  const ADDR_PAGE_STEP = 15
  const addrVisibleLimit = ref(ADDR_PAGE_STEP)
  const visibleAddresses = computed(() => addresses.value.slice(0, addrVisibleLimit.value))
  const hasLoadedAllAddresses = computed(
    () => addresses.value.length > 0 && visibleAddresses.value.length >= addresses.value.length
  )
  const isSelectMode = ref(false)
  const navTitle = computed(() => (isSelectMode.value ? '选择寄件信息' : '我的地址'))

  /** 左滑「删除」按钮样式（与 $danger 一致） */
  const swipeDeleteOptions = [
    {
      text: '删除',
      style: {
        backgroundColor: themeColor.danger
      }
    }
  ]

  type SwipeItemClickEvent = {
    position?: string
    content?: { text?: string }
  }

  const onSwipeItemClick = (e: SwipeItemClickEvent, id: string) => {
    if (isSelectMode.value) return
    if (e.position === 'right' && e.content?.text === '删除') {
      removeAddress(id)
    }
  }

  onLoad((options?: Record<string, string>) => {
    isSelectMode.value = options?.mode === 'selectShipping'
  })

  /**
   * 从服务端拉取地址列表并同步本地缓存（供编辑页读取）
   */
  const refresh = async () => {
    try {
      const res = await listCustomerAddress()
      const vos = res.data ?? []
      const sorted = [...vos].sort((a, b) => (b.isDefault ?? 0) - (a.isDefault ?? 0))
      const list = sorted.map(customerAddressVOToSavedAddress)
      saveAddresses(list)
      addresses.value = list
      addrVisibleLimit.value = ADDR_PAGE_STEP
    } catch {
      addresses.value = loadAddresses()
      addrVisibleLimit.value = ADDR_PAGE_STEP
    }
  }

  const { refresherTriggered, onRefresherRefresh } = useScrollRefresher(async () => {
    await refresh()
  })

  const loadMoreAddresses = () => {
    if (visibleAddresses.value.length >= addresses.value.length) return
    addrVisibleLimit.value += ADDR_PAGE_STEP
  }

  /**
   * 页面显示时刷新地址列表
   */
  onShow(() => {
    refresh()
  })

  /**
   * 格式化地址
   * @param a 地址对象
   * @returns 格式化后的地址
   */
  const fullAddress = (a: SavedAddress) => {
    return `${a.province}${a.city}${a.county}${a.detail}`
  }

  /**
   * 调起微信收货地址（主要支持微信小程序；其他端会失败并提示）
   */
  const importFromWeChat = () => {
    uni.chooseAddress({
      success: (res) => {
        uni.showLoading({ title: '保存中', mask: true })
        addCustomerAddress({
          province: res.provinceName,
          city: res.cityName,
          county: res.countyName || '',
          detailAddress: res.detailInfo,
          contactName: res.userName,
          contactMobile: res.telNumber,
          isDefault: 0
        })
          .then((apiRes) => {
            const next: SavedAddress = {
              id: String(apiRes.data),
              name: res.userName,
              phone: res.telNumber,
              province: res.provinceName,
              city: res.cityName,
              county: res.countyName,
              detail: res.detailInfo,
              postalCode: res.postalCode,
              nationalCode: res.nationalCode
            }
            const list = loadAddresses()
            list.unshift(next)
            saveAddresses(list)
            refresh()
            uni.showToast({ title: '已保存', icon: 'none', duration: 1500 })
          })
          .catch(() => {
            /* http 已 toast */
          })
          .finally(() => {
            uni.hideLoading()
          })
      },
      fail: () => {
        uni.showToast({
          title: '请在微信小程序内使用或检查授权',
          icon: 'none',
          duration: 1500
        })
      }
    })
  }

  /**
   * 跳转到手动填写地址页面
   */
  const goManualAdd = () => {
    uni.navigateTo({ url: '/pages/address/edit' })
  }

  /**
   * 跳转到编辑地址页面
   * @param id 地址ID
   */
  const goEdit = (id: string) => {
    uni.navigateTo({ url: `/pages/address/edit?id=${encodeURIComponent(id)}` })
  }

  const selectAddressForRepair = (item: SavedAddress) => {
    saveSelectedShippingAddress(item)
    uni.navigateBack()
  }

  /**
   * 设为默认地址
   * @param id 地址ID
   */
  const setAsDefault = (id: string) => {
    const idNum = Number(id)
    if (!Number.isFinite(idNum)) {
      uni.showToast({ title: '无法设为默认', icon: 'none', duration: 1500 })
      return
    }
    uni.showLoading({ title: '设置中', mask: true })
    setDefaultCustomerAddress(idNum)
      .then(() => refresh())
      .then(() => {
        uni.showToast({ title: '已设为默认', icon: 'none', duration: 1500 })
      })
      .catch(() => {
        /* http 已 toast */
      })
      .finally(() => {
        uni.hideLoading()
      })
  }

  /**
   * 删除地址
   * @param id 地址ID
   */
  const removeAddress = (id: string) => {
    uni.showModal({
      title: '提示',
      content: '确定删除该地址？',
      success: (r) => {
        if (!r.confirm) return
        const idNum = Number(id)
        if (!Number.isFinite(idNum)) {
          const list = loadAddresses().filter((a) => a.id !== id)
          saveAddresses(list)
          addresses.value = list
          return
        }
        uni.showLoading({ title: '删除中', mask: true })
        deleteCustomerAddress(idNum)
          .then(() => {
            const list = loadAddresses().filter((a) => a.id !== id)
            saveAddresses(list)
            addresses.value = list
            return refresh()
          })
          .then(() => {
            uni.showToast({ title: '已删除', icon: 'none', duration: 1500 })
          })
          .catch(() => {
            /* http 已 toast */
          })
          .finally(() => {
            uni.hideLoading()
          })
      }
    })
  }
</script>

<style lang="scss" scoped>
  .addr-page-root {
    display: flex;
    flex-direction: column;
    height: 100vh;
    box-sizing: border-box;
    overflow: hidden;
  }

  .addr-scroll {
    flex: 1;
    height: 0;
    min-height: 0;
  }

  .section {
    @include flex-column-gap;
    flex: 1;
    padding-bottom: calc(#{$space-xl} + env(safe-area-inset-bottom));
    box-sizing: border-box;
    padding-top: $space-lg;
  }

  .action-row {
    display: flex;
    flex-direction: column;
    gap: $space-md;
  }

  .action-card {
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: 28rpx $space-lg;
    border: 2rpx solid $bg-hover;
    box-sizing: border-box;
    @include flex-row;
    gap: $space-md;

    &:active {
      opacity: 0.92;
    }

    .action-icon {
      flex-shrink: 0;
    }

    &.wechat .action-icon,
    &.manual .action-icon {
      width: $font-title;
      height: $font-title;
    }

    .action-texts {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: $space-xs;
    }

    .action-title {
      font-size: $font-lg;
      font-weight: 600;
      color: $text-slate-900;
    }

    .action-desc {
      font-size: $font-sm;
      color: $text-muted;
    }
  }

  .section-label {
    font-size: $font-md;
    font-weight: 600;
    color: $text-label;
  }

  .empty-hint {
    @include empty-state;
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: 64rpx $space-lg;
    border: 2rpx dashed $border-slate;

    .empty-title {
      display: block;
      font-size: 30rpx;
      color: $text-slate-500;
      margin-bottom: 12rpx;
    }

    .empty-desc {
      font-size: 26rpx;
      color: $text-slate-400;
      line-height: 1.5;
    }
  }

  .addr-swipe-item {
    @include flex-column-gap;
  }

  .addr-card {
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: 28rpx $space-lg;
    border: 2rpx solid $bg-hover;
    box-sizing: border-box;

    .addr-top {
      @include flex-row;
      align-items: center;
      gap: $space-md;
      margin-bottom: 12rpx;
    }

    .addr-top-left {
      flex: 1;
      min-width: 0;
      display: flex;
      align-items: center;
      gap: $space-xs;
    }

    .addr-top-right {
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 10rpx;
    }

    .default-badge {
      font-size: 22rpx;
      color: $primary;
      background-color: rgba($primary, 0.12);
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
    }

    .addr-edit-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 8rpx;
      margin: -8rpx;

      &:active {
        opacity: 0.65;
      }
    }

    .addr-name {
      font-size: $font-lg;
      font-weight: 600;
      color: $text-slate-900;
    }

    .addr-phone {
      font-size: $font-md;
      color: $text-slate-500;
    }

    .addr-line {
      font-size: $font-md;
      color: $text-slate-700;
      line-height: 1.5;
    }

    .addr-actions {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: $space-lg;
      margin-top: 20rpx;
      padding-top: 20rpx;
      border-top: 2rpx solid $border-lighter;
    }

    .link-primary {
      font-size: 26rpx;
      color: $primary;
    }

    .link-danger {
      font-size: 26rpx;
      color: $danger;
    }
  }
</style>
