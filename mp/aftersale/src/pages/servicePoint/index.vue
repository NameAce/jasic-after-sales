<template>
  <view class="service-point-page-root">
    <custom-nav-bar title="选择附近网点" surface="sticky" />
    <scroll-view
      class="service-point-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="refresherTriggered"
      @refresherrefresh="onRefresherRefresh"
    >
      <view class="page-padding service-point-body">
        <view
          v-if="loading && servicePointList.length === 0"
          class="list-end-hint list-loading-top"
        >
          <text class="list-end-text">加载中...</text>
        </view>
        <ListEmpty
          v-else-if="!loading && servicePointList.length === 0"
          title="暂无附近网点"
          desc="请确认已开启定位权限，或下拉刷新重试"
        />
        <view
          v-for="(item, index) in servicePointList"
          :key="`${item.id}-${index}`"
          class="service-point-item"
          @click="selectPoint(item)"
        >
          <view class="item-left">
            <uni-icons type="location-filled" size="20" :color="themeColor.info"></uni-icons>
            <view v-if="item.hasRepairHistory">服务过</view>
          </view>
          <view class="item-content">
            <view>{{ item.companyName }}</view>
            <text>{{ item.address }}</text>
          </view>
          <view class="item-right">
            <view class="action-icons">
              <view @click.stop="navigateToPoint(item)">
                <uni-icons type="navigate-filled" size="18" :color="themeColor.info"></uni-icons>
                <view>{{ item.distance }}</view>
              </view>
              <view @click.stop="callPhone(item)">
                <uni-icons type="phone-filled" size="18" :color="themeColor.info"></uni-icons>
                <view>电话</view>
              </view>
            </view>
          </view>
        </view>
        <ListNoMore v-if="servicePointList.length > 0" />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { themeColor } from '@/constants/theme'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'
  import {
    listNearbyServiceCompanyOptions,
    mapNearbyToServicePoint,
    type ServicePointDTO
  } from '@/api/servicePoint'

  /** 网点列表 */
  const servicePointList = ref<ServicePointDTO[]>([])
  /** 首屏/刷新加载中 */
  const loading = ref(true)
  /** 最近一次成功定位，下拉刷新失败时用于兜底重载 */
  const lastCoords = ref<{ latitude: number; longitude: number } | null>(null)

  const NEARBY_LIMIT = 20

  /**
   * 加载附近网点（需定位 latitude / longitude，及 limit）
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const loadServicePoints = async (latitude: number, longitude: number) => {
    loading.value = true
    lastCoords.value = { latitude, longitude }
    try {
      const res = await listNearbyServiceCompanyOptions({
        latitude,
        longitude,
        limit: NEARBY_LIMIT
      })
      servicePointList.value = res.data.map(mapNearbyToServicePoint)
    } catch {
      servicePointList.value = []
      /* 失败提示由 http 层使用接口 msg */
    }
    loading.value = false
  }

  /**
   * 根据 getLocation 失败信息区分：权限拒绝、系统定位关闭、超时等
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const getLocationFailHint = (errMsg: string) => {
    const m = errMsg.toLowerCase()
    if (
      m.includes('system') ||
      m.includes('locationswitch') ||
      m.includes('location_switch') ||
      m.includes('nocell&wifi')
    ) {
      return '手机系统定位服务可能未开启，请到系统设置中打开「定位/GPS」后再试'
    }
    if (m.includes('timeout')) {
      return '定位超时，请到信号较好处重试，或检查是否开启定位服务'
    }
    return '查询附近网点需要获取你的位置：请在小程序授权页开启位置权限；若已开启仍失败，请检查手机系统定位是否打开'
  }

  /**
   * 获取定位并加载附近网点；失败时引导去设置，返回后延迟补重试
   * @param options.silent 为 true 时不弹授权引导（用于下拉刷新静默失败）
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const requestLocationAndLoad = (options?: { silent?: boolean }) => {
    const silent = options?.silent === true
    if (!silent) {
      loading.value = true
    }
    return new Promise<void>((resolve) => {
      uni.getLocation({
        type: 'gcj02',
        success: async (res) => {
          await loadServicePoints(res.latitude, res.longitude)
          resolve()
        },
        fail: (err) => {
          console.log('requestLocationAndLoad fail', err)
          if (!silent) {
            loading.value = false
            servicePointList.value = []
          }
          const errMsg =
            err && typeof err === 'object' && 'errMsg' in err
              ? String((err as { errMsg?: string }).errMsg || '')
              : ''
          if (silent && lastCoords.value) {
            loadServicePoints(lastCoords.value.latitude, lastCoords.value.longitude).then(() =>
              resolve()
            )
            return
          }
          if (silent) {
            loading.value = false
            resolve()
            return
          }
          uni.showModal({
            title: '无法获取位置',
            content: getLocationFailHint(errMsg),
            confirmText: '去设置',
            cancelText: '取消',
            success: (r) => {
              if (!r.confirm || typeof uni.openSetting !== 'function') {
                resolve()
                return
              }
              uni.openSetting({
                complete: () => {
                  setTimeout(() => {
                    requestLocationAndLoad().then(resolve)
                  }, 300)
                }
              })
            }
          })
        }
      })
    })
  }

  const { refresherTriggered, onRefresherRefresh } = useScrollRefresher(async () => {
    await requestLocationAndLoad({ silent: true })
  })

  onLoad(() => {
    requestLocationAndLoad()
  })

  /**
   * 选择网点
   * @param item - 网点
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const selectPoint = (item: ServicePointDTO) => {
    uni.setStorageSync('selectedServicePoint', item)
    uni.navigateBack()
  }

  /**
   * 导航到网点
   * @param item - 网点
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const navigateToPoint = (item: ServicePointDTO) => {
    uni.openLocation({
      latitude: item.latitude,
      longitude: item.longitude,
      name: item.companyName,
      address: item.address,
      fail: () => {
        uni.showToast({ title: '打开地图失败', icon: 'none', duration: 1500 })
      }
    })
  }

  /**
   * 拨打电话
   * @param item - 网点
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const callPhone = (item: ServicePointDTO) => {
    uni.makePhoneCall({
      phoneNumber: item.phone,
      fail: () => {
        uni.showToast({ title: '拨打电话失败', icon: 'none', duration: 1500 })
      }
    })
  }
</script>

<style lang="scss" scoped>
  /* 顶栏固定，仅列表区域滚动（与地址列表、工单列表布局一致） */
  .service-point-page-root {
    display: flex;
    flex-direction: column;
    height: 100vh;
    box-sizing: border-box;
    overflow: hidden;
  }

  .service-point-scroll {
    flex: 1;
    height: 0;
    min-height: 0;
  }

  .service-point-body {
    @include flex-column-gap;
    padding-top: $space-lg;
    padding-bottom: calc(#{$space-xl} + env(safe-area-inset-bottom));
    box-sizing: border-box;
  }

  .list-end-hint {
    @include flex-center;
    padding-bottom: $space-xl;

    .list-end-text {
      font-size: $font-sm;
      color: $text-muted;
    }

    &.list-loading-top {
      padding-top: $space-xl;
      padding-bottom: $space-md;
    }
  }

  .service-point-item {
    @include white-card($radius-md, $space-lg $space-md);
    @include flex-between;
    gap: $space-md;

    .item-left {
      @include flex-column-center;

      view {
        @include serviced-tag-style($primary);
      }
    }

    .item-content {
      flex: 1;
      @include flex-column;
      gap: $space-xs;

      view {
        font-size: $font-md;
        font-weight: bold;
        @include ellipsis(1);
      }

      text {
        font-size: $font-sm;
        color: $text-secondary;
        @include ellipsis(1);
      }
    }

    .item-right {
      margin-left: $space-md;
      min-width: 120rpx;

      .action-icons {
        @include flex-center;
        gap: $space-sm;

        view {
          @include flex-column-center;

          view {
            font-size: $font-sm;
            color: $text-secondary;
          }
        }
      }
    }
  }
</style>
