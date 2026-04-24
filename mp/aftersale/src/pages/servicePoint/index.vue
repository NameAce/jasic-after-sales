<template>
  <view class="page-index">
    <custom-nav-bar title="选择附近网点" surface="sticky" />
    <view class="page-padding service-point-body">
    <!-- 网点列表 -->
    <view
      v-for="(item, index) in servicePointList"
      :key="index"
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
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { themeColor } from '@/constants/theme'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import {
    listNearbyServiceCompanyOptions,
    mapNearbyToServicePoint,
    type ServicePointDTO
  } from '@/api/servicePoint'

  // 网点列表
  const servicePointList = ref<ServicePointDTO[]>([])
  // 加载状态
  const loading = ref(true)

  const NEARBY_LIMIT = 20

  /**
   * 加载附近网点（需定位 latitude / longitude，及 limit）
   * @returns void
   */
  const loadServicePoints = async (latitude: number, longitude: number) => {
    loading.value = true
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
   * 获取定位并加载附近网点；失败时引导去设置，返回后延迟/ onShow 补重试
   */
  const requestLocationAndLoad = () => {
    loading.value = true
    uni.getLocation({
      type: 'gcj02',
      success: (res) => {
        loadServicePoints(res.latitude, res.longitude)
      },
      fail: (err) => {
        console.log('requestLocationAndLoad fail', err)
        loading.value = false
        servicePointList.value = []
        const errMsg =
          err && typeof err === 'object' && 'errMsg' in err
            ? String((err as { errMsg?: string }).errMsg || '')
            : ''
        uni.showModal({
          title: '无法获取位置',
          content: getLocationFailHint(errMsg),
          confirmText: '去设置',
          cancelText: '取消',
          success: (r) => {
            if (!r.confirm || typeof uni.openSetting !== 'function') {
              return
            }
            uni.openSetting({
              complete: () => {
                setTimeout(() => {
                  requestLocationAndLoad()
                }, 300)
              }
            })
          }
        })
      }
    })
  }

  onLoad(() => {
    requestLocationAndLoad()
  })

  /**
   * 选择网点
   * @param item - 网点
   * @returns void
   */
  const selectPoint = (item: ServicePointDTO) => {
    uni.setStorageSync('selectedServicePoint', item)
    uni.navigateBack()
  }

  /**
   * 导航到网点
   * @param item - 网点
   * @returns void
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
  .service-point-body {
    @include flex-column-gap;
    flex: 1;
    min-height: 0;
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
