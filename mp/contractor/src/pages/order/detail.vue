<template>
  <CustomNavBar
    title="工单详情"
    surface="sticky"
    color="#ffffff"
    background="#f26604"
    :show-shadow="false"
  />
  <view class="page-container order-detail-page">
    <!-- 状态栏 -->
    <OrderDetailStatusBanner :status="orderStatus" />
    <!-- 主内容区域 -->
    <view class="main-content" :class="{ 'main-content--with-bottom-bar': hasBottomActionBar }">
      <view class="content-wrap">
        <view v-if="isTransferredOutViewer" class="transfer-out-tip">
          <text class="transfer-out-tip-text">
            该工单已转出，当前为转出网点视角，仅可查看，不可接单或登记。
          </text>
        </view>
        <!-- 标签容器 -->
        <view class="tab-container">
          <!-- 标签栏 -->
          <view class="tab-bar">
            <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">
              <text class="tab-text">申请内容</text>
              <view v-if="currentTab === 0" class="tab-line"></view>
            </view>
            <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">
              <text class="tab-text">维修过程</text>
              <view v-if="currentTab === 1" class="tab-line"></view>
            </view>
            <view
              v-if="showEvaluateTab"
              class="tab-item"
              :class="{ active: currentTab === 2 }"
              @click="currentTab = 2"
            >
              <text class="tab-text">客户评价</text>
              <view v-if="currentTab === 2" class="tab-line"></view>
            </view>
          </view>

          <!-- 申请内容 -->
          <view v-if="currentTab === 0 && hasOrderBaseInfo" class="section-box">
            <OrderDetailBaseInfoCard :base="order.base" />
          </view>

          <!-- 维修过程 -->
          <view v-if="currentTab === 1 && hasRepairProcessContent" class="section-box">
            <!-- ---- 待接单: 未接单仅看故障点；接单(action=accept) 后才显示维修信息表单 ---- -->
            <OrderDetailFaultJudgeForm
              v-if="isPending && canEditFaultJudge"
              v-model:fault-judge="faultJudgeSelect"
              v-model:repair-quote="repairQuoteInput"
              v-model:quote-desc="quoteDescInput"
            />
            <!-- 故障点信息 -->
            <OrderDetailFaultPointCard
              v-else-if="isPending && hasFaultPoint"
              :as-card="false"
              history-title="最近维修记录"
              record-label="最近一次维修"
              :date="order.faultPoint.current.date"
              :desc="order.faultPoint.current.desc"
              :order-id="orderNavId"
            />

            <!-- ---- 维修中 / 已完成复检: 故障点登记 ---- -->
            <OrderDetailFaultRegisterForm
              v-if="canEditFaultPoint"
              v-model:fault-desc="faultDescSelect"
              v-model:repair-desc="repairDescSelect"
              v-model:other-repair-desc="otherRepairDesc"
              v-model:replace-part="replacePart"
              v-model:replace-quantity="replaceQuantity"
              v-model:fault-old-images="faultOldImages"
              v-model:fault-point-images="faultPointImages"
              v-model:machine-front-images="machineFrontImages"
              v-model:machine-barcode-images="machineBarcodeImages"
              v-model:other-images="otherImages"
              :is-recheck="detailEntryAction === 'recheck'"
              :fault-options="repairFaultOptions"
            />

            <!-- 故障点信息（复检编辑时不展示，避免与上方登记表单重复） -->
            <OrderDetailFaultPointCard
              v-if="(isCompleted || isClosed) && hasFaultPoint && detailEntryAction !== 'recheck'"
              :as-card="false"
              history-title="维修记录"
              record-label="当前维修"
              :date="order.faultPoint.current.date"
              :desc="order.faultPoint.current.desc"
              :order-id="orderNavId"
            />
          </view>

          <!-- 客户评价 -->
          <OrderDetailEvaluateSection
            v-if="showEvaluateTab && currentTab === 2"
            :evaluate="order.evaluate"
          />
        </view>

        <!-- 申请内容 Extra Cards -->
        <template v-if="currentTab === 0">
          <OrderDetailProductCard :product="order.product" />
          <OrderDetailServiceCard
            :service="order.service"
            :transfer-from-site="order.base.transferFromSite"
          />
        </template>

        <!-- 维修过程 Extra Cards（repairExtrasLayout：pending | active_repair | readonly_summary） -->
        <template v-if="currentTab === 1">
          <template v-if="repairExtrasLayout === 'pending'">
            <!-- 故障信息 Card -->
            <OrderDetailFaultCard :fault="order.fault" />

            <!-- 故障点信息 Card：仅接单填写维修信息时补充展示（避免与上方 Tab 内故障点区块重复） -->
            <OrderDetailFaultPointCard
              v-if="hasFaultPoint && canEditFaultJudge"
              history-title="最近维修记录"
              record-label="最近一次维修"
              :date="order.faultPoint.current.date"
              :desc="order.faultPoint.current.desc"
              :order-id="orderNavId"
            />

            <!-- 底部按钮 -->
            <base-button v-if="canEditFaultJudge && faultJudgeSelect === '无故障'">
              <view class="btn btn-primary action-wrap" @click="onRepairComplete">
                <image class="btn-icon" :src="completeCheckIcon" mode="aspectFit" />维修完成
              </view>
            </base-button>
            <base-button v-if="canEditFaultJudge && faultJudgeSelect === '有故障'">
              <view class="btn btn-primary action-wrap" @click="onSubmitQuote">
                <image class="btn-icon" :src="submitQuoteIcon" mode="aspectFit" />提交报价
              </view>
            </base-button>
          </template>
          <!-- 维修中 -->
          <template v-else-if="repairExtrasLayout === 'active_repair'">
            <OrderDetailRepairMetaCard :repair="order.repair" />
            <OrderDetailAcceptorCard :acceptor="order.acceptor" />
            <OrderDetailFaultCard :fault="order.fault" />
            <OrderDetailFaultPointCard
              v-if="hasFaultPoint"
              history-title="最近维修记录"
              record-label="最近一次维修"
              :date="order.faultPoint.current.date"
              :desc="order.faultPoint.current.desc"
              :order-id="orderNavId"
            />

            <base-button v-if="canEditFaultPoint">
              <view class="btn btn-primary action-wrap" @click="onSubmitFaultPoint">
                <image class="btn-icon" :src="saveIcon" mode="aspectFit" />提交登记
              </view>
            </base-button>
          </template>
          <!-- 已维修完成 -->
          <template v-else-if="repairExtrasLayout === 'readonly_summary'">
            <OrderDetailRepairMetaCard :repair="order.repair" />
            <OrderDetailAcceptorCard :acceptor="order.acceptor" />
            <OrderDetailFaultCard :fault="order.fault" />
          </template>
        </template>
      </view>
    </view>

    <!-- 机器返回方式弹窗 -->
    <ReturnMethodModal
      v-model="showReturnMethodModal"
      :initial-type="returnMethodType"
      :initial-mail="returnMethodInitialMail"
      @confirm="onReturnMethodConfirm"
    />
    <!-- 工单关闭弹窗 -->
    <CloseOrderModal v-model="showCloseOrderModal" @confirm="onCloseOrderConfirm" />
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import CloseOrderModal from '@/components/CloseOrderModal/CloseOrderModal.vue'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import ReturnMethodModal from '@/components/ReturnMethodModal/ReturnMethodModal.vue'
  import OrderDetailAcceptorCard from './components/OrderDetailAcceptorCard.vue'
  import OrderDetailBaseInfoCard from './components/OrderDetailBaseInfoCard.vue'
  import OrderDetailEvaluateSection from './components/OrderDetailEvaluateSection.vue'
  import OrderDetailFaultCard from './components/OrderDetailFaultCard.vue'
  import OrderDetailFaultJudgeForm from './components/OrderDetailFaultJudgeForm.vue'
  import OrderDetailFaultPointCard from './components/OrderDetailFaultPointCard.vue'
  import OrderDetailFaultRegisterForm from './components/OrderDetailFaultRegisterForm.vue'
  import OrderDetailProductCard from './components/OrderDetailProductCard.vue'
  import OrderDetailRepairMetaCard from './components/OrderDetailRepairMetaCard.vue'
  import OrderDetailServiceCard from './components/OrderDetailServiceCard.vue'
  import OrderDetailStatusBanner from './components/OrderDetailStatusBanner.vue'
  import { fetchOrderDetail, fetchRepairFaultOptions, type WorkOrderRepairFaultOptionVO } from '@/api/order'
  import {
    cloneOrderDetail,
    createEmptyOrderDetail,
    getReturnMethodInitialMail,
    type OrderDetail,
    type OrderStatus
  } from '@/models/order'
  import { isOrderStatus } from '@/utils/orderStatus'
  import { completeCheckIcon, saveIcon, submitQuoteIcon } from '@/svgs'
  import {
    isDetailEntryAction,
    shouldOpenRepairTab,
    useOrderDetailPage,
    type DetailEntryAction
  } from './useOrderDetailPage'

  // 当前 Tab
  const currentTab = ref(0)
  // 工单状态
  const orderStatus = ref<OrderStatus>('pending')
  // 工单 ID
  const orderId = ref('')
  // 接单操作
  const detailEntryAction = ref<DetailEntryAction>('')

  // ==================== 表单状态 ====================

  // 待接单 - 故障判定
  const faultJudgeSelect = ref('')
  // 维修报价
  const repairQuoteInput = ref('')
  // 报价说明
  const quoteDescInput = ref('')

  // 无故障维修完成 → 机器返回方式（必填）→ 关闭工单原因
  const showReturnMethodModal = ref(false)
  // 关闭工单弹窗
  const showCloseOrderModal = ref(false)
  // 由「维修完成」打开返回方式弹窗时为 true，仅此时确认返回方式后再弹关闭工单
  const pendingNoFaultRepairAfterReturnMethod = ref(false)
  // 返回方式类型
  const returnMethodType = ref<'' | 'self' | 'mail'>('')

  /**
   * 监听返回方式弹窗
   * @param open 是否打开
   * @returns void
   */
  watch(showReturnMethodModal, (open) => {
    if (!open && pendingNoFaultRepairAfterReturnMethod.value) {
      pendingNoFaultRepairAfterReturnMethod.value = false
    }
  })

  watch(faultJudgeSelect, (v) => {
    if (v !== '无故障') {
      returnMethodType.value = ''
    }
  })

  // 维修中 - 故障点登记
  const faultDescSelect = ref('')
  const repairDescSelect = ref('')
  // 其它维修说明
  const otherRepairDesc = ref('')
  // 更换配件
  const replacePart = ref('')
  // 更换数量
  const replaceQuantity = ref('')
  // 故障旧图片
  const faultOldImages = ref<any[]>([])
  // 故障处图片
  const faultPointImages = ref<any[]>([])
  // 机器正面图片
  const machineFrontImages = ref<any[]>([])
  // 机器条码图片
  const machineBarcodeImages = ref<any[]>([])
  // 其它图片
  const otherImages = ref<any[]>([])

  // ==================== 工单数据 ====================
  // 工单详情
  const order = ref<OrderDetail>(createEmptyOrderDetail())
  // 机器返回方式初始邮件
  const returnMethodInitialMail = computed(() => getReturnMethodInitialMail(order.value))

  /** 历史记录页等跳转用工单标识 */
  const orderNavId = computed(() => String(order.value.id || orderId.value || '').trim())

  const {
    isPending,
    isCompleted,
    isClosed,
    canOperateTransferredOrder,
    isTransferredOutViewer,
    canEditFaultJudge,
    canEditFaultPoint,
    hasBottomActionBar,
    showEvaluateTab,
    hasFaultPoint,
    hasRepairProcessContent,
    hasOrderBaseInfo,
    repairExtrasLayout
  } = useOrderDetailPage({
    order,
    orderStatus,
    detailEntryAction,
    currentTab,
    faultJudgeSelect
  })

  // ==================== 生命周期 ====================

  // 页面加载
  onLoad((options: any) => {
    orderId.value = String(options?.id || options?.orderId || '')
    // 设置工单状态
    if (isOrderStatus(options?.status)) {
      orderStatus.value = options.status
    }

    // 设置接单操作
    if (isDetailEntryAction(options?.action)) {
      detailEntryAction.value = options.action
    }

    // 设置当前 Tab
    currentTab.value = shouldOpenRepairTab(detailEntryAction.value) ? 1 : isPending.value ? 0 : 1

    // 加载工单详情
    loadDetail()
  })

  // 维修登记可选故障/说明选项
  const repairFaultOptions = ref<WorkOrderRepairFaultOptionVO[]>([])

  /**
   * 加载工单详情
   * @returns void
   */
  const loadDetail = async () => {
    if (!orderId.value) return
    try {
      const detail = await fetchOrderDetail(orderId.value)
      order.value = cloneOrderDetail(detail)
      if (isOrderStatus(detail.status)) {
        orderStatus.value = detail.status
      }

      // 仅在进入“维修登记/复检登记”时加载选项
      const woId = Number(orderId.value)
      if (
        Number.isFinite(woId) &&
        woId > 0 &&
        (detailEntryAction.value === 'repair' || detailEntryAction.value === 'recheck')
      ) {
        repairFaultOptions.value = await fetchRepairFaultOptions(woId)
      } else {
        repairFaultOptions.value = []
      }
    } catch (e) {
      // 接口失败时静默，保持页面可正常渲染
      console.log(e)
    }
  }

  // ==================== 操作方法 ====================

  /**
   * 维修完成（无故障）：弹出机器返回方式，确认后再填写关闭工单原因
   * @returns void
   */
  const onRepairComplete = () => {
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }
    returnMethodType.value = ''
    pendingNoFaultRepairAfterReturnMethod.value = true
    showReturnMethodModal.value = true
  }

  /**
   * 确认机器返回方式
   * @param data 数据
   * @returns void
   */
  const onReturnMethodConfirm = (data: {
    type: 'self' | 'mail'
    mail?: {
      receiverName: string
      receiverPhone: string
      receiverAddress: string
      receiptImagePaths: string[]
    }
  }) => {
    returnMethodType.value = data.type
    uni.showToast({
      title: `已选择${data.type === 'self' ? '自提' : '回寄'}`,
      icon: 'none'
    })
    if (pendingNoFaultRepairAfterReturnMethod.value) {
      pendingNoFaultRepairAfterReturnMethod.value = false
      setTimeout(() => {
        showCloseOrderModal.value = true
      }, 400)
    }
  }

  /**
   * 确认工单关闭
   * @param reason 关闭原因
   * @returns void
   */
  const onCloseOrderConfirm = (reason: string) => {
    const id = order.value.id || orderId.value
    uni.showToast({
      title: id ? `工单 ${id} 已关闭，原因：${reason}` : `工单已关闭，原因：${reason}`,
      icon: 'none',
      duration: 2000
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 2000)
  }

  /**
   * 提交报价
   * @returns void
   */
  const onSubmitQuote = () => {
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }
    if (!repairQuoteInput.value) {
      uni.showToast({ title: '请输入维修报价', icon: 'none' })
      return
    }
    uni.showToast({
      title: '报价已提交',
      icon: 'success',
      duration: 1500
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }

  /**
   * 提交故障点登记 / 复检登记
   * @returns void
   */
  const onSubmitFaultPoint = () => {
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }
    if (!repairDescSelect.value) {
      uni.showToast({ title: '请选择维修说明', icon: 'none' })
      return
    }
    if (repairDescSelect.value === '其它维修说明') {
      const otherDesc = (otherRepairDesc.value || '').trim()
      if (!otherDesc) {
        uni.showToast({ title: '请输入其它维修说明', icon: 'none' })
        return
      }
    }
    const partName = (replacePart.value || '').trim()
    if (!partName) {
      uni.showToast({ title: '请输入配件名称', icon: 'none' })
      return
    }
    const qtyStr = String(replaceQuantity.value ?? '').trim()
    const qty = Number(qtyStr)
    if (!qtyStr || Number.isNaN(qty) || qty <= 0) {
      uni.showToast({ title: '请输入有效的配件数量', icon: 'none' })
      return
    }
    const isRecheck = detailEntryAction.value === 'recheck'
    uni.showToast({
      title: isRecheck ? '复检登记已提交' : '故障点登记已提交',
      icon: 'success',
      duration: 1500
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }
</script>

<style lang="scss" scoped>
  /* 工单详情（pages/order/detail）— 根节点需加 class order-detail-page */
  .order-detail-page {
    /* 勿写 min-height:100vh：上方还有 CustomNavBar 占位，与 100vh 叠加会使整页高度 > 视口，短内容也会出现滚动条 */
    width: 100%;
    box-sizing: border-box;
    background-color: $surface-app;
    @include flex-col;

    .main-content {
      /* 覆盖 order-pages 中 .page-container .main-content 的 flex:1 + overflow-y:auto，
         避免短页面出现内部滚动区域/滚动条；内容超出时由页面整体滚动 */
      flex: none;
      overflow: visible;
      padding-bottom: 64rpx;
      margin-top: -64rpx;
      position: relative;
      z-index: 10;

      /* 底栏：上内边距 + 按钮高 80rpx + 下内边距（含安全区），与 :deep(.base-btn) 一致 */
      &--with-bottom-bar {
        padding-bottom: calc(
          64rpx + #{$space-md} + 80rpx + #{$space-md} + constant(safe-area-inset-bottom)
        );
        padding-bottom: calc(
          64rpx + #{$space-md} + 80rpx + #{$space-md} + env(safe-area-inset-bottom)
        );
      }
    }

    .content-wrap {
      padding: 0 $space-lg;
      @include flex-col;
      gap: $space-lg;
    }

    .transfer-out-tip {
      padding: 20rpx $space-md;
      background: $primary-alpha-10;
      border-radius: $radius-md;
      border: 2rpx solid $primary-alpha-25;

      .transfer-out-tip-text {
        font-size: 26rpx;
        color: $orange-800;
        line-height: 1.5;
      }
    }

    .tab-container {
      @include sheet-white;
    }

    .tab-bar {
      display: flex;
      justify-content: space-around;
      padding: 0 $space-lg;
      border-bottom: 2rpx solid $surface-slate-50;

      .tab-item {
        padding: $space-lg 0;
        position: relative;
        flex: 1;
        text-align: center;

        .tab-text {
          font-size: $font-md;
          font-weight: 500;
          color: $text-slate-500;
        }

        &.active {
          .tab-text {
            font-weight: bold;
            color: $primary;
          }

          .tab-line {
            position: absolute;
            bottom: 0;
            left: 50%;
            transform: translateX(-50%);
            width: $space-xl;
            height: 4rpx;
            background-color: $primary;
          }
        }
      }
    }

    .section-box {
      padding: $space-lg;
    }

    .action-wrap {
      .btn-icon {
        font-size: 40rpx;
        margin-right: $space-sm;
      }

      image.btn-icon {
        width: 40rpx;
        height: 40rpx;
        flex-shrink: 0;
      }

      &::after {
        border: none;
      }

      &:active {
        opacity: 0.9;
      }
    }

    /* 底部固定按钮：基础留白 + 安全区（覆盖 fixed-btn 仅 env 底边距，无刘海机型也有间距） */
    :deep(.base-btn) {
      padding-bottom: calc(#{$space-md} + constant(safe-area-inset-bottom));
      padding-bottom: calc(#{$space-md} + env(safe-area-inset-bottom));
    }
  }
</style>
