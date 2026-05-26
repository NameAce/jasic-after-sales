<template>
  <view class="page-index order-list-page">
    <!-- 维修工程师/派单员视图 -->
    <CustomNavBar title="工单列表" surface="sticky" :shadow="false" :show-back="false">
      <!-- 搜索栏 -->
      <view class="search-wrap">
        <view class="search-box">
          <uni-icons
            type="search"
            size="18"
            :color="themeColors.textMuted"
            class="search-icon"
          ></uni-icons>
          <view class="search-input-wrap">
            <input
              :value="searchQuery"
              class="search-input"
              :class="{ 'search-input--with-clear': showSearchClear }"
              :placeholder="searchInputPlaceholder"
              placeholder-class="placeholder-text"
              confirm-type="search"
              @input="onSearchInput"
              @blur="onSearchInputBlur"
            />
            <view
              v-if="showSearchClear"
              class="search-clear-hit"
              @touchstart.stop.prevent="onSearchClear"
              @click.stop="onSearchClear"
            >
              <uni-icons type="closeempty" size="18" :color="themeColors.textMuted" />
            </view>
          </view>
        </view>

        <!-- 一级 Tab 栏 -->
        <view class="tabs-primary-wrap">
          <TabBar
            variant="pill"
            :tabs="primaryTabs"
            :model-value="primaryTab"
            :scrollable="false"
            @change="(val) => setPrimaryTab(val as PrimaryTab)"
          />
        </view>

        <!-- 二级 Tab 栏（总部"网点工单"时不显示）；scroll-into-view 使靠后的 Tab（如已关闭）滚入可视区 -->
        <TabBar
          v-if="!showBranchView"
          class="tabs-secondary"
          variant="underline"
          tone="list"
          padding="sm"
          :tabs="secondaryTabs"
          :model-value="secondaryTab"
          :scroll-into-view="secondaryTabScrollIntoView"
          id-prefix="order-list-sec-"
          :scrollable="true"
          @change="(val) => setSecondaryTab(val as SecondaryTab)"
        />
      </view>
    </CustomNavBar>

    <!-- 总部 - 网点工单视图 -->
    <scroll-view
      v-if="showBranchView"
      class="main-content"
      scroll-y
      lower-threshold="120"
      refresher-enabled
      :refresher-triggered="branchViewRefresherTriggered"
      @refresherrefresh="onBranchViewRefresherRefresh"
      @refresherrestore="onBranchViewRefresherRestore"
      @scrolltolower="loadMoreBranches"
    >
      <!-- 网点统计概览 -->
      <view class="branch-summary-header">
        <text class="branch-summary-title">网点统计概览</text>
        <text class="branch-summary-count">共 {{ branchTotalCount }} 个分中心</text>
      </view>
      <!-- 网点列表 -->
      <view class="branch-list-container">
        <view v-for="branch in branchDisplayList" :key="branch.id" class="branch-card">
          <view class="branch-card-header" @tap="goToBranchDetail(branch, 'all')">
            <view class="branch-info">
              <view class="branch-icon-wrap">
                <image class="branch-icon" :src="storeIcon" mode="aspectFit" />
              </view>
              <text class="branch-name">{{ branch.name }}</text>
            </view>
            <uni-icons type="right" size="18" :color="themeColors.iconSlateLight"></uni-icons>
          </view>
          <!-- 网点工单统计 -->
          <view class="branch-stats">
            <view class="stat-item stat-total" @tap.stop="goToBranchDetail(branch, 'all')">
              <text class="stat-label">总工单</text>
              <text class="stat-value">{{ branch.total }}</text>
            </view>
            <view class="stat-item stat-pending" @tap.stop="goToBranchDetail(branch, 'pending')">
              <text class="stat-label">待接单</text>
              <text class="stat-value">{{ branch.pending }}</text>
            </view>
            <view
              class="stat-item stat-processing"
              @tap.stop="goToBranchDetail(branch, 'processing')"
            >
              <text class="stat-label">维修中</text>
              <text class="stat-value">{{ branch.processing }}</text>
            </view>
            <view
              class="stat-item stat-completed"
              @tap.stop="goToBranchDetail(branch, 'completed')"
            >
              <text class="stat-label">已完成</text>
              <text class="stat-value">{{ branch.completed }}</text>
            </view>
          </view>
        </view>
      </view>
      <ListNoMore v-if="branchDisplayList.length > 0 && hasLoadedAllBranches" />
    </scroll-view>

    <!-- 工单列表视图 -->
    <scroll-view
      v-else
      class="main-content order-list-scroll"
      scroll-y
      lower-threshold="120"
      refresher-enabled
      :refresher-triggered="orderListRefresherTriggered"
      @refresherrefresh="onOrderListRefresherRefresh"
      @refresherrestore="onOrderListRefresherRestore"
      @scrolltolower="loadMoreOrders"
    >
      <OrderCardList
        :orders="orderList"
        :status-text="listStatusText"
        :empty-title="listEmptyTitle"
        :empty-desc="listEmptyDesc"
        :show-inbound-transfer-tag="showInboundTransferTag"
        :show-transferred-tag="(order) => !!order.transferred && !isHqUser && !isHistoryListView"
        :show-repair-site-rows="false"
        :show-no-more="orderList.length > 0 && hasLoadedAll"
        @order-click="onOrderClick"
      >
        <!-- 额外信息（维修方式由 OrderCardList 正文区统一展示） -->
        <template #extra-info="{ order }">
          <view v-if="getAssignedUserName(order)" class="info-item">
            <text class="label">当前维修人员</text>
            <text class="value value-repair-assignee">{{ getAssignedUserName(order) }}</text>
          </view>
          <view v-if="isHqProcessView && order.source" class="info-item">
            <text class="label">申请来源</text>
            <text class="value">{{ order.source }}</text>
          </view>
          <view v-if="isHqProcessView && order.transferNetwork" class="info-item">
            <text class="label">被转单网点</text>
            <text class="value">{{ order.transferNetwork }}</text>
          </view>
        </template>
        <!-- 操作：当前处理按 availableActions 渲染；历史转出仅展示「上传寄件单号」例外动作 -->
        <template #actions="{ order }">
          <view v-if="resolveListRowActions(order).length > 0" class="action-wrap">
            <button
              v-for="action in resolveListRowActions(order)"
              :key="`${order.id}-${action.key}`"
              :class="`btn-action ${action.className}`"
              @tap.stop="dispatchWorkOrderAction(action.key, order.id)"
            >
              {{ action.label }}
            </button>
          </view>
        </template>
      </OrderCardList>
    </scroll-view>

    <!-- 转单弹窗（派单员使用） -->
    <TransferModal
      v-model="showTransferModal"
      v-model:selected-network="selectedNetwork"
      v-model:reason="transferReason"
      :network-list="networkList"
      @confirm="onTransferConfirm"
    />

    <!-- 派单弹窗（派单员使用） -->
    <AssignTechnicianModal
      v-model="showAssignModal"
      v-model:selected-tech-id="selectedTechId"
      :assign-work-order-id="currentOrderId"
      :technician-list="technicianList"
      @close="closeAssignModal"
      @confirm="onAssignConfirm"
    />
    <!-- 机器返回方式弹窗 -->
    <ReturnMethodModal
      v-model="showReturnMethodModal"
      :initial-mail="returnMethodInitialMailMerged"
      @confirm="onReturnMethodConfirm"
    />

    <!-- 工单关闭弹窗 -->
    <CloseOrderModal
      v-model="showCloseOrderModal"
      no-fault-required
      @confirm="onCloseOrderConfirm"
    />

    <!-- 上传寄件单号弹窗（UPLOAD_SEND_EXPRESS 动作） -->
    <UploadSendExpressModal
      v-model:visible="showUploadSendExpressModal"
      :work-order-id="currentUploadSendExpressOrderId"
      @confirm="onUploadSendExpressConfirm"
    />
  </view>
