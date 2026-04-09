<template>
  <view class="order-list-layout">
    <custom-nav-bar title="工单列表" surface="sticky" :shadow="false">
      <!-- 搜索框 -->
      <view class="search-wrap">
        <view class="search-box">
          <uni-icons type="search" size="18" color="#94a3b8" class="search-icon"></uni-icons>
          <input
            v-model="searchKeyword"
            class="search-input"
            placeholder="搜索工单号或故障描述"
            confirm-type="search"
          />
        </view>

        <!-- 工单状态标签 -->
        <scroll-view class="tabs" scroll-x :show-scrollbar="false">
          <view class="tabs-inner">
            <view
              v-for="(tab, index) in tabs"
              :key="index"
              :class="['tab-item', currentTab === index ? 'active' : '']"
              @click="currentTab = index"
            >
              <text class="tab-text">{{ tab }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </custom-nav-bar>
    <view :class="['page-container page-index order-list-page', showUploadModal ? 'blur-bg' : '']">
      <!-- 工单列表 -->
      <scroll-view class="main-content" scroll-y>
        <view class="order-list page-padding">
          <view v-if="filteredOrderList.length === 0" class="empty-hint">
            <view class="empty-icon-wrap">
              <image class="empty-list-illus" :src="emptyOrderListIcon" mode="aspectFit" />
            </view>
            <text class="empty-title">暂无相关工单</text>
            <text class="empty-desc">{{
              searchKeyword.trim()
                ? '未找到匹配的工单，请更换关键词重试'
                : currentTab === 0
                  ? '当前没有任何工单记录'
                  : `当前没有"${tabs[currentTab]}"的工单`
            }}</text>
          </view>
          <view
            v-for="order in filteredOrderList"
            :key="order.id"
            class="order-card"
            @click="goToOrderDetail(order.id, order.status)"
          >
            <!-- 头部 -->
            <view class="card-header">
              <view class="order-info">
                <view class="id-wrap">
                  <text class="id-main">{{ order.orderNo }}</text>
                </view>
              </view>
              <view
                :class="`status-badge ${order.status === '待接单' ? 'status-pending' : order.status === '维修中' ? 'status-repairing' : order.status === '已完成' ? 'status-finished' : 'status-closed'}`"
              >
                {{ order.status }}
              </view>
            </view>

            <!-- 工单类型（佳士-橙 / 非佳士-灰）、机器型号（有条码才显示-红） -->
            <view class="tags">
              <text :class="['tag', order.isJasic ? 'tag-jasic' : 'tag-non-jasic']">
                {{ order.isJasic ? '佳士' : '非佳士' }}
              </text>
              <text v-if="hasBarcode(order)" class="tag tag-model">{{ order.modelName }}</text>
            </view>

            <!-- 待接单：维修方式、条码（有条码才显示）；其余状态：网点、电话、方式、价格、条码（有条码才显示） -->
            <view class="details-grid">
              <template v-if="order.status === '待接单'">
                <view class="detail-item">
                  <text class="d-label">维修方式</text>
                  <text class="d-value">{{ order.repairType }}</text>
                </view>
                <view v-if="hasBarcode(order)" class="detail-item">
                  <text class="d-label">条码</text>
                  <text class="d-value">{{ order.qrCode }}</text>
                </view>
              </template>
              <template v-else>
                <view class="detail-item">
                  <text class="d-label">维修网点</text>
                  <text class="d-value">{{ order.centerName }}</text>
                </view>
                <view class="detail-item">
                  <text class="d-label">网点电话</text>
                  <text class="d-value text-primary">{{ order.phone }}</text>
                </view>
                <view class="detail-item">
                  <text class="d-label">维修方式</text>
                  <text class="d-value">{{ order.repairType }}</text>
                </view>
                <view class="detail-item">
                  <text class="d-label">维修价格</text>
                  <text class="d-value font-bold">{{ order.price || '—' }}</text>
                </view>
                <view v-if="hasBarcode(order)" class="detail-item detail-item-full">
                  <text class="d-label">条码</text>
                  <text class="d-value">{{ order.qrCode }}</text>
                </view>
              </template>
            </view>

            <!-- 故障描述 -->
            <!-- <view class="desc-box">
              <text class="desc-text">
                <text class="desc-label">故障描述：</text>{{ order.description }}
              </text>
            </view> -->

            <!-- 底部 -->
            <view class="card-footer">
              <view class="time-wrap">
                <image class="time-icon" :src="scheduleIcon" mode="aspectFit" />
                <text class="time-text">{{ order.time }}</text>
              </view>
              <view class="action-wrap">
                <button
                  v-if="order.status === '待接单'"
                  class="btn-action primary"
                  @click.stop="goToUploadLogistics(order.id)"
                >
                  上传物流单号
                </button>
                <button
                  v-if="order.canEvaluate"
                  class="btn-action primary"
                  @click.stop="goToEvaluate(order.id, order.orderNo)"
                >
                  去评价
                </button>
              </view>
            </view>
          </view>
          <view v-if="filteredOrderList.length > 0" class="list-end-hint">
            <text class="list-end-text">没有更多了</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>

  <!-- 上传快递单号弹窗 -->
  <view v-if="showUploadModal" class="modal-overlay" @click="closeUploadModal">
    <view class="modal-content" @click.stop>
      <view class="modal-handle-wrap">
        <view class="modal-handle"></view>
      </view>
      <text class="modal-title">上传快递单号</text>

      <view class="upload-area" @click="chooseImage">
        <template v-if="uploadImagePath">
          <image class="upload-preview" :src="uploadImagePath" mode="aspectFit" />
          <text class="upload-text">快递单号预览</text>
          <text class="upload-tips">点击可重新选择图片</text>
          <button class="btn-choose" @click.stop="chooseImage">重新选择</button>
        </template>
        <template v-else>
          <view class="icon-wrap">
            <image class="camera-icon" :src="photoCameraIcon" mode="aspectFit" />
          </view>
          <text class="upload-text">点击上传快递单号照片</text>
          <text class="upload-tips">支持JPG、PNG格式，大小不超过5MB</text>
          <button class="btn-choose" @click.stop="chooseImage">选择图片</button>
        </template>
      </view>
      <!-- 确认提交 -->
      <view class="modal-actions">
        <button class="btn-cancel" @click="closeUploadModal">取消</button>
        <button class="btn-confirm" @click="submitUpload">确认提交</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed } from 'vue'
  import { onLoad, onShow } from '@dcloudio/uni-app'
  import {
    getOrderListAPI,
    mapWorkOrderListRecordToItem,
    uploadLogisticsAPI,
    type OrderListItemDTO
  } from '@/api/order'
  import { uploadCustomerFile } from '@/api/file'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { emptyOrderListIcon, photoCameraIcon, scheduleIcon } from '@/svgs'

  // 工单状态标签
  const tabs = ['全部', '待接单', '维修中', '已完成', '已关闭'] as const
  // 当前工单状态标签索引
  const currentTab = ref(0)
  // 工单列表
  const orderList = ref<OrderListItemDTO[]>([])
  // 搜索关键词
  const searchKeyword = ref('')

  /**
   * 是否包含条码
   * @param order - 工单
   * @returns boolean
   */
  function hasBarcode(order: OrderListItemDTO) {
    return Boolean(order.qrCode?.trim())
  }

  /**
   * 过滤工单列表
   * @returns OrderListItemDTO[]
   */
  const filteredOrderList = computed(() => {
    let list = orderList.value
    const label = tabs[currentTab.value]
    if (label !== '全部') {
      list = list.filter((o) => o.status === label)
    }
    const kw = searchKeyword.value.trim().toLowerCase()
    if (!kw) return list
    return list.filter(
      (o) =>
        o.orderNo.toLowerCase().includes(kw) ||
        o.description.toLowerCase().includes(kw) ||
        o.qrCode.toLowerCase().includes(kw) ||
        o.modelName.toLowerCase().includes(kw)
    )
  })

  /**
   * 加载工单列表
   * @returns void
   */
  const loadOrderList = async () => {
    try {
      const res = await getOrderListAPI({ pageNum: 1, pageSize: 500 })
      const records = res.result?.records ?? []
      orderList.value = records.map(mapWorkOrderListRecordToItem)
    } catch {
      orderList.value = []
    }
  }

  /**
   * 页面加载
   * @param options - 选项
   * @returns void
   */
  onLoad((options?: Record<string, string>) => {
    const raw = options?.tab
    if (raw != null && raw !== '') {
      const n = Number.parseInt(raw, 10)
      if (!Number.isNaN(n) && n >= 0 && n < tabs.length) {
        currentTab.value = n
      }
    }
  })

  /**
   * 页面显示
   * @returns void
   */
  onShow(() => {
    loadOrderList()
  })

  /**
   * 跳转到工单详情
   * @param id - 工单ID
   * @param status - 工单状态
   * @returns void
   */
  const goToOrderDetail = (id: string, status: string) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${encodeURIComponent(id)}&status=${encodeURIComponent(status)}`
    })
  }

  // 上传物流单号弹窗状态
  const showUploadModal = ref(false)
  const currentUploadId = ref<string | null>(null)
  /** 选中的快递单照片本地临时路径，用于预览与后续上传 */
  const uploadImagePath = ref('')

  /**
   * 打开上传物流单号弹窗
   * @param id - 工单ID
   * @returns void
   */
  const goToUploadLogistics = (id: string) => {
    currentUploadId.value = id
    uploadImagePath.value = ''
    showUploadModal.value = true
  }

  /**
   * 关闭上传物流单号弹窗
   * @returns void
   */
  const closeUploadModal = () => {
    showUploadModal.value = false
    currentUploadId.value = null
    uploadImagePath.value = ''
  }

  /**
   * 选择图片
   * @returns void
   */
  const chooseImage = () => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const path = res.tempFilePaths[0]
        if (path) {
          uploadImagePath.value = path
        }
      }
    })
  }

  /**
   * 提交上传物流单号
   * @returns void
   */
  const submitUpload = async () => {
    if (!uploadImagePath.value) {
      uni.showToast({ title: '请先选择快递单照片', icon: 'none', duration: 1500 })
      return
    }
    try {
      uni.showLoading({ title: '上传中...' })
      const uploaded = await uploadCustomerFile(uploadImagePath.value)
      const fileId = Number(uploaded.fileId)
      const workOrderId = Number(currentUploadId.value)
      if (!Number.isFinite(fileId) || fileId <= 0) {
        uni.hideLoading()
        uni.showToast({ title: '上传失败：未获取到凭证文件ID', icon: 'none', duration: 1800 })
        return
      }
      if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
        uni.hideLoading()
        uni.showToast({ title: '工单ID无效，请重试', icon: 'none', duration: 1800 })
        return
      }
      await uploadLogisticsAPI({
        senderVoucherFileIds: [fileId],
        workOrderId
      })
      uni.hideLoading()
      uni.showToast({ title: '上传成功', icon: 'success' })
      closeUploadModal()
      loadOrderList()
    } catch {
      uni.hideLoading()
      /* 失败提示由 http 层使用接口 msg */
    }
  }

  /**
   * 跳转到评价
   * @param id - 工单ID
   * @returns void
   */
  const goToEvaluate = (id: string, orderNo?: string) => {
    let url = `/pages/order/evaluate?id=${encodeURIComponent(id)}`
    if (orderNo) url += `&orderNo=${encodeURIComponent(orderNo)}`
    uni.navigateTo({ url })
  }
</script>

<style lang="scss" scoped>
  .order-list-layout {
    height: 100vh;
    min-height: 100vh;
    max-height: 100vh;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  /* 整页限高，仅列表区域滚动，导航+搜索+标签始终留在顶部 */
  .order-list-page.page-container.page-index {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    box-sizing: border-box;
  }

  .search-wrap {
    background-color: $bg-card;
    @include flex-column-gap;

    .search-box {
      @include flex-row;
      background-color: $bg-hover;
      height: 88rpx;
      border-radius: $radius-lg;
      padding: 0 $space-lg;

      .search-icon {
        margin-right: $space-sm;
      }

      .search-input {
        flex: 1;
        font-size: $font-md;
        color: $text-dark;
        background: transparent;
      }
    }
  }

  .tabs {
    width: 100%;
    border-bottom: 2rpx solid $border-light;
    white-space: nowrap;

    .tabs-inner {
      display: inline-flex;
      padding: 0 $space-lg;
      gap: $space-xl;
    }

    .tab-item {
      @include flex-column-center;
      padding: $space-sm 0 $space-md;
      flex-shrink: 0;
      border-bottom: 4rpx solid transparent;

      .tab-text {
        font-size: $font-md;
        font-weight: 500;
        color: $text-label;
        transition: color 0.2s;
      }

      &.active {
        border-bottom-color: $primary;

        .tab-text {
          font-weight: bold;
          color: $primary;
        }
      }
    }
  }

  .main-content {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    box-sizing: border-box;
  }

  .order-list {
    @include flex-column-gap;
    padding-top: $space-lg;

    .empty-hint {
      @include empty-state;
      padding: 160rpx 64rpx 120rpx;

      .empty-icon-wrap {
        width: 520rpx;
        max-width: 92%;
        height: 200rpx;
        border-radius: 24rpx;
        background-color: $bg-hover;
        @include flex-center;
        margin-bottom: $space-lg;
        padding: 20rpx 24rpx;
        box-sizing: border-box;

        .empty-list-illus {
          width: 100%;
          height: 160rpx;
        }
      }

      .empty-title {
        font-size: $font-lg;
        font-weight: 600;
        color: $text-label;
        margin-bottom: 12rpx;
      }

      .empty-desc {
        font-size: 26rpx;
        color: $text-muted;
        line-height: 1.5;
      }
    }

    .list-end-hint {
      @include flex-center;
      padding-bottom: $space-xl;

      .list-end-text {
        font-size: $font-sm;
        color: $text-muted;
      }
    }
  }

  .order-card {
    @include white-card;
    @include flex-column;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);

    .card-header {
      @include flex-between;
      align-items: flex-start;
      margin-bottom: $space-md;

      .order-info {
        @include flex-column;
        gap: $space-xs;

        .id-main {
          font-size: $font-lg;
          font-weight: bold;
          color: $text-dark;
          letter-spacing: -0.5rpx;
        }
      }
    }

    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: $space-sm;
      margin-bottom: $space-lg;
    }

    .details-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: $space-md;
      margin-bottom: $space-lg;

      .detail-item {
        @include flex-column;
        gap: 4rpx;

        &.detail-item-full {
          grid-column: 1 / -1;
        }

        .d-label {
          font-size: 22rpx;
          color: $text-muted;
        }

        .d-value {
          font-size: 26rpx;
          color: $text-body;
          font-weight: 500;

          &.text-primary {
            color: $primary;
          }
          &.font-bold {
            font-weight: bold;
            color: $text-dark;
          }
        }
      }
    }

    .desc-box {
      margin-bottom: $space-lg;

      .desc-text {
        font-size: $font-sm;
        color: $text-label;
        line-height: 1.6;

        .desc-label {
          font-weight: bold;
          color: $text-desc;
        }
      }
    }

    .card-footer {
      @include flex-between;
      padding-top: $space-md;
      border-top: 2rpx solid $border-lighter;

      .time-wrap {
        @include flex-row;
        gap: $space-xs;
        color: $text-muted;

        .time-icon {
          width: 26rpx;
          height: 26rpx;
          flex-shrink: 0;
          display: block;
        }
        .time-text {
          font-size: 22rpx;
        }
      }

      .action-wrap {
        display: flex;
        justify-content: flex-end;
        gap: $space-md;
        flex: 1;

        .btn-action {
          margin: 0;
          padding: 0 $space-lg;
          height: 56rpx;
          line-height: 56rpx;
          border-radius: $radius-round;
          font-size: $font-sm;
          font-weight: bold;
          @include btn-reset;

          &.primary {
            @include btn-primary-solid;
          }
        }
      }
    }
  }

  .blur-bg {
    filter: blur(8px);
    pointer-events: none;
    transition: filter 0.3s;
  }

  // 底部弹出式上传弹窗
  .modal-overlay {
    @include modal-overlay;
    z-index: 10000;
    justify-content: center;
    align-items: flex-end;
    animation: fadeIn 0.3s ease-out;

    @media (min-height: 700px) {
      align-items: center;
    }
  }

  .modal-content {
    position: relative;
    z-index: 10001;
    background-color: $bg-card;
    width: 100%;
    max-width: 800rpx;
    border-radius: 64rpx 64rpx 0 0;
    padding: 0 $space-xl 64rpx;
    @include flex-column;
    animation: slideUp 0.3s ease-out;

    @media (min-height: 700px) {
      border-radius: 64rpx;
      margin: $space-lg;
      padding-bottom: $space-xl;
    }

    .modal-handle-wrap {
      @include flex-center;
      padding: $space-md 0 $space-xs;

      .modal-handle {
        width: 96rpx;
        height: 12rpx;
        background-color: #e5e7eb;
        border-radius: $radius-round;
      }
    }

    .modal-title {
      font-size: $font-xxl;
      font-weight: bold;
      text-align: center;
      color: $text-dark;
      margin-bottom: $space-xl;
      margin-top: $space-sm;
    }

    .upload-area {
      background-color: rgba($primary, 0.05);
      border: 4rpx dashed rgba($primary, 0.5);
      border-radius: $radius-lg;
      padding: 64rpx $space-lg;
      @include flex-column-center;
      margin-bottom: $space-xl;

      .icon-wrap {
        width: 128rpx;
        height: 128rpx;
        background-color: rgba($primary, 0.1);
        border-radius: 50%;
        @include flex-center;
        margin-bottom: $space-lg;

        .camera-icon {
          width: 72rpx;
          height: 72rpx;
        }
      }

      .upload-text {
        font-size: $font-lg;
        font-weight: 500;
        color: $text-dark;
        margin-bottom: $space-xs;
      }

      .upload-tips {
        font-size: $font-sm;
        color: $text-muted;
        margin-bottom: $space-lg;
      }

      .upload-preview {
        width: 100%;
        max-height: 360rpx;
        border-radius: $radius-md;
        margin-bottom: $space-md;
        background-color: $bg-hover;
      }

      .btn-choose {
        background-color: rgba($primary, 0.15);
        color: $primary;
        font-size: $font-md;
        font-weight: bold;
        padding: 0 $space-xl;
        height: 72rpx;
        line-height: 72rpx;
        border-radius: $radius-lg;
        margin: 0;
        @include btn-reset;

        &:active {
          opacity: 0.8;
        }
      }
    }

    .modal-actions {
      @include modal-actions;

      button {
        height: 96rpx;
        line-height: 96rpx;
        border-radius: $radius-xl;
        font-size: $font-lg;

        &:active {
          transform: scale(0.98);
        }
      }

      .btn-cancel {
        @include modal-btn-cancel;
      }
      .btn-confirm {
        @include modal-btn-confirm;
      }
    }
  }
</style>
