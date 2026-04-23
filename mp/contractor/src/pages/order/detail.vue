<template>
  <CustomNavBar
    title="工单详情"
    surface="sticky"
    :color="themeColors.textBg"
    :background="themeColors.primary"
    :show-shadow="false"
  />
  <view class="page-container order-detail-page">
    <!-- 状态栏 -->
    <OrderDetailStatusBanner :status="orderStatus" />
    <!-- 主内容区域 -->
    <view class="main-content" :class="{ 'main-content--with-bottom-bar': hasBottomActionBar }">
      <view class="content-wrap">
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
            <!-- ---- 待接单：action=accept 时填写故障判定与报价，提交时再调接单接口 ---- -->
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
              :repair-faults="order.faultPoint.currentFaults"
              :history-records="order.faultPoint.allRepairsFaultRecords"
              :repair-time-fallback="order.faultPoint.current.date"
              :order-id="orderNavId"
              :flow-items="order.processFlows"
            />

            <!-- ---- 维修中 / 已完成复检: 故障点登记（与维修登记同款；复检仅底部按钮文案不同） ---- -->
            <OrderDetailFaultRegisterForm
              v-if="canEditFaultPoint"
              v-model:fault-items="faultItemsSelect"
              v-model:fault-remark="faultRemarkInput"
              v-model:repair-desc="repairDescSelect"
              v-model:other-repair-desc="otherRepairDesc"
              v-model:replace-parts="replaceParts"
              v-model:fault-old-images="faultOldImages"
              v-model:fault-point-images="faultPointImages"
              v-model:machine-front-images="machineFrontImages"
              v-model:machine-barcode-images="machineBarcodeImages"
              v-model:other-images="otherImages"
              :is-recheck="detailEntryAction === 'recheck'"
              :fault-options="repairFaultOptions"
              :first-repair-fault-desc="firstRepairFaultDescText"
              :first-repair-fault-remark="firstRepairFaultRemarkText"
            />

            <!-- 故障点信息（复检编辑时不展示，避免与上方登记表单重复） -->
            <OrderDetailFaultPointCard
              v-if="(isCompleted || isClosed) && hasFaultPoint && detailEntryAction !== 'recheck'"
              :as-card="false"
              history-title="最近维修记录"
              record-label="当前维修"
              :repair-faults="order.faultPoint.currentFaults"
              :history-records="order.faultPoint.allRepairsFaultRecords"
              :repair-time-fallback="order.faultPoint.current.date"
              :order-id="orderNavId"
              :flow-items="order.processFlows"
            />
          </view>

          <!-- 客户评价（只读展示；contractor 端不提供评价入口） -->
          <OrderDetailEvaluateSection
            v-if="showEvaluateTab && currentTab === 2"
            :evaluate="order.evaluate"
          />
        </view>

        <!-- 申请内容 Extra Cards -->
        <template v-if="currentTab === 0">
          <OrderDetailProductCard
            :product="order.product"
            :need-supplement="needSupplementMachineModel"
            @supplement="openMachineModelSupplement"
          />
          <OrderDetailServiceCard :service="order.service" />
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
              :repair-faults="order.faultPoint.currentFaults"
              :history-records="order.faultPoint.allRepairsFaultRecords"
              :repair-time-fallback="order.faultPoint.current.date"
              :order-id="orderNavId"
              :flow-items="order.processFlows"
            />

            <!-- 底部按钮 -->
            <base-button
              v-if="canEditFaultJudge && faultJudgeSelect === '无故障' && canCloseWorkOrder"
            >
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
            <OrderDetailRepairMetaCard
              v-model:repair-quote="repairQuoteInput"
              v-model:quote-desc="quoteDescInput"
              :order="order"
              :quote-editable="canEditFaultPoint && detailEntryAction !== 'recheck'"
            />
            <OrderDetailAcceptorCard :acceptor="order.acceptor" />
            <OrderDetailFaultCard :fault="order.fault" />
            <OrderDetailFaultPointCard
              v-if="hasFaultPoint"
              history-title="最近维修记录"
              record-label="最近一次维修"
              :repair-faults="order.faultPoint.currentFaults"
              :history-records="order.faultPoint.allRepairsFaultRecords"
              :repair-time-fallback="order.faultPoint.current.date"
              :order-id="orderNavId"
              :flow-items="order.processFlows"
            />

            <base-button v-if="canEditFaultPoint">
              <view class="btn btn-primary action-wrap" @click="onSubmitFaultPoint">
                <image class="btn-icon" :src="saveIcon" mode="aspectFit" />
                {{ detailEntryAction === 'recheck' ? '提交复检登记' : '提交维修登记' }}
              </view>
            </base-button>
          </template>
          <!-- UPLOAD_SEND_EXPRESS：待受理+邮寄时后端会在 availableActions 下发本动作 -->
          <base-button v-if="canUploadSendExpress">
            <view class="btn btn-primary action-wrap" @click="openUploadSendExpressModal">
              上传寄件单号
            </view>
          </base-button>
          <!-- 已维修完成 -->
          <template v-else-if="repairExtrasLayout === 'readonly_summary'">
            <OrderDetailRepairMetaCard :order="order" />
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
      :initial-mail="returnMethodInitialMailMerged"
      @confirm="onReturnMethodConfirm"
    />
    <!-- 工单关闭弹窗 -->
    <CloseOrderModal
      v-model="showCloseOrderModal"
      no-fault-required
      @confirm="onCloseOrderConfirm"
    />

    <!-- 机型补录弹窗：进入维修登记/复检时，佳士品牌且机型缺失自动弹出 -->
    <MachineModelSupplementModal
      v-model:visible="showMachineModelSupplement"
      :work-order-id="machineModelSupplementWorkOrderId"
      @confirm="onMachineModelSupplementConfirm"
      @cancel="onMachineModelSupplementCancel"
    />

    <!-- 上传寄件单号弹窗 -->
    <UploadSendExpressModal
      v-model:visible="showUploadSendExpressModal"
      :work-order-id="resolveWorkOrderId()"
      @confirm="onSubmitSendExpress"
    />
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { onLoad, onShow } from '@dcloudio/uni-app'
  import { themeColors } from '@/theme/colors'
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
  import MachineModelSupplementModal from './components/MachineModelSupplementModal.vue'
  import UploadSendExpressModal from '@/components/UploadSendExpressModal/UploadSendExpressModal.vue'
  import {
    getWorkOrder,
    listRepairFaultOptions,
    repairWorkOrder,
    reviewWorkOrder,
    techAcceptWorkOrder,
    updateRepairProductModel,
    updateWorkOrderSendExpress,
    type ReturnMethodConfirmPayload,
    type WorkOrderRepairFaultOptionVO,
    type WorkOrderReviewDTO
  } from '@/api/workOrder'
  import {
    cloneOrderDetail,
    createEmptyOrderDetail,
    getReturnMethodInitialMail,
    type OrderDetail,
    type SysFileItemVO,
    type WorkOrderMainStatus,
  } from '@/models/order'
  import { useAppStore, useUserStore } from '@/stores'
  import { isOrderStatus } from '@/utils/orderStatus'
  import { Perms } from '@/utils/permissions'
  import { getApiMessage } from '@/utils/http'
  import { takeSelectedShippingAddress } from '@/utils/addressStorage'
  import { hasVal } from '@/utils/value'
  import { resolvePreviewableUrl } from '@/utils/mediaPreview'
  import { asUnknownArray, collectVoucherFileIds, hasUnuploadedMediaItems } from '@/utils/workOrderFileIds'
  import { completeCheckIcon, saveIcon, submitQuoteIcon } from '@/svgs'
  import {
    isDetailEntryAction,
    shouldOpenRepairTab,
    useOrderDetailPage,
    type DetailEntryAction
  } from './useOrderDetailPage'

  const appStore = useAppStore()
  const userStore = useUserStore()

  /** 无故障「维修完成」会走机器返回方式并关单，需工单关闭权限 */
  const canCloseWorkOrder = computed(() => userStore.hasPermission(Perms.WORKORDER_CLOSE))

  const OTHER_REPAIR_LABEL = '其它维修说明'

  // 当前 Tab
  const currentTab = ref(0)
  /** 维修登记/复检入口：仅在首次拉取详情后根据机器型号校正 Tab，避免重复 loadDetail 抢切用户当前 Tab */
  const repairEntryTabInitialized = ref(false)
  // 工单状态
  const orderStatus = ref<WorkOrderMainStatus>('PENDING_ASSIGN')
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

  // 机器返回方式弹窗（无故障维修完成：确认后填关单原因）
  const showReturnMethodModal = ref(false)
  // 关闭工单弹窗
  const showCloseOrderModal = ref(false)
  // 由「维修完成」打开返回方式弹窗时为 true，仅此时确认返回方式后再弹关闭工单
  const pendingNoFaultRepairAfterReturnMethod = ref(false)
  /** 无故障闭环：关闭工单接口需带上用户刚确认的返回方式（与 CloseOrderModal 配套） */
  const closeOrderReturnMethodPayload = ref<ReturnMethodConfirmPayload | null>(null)
  // 返回方式类型
  const returnMethodType = ref<'' | 'self' | 'mail'>('')

  watch(faultJudgeSelect, (v) => {
    if (v !== '无故障') {
      returnMethodType.value = ''
    }
  })

  // 维修中 - 故障点登记
  /** 仅佳士且详情无型号时，维修登记/复检提交需强制补填机器型号（通过 MachineModelSupplementModal 弹窗补录） */
  const requireMachineModelForJiashi = ref(false)
  /** 机型补录弹窗显隐 */
  const showMachineModelSupplement = ref(false)
  /** 机型补录弹窗锁定的工单ID（打开弹窗瞬间快照） */
  const machineModelSupplementWorkOrderId = ref(0)
  /** 进入维修登记/复检时，只需在首次触发时自动弹出一次，避免用户手动关闭后反复弹 */
  const machineModelAutoOpened = ref(false)

  const needSupplementMachineModel = computed(() => {
    if (!requireMachineModelForJiashi.value) return false
    return !hasVal(order.value.product?.model)
  })

  /** 上传寄件单号弹窗显隐 */
  const showUploadSendExpressModal = ref(false)

  /**
   * 是否展示「上传寄件单号」按钮：
   * - 当前登录用户具备派单权限（后端 SaCheckPermission("workorder:assign")）
   * - 详情 `availableActions` 含 UPLOAD_SEND_EXPRESS（后端已结合"待受理+邮寄"给出）
   */
  const canUploadSendExpress = computed(() => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    return (order.value.availableActions || []).includes('UPLOAD_SEND_EXPRESS')
  })
  /** 维修确认故障多选（对应后端 WorkOrderRepairDTO.faultItems） */
  const faultItemsSelect = ref<string[]>([])
  /** 其它故障说明（faultItems 含「其它」时必填） */
  const faultRemarkInput = ref('')
  /** 与后端一致的「其它故障」标签 */
  const OTHER_FAULT_LABEL = '其它'
  const repairDescSelect = ref<string[]>([])
  // 其它维修说明
  const otherRepairDesc = ref('')
  let replacePartRowUid = 0
  const nextReplacePartRowId = () => ++replacePartRowUid
  /** 更换配件多行（删除的行不会提交） */
  const replaceParts = ref<{ id: number; part: string; quantity: string }[]>([
    { id: nextReplacePartRowId(), part: '', quantity: '' }
  ])
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
  /** 从地址簿选择回寄信息后覆盖弹窗内收件字段（与接口回显合并） */
  const mailReturnAddressOverride = ref<{
    receiverName: string
    receiverPhone: string
    receiverAddress: string
  } | null>(null)
  const returnMethodInitialMailMerged = computed(() => {
    const base = getReturnMethodInitialMail(order.value)
    const o = mailReturnAddressOverride.value
    if (!o) return base
    return {
      ...base,
      receiverName: o.receiverName,
      receiverPhone: o.receiverPhone,
      receiverAddress: o.receiverAddress
    }
  })

  watch(showReturnMethodModal, (open, prevOpen) => {
    if (open && !prevOpen) {
      mailReturnAddressOverride.value = null
    }
    if (!open && pendingNoFaultRepairAfterReturnMethod.value) {
      pendingNoFaultRepairAfterReturnMethod.value = false
    }
  })

  /** 历史记录页等跳转用工单标识 */
  const orderNavId = computed(() => String(order.value.id || orderId.value || '').trim())

  /**
   * 复检登记时只读回显的"首次维修确认故障"：
   * 取 faultPoint.allRepairsFaultRecords 第 0 条的 faultDesc；
   * 仅当 faultDesc 含「其它」且 otherDesc 非空时，将 otherDesc 作为 faultRemark 展示。
   */
  const firstRepairFaultDescText = computed(() => {
    const first = order.value.faultPoint?.allRepairsFaultRecords?.[0]
    return String(first?.faultDesc || '').trim()
  })
  const firstRepairFaultRemarkText = computed(() => {
    const first = order.value.faultPoint?.allRepairsFaultRecords?.[0]
    const faultDesc = String(first?.faultDesc || '')
    const otherDesc = String(first?.otherDesc || '').trim()
    if (!otherDesc) return ''
    if (faultDesc.includes('其它') || faultDesc.includes('其他')) return otherDesc
    return ''
  })

  const {
    isPending,
    isCompleted,
    isClosed,
    canOperateTransferredOrder,
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
    faultJudgeSelect,
    canCompleteNoFaultRepair: canCloseWorkOrder
  })

  // ==================== 生命周期 ====================

  /** 复检登记：把最近一次维修登记的选项/说明/配件/图片回显到登记表单 */
  const applyRecheckRepairRegistrationEcho = (echo: OrderDetail['repairRegistrationEcho']) => {
    if (!echo) return

    if (echo.repairItems.length) {
      repairDescSelect.value = [...echo.repairItems]
    }

    const od = (echo.otherDesc || '').trim()
    if (od) otherRepairDesc.value = od

    const mapSysFilesToMedia = (files: SysFileItemVO[]) =>
      (Array.isArray(files) ? files : [])
        .map((f) => {
          const fileId = f.fileId != null ? Number(f.fileId) : NaN
          const preview = String(f.previewUrl || '').trim()
          const url = (resolvePreviewableUrl(preview) || preview).trim()
          if (!url && !Number.isFinite(fileId)) return null
          return {
            fileId: Number.isFinite(fileId) ? fileId : undefined,
            id: Number.isFinite(fileId) ? fileId : undefined,
            url,
            path: url,
            previewUrl: f.previewUrl,
            name: String(f.originalName || '').trim()
          }
        })
        .filter((x): x is NonNullable<typeof x> => x != null)

    faultOldImages.value = mapSysFilesToMedia(echo.faultOldImageFiles)
    faultPointImages.value = mapSysFilesToMedia(echo.faultNewImageFiles)
    machineFrontImages.value = mapSysFilesToMedia(echo.machineImageFiles)
    machineBarcodeImages.value = mapSysFilesToMedia(echo.machineBarcodeImageFiles)
    otherImages.value = mapSysFilesToMedia(echo.otherImageFiles)

    if (echo.parts.length) {
      replaceParts.value = echo.parts.map((p, idx) => ({
        id: idx + 1,
        part: String(p.partName || '').trim(),
        quantity: String(p.partQty ?? '')
      }))
      replacePartRowUid = echo.parts.length
    } else {
      replaceParts.value = [{ id: nextReplacePartRowId(), part: '', quantity: '' }]
    }
  }

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

  onShow(() => {
    const picked = takeSelectedShippingAddress()
    if (!picked) return
    mailReturnAddressOverride.value = {
      receiverName: picked.name,
      receiverPhone: picked.phone,
      receiverAddress: picked.fullAddress
    }
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
      const detail = await getWorkOrder(orderId.value)
      order.value = cloneOrderDetail(detail)
      if (isOrderStatus(detail.status)) {
        orderStatus.value = detail.status
      }
      requireMachineModelForJiashi.value =
        !!detail.brand?.isJiashi && !hasVal(detail.product?.model)

      // 待接单·接单入口：用详情当前报价（quotes → repair.faultJudge 等）回显表单
      if (
        detailEntryAction.value === 'accept' &&
        (detail.status === 'PENDING_ASSIGN' || detail.status === 'PENDING_TECH_ACCEPT')
      ) {
        const fj = String(detail.repair?.faultJudge ?? '').trim()
        if (fj === '有故障' || fj === '无故障') {
          faultJudgeSelect.value = fj
        }
        const rawAmt = String(detail.repair?.quoteAmount ?? '').trim()
        if (rawAmt && rawAmt !== '0.00' && rawAmt !== '0') {
          repairQuoteInput.value = rawAmt
        }
        quoteDescInput.value = String(detail.repair?.quoteDesc ?? '').trim()
      }

      // 维修中·维修登记：报价与说明可从详情修改后随 repair 接口再次提交
      if (detailEntryAction.value === 'repair' && detail.status === 'IN_PROGRESS') {
        const rawAmtRepair = String(detail.repair?.quoteAmount ?? '').trim()
        if (rawAmtRepair && rawAmtRepair !== '0.00' && rawAmtRepair !== '0') {
          repairQuoteInput.value = rawAmtRepair
        } else {
          repairQuoteInput.value = ''
        }
        quoteDescInput.value = String(detail.repair?.quoteDesc ?? '').trim()
        // 无条码时，维修说明默认选「其它维修说明」
        if (!hasVal(detail.product?.barcode) && repairDescSelect.value.length === 0) {
          repairDescSelect.value = [OTHER_REPAIR_LABEL]
        }
      }

      // 复检登记·非佳士：维修说明默认选「其它维修说明」（佳士可走条码关联的选项）
      if (
        detailEntryAction.value === 'recheck' &&
        detail.status === 'COMPLETED' &&
        !detail.brand?.isJiashi &&
        repairDescSelect.value.length === 0
      ) {
        repairDescSelect.value = [OTHER_REPAIR_LABEL]
      }

      // 仅在进入「维修登记/复检登记」时加载选项
      const woId = Number(orderId.value)
      if (
        Number.isFinite(woId) &&
        woId > 0 &&
        (detailEntryAction.value === 'repair' || detailEntryAction.value === 'recheck')
      ) {
        repairFaultOptions.value = await listRepairFaultOptions(woId)
      } else {
        repairFaultOptions.value = []
      }

      // 复检登记：回显上次维修登记提交的选项、说明、配件与图片
      if (detailEntryAction.value === 'recheck' && detail.status === 'COMPLETED') {
        applyRecheckRepairRegistrationEcho(detail.repairRegistrationEcho)
      }

      // 复检登记·非佳士：无回显项时维修说明默认选「其它维修说明」（佳士可走条码关联的选项）
      if (
        detailEntryAction.value === 'recheck' &&
        detail.status === 'COMPLETED' &&
        !detail.brand?.isJiashi &&
        repairDescSelect.value.length === 0
      ) {
        repairDescSelect.value = [OTHER_REPAIR_LABEL]
      }

      // 维修登记 / 复检：默认进「维修过程」。
      // 佳士品牌且机型为空时，不再把 Tab 切到「申请内容」让用户手填，
      // 改为弹出 MachineModelSupplementModal 机型补录弹窗（对齐 jasic-ui 做法）。
      if (
        !repairEntryTabInitialized.value &&
        (detailEntryAction.value === 'repair' || detailEntryAction.value === 'recheck')
      ) {
        const repairEntry =
          detailEntryAction.value === 'repair' && orderStatus.value === 'IN_PROGRESS'
        const recheckEntry =
          detailEntryAction.value === 'recheck' && orderStatus.value === 'COMPLETED'
        if (repairEntry || recheckEntry) {
          repairEntryTabInitialized.value = true
          currentTab.value = 1
          if (needSupplementMachineModel.value && !machineModelAutoOpened.value) {
            machineModelAutoOpened.value = true
            openMachineModelSupplement()
          }
        }
      }
    } catch (e) {
      // 接口失败时静默，保持页面可正常渲染
      console.log(e)
    }
  }

  // ==================== 操作方法 ====================

  const resolveWorkOrderId = () => {
    const id = Number(order.value.id || orderId.value)
    return Number.isFinite(id) && id > 0 ? id : 0
  }

  /** 打开机型补录弹窗（由「点击补录机器型号」或维修登记提交时缺机型触发） */
  const openMachineModelSupplement = () => {
    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    machineModelSupplementWorkOrderId.value = wid
    showMachineModelSupplement.value = true
  }

  /** 弹窗确认：调 PUT /repair-product-model 写入后刷新详情，让「维修过程」后续能拿到 repair-fault-options */
  const onMachineModelSupplementConfirm = async (productModel: string) => {
    const wid = machineModelSupplementWorkOrderId.value
    if (!wid) {
      showMachineModelSupplement.value = false
      return
    }
    uni.showLoading({ title: '提交中...' })
    try {
      await updateRepairProductModel({ workOrderId: wid, productModel })
      showMachineModelSupplement.value = false
      if (order.value.product) {
        order.value.product.model = productModel
      }
      await loadDetail()
      uni.showToast({ title: '机器型号已更新', icon: 'none' })
    } catch {
      // updateRepairProductModel 内已 toast
    } finally {
      uni.hideLoading()
    }
  }

  const onMachineModelSupplementCancel = () => {
    // 取消：保留已要求补录的标记，提交维修登记前若仍缺机型会再次弹出
  }

  /** 打开「上传寄件单号」弹窗（待受理+邮寄阶段） */
  const openUploadSendExpressModal = () => {
    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    showUploadSendExpressModal.value = true
  }

  /** 弹窗确认：PUT `/system/work-order/send-express`，成功后刷新详情 */
  const onSubmitSendExpress = async (payload: {
    workOrderId: number
    sendExpressNo: string
    senderVoucherFileIds?: number[]
  }) => {
    uni.showLoading({ title: '提交中...' })
    try {
      await updateWorkOrderSendExpress(payload)
      showUploadSendExpressModal.value = false
      uni.showToast({ title: '寄件单号已上传', icon: 'none', duration: 1500 })
      appStore.markOrderListScrollRefresherOnNextShow()
      await loadDetail()
    } catch {
      // 失败提示已在 http 层处理
    } finally {
      uni.hideLoading()
    }
  }

  /** 维修报价选填：空为未填；有内容则须为有效非负数 */
  const parseOptionalRepairQuoteAmount = (
    raw: string
  ): { ok: true; value?: number } | { ok: false } => {
    const s = String(raw ?? '').trim()
    if (!s) return { ok: true, value: undefined }
    const n = Number(s)
    if (!Number.isFinite(n) || n < 0) return { ok: false }
    return { ok: true, value: n }
  }

  /**
   * 维修完成（无故障）：先打开机器返回方式；填完关单原因后仅调关闭工单接口（不调 tech-accept）
   * @returns void
   */
  const onRepairComplete = () => {
    if (!canCloseWorkOrder.value) {
      uni.showToast({ title: '暂无工单关闭权限', icon: 'none' })
      return
    }
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }
    if (faultJudgeSelect.value !== '无故障') {
      uni.showToast({ title: '请选择「无故障」', icon: 'none' })
      return
    }
    const parsed = parseOptionalRepairQuoteAmount(repairQuoteInput.value)
    if (!parsed.ok) {
      uni.showToast({ title: '维修报价格式不正确', icon: 'none' })
      return
    }
    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    returnMethodType.value = ''
    closeOrderReturnMethodPayload.value = null
    pendingNoFaultRepairAfterReturnMethod.value = true
    showReturnMethodModal.value = true
  }

  /**
   * 无故障维修完成：携带返回方式数据调用维修员接单接口（tech-accept）
   */
  const submitCloseOrderWithReturnPayload = async (
    payload: ReturnMethodConfirmPayload,
    closeReason: string
  ) => {
    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    const cr = (closeReason || '').trim()
    if (!cr) {
      uni.showToast({ title: '请填写关闭原因（无故障必填）', icon: 'none' })
      return
    }

    const parsedQuote = parseOptionalRepairQuoteAmount(repairQuoteInput.value)
    if (!parsedQuote.ok) {
      uni.showToast({ title: '维修报价格式不正确', icon: 'none' })
      return
    }
    const qd = (quoteDescInput.value || '').trim()

    const base = {
      workOrderId: wid,
      faultJudge: '无故障',
      closeReason: cr,
      returnMethod: payload.type === 'self' ? '自提' : '回寄',
      ...(parsedQuote.value !== undefined ? { quoteAmount: parsedQuote.value } : {}),
      ...(qd ? { quoteDesc: qd } : {})
    } as const
    const dto =
      payload.type === 'mail'
        ? {
            ...base,
            ...(payload.mail.returnVoucherFileIds.length
              ? { returnVoucherFileIds: payload.mail.returnVoucherFileIds }
              : {})
          }
        : base

    uni.showLoading({ title: '提交中...' })
    try {
      const res = await techAcceptWorkOrder(dto)
      appStore.markOrderListScrollRefresherOnNextShow()
      closeOrderReturnMethodPayload.value = null
      uni.showToast({
        title: getApiMessage(res, '工单已关闭'),
        icon: 'none',
        duration: 1500
      })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    } catch {
      // techAcceptWorkOrder 内已 toast
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 确认机器返回方式
   * @param data 数据
   * @returns void
   */
  const onReturnMethodConfirm = async (data: ReturnMethodConfirmPayload) => {
    returnMethodType.value = data.type

    uni.showToast({
      title: `已选择${data.type === 'self' ? '自提' : '回寄'}`,
      icon: 'none'
    })
    if (pendingNoFaultRepairAfterReturnMethod.value) {
      pendingNoFaultRepairAfterReturnMethod.value = false
      closeOrderReturnMethodPayload.value = data
      setTimeout(() => {
        showCloseOrderModal.value = true
      }, 400)
    }
  }

  /**
   * 确认工单关闭（无故障：仅 PUT /api/system/work-order/close，携带返回方式与关闭原因）
   * @param reason 关闭原因
   * @returns void
   */
  const onCloseOrderConfirm = async (reason: string) => {
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }
    const payload = closeOrderReturnMethodPayload.value
    if (!payload) {
      uni.showToast({ title: '请先完成机器返回方式', icon: 'none' })
      return
    }
    await submitCloseOrderWithReturnPayload(payload, reason)
  }

  /**
   * 提交报价（有故障）：仅调维修员接单接口（tech-accept），成功后刷新详情进入后续维修登记流程
   * @returns void
   */
  const onSubmitQuote = async () => {
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }
    if (faultJudgeSelect.value !== '有故障') {
      uni.showToast({ title: '请选择「有故障」', icon: 'none' })
      return
    }
    const qd = (quoteDescInput.value || '').trim()
    const parsed = parseOptionalRepairQuoteAmount(repairQuoteInput.value)
    if (!parsed.ok) {
      uni.showToast({ title: '维修报价格式不正确', icon: 'none' })
      return
    }
    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    uni.showLoading({ title: '提交中...' })
    try {
      const res = await techAcceptWorkOrder({
        workOrderId: wid,
        faultJudge: '有故障',
        ...(parsed.value !== undefined ? { quoteAmount: parsed.value } : {}),
        ...(qd ? { quoteDesc: qd } : {})
      })
      appStore.markOrderListScrollRefresherOnNextShow()
      uni.showToast({ title: getApiMessage(res, '接单成功'), icon: 'none', duration: 1500 })
      await loadDetail()
    } catch {
      // api 内已 toast
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 提交故障点登记（POST `/api/system/work-order/repair`）或复检登记（POST `/api/system/work-order/review`）
   * @returns void
   */
  const onSubmitFaultPoint = async () => {
    if (!canOperateTransferredOrder.value) {
      uni.showToast({ title: '转出网点不可操作此工单', icon: 'none' })
      return
    }

    // 仅"佳士品牌且详情原始型号为空"时，提交维修登记/复检需先补录机器型号
    // 对齐 jasic-ui：此时弹 MachineModelSupplementModal，由用户从后端候选里选/手填后重新提交
    if (requireMachineModelForJiashi.value && !hasVal(order.value.product?.model)) {
      uni.showToast({ title: '请先补录机器型号', icon: 'none' })
      openMachineModelSupplement()
      return
    }

    const isRecheck = detailEntryAction.value === 'recheck'

    // 维修登记：有"故障与维修配置"时需校验 faultItems（对齐后端 WorkOrderServiceImpl.resolveRepairFaultSelectionForSaveRepair）
    const faultItemsTrimmed = (faultItemsSelect.value || [])
      .map((x) => String(x || '').trim())
      .filter(Boolean)
    const faultRemarkTrimmed = String(faultRemarkInput.value || '').trim()
    const hasRepairFaultConfig = (repairFaultOptions.value || []).length > 0
    if (!isRecheck && hasRepairFaultConfig) {
      if (faultItemsTrimmed.length === 0) {
        uni.showToast({ title: '请选择维修确认故障', icon: 'none' })
        return
      }
      if (faultItemsTrimmed.includes(OTHER_FAULT_LABEL) && !faultRemarkTrimmed) {
        uni.showToast({ title: '请填写其它故障说明', icon: 'none' })
        return
      }
    }

    const repairItems = repairDescSelect.value.map((x) => String(x || '').trim()).filter(Boolean)
    if (repairItems.length === 0) {
      uni.showToast({ title: '请选择维修说明', icon: 'none' })
      return
    }
    const hasOtherRepairDesc = repairItems.includes(OTHER_REPAIR_LABEL)
    if (hasOtherRepairDesc && !(otherRepairDesc.value || '').trim()) {
      uni.showToast({ title: '请输入其它维修说明', icon: 'none' })
      return
    }

    const rows = replaceParts.value ?? []
    const completePartRows: { part: string; qty: number }[] = []
    for (const r of rows) {
      const partName = String(r?.part ?? '').trim()
      const qtyStr = String(r?.quantity ?? '').trim()
      const qty = Number(qtyStr)
      if (!partName && !qtyStr) continue
      if (!partName || !qtyStr || Number.isNaN(qty) || qty <= 0) {
        uni.showToast({ title: '请完整填写每项配件名称与数量', icon: 'none' })
        return
      }
      completePartRows.push({ part: partName, qty })
    }

    const mediaLists = [
      faultOldImages.value,
      faultPointImages.value,
      machineFrontImages.value,
      machineBarcodeImages.value,
      otherImages.value
    ]
    for (const list of mediaLists) {
      if (hasUnuploadedMediaItems(list)) {
        uni.showToast({ title: '图片正在上传，请稍候再试', icon: 'none' })
        return
      }
    }

    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }

    // 组装 repairDesc：选项中文本 +（若勾选）其它说明；接口上 repairDesc / repairItems / partList / quote 均为可选
    const selectedRepairDescText = repairItems.filter((x) => x !== OTHER_REPAIR_LABEL).join('、')
    const repairDescFault = hasOtherRepairDesc
      ? [selectedRepairDescText, (otherRepairDesc.value || '').trim()].filter(Boolean).join('；')
      : selectedRepairDescText

    /** 维修登记：quoteAmount / quoteDesc 可选；有填写金额时须为合法非负数 */
    let repairQuoteAmount = 0
    let repairQuoteDescStr = ''
    if (!isRecheck) {
      repairQuoteDescStr = (quoteDescInput.value || '').trim()
      const quoteRaw = String(repairQuoteInput.value || '').trim()
      if (quoteRaw) {
        const quoteParsed = parseOptionalRepairQuoteAmount(repairQuoteInput.value)
        if (!quoteParsed.ok) {
          uni.showToast({ title: '维修报价格式不正确', icon: 'none' })
          return
        }
        repairQuoteAmount = quoteParsed.value !== undefined ? quoteParsed.value : 0
      }
    }

    uni.showLoading({ title: '提交中...' })
    try {
      const reviewDto: WorkOrderReviewDTO = {
        faultNewImageFileIds: collectVoucherFileIds(asUnknownArray(faultPointImages.value)),
        faultOldImageFileIds: collectVoucherFileIds(asUnknownArray(faultOldImages.value)),
        machineBarcodeImageFileIds: collectVoucherFileIds(
          asUnknownArray(machineBarcodeImages.value)
        ),
        machineImageFileIds: collectVoucherFileIds(asUnknownArray(machineFrontImages.value)),
        otherDesc: hasOtherRepairDesc ? (otherRepairDesc.value || '').trim() : '',
        otherImageFileIds: collectVoucherFileIds(asUnknownArray(otherImages.value)),
        partList: completePartRows.map((r) => ({
          partName: r.part,
          partQty: r.qty
        })),
        repairDesc: repairDescFault,
        repairItems,
        workOrderId: wid
      }

      const res = isRecheck
        ? await reviewWorkOrder(reviewDto)
        : await repairWorkOrder({
            workOrderId: wid,
            // 有配置时传筛选出的故障项；无配置时按后端 fallback 依然传空数组（后端走 buildFallbackRepairFaultSelection 忽略）
            faultItems: hasRepairFaultConfig ? faultItemsTrimmed : [],
            faultRemark:
              hasRepairFaultConfig && faultItemsTrimmed.includes(OTHER_FAULT_LABEL)
                ? faultRemarkTrimmed
                : '',
            faultOldImageFileIds: collectVoucherFileIds(asUnknownArray(faultOldImages.value)),
            faultNewImageFileIds: collectVoucherFileIds(asUnknownArray(faultPointImages.value)),
            machineImageFileIds: collectVoucherFileIds(asUnknownArray(machineFrontImages.value)),
            machineBarcodeImageFileIds: collectVoucherFileIds(
              asUnknownArray(machineBarcodeImages.value)
            ),
            otherImageFileIds: collectVoucherFileIds(asUnknownArray(otherImages.value)),
            otherDesc: hasOtherRepairDesc ? (otherRepairDesc.value || '').trim() : '',
            partList: completePartRows.map((r) => ({
              partName: r.part,
              partQty: r.qty
            })),
            quoteAmount: repairQuoteAmount,
            quoteDesc: repairQuoteDescStr,
            repairDesc: repairDescFault,
            repairItems
          })
      appStore.markOrderListScrollRefresherOnNextShow()
      uni.showToast({
        title: getApiMessage(res, isRecheck ? '复检登记已提交' : '登记成功'),
        icon: 'none',
        duration: 1500
      })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    } catch {
      // 失败提示已在 http / submit 中处理
    } finally {
      uni.hideLoading()
    }
  }
</script>

<style lang="scss" scoped>
  /* 工单详情（pages/order/detail）— 根节点需加 class order-detail-page */
  .order-detail-page {
    /* 勿写 min-height:100vh：上方还有 CustomNavBar 占位，与 100vh 叠加会使整页高度 > 视口，短内容也会出现滚动条 */
    width: 100%;
    box-sizing: border-box;
    background-color: $bg-page;
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

    .tab-container {
      @include sheet-white;
    }

    .tab-bar {
      display: flex;
      justify-content: space-around;
      padding: 0 $space-lg;
      border-bottom: 2rpx solid $bg-light;

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