</template>

<script setup lang="ts">
  /**
   * 工单库：路由仅要求登录；列表 Tab 与操作按钮用 Perms + userStore.hasPermission / canAny / canAll。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  import { ref, computed, nextTick, watch } from 'vue'
  import { onLoad, onShow, onHide } from '@dcloudio/uni-app'
  import { themeColors } from '@/theme/colors'
  import { useAppStore, useUserStore } from '@/stores'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import TabBar from '@/components/TabBar/TabBar.vue'
  import OrderCardList from '@/components/OrderCardList/OrderCardList.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import TransferModal from '@/components/TransferModal/TransferModal.vue'
  import AssignTechnicianModal, {
    type Technician
  } from '@/components/AssignTechnicianModal/AssignTechnicianModal.vue'
  import ReturnMethodModal from '@/components/ReturnMethodModal/ReturnMethodModal.vue'
  import CloseOrderModal from '@/components/CloseOrderModal/CloseOrderModal.vue'
  import UploadSendExpressModal from '@/components/UploadSendExpressModal/UploadSendExpressModal.vue'
  import {
    assignWorkOrder,
    listHqSiteSummary,
    listAssignUserOptions,
    getWorkOrder,
    listWorkOrder,
    closeWorkOrder,
    transferWorkOrder,
    listTransferTargetOptions,
    updateWorkOrderSendExpress,
    WORK_ORDER_FAULT_CLOSE_REASON,
    type OrderListQuery,
    type ReturnMethodConfirmPayload
  } from '@/api/workOrder'
  import {
    getReturnMethodInitialMail,
    type BranchItem,
    type OrderDetail,
    type OrderListItem
  } from '@/models/order'
  import { hasInboundTransferFromSite } from '@/utils/orderTransfer'
  import { ORDER_STATUS_TEXT_MAP, isPendingMainStatus } from '@/utils/orderStatus'
  import { Perms } from '@/utils/permissions'
  import { getApiMessage } from '@/utils/http'
  import { requestWorkOrderSubscribe } from '@/utils/requestWorkOrderSubscribe'
  import { takeSelectedShippingAddress } from '@/utils/addressStorage'
  import { storeIcon } from '@/svgs'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'
  import { hideRequestLoading, showApiToast, showRequestLoading } from '@/utils/uiFeedback'
  import { isWorkOrderPendingTechAcceptMainStatus } from '@/utils/workOrderMainStatus'
  import type { WorkOrderActionKey } from '@/constants/orderActions'
  import { useWorkOrderVisibleActions } from '@/composables/useWorkOrderVisibleActions'

  type PrimaryTab = 'untransferred' | 'transferred'
  type SecondaryTab = 'all' | 'pending' | 'pending_accept' | 'processing' | 'completed' | 'closed'

  // 应用商店
  const appStore = useAppStore()
  // 用户商店
  const userStore = useUserStore()
  // 是否总部用户（按组织类型判断，不依赖权限码）
  const isHqUser = computed(() => {
    const code = userStore.userInfo?.currentTypeCode
    return !!code?.startsWith('HQ')
  })

  // ==================== 角色相关计算属性 ====================

  // 一级Tab文案：总部显示"总部处理/网点工单"，其他角色显示"当前处理/历史转出"
  const primaryTabLabels = computed<[string, string]>(() => {
    if (isHqUser.value) return ['总部处理', '网点工单']
    return ['当前处理', '历史转出']
  })

  /**
   * 非总部「历史转出」Tab：列表走 viewScope=HISTORY，行内仅展示「上传寄件单号」例外动作
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const isHistoryListView = computed(() => !isHqUser.value && primaryTab.value === 'transferred')

  // 一级 Tab 列表（总部/其他角色文案由 primaryTabLabels 控制）
  const primaryTabs = computed(() => {
    return [
      { label: primaryTabLabels.value[0], value: 'untransferred' as PrimaryTab },
      { label: primaryTabLabels.value[1], value: 'transferred' as PrimaryTab }
    ]
  })

  // 是否显示网点概览视图（总部 + 网点工单tab）
  const showBranchView = computed(() => isHqUser.value && primaryTab.value === 'transferred')

  // 是否为总部"总部处理"视图
  const isHqProcessView = computed(() => isHqUser.value && primaryTab.value === 'untransferred')

  /**
   * 列表 `currentAcceptCompanyName`（映射为 `siteName`）与当前登录主体公司名一致时，
   * 才允许除查看外的操作。任一方无有效名称时不收紧（兼容旧数据）。总部「总部处理」不参与此限制。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isOrderAcceptedByCurrentCompany = (order: OrderListItem) => {
    if (isHqProcessView.value) return true
    const acceptName = String(order.siteName ?? '').trim()
    const myName = String(userStore.currentNetworkName ?? '').trim()
    if (!acceptName || !myName) return true
    return acceptName === myName
  }

  /**
   * 当前处理/总部处理 Tab：由其他网点转入时展示「转单」标记，紧跟在质保等标签后
   * @param order 工单
   * @returns 是否展示转单标记
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const showInboundTransferTag = (order: OrderListItem) => {
    if (primaryTab.value !== 'untransferred') return false
    return hasInboundTransferFromSite(order.transferFromSite)
  }

  // ==================== 状态与Tab ====================
  // 状态文本映射
  const statusTextMap = ORDER_STATUS_TEXT_MAP

  /**
   * 待接单：仅 mainStatus=PENDING_TECH_ACCEPT
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isOrderPendingTechAccept = (order: OrderListItem) => order.status === 'PENDING_TECH_ACCEPT'

  /**
   * 待派单：PENDING_ASSIGN（且接口原始 mainStatus 非 PENDING_TECH_ACCEPT 兜底）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isOrderPendingAssign = (order: OrderListItem) =>
    order.status === 'PENDING_ASSIGN' && !isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)

  /**
   * 列表卡片状态文案：派单员视角按接口 mainStatus 区分「待派单 / 待接单」
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const listStatusText = (order: OrderListItem) => {
    const status = order.status
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && isPendingMainStatus(status)) {
      return isOrderPendingTechAccept(order) ? '待接单' : '待派单'
    }
    return statusTextMap[status]
  }

  /**
   * 兼容字段类型未同步时读取当前处理人姓名
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const getAssignedUserName = (order: OrderListItem) =>
    ((order as { assignedUserName?: string }).assignedUserName ?? '').trim()

  // 搜索关键词
  const searchQuery = ref('')

  /** 是否展示清除按钮（有非空关键词时） */
  const showSearchClear = computed(() => !!searchQuery.value.trim())

  /** 工单列表仅按 orderNo 模糊；总部「网点工单」Tab 按网点名称筛汇总 */
  const searchInputPlaceholder = computed(() =>
    showBranchView.value ? '搜索网点名称' : '搜索工单号'
  )

  /**
   * 搜索框输入：使用 :value + @input，避免小程序端 v-model 程序化清空不同步
   * @修改人 黄碧莲
   * @修改时间 2026-05-26
   */
  const onSearchInput = (e: { detail?: { value?: string } }) => {
    searchQuery.value = String(e.detail?.value ?? '')
  }
  // 一级Tab
  const primaryTab = ref<PrimaryTab>('untransferred')
  // 二级Tab
  const secondaryTab = ref<SecondaryTab>('all')
  // onLoad 内同步完成路由入参/Tab 后再为 true，避免 onShow 早于 onLoad
  const hasParsedEntryOptions = ref(false)

  // 二级 Tab 列表（根据派单权限动态增删 pending_accept）
  const secondaryTabs = computed(() => {
    const pendingLabel = userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '待派单' : '待接单'
    // 二级Tab列表
    const tabs: Array<{ label: string; value: SecondaryTab }> = [
      { label: '全部', value: 'all' },
      { label: pendingLabel, value: 'pending' }
    ]

    // 如果用户有派单权限，则添加待接单Tab
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) {
      tabs.push({ label: '待接单', value: 'pending_accept' })
    }

    // 添加维修中、已完成、已关闭Tab
    tabs.push(
      { label: '维修中', value: 'processing' },
      { label: '已完成', value: 'completed' },
      { label: '已关闭', value: 'closed' }
    )

    return tabs
  })
  // 横向二级 Tab 滚动定位（与各 tab 节点 id 前缀 `order-list-sec-` 对应）
  const secondaryTabScrollIntoView = ref('')

  // 搜索输入防抖（避免每个字符都打接口）
  const searchDebounceTimer = ref<ReturnType<typeof setTimeout> | null>(null)
  /** 点击清除时置位，避免 blur 与 clear 竞态导致输入框未清空 */
  const clearingSearch = ref(false)

  /**
   * 将指定二级 Tab 滚入横向 scroll-view 可视区域（避免「已关闭」等项在屏外点不到/看不见）
   * @param tab 二级Tab
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const scrollSecondaryTabIntoView = (tab: SecondaryTab) => {
    secondaryTabScrollIntoView.value = ''
    nextTick(() => {
      if (isHqUser.value && primaryTab.value === 'transferred') return
      secondaryTabScrollIntoView.value = `order-list-sec-${tab}`
    })
  }

  /**
   * 设置一级Tab
   * @param tab 一级Tab
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const setPrimaryTab = (tab: PrimaryTab) => {
    primaryTab.value = tab
    secondaryTab.value = 'all'
    scrollSecondaryTabIntoView('all')
    // 总部「网点工单」：显示网点概览，不请求工单列表
    if (showBranchView.value) {
      baseOrderList.value = []
      refreshBranches()
      return
    }
    refreshOrders()
  }

  /**
   * 设置二级Tab
   * @param tab 二级Tab
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const setSecondaryTab = (tab: SecondaryTab) => {
    secondaryTab.value = tab
    scrollSecondaryTabIntoView(tab)
    refreshOrders()
  }

  /**
   * 地址簿选中的寄件信息（onShow 写入，与 ReturnMethodModal 的 initial-mail 合并）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const mailReturnAddressOverride = ref<{
    receiverName: string
    receiverPhone: string
    receiverAddress: string
  } | null>(null)

  /**
   * 列表页统一刷新主路径：
   * - 首次/返回列表均由 onShow 走该路径，避免 onLoad+onShow 各拉一次重复请求；
   * - 下拉刷新 / 普通 Tab 操作仍汇聚到该路径；
   * - 并发保护由 refreshOrders + loadMoreOrders 内部 requestVersion/loadingMore 处理。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const refreshListEntry = async (useScrollRefresherUi: boolean) => {
    if (showBranchView.value) {
      baseOrderList.value = []
      if (useScrollRefresherUi) {
        await runBranchViewWithRefresherFeedback()
      } else {
        await refreshBranches()
      }
      return
    }
    if (useScrollRefresherUi) {
      await runOrderListWithRefresherFeedback(async () => {
        await refreshOrders()
        if (isHqUser.value) await refreshBranches()
      })
      return
    }
    await refreshOrders()
    if (isHqUser.value) await refreshBranches()
  }

  /**
   * 页面加载：只同步解析外部路由与 Tab 状态，列表请求在 onShow 统一拉取（与首次 onShow 合并为一次请求）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  onLoad((options?: Record<string, string>) => {
    const picked = takeSelectedShippingAddress()
    if (picked) {
      mailReturnAddressOverride.value = {
        receiverName: picked.name,
        receiverPhone: picked.phone,
        receiverAddress: picked.fullAddress
      }
    }
    const rawPrimary = String(options?.primaryTab ?? '').trim()
    const rawSecondary = String(options?.secondaryTab ?? '').trim()
    if (rawPrimary === 'untransferred' || rawPrimary === 'transferred') {
      primaryTab.value = rawPrimary
    }
    if (
      rawSecondary === 'all' ||
      rawSecondary === 'pending' ||
      rawSecondary === 'pending_accept' ||
      rawSecondary === 'processing' ||
      rawSecondary === 'completed' ||
      rawSecondary === 'closed'
    ) {
      let sec = rawSecondary as SecondaryTab
      if (sec === 'pending_accept' && !userStore.hasPermission(Perms.WORKORDER_ASSIGN))
        sec = 'pending'
      secondaryTab.value = sec
      scrollSecondaryTabIntoView(sec)
    }
    hasParsedEntryOptions.value = true
  })

  /**
   * 页面显示：消费跨页回跳参数并复用统一刷新主路径
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  onShow(async () => {
    if (!hasParsedEntryOptions.value) return
    const picked = takeSelectedShippingAddress()
    if (picked) {
      mailReturnAddressOverride.value = {
        receiverName: picked.name,
        receiverPhone: picked.phone,
        receiverAddress: picked.fullAddress
      }
    }
    const target = appStore.consumeOrderListNavTarget()
    if (target) {
      primaryTab.value = target.primaryTab
      let sec = target.secondaryTab
      if (sec === 'pending_accept' && !userStore.hasPermission(Perms.WORKORDER_ASSIGN))
        sec = 'pending'
      secondaryTab.value = sec
      scrollSecondaryTabIntoView(sec)
    }
    const useScrollRefresherUi = appStore.consumeOrderListScrollRefresherOnNextShow()
    await refreshListEntry(useScrollRefresherUi)
  })

  // ==================== 工单列表 ====================

  const baseOrderList = ref<OrderListItem[]>([])
  const pageNum = ref(1)
  const pageSize = 10
  const totalOrders = ref(0)
  const loadingMore = ref(false)
  const requestVersion = ref(0)
  const hasLoadedAll = computed(
    () => baseOrderList.value.length >= totalOrders.value && totalOrders.value > 0
  )

  /**
   * 二级 Tab → 列表接口 mainStatus
   * - 待派单：PENDING_ASSIGN
   * - 待接单：PENDING_TECH_ACCEPT
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const secondaryTabToMainStatus = (tab: SecondaryTab): string | undefined => {
    if (tab === 'all') return undefined
    if (tab === 'pending') {
      if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return 'PENDING_ASSIGN'
      return 'PENDING_TECH_ACCEPT'
    }
    if (tab === 'pending_accept') return 'PENDING_TECH_ACCEPT'
    if (tab === 'processing') return 'IN_PROGRESS'
    if (tab === 'completed') return 'COMPLETED'
    return 'CLOSED'
  }

  /**
   * 构造列表请求参数。
   *
   * 关键约束：`refreshOrders` 与 `loadMoreOrders` 必须基于相同的筛选上下文
   * 构造 query，仅 `pageNum` 不同；否则翻页会出现数据漂移/错位。
   *
   * @param targetPageNum - 目标页码
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const buildListQuery = (targetPageNum: number): OrderListQuery => {
    const q = searchQuery.value?.trim()
    const primary = primaryTab.value
    const secondary = secondaryTab.value

    const query: OrderListQuery = {
      pageNum: targetPageNum,
      pageSize,
      companyId: userStore.userInfo?.currentCompanyId,
      // CURRENT / HISTORY 均会填充 availableActions；非总部历史转出走 HISTORY 口径
      viewScope: primary === 'transferred' && !isHqUser.value ? 'HISTORY' : 'CURRENT'
    }

    if (q) query.orderNo = q

    const ms = secondaryTabToMainStatus(secondary)
    if (ms !== undefined) query.mainStatus = ms

    return query
  }

  /**
   * 刷新工单列表
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const refreshOrders = async () => {
    const currentVersion = ++requestVersion.value
    try {
      // 总部「网点工单」tab 不展示工单列表
      if (showBranchView.value) {
        baseOrderList.value = []
        pageNum.value = 1
        totalOrders.value = 0
        return
      }

      pageNum.value = 1
      const query = buildListQuery(pageNum.value)

      const page = await listWorkOrder(query)
      if (currentVersion !== requestVersion.value) return
      baseOrderList.value = page.records
      totalOrders.value = page.total
    } catch (e) {
      console.log(e)
      if (currentVersion !== requestVersion.value) return
      baseOrderList.value = []
      totalOrders.value = 0
    }
  }

  /**
   * 触底加载下一页
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const loadMoreOrders = async () => {
    if (showBranchView.value) return
    if (loadingMore.value) return
    if (hasLoadedAll.value) return
    if (!baseOrderList.value.length && pageNum.value === 1) return

    loadingMore.value = true
    const currentVersion = requestVersion.value
    try {
      const nextPage = pageNum.value + 1
      const query = buildListQuery(nextPage)

      const page = await listWorkOrder(query)
      if (currentVersion !== requestVersion.value) return
      pageNum.value = nextPage
      totalOrders.value = page.total
      if (page.records.length) {
        baseOrderList.value = baseOrderList.value.concat(page.records)
      }
    } catch (e) {
      console.log(e)
    } finally {
      loadingMore.value = false
    }
  }

  /**
   * 列表数据与接口分页一致；一级/二级 Tab 与搜索在 refreshOrders / loadMoreOrders 中通过 query 请求服务端筛选。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const orderList = computed<OrderListItem[]>(() => baseOrderList.value)

  // 搜索：防抖后刷新列表（接口仅支持工单号/条码等字段时，可按需扩展 query）
  watch(
    () => searchQuery.value,
    () => {
      if (searchDebounceTimer.value) clearTimeout(searchDebounceTimer.value)
      searchDebounceTimer.value = setTimeout(() => {
        if (showBranchView.value) {
          refreshBranches()
          return
        }
        refreshOrders()
      }, 300)
    }
  )

  /**
   * 搜索输入框失焦时立即触发一次刷新：
   * - 清理防抖定时器，避免紧接着再触发一次延迟请求；
   * - 保持与当前视图一致：网点视图刷新网点，工单视图刷新工单。
   * @修改人 黄碧莲
   * @修改时间 2026-05-26
   */
  const onSearchInputBlur = () => {
    if (clearingSearch.value) return
    if (searchDebounceTimer.value) {
      clearTimeout(searchDebounceTimer.value)
      searchDebounceTimer.value = null
    }
    if (showBranchView.value) {
      refreshBranches()
      return
    }
    refreshOrders()
  }

  /**
   * 点击清除图标：清空关键词并立即刷新当前列表（工单列表 / 网点汇总）
   * @修改人 黄碧莲
   * @修改时间 2026-05-26
   */
  const onSearchClear = () => {
    if (!searchQuery.value.trim()) return
    clearingSearch.value = true
    if (searchDebounceTimer.value) {
      clearTimeout(searchDebounceTimer.value)
      searchDebounceTimer.value = null
    }
    searchQuery.value = ''
    if (showBranchView.value) {
      void refreshBranches()
    } else {
      void refreshOrders()
    }
    nextTick(() => {
      clearingSearch.value = false
    })
  }
  // 空列表标题（根据搜索关键词判断）
  const listEmptyTitle = computed(() => (searchQuery.value?.trim() ? '未找到相关工单' : '暂无工单'))

  // 空列表描述（根据搜索关键词判断）
  const listEmptyDesc = computed(() =>
    searchQuery.value?.trim() ? '试试更换关键词或清空搜索' : '当前筛选条件下没有工单'
  )

  // ==================== 网点列表（总部-网点工单） ====================

  const baseBranchList = ref<BranchItem[]>([])
  const BRANCH_PAGE_STEP = 15
  const branchVisibleLimit = ref(BRANCH_PAGE_STEP)
  const branchTotalCount = computed(() => baseBranchList.value.length)
  const branchDisplayList = computed<BranchItem[]>(() =>
    baseBranchList.value.slice(0, branchVisibleLimit.value)
  )
  const hasLoadedAllBranches = computed(
    () =>
      baseBranchList.value.length > 0 &&
      branchDisplayList.value.length >= baseBranchList.value.length
  )

  const refreshBranches = async () => {
    try {
      baseBranchList.value = await listHqSiteSummary({ siteName: searchQuery.value })
      branchVisibleLimit.value = BRANCH_PAGE_STEP
    } catch {
      baseBranchList.value = []
      branchVisibleLimit.value = BRANCH_PAGE_STEP
    }
  }

  const loadMoreBranches = () => {
    if (branchDisplayList.value.length >= baseBranchList.value.length) return
    branchVisibleLimit.value += BRANCH_PAGE_STEP
  }

  const {
    refresherTriggered: orderListRefresherTriggered,
    onRefresherRefresh: onOrderListRefresherRefresh,
    onRefresherRestore: onOrderListRefresherRestore,
    runWithRefresherFeedback: runOrderListWithRefresherFeedback,
    resetScrollRefresher: resetOrderListRefresher
  } = useScrollRefresher(async () => {
    await refreshOrders()
  })

  const {
    refresherTriggered: branchViewRefresherTriggered,
    onRefresherRefresh: onBranchViewRefresherRefresh,
    onRefresherRestore: onBranchViewRefresherRestore,
    runWithRefresherFeedback: runBranchViewWithRefresherFeedback,
    resetScrollRefresher: resetBranchViewRefresher
  } = useScrollRefresher(async () => {
    await refreshBranches()
  })

  /**
   * Tab / 列表视图切换时复位两侧 refresher，避免下拉动画残留挡住一级、二级 Tab 点击
   * @修改人 黄碧莲
   * @修改时间 2026-05-25
   */
  const resetAllListRefreshers = () => {
    resetOrderListRefresher()
    resetBranchViewRefresher()
  }

  watch([primaryTab, secondaryTab, showBranchView], () => {
    resetAllListRefreshers()
  })

  /** 离开工单库页时兜底，防止返回后 refresher 仍处于 triggered */
  onHide(() => {
    resetAllListRefreshers()
  })

  // ==================== 派单员操作 ====================

  // 转单弹窗（派单员使用）
  const showTransferModal = ref(false)
  // 选中网点（转单弹窗中选择）
  const selectedNetwork = ref<any>(null)
  // 转单原因（转单弹窗中输入）
  const transferReason = ref('')
  // 当前转单工单ID（转单弹窗中选择）
  const currentTransferOrderId = ref('')

  // 派单弹窗（派单员使用）
  const showAssignModal = ref(false)
  // 当前派单工单ID
  const currentOrderId = ref('')
  // 选中维修员ID
  const selectedTechId = ref<number | string | null>(null)

  /**
   * 维修员列表
   * @returns 维修员列表
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const technicianList = ref<Technician[]>([])

  /**
   * 转单网点列表
   * @returns 转单网点列表
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  type TransferNetworkItem = { id: string | number; name: string; [k: string]: any }
  const transferTargetOptions = ref<TransferNetworkItem[]>([])
  const networkList = computed(() => transferTargetOptions.value)

  /**
   * 打开转单弹窗
   * @param orderId 当前转单工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const openTransferModal = async (orderId: string) => {
    currentTransferOrderId.value = orderId
    // 每次打开都重置上一次选择，避免串单
    selectedNetwork.value = null
    transferReason.value = ''
    transferTargetOptions.value = []

    const workOrderId = Number(orderId)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
      void showApiToast('工单ID无效')
      return
    }

    // 网点列表是 GET 查询接口（默认不显示 loading），但弹窗打开需要可见反馈，故业务侧手动加 loading
    showRequestLoading('加载网点...')
    try {
      const list = await listTransferTargetOptions(workOrderId)
      transferTargetOptions.value = list.map((c) => ({
        id: c.id,
        name: c.companyName || c.companyCode || String(c.id),
        raw: c
      }))
      if (!transferTargetOptions.value.length) {
        void showApiToast('暂无可转单目标')
        currentTransferOrderId.value = ''
        return
      }
      showTransferModal.value = true
    } catch {
      // http.ts 内已统一 showApiToast；这里不重复提示
    } finally {
      hideRequestLoading()
    }
  }

  /**
   * 确认转单
   * @param payload 转单数据
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onTransferConfirm = async (payload: { selectedNetwork: any; reason: string }) => {
    if (!payload.selectedNetwork) {
      void showApiToast('请选择转单网点')
      return
    }
    if (!payload.reason?.trim()) {
      void showApiToast('请填写转单原因')
      return
    }

    const workOrderId = Number(currentTransferOrderId.value)
    const targetCompanyId = Number(payload.selectedNetwork?.id)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
      void showApiToast('工单ID无效')
      return
    }
    if (!Number.isFinite(targetCompanyId) || targetCompanyId <= 0) {
      void showApiToast('目标网点ID无效')
      return
    }

    // 与「提交报价」一致：校验通过后、业务请求前弹出工单订阅消息授权（仍在用户点击链路内）
    await requestWorkOrderSubscribe()

    try {
      // 写接口 transferWorkOrder 走 PUT，http.ts 自动显示带 mask 的 loading
      const res = await transferWorkOrder({
        workOrderId,
        targetCompanyId,
        remark: payload.reason.trim()
      })

      // 关闭转单弹窗并重置状态
      showTransferModal.value = false
      selectedNetwork.value = null
      transferReason.value = ''
      currentTransferOrderId.value = ''

      // 先刷新列表（用户在 toast 期间看到新数据，体验更顺）；再 await 提示以阻塞后续交互
      await nextTick()
      await refreshOrders()

      await showApiToast(getApiMessage(res, '转单已提交'))
    } catch {
      // http.ts / api 内已 showApiToast；这里不重复
    }
  }

  /**
   * 打开派单弹窗
   * @param orderId 当前派单工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const openAssignModal = (orderId: string | number) => {
    const openedFor = String(orderId ?? '').trim()
    currentOrderId.value = openedFor
    showAssignModal.value = true
    selectedTechId.value = null

    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) {
      technicianList.value = []
      return
    }

    technicianList.value = []

    const workOrderId = Number(openedFor)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) return
    listAssignUserOptions(workOrderId)
      .then((list) => {
        if (String(currentOrderId.value).trim() !== openedFor) return
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
        technicianList.value = mapped
      })
      .catch(() => {
        // http.ts 内已有 toast；这里不重复提示
      })
  }

  /**
   * 关闭派单弹窗
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const closeAssignModal = () => {
    showAssignModal.value = false
    currentOrderId.value = ''
    selectedTechId.value = null
    technicianList.value = []
  }

  /**
   * 确认派单：PUT `/api/system/work-order/assign`（assignedUserId + workOrderId）
   * @param payload 所选维修员
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onAssignConfirm = async (payload: {
    workOrderId: string | number
    selectedTechId: number | string
  }) => {
    const workOrderId = Number(payload.workOrderId ?? currentOrderId.value)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
      void showApiToast('工单ID无效')
      return
    }

    const assignedUserId = Number(payload?.selectedTechId)
    if (!Number.isFinite(assignedUserId) || assignedUserId <= 0) {
      void showApiToast('维修员ID无效')
      return
    }
    const selfId = Number(userStore.userInfo?.id)
    const isSelf = Number.isFinite(selfId) && selfId > 0 && assignedUserId === selfId
    try {
      // 派单是 PUT 写接口，http.ts 自动显示带 mask 的 loading
      const res = await assignWorkOrder({ workOrderId, assignedUserId })
      closeAssignModal()
      // 列表刷新与提示并行：toast mask 期间数据已刷好，关闭后用户即可看到最新状态
      refreshOrders()
      const tip = isSelf ? '已派单给自己，可在「待接单」中接单' : getApiMessage(res, '派单成功')
      await showApiToast(tip)
    } catch {
      // assignWorkOrder / http 内已 showApiToast
    }
  }

  /**
   * 跳转到工单详情（仅查看；故障点登记需通过「维修登记」「复检登记」按钮进入并带 action）
   * @param order 工单
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onOrderClick = (order: OrderListItem) => {
    const viewOnly =
      isHistoryListView.value || !isOrderAcceptedByCurrentCompany(order) ? '&viewOnly=1' : ''
    uni.navigateTo({
      url: `/pages/order/detail?id=${order.id}&status=${order.status}${viewOnly}`
    })
  }

  /**
   * 跳转到网点工单详情（tab 与详情页二级 Tab 一致：全部/待接单/维修中/已完成）
   * @param branch 网点
   * @param tab 二级Tab
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const goToBranchDetail = (branch: BranchItem, tab: SecondaryTab = 'all') => {
    uni.navigateTo({
      url: `/pages/order/branch-detail?id=${branch.id}&name=${encodeURIComponent(branch.name)}&tab=${encodeURIComponent(tab)}&total=${branch.total}&pending=${branch.pending}&processing=${branch.processing}&completed=${branch.completed}`
    })
  }

  const { getVisibleActions, hasVisibleAction } = useWorkOrderVisibleActions({
    primaryTab,
    isOrderAcceptedByCurrentCompany,
    isHistoryListView
  })

  /**
   * 列表行内操作按钮（避免模板内重复调用 getVisibleActions）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const resolveListRowActions = (order: OrderListItem) => getVisibleActions(order)

  /**
   * 接单：进入详情填写故障判定与维修报价，用户提交后再调接单接口（与首页一致）
   * @param orderId 工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onAcceptOrder = (orderId: string) => {
    const id = Number(orderId)
    if (!Number.isFinite(id) || id <= 0) {
      void showApiToast('工单ID无效')
      return
    }
    uni.navigateTo({
      url: `/pages/order/detail?id=${orderId}&action=accept`
    })
  }

  /**
   * 维修登记
   * @param orderId 工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onRepairRegister = (orderId: string) => {
    uni.navigateTo({ url: `/pages/order/detail?id=${orderId}&action=repair` })
  }

  // 机器返回方式弹窗
  const showReturnMethodModal = ref(false)
  // 当前机器返回方式工单ID
  const currentReturnOrderId = ref('')
  const currentReturnOrderDetail = ref<OrderDetail>()
  // 当前选择的机器返回方式（展示/状态）
  const currentReturnMethodType = ref<'' | 'self' | 'mail'>('')
  /**
   * 「无故障」闭环：关闭工单 PUT 需携带刚确认的返回方式
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const closeOrderReturnMethodPayload = ref<ReturnMethodConfirmPayload | null>(null)
  // 工单关闭弹窗
  const showCloseOrderModal = ref(false)

  const returnMethodInitialMailMerged = computed(() => {
    const base = getReturnMethodInitialMail(currentReturnOrderDetail.value)
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
  })

  /**
   * 打开机器返回方式弹窗
   * @param orderId 工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onReturnMethod = (orderId: string) => {
    currentReturnOrderId.value = orderId
    currentReturnOrderDetail.value = undefined
    currentReturnMethodType.value = ''
    closeOrderReturnMethodPayload.value = null
    getWorkOrder(orderId)
      .then((d) => {
        currentReturnOrderDetail.value = d
      })
      .catch(() => {
        currentReturnOrderDetail.value = undefined
      })
    showReturnMethodModal.value = true
  }

  /**
   * 确认机器返回方式：有故障直接关单；无故障先弹关单原因，确认后再关单（入参含返回方式）
   * @param data 机器返回方式数据
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onReturnMethodConfirm = async (data: ReturnMethodConfirmPayload) => {
    currentReturnMethodType.value = data.type
    const id = Number(currentReturnOrderId.value)
    if (!Number.isFinite(id) || id <= 0) {
      void showApiToast('工单ID无效')
      return
    }

    const judge = String(currentReturnOrderDetail.value?.repair?.faultJudge ?? '').trim()

    if (judge === '有故障') {
      const base = {
        workOrderId: id,
        closeReason: WORK_ORDER_FAULT_CLOSE_REASON,
        returnMethod: data.type === 'self' ? '自提' : '回寄'
      } as const
      const dto =
        data.type === 'mail'
          ? {
              ...base,
              ...(data.mail.returnVoucherFileIds.length
                ? { returnVoucherFileIds: data.mail.returnVoucherFileIds }
                : {})
            }
          : base

      try {
        // 关单是 PUT 写接口，http.ts 自动显示带 mask 的 loading
        const res = await closeWorkOrder(dto)
        closeOrderReturnMethodPayload.value = null
        // 列表刷新与提示并行，toast 阻塞期内数据已就绪
        refreshOrders()
        await showApiToast(getApiMessage(res, '工单已关闭'))
      } catch {
        // closeWorkOrder 内已 showApiToast
      }
      return
    }

    if (judge === '无故障') {
      closeOrderReturnMethodPayload.value = data
      // 提示选择结果并等待用户看完后再弹出关单原因弹窗，避免两次交互重叠
      await showApiToast(
        `工单 ${currentReturnOrderId.value} 已选择 ${data.type === 'self' ? '自提' : '回寄'}`
      )
      showCloseOrderModal.value = true
      return
    }

    void showApiToast('工单状态未就绪，请稍后重试')
  }

  /**
   * 确认工单关闭
   * @param reason 关闭原因
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onCloseOrderConfirm = async (reason: string) => {
    const id = Number(currentReturnOrderId.value)
    if (!Number.isFinite(id) || id <= 0) {
      void showApiToast('工单ID无效')
      return
    }
    const payload = closeOrderReturnMethodPayload.value
    if (!payload) {
      void showApiToast('请先完成机器返回方式')
      return
    }

    const cr = (reason || '').trim()
    if (!cr) {
      void showApiToast('请填写关闭原因（无故障必填）')
      return
    }

    const base = {
      workOrderId: id,
      closeReason: cr,
      returnMethod: payload.type === 'self' ? '自提' : '回寄'
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

    try {
      // 关单是 PUT 写接口，http.ts 自动显示带 mask 的 loading
      const res = await closeWorkOrder(dto)
      closeOrderReturnMethodPayload.value = null
      // 关闭成功后刷新列表与提示并行，避免本地状态与后端不一致
      refreshOrders()
      await showApiToast(getApiMessage(res, '工单已关闭'))
    } catch {
      // closeWorkOrder 内已 showApiToast
    }
  }

  /**
   * 复检登记：进入详情，与维修登记同款表单，顶部状态为已完成
   * @param orderId 工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onRecheck = (orderId: string) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${orderId}&status=COMPLETED&action=recheck`
    })
  }

  // 上传寄件单号弹窗（UPLOAD_SEND_EXPRESS）
  const showUploadSendExpressModal = ref(false)
  const currentUploadSendExpressOrderId = ref('')

  /**
   * 打开上传寄件单号弹窗
   * @param orderId 工单 ID
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const onUploadSendExpress = (orderId: string) => {
    currentUploadSendExpressOrderId.value = orderId
    showUploadSendExpressModal.value = true
  }

  /**
   * 确认上传寄件单号：提交后刷新历史转出列表
   * @param payload 寄件单号与凭证
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const onUploadSendExpressConfirm = async (payload: {
    workOrderId: number
    sendExpressNo: string
    senderVoucherFileIds?: number[]
  }) => {
    try {
      // 上传寄件单号是 PUT 写接口，http.ts 自动显示带 mask 的 loading
      const res = await updateWorkOrderSendExpress(payload)
      showUploadSendExpressModal.value = false
      // 历史转出列表刷新与提示并行
      refreshOrders()
      await showApiToast(getApiMessage(res, '提交成功'))
    } catch {
      // updateWorkOrderSendExpress / http 内已 showApiToast
    }
  }

  /**
   * 动作统一分发：将工单动作语义映射到当前页面既有行为实现。
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const workOrderActionHandlers: Record<
    | 'ASSIGN'
    | 'TECH_ACCEPT'
    | 'TRANSFER'
    | 'REPAIR_FINISH'
    | 'REVIEW'
    | 'CLOSE'
    | 'UPLOAD_SEND_EXPRESS',
    (orderId: string) => void
  > = {
    ASSIGN: (orderId) => openAssignModal(orderId),
    TECH_ACCEPT: (orderId) => onAcceptOrder(orderId),
    TRANSFER: (orderId) => {
      void openTransferModal(orderId)
    },
    REPAIR_FINISH: (orderId) => onRepairRegister(orderId),
    REVIEW: (orderId) => onRecheck(orderId),
    CLOSE: (orderId) => onReturnMethod(orderId),
    UPLOAD_SEND_EXPRESS: (orderId) => onUploadSendExpress(orderId)
  }

  /**
   * 处理工单动作点击。
   * @param actionKey 工单动作 key
   * @param orderId 工单ID
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const dispatchWorkOrderAction = (actionKey: WorkOrderActionKey, orderId: string | number) => {
    const id = String(orderId ?? '').trim()
    if (!id) {
      void showApiToast('工单ID无效')
      return
    }
    const orderRow = orderList.value.find((o) => o.id === id)
    if (
      orderRow &&
      actionKey !== 'UPLOAD_SEND_EXPRESS' &&
      !isHistoryListView.value &&
      !isOrderAcceptedByCurrentCompany(orderRow)
    ) {
      void showApiToast('受理方非您所在主体，仅可查看')
      return
    }
    if (orderRow && !hasVisibleAction(orderRow, actionKey)) {
      void showApiToast('当前工单状态已变更，请刷新后重试')
      return
    }
    if (!(actionKey in workOrderActionHandlers)) {
      void showApiToast('暂不支持该操作')
      return
    }
    workOrderActionHandlers[actionKey as keyof typeof workOrderActionHandlers](id)
  }
</script>

<style lang="scss" scoped>
  .order-list-page {
    /* 插槽渲染在 OrderCardList 内，需穿透 scoped */
    :deep(.value-repair-assignee) {
      color: $emerald-600;
      font-weight: 600;
    }

    .order-list-scroll {
      padding-top: $space-md;
      box-sizing: border-box;
    }

    .tabs-primary-wrap {
      .tabs-primary {
        @include pill-tabs;
      }
    }

    .tabs-secondary {
      width: 100%;
      border-bottom: 2rpx solid $bg-hover;

      .tabs-inner {
        @include tabs-track;
      }

      .tab-item {
        @include tab-underline-item;
        padding: $space-sm 0 $space-md;
      }
    }

    .branch-summary-header {
      @include flex-between;
      padding: $space-sm $space-lg 0;
      background-color: $bg-light;

      .branch-summary-title {
        font-size: 22rpx;
        font-weight: bold;
        color: $text-slate-500;
        letter-spacing: 0.1em;
      }

      .branch-summary-count {
        font-size: 22rpx;
        color: $text-slate-400;
      }
    }

    .branch-list-container {
      @include flex-col;
      gap: $space-md;
      padding: $space-md $space-lg;
      background-color: $bg-light;
    }

    .branch-card {
      @include sheet-white($space-lg);
      border: 2rpx solid $bg-hover;

      .branch-card-header {
        @include flex-between;
        margin-bottom: $space-lg;

        .branch-info {
          @include flex-row;
          gap: $space-sm;
        }

        .branch-icon-wrap {
          width: 64rpx;
          height: 64rpx;
          border-radius: $radius-md;
          background-color: $tag-brand-bg;
          @include flex-center;
        }

        .branch-icon {
          width: 40rpx;
          height: 40rpx;
        }

        .branch-name {
          font-size: $font-lg;
          font-weight: bold;
          color: $text-slate-900;
        }

        .branch-arrow {
          font-size: 40rpx;
          color: $icon-slate-light;
        }
      }

      .branch-stats {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: $space-sm;

        .stat-item {
          @include flex-column-center;
          padding: $space-sm;
          border-radius: $radius-md;

          .stat-label {
            font-size: 20rpx;
            margin-bottom: $space-xs;
          }

          .stat-value {
            font-size: $font-md;
            font-weight: bold;
          }

          &.stat-total {
            @include surface-muted;
            .stat-label {
              color: $text-slate-400;
            }
            .stat-value {
              color: $text-slate-900;
            }
          }

          &.stat-pending {
            background-color: rgba($tag-brand-bg, 0.5);
            .stat-label {
              color: $primary;
            }
            .stat-value {
              color: $primary;
            }
          }

          &.stat-processing {
            background-color: rgba($tag-transfer-bg, 0.5);
            .stat-label {
              color: $blue-500;
            }
            .stat-value {
              color: $tag-transfer-text;
            }
          }

          &.stat-completed {
            background-color: rgba($emerald-50, 0.5);
            .stat-label {
              color: $emerald-500;
            }
            .stat-value {
              color: $emerald-600;
            }
          }
        }
      }
    }
  }
</style>
