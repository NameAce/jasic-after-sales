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
    <view class="main-content" :class="{ 'main-content--with-bottom-bar': detailHasBottomBar }">
      <view
        class="content-wrap"
        :class="{
          /* 维修过程 Tab 内无 section 时（仅 tab 条）去掉与下方 Extra 的 gap，与 hasRepairProcessContent 一致 */
          'content-wrap--repair-tab-tight': currentTab === 1 && !hasRepairProcessContent
        }"
      >
        <!-- 标签容器 -->
        <view class="tab-container">
          <!-- 标签栏 -->
          <view class="tab-bar">
            <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">
              <text class="tab-text">申请内容</text>
            </view>
            <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">
              <text class="tab-text">维修过程</text>
            </view>
            <view
              v-if="showEvaluateTab"
              class="tab-item"
              :class="{ active: currentTab === 2 }"
              @click="currentTab = 2"
            >
              <text class="tab-text">客户评价</text>
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
              :show-repair-history-link="!detailViewOnly"
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
              :recheck-confirm-fault-items="recheckConfirmFaultItems"
            />

            <!-- 故障点信息（复检编辑时不展示，避免与上方登记表单重复） -->
            <OrderDetailFaultPointCard
              v-if="(isCompleted || isClosed) && hasFaultPoint && detailEntryAction !== 'recheck'"
              :as-card="false"
              :show-repair-history-link="!detailViewOnly"
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

        <!-- Tab 外区块：单独容器用 gap 排版，避免 WXSS 不支持通配符 *（> * + * 会编译失败） -->
        <view class="content-wrap-siblings">
          <!-- 申请内容 Extra Cards -->
          <template v-if="currentTab === 0">
            <OrderDetailProductCard
              :product="order.product"
              :need-supplement="!detailViewOnly && needSupplementMachineModel"
              @supplement="openMachineModelSupplement"
            />
            <OrderDetailServiceCard
              :service="order.service"
              :acceptor-site-name="order.acceptor.currentAcceptCompanyName"
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
                :show-repair-history-link="!detailViewOnly"
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
                v-if="
                  !detailViewOnly &&
                  canEditFaultJudge &&
                  faultJudgeSelect === '无故障' &&
                  canCloseWorkOrder
                "
              >
                <view class="btn btn-primary action-wrap" @click="onRepairComplete">
                  <image class="btn-icon" :src="completeCheckIcon" mode="aspectFit" />维修完成
                </view>
              </base-button>
              <base-button
                v-if="!detailViewOnly && canEditFaultJudge && faultJudgeSelect === '有故障'"
              >
                <view class="btn btn-primary action-wrap" @click="onSubmitQuote">
                  <image class="btn-icon" :src="submitQuoteIcon" mode="aspectFit" />提交报价
                </view>
              </base-button>
            </template>
            <!-- 维修中 -->
            <template v-else-if="repairExtrasLayout === 'active_repair'">
              <!-- 维修/复检登记默认在「维修过程」Tab，补录入口需在此区重复展示（与 jasic-ui 先补机型再登记一致） -->
              <OrderDetailProductCard
                v-if="!detailViewOnly && needSupplementMachineModel"
                :product="order.product"
                :need-supplement="true"
                @supplement="openMachineModelSupplement"
              />
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
                :show-repair-history-link="!detailViewOnly"
                history-title="最近维修记录"
                record-label="最近一次维修"
                :repair-faults="order.faultPoint.currentFaults"
                :history-records="order.faultPoint.allRepairsFaultRecords"
                :repair-time-fallback="order.faultPoint.current.date"
                :order-id="orderNavId"
                :flow-items="order.processFlows"
              />

              <base-button v-if="!detailViewOnly && canEditFaultPoint">
                <view class="btn btn-primary action-wrap" @click="onSubmitFaultPoint">
                  <image class="btn-icon" :src="saveIcon" mode="aspectFit" />
                  {{ detailEntryAction === 'recheck' ? '提交复检登记' : '提交维修登记' }}
                </view>
              </base-button>
            </template>
            <!-- 已维修完成 -->
            <template v-else-if="repairExtrasLayout === 'readonly_summary'">
              <OrderDetailRepairMetaCard :order="order" />
              <OrderDetailAcceptorCard :acceptor="order.acceptor" />
              <OrderDetailFaultCard :fault="order.fault" />
            </template>
          </template>
        </view>
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

    <!-- 机型补录弹窗：维修登记无机型时自动弹出/可手动补录；复检仅佳士无机型时 -->
    <MachineModelSupplementModal
      v-model:visible="showMachineModelSupplement"
      :work-order-id="machineModelSupplementWorkOrderId"
      @confirm="onMachineModelSupplementConfirm"
      @cancel="onMachineModelSupplementCancel"
    />

    <!-- 待派单：全 Tab 底部固定派单（样式与维修登记「提交维修登记」一致） -->
    <base-button v-if="showDetailAssignButton">
      <view class="btn btn-primary action-wrap" @click="openAssignModal">
        <image class="btn-icon" :src="saveIcon" mode="aspectFit" />派单
      </view>
    </base-button>

    <AssignTechnicianModal
      v-model="showAssignModal"
      v-model:selected-tech-id="selectedAssignTechId"
      :assign-work-order-id="currentAssignWorkOrderId"
      :technician-list="assignTechnicianList"
      @close="closeAssignModal"
      @confirm="onAssignConfirm"
    />
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch, nextTick } from 'vue'
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
  import AssignTechnicianModal, {
    type Technician
  } from '@/components/AssignTechnicianModal/AssignTechnicianModal.vue'
  import {
    assignWorkOrder,
    getWorkOrder,
    listAssignUserOptions,
    listRepairFaultOptions,
    listRepairProductModelOptions,
    repairWorkOrder,
    reviewWorkOrder,
    techAcceptWorkOrder,
    updateRepairProductModel,
    type ReturnMethodConfirmPayload,
    type WorkOrderRepairDTO,
    type WorkOrderRepairFaultOptionVO,
    type WorkOrderReviewDTO
  } from '@/api/workOrder'
  import {
    cloneOrderDetail,
    createEmptyOrderDetail,
    getReturnMethodInitialMail,
    type OrderDetail,
    type SysFileItemVO,
    type WorkOrderMainStatus
  } from '@/models/order'
  import { useAppStore, useUserStore } from '@/stores'
  import { isOrderStatus } from '@/utils/orderStatus'
  import { isWorkOrderPendingTechAcceptMainStatus } from '@/utils/workOrderMainStatus'
  import { Perms } from '@/utils/permissions'
  import { getApiMessage } from '@/utils/http'
  import { takeSelectedShippingAddress } from '@/utils/addressStorage'
  import { hasVal } from '@/utils/value'
  import { resolvePreviewableUrl } from '@/utils/mediaPreview'
  import {
    asUnknownArray,
    collectVoucherFileIds,
    hasUnuploadedMediaItems
  } from '@/utils/workOrderFileIds'
  import { completeCheckIcon, saveIcon, submitQuoteIcon } from '@/svgs'
  import {
    isDetailEntryAction,
    shouldOpenRepairTab,
    useOrderDetailPage,
    type DetailEntryAction
  } from './useOrderDetailPage'
  import type { WorkOrderActionKey } from '@/constants/orderActions'

  const appStore = useAppStore()
  const userStore = useUserStore()

  /** 无故障「维修完成」会走机器返回方式并关单，需工单关闭权限 */
  const canCloseWorkOrder = computed(() => userStore.hasPermission(Perms.WORKORDER_CLOSE))

  const OTHER_REPAIR_LABEL = '其它维修说明'

  /**
   * 复检回显：按 、,，;； 拆成多选（如 `3;33` 勾 `3` 与 `33`），保留原文、不做配置改写
   */
  const splitEchoRepairItemStrings = (items: string[]) => {
    const sep = /[、,，;；]+/
    const out: string[] = []
    const seen = new Set<string>()
    for (const raw of items) {
      for (const seg of String(raw || '').split(sep)) {
        const t = String(seg || '').trim()
        if (!t || seen.has(t)) continue
        seen.add(t)
        out.push(t)
      }
    }
    return out
  }

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
  /** 列表「已转单」Tab 点入：仅查看，不展示操作按钮与派单底栏 */
  const detailViewOnly = ref(false)

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
  /**
   * 详情加载后快照：当前入口是否走「须补录机型」策略。
   * - 维修登记：无机型即须补录（任意品牌）
   * - 复检登记：仅佳士且无机型时须补录
   */
  const machineModelSupplementRequired = ref(false)
  /** 机型补录弹窗显隐 */
  const showMachineModelSupplement = ref(false)
  /** 机型补录弹窗锁定的工单ID（打开弹窗瞬间快照） */
  const machineModelSupplementWorkOrderId = ref(0)
  /** 进入「维修登记」补录流程时，自动弹窗只触发一次 */
  const machineModelAutoOpened = ref(false)

  const needSupplementMachineModel = computed(() => {
    if (detailEntryAction.value !== 'repair' && detailEntryAction.value !== 'recheck') {
      return false
    }
    if (!machineModelSupplementRequired.value) return false
    return !hasVal(order.value.product?.model)
  })

  /** 维修确认故障多选（对应后端 WorkOrderRepairDTO.faultItems） */
  const faultItemsSelect = ref<string[]>([])
  /** 其它故障说明（faultItems 含「其它故障」时必填） */
  const faultRemarkInput = ref('')
  /** 与后端 WorkOrderServiceImpl.OTHER_FAULT_LABEL / 登记表单一致 */
  const OTHER_FAULT_LABEL = '其它故障'
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
   * 复检登记只读「维修确认故障」：优先与 repairRegistrationEcho 同源（最近一次非复检维修 faults），
   * 避免误用全量历史记录第 0 条。
   */
  const firstRepairFaultDescText = computed(() => {
    const items = order.value.repairRegistrationEcho?.confirmFaultItems
    if (items?.length) return items.join('、')
    const first = order.value.faultPoint?.allRepairsFaultRecords?.[0]
    return String(first?.faultDesc || '').trim()
  })
  const firstRepairFaultRemarkText = computed(() => {
    const fromEcho = String(
      order.value.repairRegistrationEcho?.confirmFaultOtherRemark || ''
    ).trim()
    if (fromEcho) return fromEcho
    const first = order.value.faultPoint?.allRepairsFaultRecords?.[0]
    const faultDesc = String(first?.faultDesc || '')
    const otherDesc = String(first?.otherDesc || '').trim()
    if (!otherDesc) return ''
    const fd = faultDesc.trim()
    if (fd.includes('其它故障') || fd.includes('其他故障') || fd === '其它' || fd === '其他') {
      return otherDesc
    }
    return ''
  })

  /** 复检：维修说明下拉过滤用的确认故障项列表 */
  const recheckConfirmFaultItems = computed(() => {
    const items = order.value.repairRegistrationEcho?.confirmFaultItems
    if (items?.length) return items
    const t = String(order.value.faultPoint?.allRepairsFaultRecords?.[0]?.faultDesc || '').trim()
    if (!t) return []
    return t
      .split(/[、,，;；]+/)
      .map((s) => s.trim())
      .filter(Boolean)
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

  /** 与列表 `isOrderPendingAssign` 一致：真·待派单（非 mainStatus 已进待接单） */
  const isOrderDetailPendingAssign = (d: OrderDetail) =>
    d.status === 'PENDING_ASSIGN' && !isWorkOrderPendingTechAcceptMainStatus(d.mainStatus)

  /** 派单权限下：已指派给他人则仅查看（与 list.vue `isDispatcherOrderAssignedToOther` 一致） */
  const dispatcherDetailAssignedToOther = computed(() => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    const aid = order.value.assignedUserId
    if (aid === undefined || aid === null) return false
    const assigned = Number(aid)
    if (!Number.isFinite(assigned) || assigned <= 0) return false
    const selfId = userStore.userInfo?.id
    if (!Number.isFinite(Number(selfId))) return false
    return assigned !== Number(selfId)
  })

  /**
   * 待派单：各 Tab 底部展示「派单」。
   * - 与列表一致：`availableActions` 含 ASSIGN 优先；无动作列表时按待派单状态兜底。
   * - `action=accept` 为接单填写入口，底部已有报价/完成按钮，避免双 fixed 条重叠故不展示派单。
   */
  const showDetailAssignButton = computed(() => {
    if (detailViewOnly.value) return false
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    if (detailEntryAction.value === 'accept') return false
    if (!canOperateTransferredOrder.value) return false
    if (dispatcherDetailAssignedToOther.value) return false
    const acts = order.value.availableActions
    if (Array.isArray(acts) && acts.length > 0) {
      return (acts as WorkOrderActionKey[]).includes('ASSIGN')
    }
    return isOrderDetailPendingAssign(order.value)
  })

  /** 主内容底部留白：维修过程底栏 + 全 Tab 派单底栏 */
  const detailHasBottomBar = computed(
    () => !detailViewOnly.value && (hasBottomActionBar.value || showDetailAssignButton.value)
  )

  const showAssignModal = ref(false)
  const currentAssignWorkOrderId = ref<string | number>('')
  const selectedAssignTechId = ref<number | string | null>(null)
  const assignTechnicianList = ref<Technician[]>([])

  const closeAssignModal = () => {
    showAssignModal.value = false
    currentAssignWorkOrderId.value = ''
    selectedAssignTechId.value = null
    assignTechnicianList.value = []
  }

  const openAssignModal = () => {
    const openedFor = orderNavId.value.trim()
    if (!openedFor) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    currentAssignWorkOrderId.value = openedFor
    showAssignModal.value = true
    selectedAssignTechId.value = null

    assignTechnicianList.value = []
    const workOrderId = Number(openedFor)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) return
    listAssignUserOptions(workOrderId)
      .then((list) => {
        if (String(currentAssignWorkOrderId.value).trim() !== openedFor) return
        const selfId = Number(userStore.userInfo?.id)
        const mapped: Technician[] = list.map((u) => ({
          id: u.id,
          name:
            Number(u.id) === selfId
              ? `${u.realName || u.phone || `用户${u.id}`}（本人）`
              : u.realName || u.phone || `用户${u.id}`,
          phone: u.phone || '',
          avatar: '',
          desc: u.phone || '',
          isRecommend: false,
          distance: '',
          time: '',
          isBusy: false
        }))
        assignTechnicianList.value = mapped
      })
      .catch(() => {})
  }

  const onAssignConfirm = async (payload: {
    workOrderId: string | number
    selectedTechId: number | string
  }) => {
    const workOrderId = Number(payload.workOrderId ?? currentAssignWorkOrderId.value)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    const assignedUserId = Number(payload?.selectedTechId)
    if (!Number.isFinite(assignedUserId) || assignedUserId <= 0) {
      uni.showToast({ title: '维修员ID无效', icon: 'none' })
      return
    }
    const selfId = Number(userStore.userInfo?.id)
    const isSelf = Number.isFinite(selfId) && selfId > 0 && assignedUserId === selfId
    try {
      const res = await assignWorkOrder({ workOrderId, assignedUserId })
      if (isSelf) {
        uni.showToast({ title: '已派单给自己，可在「待接单」中接单', icon: 'none' })
      } else {
        uni.showToast({ title: getApiMessage(res, '派单成功'), icon: 'none', duration: 1500 })
      }
      closeAssignModal()
      await nextTick()
      await loadDetail()
    } catch {
      // assignWorkOrder / http 内已 toast
    }
  }

  // ==================== 生命周期 ====================

  /**
   * 维修登记入口：将客户申请中的故障描述（详情 `fault.desc` / `fault.faultExplain`）
   * 映射为「维修确认故障」多选初值；与 `OrderDetailFaultCard` 展示口径一致。
   * 仅在 faultItems 尚未选择时由 loadDetail 调用。
   */
  const deriveFaultItemsPrefillFromCustomerFault = (
    faultDescRaw: string | undefined,
    faultExplainRaw: string | undefined,
    options: WorkOrderRepairFaultOptionVO[],
    otherLabel: string
  ): { items: string[]; remark: string } => {
    const labelsOrdered = (Array.isArray(options) ? options : [])
      .map((x) => String(x?.faultDesc ?? '').trim())
      .filter(Boolean)
    if (!labelsOrdered.length) return { items: [], remark: '' }

    const labelSet = new Set(labelsOrdered)
    if (!labelSet.has(otherLabel)) {
      labelSet.add(otherLabel)
    }

    const desc = String(faultDescRaw ?? '').trim()
    const explain = String(faultExplainRaw ?? '').trim()

    const isOtherFaultToken = (p: string) =>
      p === '其它故障' || p === '其他故障' || p === otherLabel || p === '其它' || p === '其他'

    const asOther = (): { items: string[]; remark: string } => ({
      items: [otherLabel],
      remark: explain
    })

    if (!desc) {
      if (!explain) return { items: [], remark: '' }
      return asOther()
    }

    if (desc === '其它故障' || desc === '其他故障') {
      return asOther()
    }

    const parts = desc
      .split(/[、,，;；]+/)
      .map((s) => s.trim())
      .filter(Boolean)

    const matched: string[] = []
    for (const p of parts) {
      if (isOtherFaultToken(p)) {
        matched.push(otherLabel)
        continue
      }
      if (labelSet.has(p)) {
        matched.push(p)
      }
    }

    const uniq = [...new Set(matched)]
    if (uniq.length) {
      const remark = uniq.includes(otherLabel)
        ? explain || parts.filter(isOtherFaultToken).join('、') || (desc === otherLabel ? desc : '')
        : ''
      return { items: uniq, remark: String(remark || '').trim() }
    }

    if (labelSet.has(desc)) {
      if (isOtherFaultToken(desc)) {
        return { items: [otherLabel], remark: explain }
      }
      return { items: [desc], remark: '' }
    }

    return {
      items: [otherLabel],
      remark: [desc, explain].filter(Boolean).join('；')
    }
  }

  /** 复检登记：把最近一次维修登记的选项/说明/配件/图片回显到登记表单 */
  const applyRecheckRepairRegistrationEcho = (echo: OrderDetail['repairRegistrationEcho']) => {
    if (!echo) return

    if (echo.repairItems.length) {
      repairDescSelect.value = splitEchoRepairItemStrings(
        echo.repairItems.map((x) => String(x ?? ''))
      )
    }

    const od = (echo.otherDesc || '').trim()
    if (od) otherRepairDesc.value = od
    if (od && !repairDescSelect.value.includes(OTHER_REPAIR_LABEL)) {
      repairDescSelect.value = [...repairDescSelect.value, OTHER_REPAIR_LABEL]
    }

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
    detailViewOnly.value = String(options?.viewOnly ?? '').trim() === '1'
    // 设置工单状态
    if (isOrderStatus(options?.status)) {
      orderStatus.value = options.status
    }

    // 设置接单操作（纯查看入口不读取 action，避免改 URL 带出操作区）
    if (!detailViewOnly.value && isDetailEntryAction(options?.action)) {
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
      // 维修登记：无机型即须补录；复检：沿用佳士无机型须补录（与 jasic-ui 复检一致）
      const repairOrRecheck =
        detailEntryAction.value === 'repair' || detailEntryAction.value === 'recheck'
      const missingModel = !hasVal(detail.product?.model)
      machineModelSupplementRequired.value =
        repairOrRecheck &&
        missingModel &&
        (detailEntryAction.value === 'repair' || !!detail.brand?.isJiashi)

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

      // 维修登记：客户申请时填过的故障描述 → 回显到「维修确认故障」（未手动选过 faultItems 时）
      if (
        detailEntryAction.value === 'repair' &&
        detail.status === 'IN_PROGRESS' &&
        (repairFaultOptions.value || []).length > 0 &&
        (faultItemsSelect.value || []).length === 0
      ) {
        const pre = deriveFaultItemsPrefillFromCustomerFault(
          detail.fault?.desc,
          detail.fault?.faultExplain,
          repairFaultOptions.value,
          OTHER_FAULT_LABEL
        )
        if (pre.items.length) {
          faultItemsSelect.value = pre.items
          faultRemarkInput.value = pre.items.includes(OTHER_FAULT_LABEL) ? pre.remark : ''
        }
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
      // 须补录机型时：自动弹窗（各入口策略见 machineModelSupplementRequired）
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
          if (
            (repairEntry || recheckEntry) &&
            needSupplementMachineModel.value &&
            !machineModelAutoOpened.value
          ) {
            machineModelAutoOpened.value = true
            void openMachineModelSupplement()
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

  /**
   * 打开机型补录弹窗（jasic-ui：无启用机型时不可打开，需先维护故障与维修配置）
   */
  const openMachineModelSupplement = async () => {
    const wid = resolveWorkOrderId()
    if (!wid) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    try {
      uni.showLoading({ title: '加载中...', mask: true })
      const options = await listRepairProductModelOptions(wid, { keyword: '' })
      if (!options.length) {
        uni.showToast({
          title: '当前归属总部未配置启用机型，请先维护故障与维修配置',
          icon: 'none'
        })
        return
      }
    } catch {
      // listRepairProductModelOptions 内可能已 toast
      return
    } finally {
      uni.hideLoading()
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
      uni.showToast({ title: '机器型号补录成功', icon: 'none' })
    } catch {
      // updateRepairProductModel 内已 toast
    } finally {
      uni.hideLoading()
    }
  }

  const onMachineModelSupplementCancel = () => {
    // 取消：仍缺机型且当前入口要求补录时，提交会再次拦截并可再次打开弹窗
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

    if (needSupplementMachineModel.value) {
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

    // 手工填写的 repairDesc：仅当勾选了「其它维修说明」时与 otherDesc 同文案；其余说明只在 repairItems 里传选项
    const repairDescForApi = hasOtherRepairDesc ? (otherRepairDesc.value || '').trim() : ''

    /**
     * 与 POST `/api/system/work-order/repair` 的 WorkOrderRepairDTO 一致；未改价时从详情回退当前有效报价，避免误传 0。
     */
    let repairQuoteForSubmit: { quoteAmount: number; quoteDesc: string } | undefined
    if (!isRecheck) {
      const quoteDescTrim = (quoteDescInput.value || '').trim()
      const fromDetailDesc = String(order.value.repair?.quoteDesc ?? '').trim()
      const qFromInput = parseOptionalRepairQuoteAmount(String(repairQuoteInput.value || '').trim())
      if (!qFromInput.ok) {
        uni.showToast({ title: '维修报价格式不正确', icon: 'none' })
        return
      }
      const qFromOrder = parseOptionalRepairQuoteAmount(
        String(order.value.repair?.quoteAmount ?? '')
      )
      if (!qFromOrder.ok) {
        uni.showToast({ title: '维修报价格式不正确', icon: 'none' })
        return
      }
      const amount =
        qFromInput.value !== undefined
          ? qFromInput.value
          : qFromOrder.value !== undefined
            ? qFromOrder.value
            : 0
      const desc = quoteDescTrim || fromDetailDesc
      repairQuoteForSubmit = { quoteAmount: amount, quoteDesc: desc }
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
        repairDesc: repairDescForApi,
        repairItems,
        workOrderId: wid
      }

      const res = isRecheck
        ? await reviewWorkOrder(reviewDto)
        : await repairWorkOrder({
            faultItems: hasRepairFaultConfig ? faultItemsTrimmed : [],
            faultNewImageFileIds: collectVoucherFileIds(asUnknownArray(faultPointImages.value)),
            faultOldImageFileIds: collectVoucherFileIds(asUnknownArray(faultOldImages.value)),
            faultRemark:
              hasRepairFaultConfig && faultItemsTrimmed.includes(OTHER_FAULT_LABEL)
                ? faultRemarkTrimmed
                : '',
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
            quoteAmount: repairQuoteForSubmit!.quoteAmount,
            quoteDesc: repairQuoteForSubmit!.quoteDesc,
            repairDesc: repairDescForApi,
            repairItems,
            workOrderId: wid
          } satisfies WorkOrderRepairDTO)
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

      /* 维修过程：Tab 内无 section 时，与 tab 区之间不再用外层 gap，避免与首块内边距叠加成双段留白 */
      &--repair-tab-tight {
        gap: 0;
      }
    }

    .content-wrap-siblings {
      @include flex-col;
      gap: $space-lg;
      width: 100%;
      box-sizing: border-box;
    }

    .tab-container {
      background-color: $bg-card;
      border-radius: $radius-lg $radius-lg 0 0;
      overflow: hidden;
      box-shadow: 0 2rpx 12rpx rgba(15, 23, 42, 0.04);
    }

    .tab-bar {
      display: flex;
      justify-content: space-around;
      align-items: stretch;
      padding: 0;
      border-bottom: 1rpx solid $bg-light;

      .tab-item {
        position: relative;
        flex: 1;
        padding: 32rpx $space-sm 20rpx;
        text-align: center;
        @include flex-col;
        align-items: center;
        transition: color 0.2s;

        .tab-text {
          font-size: $font-md;
          font-weight: 500;
          line-height: 1.2;
          color: $text-slate-800;
        }

        &.active {
          .tab-text {
            color: $primary;
            font-weight: 600;
          }

          &::after {
            content: '';
            position: absolute;
            left: 50%;
            bottom: 0;
            width: 72rpx;
            height: 6rpx;
            margin-left: -36rpx;
            background-color: $primary;
            border-radius: 3rpx 3rpx 0 0;
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
