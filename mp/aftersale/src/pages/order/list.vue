<template>
  <view class="order-list-layout">
    <custom-nav-bar title="工单列表" surface="sticky" :shadow="false">
      <view class="search-wrap">
        <view class="search-box">
          <uni-icons
            type="search"
            size="18"
            :color="themeColors.textMuted"
            class="search-icon"
          ></uni-icons>
          <input
            v-model="searchKeyword"
            class="search-input"
            placeholder="请输入工单号"
            placeholder-class="placeholder-text"
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
              @click="selectTab(index)"
            >
              <text class="tab-text">{{ tab }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </custom-nav-bar>
    <view :class="['page-container page-index order-list-page', showUploadModal ? 'blur-bg' : '']">
      <!-- 工单列表 -->
      <scroll-view
        class="main-content"
        scroll-y
        :lower-threshold="100"
        refresher-enabled
        :refresher-triggered="orderListRefresherTriggered"
        @refresherrefresh="onOrderListRefresherRefresh"
        @scrolltolower="loadMore"
      >
        <view class="order-list page-padding">
          <view v-if="loading && orderList.length === 0" class="list-end-hint list-loading-top">
            <text class="list-end-text">加载中...</text>
          </view>
          <view v-else-if="!loading && orderList.length === 0" class="empty-hint">
            <view class="empty-icon-wrap">
              <image class="empty-list-illus" :src="emptyOrderListIcon" mode="aspectFit" />
            </view>
            <text class="empty-title">暂无相关工单</text>
            <text class="empty-desc">{{
              currentTab === 0 ? '当前没有任何工单记录' : `当前没有"${tabs[currentTab]}"的工单`
            }}</text>
          </view>
          <view
            v-else-if="!loading && orderList.length > 0 && displayOrderList.length === 0"
            class="empty-hint"
          >
            <view class="empty-icon-wrap">
              <image class="empty-list-illus" :src="emptyOrderListIcon" mode="aspectFit" />
            </view>
            <text class="empty-title">暂无匹配结果</text>
            <text class="empty-desc"
              >当前关键词在已加载的工单中没有匹配项，可清空关键词、下拉刷新或上拉加载更多后再试</text
            >
          </view>
          <view
            v-for="order in displayOrderList"
            :key="`${order.id}-${order.orderNo}`"
            class="order-card"
            @click="goToOrderDetail(order.id, order.status)"
          >
            <!-- 头部 -->
            <view class="card-header">
              <view class="order-info">
                <view v-if="hasDisplayText(order.orderNo)" class="id-wrap">
                  <text class="id-main">{{ order.orderNo }}</text>
                </view>
              </view>
              <view
                :class="`status-badge ${order.status === '待接单' ? 'status-pending' : order.status === '维修中' ? 'status-repairing' : order.status === '已完成' ? 'status-finished' : 'status-closed'}`"
              >
                {{ orderStatusText(order) }}
              </view>
            </view>

            <!-- 工单类型、型号标签（结构与 contractor OrderCardList.tags-wrap 一致） -->
            <view class="tags-wrap">
              <view :class="['tag', order.isJasic ? 'tag-brand' : 'tag-other-brand']">
                <text class="text">{{ orderBrandTypeText(order) }}</text>
              </view>
              <view v-if="showModelTag(order)" class="tag tag-model">
                <text class="text">{{ order.modelName }}</text>
              </view>
            </view>

            <!-- 待接单：维修方式、条码（有条码才显示）；其余状态：网点、电话、方式、价格、条码（有条码才显示） -->
            <view class="details-grid">
              <template v-if="order.status === '待接单'">
                <view v-if="hasDisplayText(order.repairType)" class="detail-item">
                  <text class="d-label">维修方式</text>
                  <text class="d-value">{{ order.repairType }}</text>
                </view>
                <view v-if="hasBarcode(order)" class="detail-item">
                  <text class="d-label">条码</text>
                  <text class="d-value">{{ order.qrCode }}</text>
                </view>
              </template>
              <template v-else>
                <view v-if="hasDisplayText(order.centerName)" class="detail-item">
                  <text class="d-label">维修网点</text>
                  <text class="d-value">{{ order.centerName }}</text>
                </view>
                <view v-if="hasDisplayText(order.phone)" class="detail-item">
                  <text class="d-label">网点电话</text>
                  <text class="d-value text-primary">{{ order.phone }}</text>
                </view>
                <view v-if="hasDisplayText(order.repairType)" class="detail-item">
                  <text class="d-label">维修方式</text>
                  <text class="d-value">{{ order.repairType }}</text>
                </view>
                <view v-if="hasDisplayText(order.price)" class="detail-item">
                  <text class="d-label">维修价格</text>
                  <text class="d-value font-bold">{{ order.price }}</text>
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
              <view v-if="hasDisplayText(order.time)" class="time-wrap">
                <image class="time-icon" :src="scheduleIcon" mode="aspectFit" />
                <text class="time-text">{{ order.time }}</text>
              </view>
              <view class="action-wrap">
                <button
                  v-if="order.canUploadSendExpress"
                  class="btn-action primary"
                  @click.stop="goToUploadLogistics(order.id)"
                >
                  上传寄件单号
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
          <view v-if="orderList.length > 0" class="list-end-hint">
            <text class="list-end-text">{{
              loadingMore ? '加载中...' : hasMore ? '下拉刷新或上拉加载更多' : '没有更多了'
            }}</text>
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
    listCustomerWorkOrder,
    mapWorkOrderListRecordToItem,
    updateCustomerWorkOrderSenderVoucher,
    type OrderListItem
  } from '@/api/workOrder'
  import { uploadCustomerFile } from '@/api/file'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { themeColors } from '@/constants/theme'
  import { emptyOrderListIcon, photoCameraIcon, scheduleIcon } from '@/svgs'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'

  // 工单状态标签
  const tabs = ['全部', '待接单', '维修中', '已完成', '已关闭'] as const
  const tabStatusMap: Record<number, string | undefined> = {
    0: undefined,
    1: 'WAIT_ACCEPT',
    2: 'IN_PROGRESS',
    3: 'COMPLETED',
    4: 'CLOSED'
  }
  // 当前工单状态标签索引
  const currentTab = ref(0)
  // 工单列表
  const orderList = ref<OrderListItem[]>([])
  // 分页状态
  const pageNum = ref(1)
  const pageSize = 10
  const total = ref(0)
  const hasMore = ref(true)
  const loading = ref(false)
  const loadingMore = ref(false)
  /** 模糊筛选关键词（仅过滤当前已加载到本地的列表，不额外请求接口） */
  const searchKeyword = ref('')

  /**
   * 是否包含条码
   * @param order - 工单
   * @returns boolean
   */
  function hasBarcode(order: OrderListItem) {
    return Boolean(order.qrCode?.trim())
  }

  /** 接口有返回非空文案时才展示对应行 */
  function hasDisplayText(v?: string | null) {
    return Boolean(String(v ?? '').trim())
  }

  /** 非佳士：有型号即展示；佳士：仍仅在有条码时展示型号 */
  function showModelTag(order: OrderListItem) {
    if (order.isJasic) return hasBarcode(order)
    return Boolean(order.modelName?.trim())
  }

  /** 列表状态角标：优先接口 `displayStatus`，与映射后的 `status` 对齐样式 */
  function orderStatusText(order: OrderListItem) {
    const fromApi = String(order.displayStatus ?? '').trim()
    return fromApi || order.status
  }

  /** 品牌类型：优先接口 `brandTypeLabel`（如佳士品牌），否则用佳士/非佳士简写 */
  function orderBrandTypeText(order: OrderListItem) {
    const fromApi = String(order.brandTypeLabel ?? '').trim()
    return fromApi || (order.isJasic ? '佳士' : '非佳士')
  }

  /** 在已加载的 orderList 上做前端模糊匹配（与 Tab、分页接口数据一致，不单独打 keyword 接口） */
  const displayOrderList = computed(() => {
    const list = orderList.value
    const kw = searchKeyword.value.trim().toLowerCase()
    if (!kw) return list
    return list.filter((o) => {
      const blob = [
        o.orderNo,
        o.description,
        o.qrCode,
        o.modelName,
        o.phone,
        o.centerName,
        o.displayStatus,
        o.brandTypeLabel,
        o.repairType,
        o.price,
        o.time
      ]
        .map((x) => String(x ?? '').toLowerCase())
        .join('\u0000')
      return blob.includes(kw)
    })
  })

  /**
   * 加载工单列表
   * @returns void
   */
  const loadOrderList = async (reset = false) => {
    if (loading.value || loadingMore.value) return
    if (!reset && !hasMore.value) return
    const targetPage = reset ? 1 : pageNum.value
    if (reset) {
      loading.value = true
      hasMore.value = true
    } else {
      loadingMore.value = true
    }
    try {
      const res = await listCustomerWorkOrder({
        pageNum: targetPage,
        pageSize,
        tabStatus: tabStatusMap[currentTab.value]
      })
      const records = res.data?.records ?? []
      const mapped = records.map(mapWorkOrderListRecordToItem)
      const nextTotal = Number(res.data?.total ?? 0)
      total.value = Number.isFinite(nextTotal) ? nextTotal : 0
      orderList.value = reset ? mapped : [...orderList.value, ...mapped]
      const loadedCount = orderList.value.length
      hasMore.value = loadedCount < total.value && mapped.length > 0
      pageNum.value = targetPage + 1
    } catch {
      if (reset) {
        orderList.value = []
        total.value = 0
        hasMore.value = false
      }
    } finally {
      loading.value = false
      loadingMore.value = false
    }
  }

  /**
   * 重置并重新加载列表
   * @returns void
   */
  const reloadOrderList = () => {
    pageNum.value = 1
    total.value = 0
    hasMore.value = true
    return loadOrderList(true)
  }

  const {
    refresherTriggered: orderListRefresherTriggered,
    onRefresherRefresh: onOrderListRefresherRefresh
  } = useScrollRefresher(async () => {
    await reloadOrderList()
  })

  /** 切换 Tab：更新状态并请求接口（全部不传 tabStatus，其余传 WAIT_ACCEPT 等） */
  function selectTab(index: number) {
    if (currentTab.value === index) return
    currentTab.value = index
    reloadOrderList()
  }

  /**
   * 触底加载更多
   * @returns void
   */
  const loadMore = () => {
    loadOrderList(false)
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
    reloadOrderList()
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

  // 上传寄件单号弹窗状态
  const showUploadModal = ref(false)
  const currentUploadId = ref<string | null>(null)
  /** 选中的快递单照片本地临时路径，用于预览与后续上传 */
  const uploadImagePath = ref('')

  /**
   * 打开上传寄件单号弹窗
   * @param id - 工单ID
   * @returns void
   */
  const goToUploadLogistics = (id: string) => {
    currentUploadId.value = id
    uploadImagePath.value = ''
    showUploadModal.value = true
  }

  /**
   * 关闭上传寄件单号弹窗
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
   * 提交上传寄件单号
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
      await updateCustomerWorkOrderSenderVoucher({
        senderVoucherFileIds: [fileId],
        workOrderId
      })
      uni.hideLoading()
      uni.showToast({ title: '上传成功', icon: 'none', duration: 1500 })
      closeUploadModal()
      reloadOrderList()
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
    uni.navigateTo({
      url,
      events: {
        /** 评价页提交成功后 emit，上一页立即拉新列表 */
        workOrderEvaluated: () => {
          reloadOrderList()
        }
      }
    })
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

  /* 整页限高，仅列表区域滚动，导航与 Tab 留在顶部 */
  .order-list-page.page-container.page-index {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    box-sizing: border-box;
  }

  .search-wrap {
    background-color: $bg-card;
    @include flex-column-gap;
    box-sizing: border-box;

    .search-box {
      @include flex-row;
      background-color: $bg-hover;
      height: 88rpx;
      border-radius: $radius-lg;
      padding: 0 $space-lg;
      box-sizing: border-box;

      .search-icon {
        margin-right: $space-sm;
      }

      .search-input {
        flex: 1;
        height: 100%;
        font-size: $font-md;
        color: $text-slate-900;
        background: transparent;
      }
    }

    /* 原生 input 占位符 class 不参与 scoped 哈希 */
    :deep(.placeholder-text) {
      color: $text-slate-400;
    }
  }

  .tabs {
    width: 100%;
    border-bottom: 2rpx solid $bg-hover;
    white-space: nowrap;
    box-sizing: border-box;

    .tabs-inner {
      display: inline-flex;
      padding: 0 $space-lg;
      gap: 48rpx;
    }

    .tab-item {
      @include flex-column-center;
      position: relative;
      flex-shrink: 0;
      padding: $space-sm 0 $space-md;
      box-sizing: border-box;

      .tab-text {
        font-size: $font-md;
        font-weight: 500;
        color: $text-main;
        transition: color 0.2s;
      }

      &.active {
        .tab-text {
          font-weight: bold;
          color: $primary;
        }

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 4rpx;
          background-color: $primary;
        }
      }
    }
  }

  .main-content {
    flex: 1;
    min-height: 0;
    height: 0;
    overflow-y: auto;
    box-sizing: border-box;
    padding-bottom: 40rpx;
  }

  .order-list {
    display: flex;
    flex-direction: column;
    padding-top: $space-lg;
    gap: 0;

    .empty-hint {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 160rpx 64rpx 120rpx;
      box-sizing: border-box;

      .empty-icon-wrap {
        width: 520rpx;
        max-width: 92%;
        height: 200rpx;
        @include flex-center;
        padding: 0 24rpx;
        box-sizing: border-box;
        margin-bottom: $space-lg;

        .empty-list-illus {
          width: 100%;
          height: 160rpx;
        }
      }

      .empty-title {
        font-size: $font-lg;
        font-weight: 600;
        color: $text-slate-500;
        margin-bottom: 12rpx;
      }

      .empty-desc {
        font-size: 26rpx;
        color: $text-slate-400;
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

      &.list-loading-top {
        padding-top: $space-xl;
        padding-bottom: $space-md;
      }
    }
  }

  .order-card {
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: $space-lg;
    margin-bottom: $space-lg;
    border: 2rpx solid $bg-hover;
    box-sizing: border-box;
    @include flex-column;

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
          color: $text-slate-900;
          letter-spacing: -0.025em;
        }
      }
    }

    .tags-wrap {
      display: flex;
      flex-wrap: wrap;
      gap: $space-sm;
      margin-bottom: $space-lg;

      .tag {
        padding: 4rpx $space-sm;
        border-radius: $radius-sm;
        border: 2rpx solid transparent;
        box-sizing: border-box;
        @include flex-center;

        .text {
          font-size: 22rpx;
          font-weight: 500;
        }

        &.tag-brand {
          background-color: $tag-brand-bg;
          border-color: $tag-brand-border;

          .text {
            color: $tag-brand-text;
          }
        }

        &.tag-other-brand {
          background-color: $tag-neutral-bg;
          border-color: $tag-neutral-border;

          .text {
            color: $text-slate-600;
          }
        }

        &.tag-model {
          background-color: $danger-tint-bg;
          border-color: $danger-tint-border;

          .text {
            color: $warranty-out;
            font-weight: 600;
          }
        }
      }
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
          color: $text-slate-400;
        }

        .d-value {
          font-size: 26rpx;
          color: $text-slate-700;
          font-weight: 500;

          &.text-primary {
            color: $primary;
          }
          &.font-bold {
            font-weight: bold;
            color: $text-slate-900;
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
      align-items: flex-end;
      gap: $space-md;
      flex-wrap: wrap;

      .time-wrap {
        @include flex-row;
        gap: $space-xs;
        color: $text-slate-400;

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
        @include flex-row;
        flex-wrap: wrap;
        justify-content: flex-end;
        gap: $space-md;
        flex: 1;

        .btn-action {
          margin: 0;
          padding: 2rpx $space-lg;
          min-width: 0;
          border-radius: 10rpx;
          font-size: 26rpx;
          font-weight: bold;
          @include btn-reset;
          @include flex-center;

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
        background-color: $border-neutral;
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
