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
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索工单号或故障描述"
            placeholder-class="placeholder-text"
          />
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
      @scrolltolower="loadMoreOrders"
    >
      <OrderCardList
        :orders="orderList"
        :status-text="listStatusText"
        :empty-title="listEmptyTitle"
        :empty-desc="listEmptyDesc"
        :show-inbound-transfer-tag="showInboundTransferTag"
        :show-transferred-tag="(order) => !!order.transferred && !isHqUser"
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
        <!-- 操作 -->
        <template #actions="{ order }">
          <view v-if="getVisibleActions(order).length > 0" class="action-wrap">
            <button
              v-for="action in getVisibleActions(order)"
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
  </view>
</template>

<script setup lang="ts">
  /**
   * 工单库：路由仅要求登录；列表 Tab 与操作按钮用 Perms + userStore.hasPermission / canAny / canAll。
   */
  import { ref, computed, nextTick, watch } from 'vue'
  import { onLoad, onShow } from '@dcloudio/uni-app'
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
  import {
    applyWorkOrderListSearchKeyword,
    assignWorkOrder,
    listHqSiteSummary,
    listAssignUserOptions,
    getWorkOrder,
    listWorkOrder,
    closeWorkOrder,
    transferWorkOrder,
    listTransferTargetOptions,
    WORK_ORDER_FAULT_CLOSE_REASON,
    type OrderListQuery,
    type ReturnMethodConfirmPayload
  } from '@/api/workOrder'
  import {
    getReturnMethodInitialMail,
    type BranchItem,
    type OrderDetail,
    type OrderListItem,
    type WorkOrderMainStatus
  } from '@/models/order'
  import {
    canCurrentSiteOperateTransferredOrder,
    hasInboundTransferFromSite
  } from '@/utils/orderTransfer'
  import { ORDER_STATUS_TEXT_MAP, isPendingMainStatus } from '@/utils/orderStatus'
  import { Perms } from '@/utils/permissions'
  import { getApiMessage } from '@/utils/http'
  import { takeSelectedShippingAddress } from '@/utils/addressStorage'
  import { storeIcon } from '@/svgs'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'
  import { isWorkOrderPendingTechAcceptMainStatus } from '@/utils/workOrderMainStatus'
  import { normalizeAvailableActions, type WorkOrderActionKey } from '@/constants/orderActions'
  import {
    ENABLE_LEGACY_STATUS_ACTION_FALLBACK,
    auditLegacyStatusFallbackHit
  } from '@/constants/orderActionFallback'

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

  // 一级Tab文案：总部显示"总部处理/网点工单"，其他角色显示"未转单/已转单"
  const primaryTabLabels = computed<[string, string]>(() => {
    if (isHqUser.value) return ['总部处理', '网点工单']
    return ['未转单', '已转单']
  })

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
  // 接单权限用户：已转单且当前网点为转出方时不展示接单/登记等按钮
  const canOperateOrder = computed(
    () => userStore.hasPermission(Perms.WORKORDER_ACCEPT) || isHqProcessView.value
  )

  /**
   * 维修工程师：已转单且当前网点为转出方时不展示接单/登记等按钮
   * @param order 工单
   * @returns 是否可以操作
   */
  const canEngineerOperateTransferredOrder = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ACCEPT)) return true
    return canCurrentSiteOperateTransferredOrder(
      !!order.transferred,
      order.transferFromSite,
      userStore.currentNetworkName
    )
  }

  /**
   * 列表 `currentAcceptCompanyName`（映射为 `siteName`）与当前登录主体公司名一致时，
   * 才允许除查看外的操作。任一方无有效名称时不收紧（兼容旧数据）。总部「总部处理」不参与此限制。
   */
  const isOrderAcceptedByCurrentCompany = (order: OrderListItem) => {
    if (isHqProcessView.value) return true
    const acceptName = String(order.siteName ?? '').trim()
    const myName = String(userStore.currentNetworkName ?? '').trim()
    if (!acceptName || !myName) return true
    return acceptName === myName
  }

  /**
   * 未转单/总部处理 Tab：由其他网点转入时展示「转单」标记，紧跟在质保等标签后
   * @param order 工单
   * @returns 是否展示转单标记
   */
  const showInboundTransferTag = (order: OrderListItem) => {
    if (primaryTab.value !== 'untransferred') return false
    return hasInboundTransferFromSite(order.transferFromSite)
  }

  // ==================== 状态与Tab ====================
  // 状态文本映射
  const statusTextMap = ORDER_STATUS_TEXT_MAP

  /** 待接单：仅 mainStatus=PENDING_TECH_ACCEPT */
  const isOrderPendingTechAccept = (order: OrderListItem) => order.status === 'PENDING_TECH_ACCEPT'

  /** 待派单：PENDING_ASSIGN（且接口原始 mainStatus 非 PENDING_TECH_ACCEPT 兜底） */
  const isOrderPendingAssign = (order: OrderListItem) =>
    order.status === 'PENDING_ASSIGN' && !isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)

  /**
   * 列表卡片状态文案：派单员视角按接口 mainStatus 区分「待派单 / 待接单」
   */
  const listStatusText = (order: OrderListItem) => {
    const status = order.status
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && isPendingMainStatus(status)) {
      return isOrderPendingTechAccept(order) ? '待接单' : '待派单'
    }
    return statusTextMap[status]
  }

  /** 兼容字段类型未同步时读取当前处理人姓名 */
  const getAssignedUserName = (order: OrderListItem) =>
    ((order as { assignedUserName?: string }).assignedUserName ?? '').trim()

  // 搜索关键词
  const searchQuery = ref('')
  // 一级Tab
  const primaryTab = ref<PrimaryTab>('untransferred')
  // 二级Tab
  const secondaryTab = ref<SecondaryTab>('all')
  // 首次 onLoad 参数解析完成后，onShow 再触发统一刷新主路径
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

  /**
   * 将指定二级 Tab 滚入横向 scroll-view 可视区域（避免「已关闭」等项在屏外点不到/看不见）
   * @param tab 二级Tab
   * @returns void
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
   */
  const setSecondaryTab = (tab: SecondaryTab) => {
    secondaryTab.value = tab
    scrollSecondaryTabIntoView(tab)
    refreshOrders()
  }

  /** 地址簿选中的寄件信息（onShow 写入，与 ReturnMethodModal 的 initial-mail 合并） */
  const mailReturnAddressOverride = ref<{
    receiverName: string
    receiverPhone: string
    receiverAddress: string
  } | null>(null)

  /**
   * 列表页统一刷新主路径：
   * - 首次参数解析放在 onLoad；
   * - onShow / 下拉刷新 / 普通刷新都汇聚到该路径；
   * - 并发保护由 refreshOrders + loadMoreOrders 内部 requestVersion/loadingMore 处理。
   */
  const refreshListEntry = async (useScrollRefresherUi: boolean) => {
    if (showBranchView.value) {
      baseOrderList.value = []
      if (useScrollRefresherUi) await onBranchViewRefresherRefresh()
      else await refreshBranches()
      return
    }
    if (useScrollRefresherUi) await onOrderListRefresherRefresh()
    else await refreshOrders()
    if (isHqUser.value) await refreshBranches()
  }

  /**
   * 页面加载：首次解析外部路由参数（Tab）并走统一刷新主路径
   */
  onLoad(async (options?: Record<string, string>) => {
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
    await refreshListEntry(false)
  })

  /**
   * 页面显示：消费跨页回跳参数并复用统一刷新主路径
   * @returns void
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
   */
  const buildListQuery = (targetPageNum: number): OrderListQuery => {
    const q = searchQuery.value?.trim()
    const primary = primaryTab.value
    const secondary = secondaryTab.value

    const query: OrderListQuery = {
      pageNum: targetPageNum,
      pageSize,
      companyId: userStore.userInfo?.currentCompanyId,
      // 总部「总部处理」：仅当前网点可见范围，与首页工作台 count 口径一致
      viewScope: isHqProcessView.value ? 'CURRENT' : 'ALL',
      hasTransfer: primary === 'transferred' ? 1 : 0
    }

    // 与接口约定一致：含中文→客户姓名模糊；长数字→条码；否则工单号模糊（避免多条件 AND 同时传）
    applyWorkOrderListSearchKeyword(query, q)

    const ms = secondaryTabToMainStatus(secondary)
    if (ms !== undefined) query.mainStatus = ms

    return query
  }

  /**
   * 刷新工单列表
   * @returns void
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

  /** 列表数据与接口分页一致；一级/二级 Tab 与搜索在 refreshOrders / loadMoreOrders 中通过 query 请求服务端筛选。 */
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
    onRefresherRefresh: onOrderListRefresherRefresh
  } = useScrollRefresher(async () => {
    await refreshOrders()
  })

  const {
    refresherTriggered: branchViewRefresherTriggered,
    onRefresherRefresh: onBranchViewRefresherRefresh
  } = useScrollRefresher(async () => {
    await refreshBranches()
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
   */
  const technicianList = ref<Technician[]>([])

  /**
   * 转单网点列表
   * @returns 转单网点列表
   */
  type TransferNetworkItem = { id: string | number; name: string; [k: string]: any }
  const transferTargetOptions = ref<TransferNetworkItem[]>([])
  const networkList = computed(() => transferTargetOptions.value)

  /**
   * 打开转单弹窗
   * @param orderId 当前转单工单ID
   * @returns void
   */
  const openTransferModal = async (orderId: string) => {
    currentTransferOrderId.value = orderId
    // 每次打开都重置上一次选择，避免串单
    selectedNetwork.value = null
    transferReason.value = ''
    transferTargetOptions.value = []

    const workOrderId = Number(orderId)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }

    uni.showLoading({ title: '加载网点...' })
    try {
      const list = await listTransferTargetOptions(workOrderId)
      transferTargetOptions.value = list.map((c) => ({
        id: c.id,
        name: c.companyName || c.companyCode || String(c.id),
        raw: c
      }))
      if (!transferTargetOptions.value.length) {
        uni.showToast({ title: '暂无可转单目标', icon: 'none' })
        currentTransferOrderId.value = ''
        return
      }
      showTransferModal.value = true
    } catch {
      // http.ts 内已 toast；这里兜底避免 loading 不消失
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 确认转单
   * @param payload 转单数据
   * @returns void
   */
  const onTransferConfirm = async (payload: { selectedNetwork: any; reason: string }) => {
    if (!payload.selectedNetwork) {
      uni.showToast({ title: '请选择转单网点', icon: 'none' })
      return
    }
    if (!payload.reason?.trim()) {
      uni.showToast({ title: '请填写转单原因', icon: 'none' })
      return
    }

    const workOrderId = Number(currentTransferOrderId.value)
    const targetCompanyId = Number(payload.selectedNetwork?.id)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    if (!Number.isFinite(targetCompanyId) || targetCompanyId <= 0) {
      uni.showToast({ title: '目标网点ID无效', icon: 'none' })
      return
    }

    uni.showLoading({ title: '正在提交...' })
    try {
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

      await nextTick()
      await refreshOrders()

      uni.showToast({ title: getApiMessage(res, '转单已提交'), icon: 'none', duration: 1500 })
    } catch {
      // http.ts / api 内已 toast；这里兜底避免 loading 不消失
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 打开派单弹窗
   * @param orderId 当前派单工单ID
   * @returns void
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
   */
  const onAssignConfirm = async (payload: {
    workOrderId: string | number
    selectedTechId: number | string
  }) => {
    const workOrderId = Number(payload.workOrderId ?? currentOrderId.value)
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
      setTimeout(() => {
        refreshOrders()
      }, 300)
    } catch {
      // assignWorkOrder / http 内已 toast
    }
  }

  // ==================== 维修工程师 & 总部操作 ====================

  // 操作按钮（操作按钮键、操作按钮标签、操作按钮类名）
  type OperatorAction = {
    key: WorkOrderActionKey
    label: string
    className: 'primary' | 'outline'
  }

  /**
   * 操作按钮映射
   * @returns 操作按钮映射（根据工单状态）
   */
  const operatorActionMap: Record<WorkOrderMainStatus, OperatorAction[]> = {
    PENDING_ASSIGN: [{ key: 'TECH_ACCEPT', label: '接单', className: 'primary' }],
    PENDING_TECH_ACCEPT: [{ key: 'TECH_ACCEPT', label: '接单', className: 'primary' }],
    IN_PROGRESS: [{ key: 'REPAIR_FINISH', label: '维修登记', className: 'primary' }],
    COMPLETED: [
      { key: 'CLOSE', label: '机器返回方式', className: 'outline' },
      { key: 'REVIEW', label: '复检登记', className: 'primary' }
    ],
    CLOSED: []
  }

  /**
   * 跳转到工单详情（仅查看；故障点登记需通过「维修登记」「复检登记」按钮进入并带 action）
   * @param order 工单
   * @returns void
   */
  const onOrderClick = (order: OrderListItem) => {
    const viewOnly =
      primaryTab.value === 'transferred' || !isOrderAcceptedByCurrentCompany(order)
        ? '&viewOnly=1'
        : ''
    uni.navigateTo({
      url: `/pages/order/detail?id=${order.id}&status=${order.status}${viewOnly}`
    })
  }

  /**
   * 跳转到网点工单详情（tab 与详情页二级 Tab 一致：全部/待接单/维修中/已完成）
   * @param branch 网点
   * @param tab 二级Tab
   * @returns void
   */
  const goToBranchDetail = (branch: BranchItem, tab: SecondaryTab = 'all') => {
    uni.navigateTo({
      url: `/pages/order/branch-detail?id=${branch.id}&name=${encodeURIComponent(branch.name)}&tab=${encodeURIComponent(tab)}&total=${branch.total}&pending=${branch.pending}&processing=${branch.processing}&completed=${branch.completed}`
    })
  }

  /**
   * 获取操作按钮（按细粒度权限过滤；总部处理视图保留完整操作能力）
   * @param status 工单状态
   * @returns 操作按钮（操作按钮键、操作按钮标签、操作按钮类名）
   */
  const getOperatorActions = (status: WorkOrderMainStatus): OperatorAction[] => {
    const base = operatorActionMap[status] ?? []
    const hq = isHqProcessView.value
    return base.filter((a) => {
      if (a.key === 'TECH_ACCEPT') return userStore.hasPermission(Perms.WORKORDER_ACCEPT)
      if (a.key === 'REPAIR_FINISH') return userStore.hasPermission(Perms.WORKORDER_REPAIR) || hq
      if (a.key === 'CLOSE')
        return (
          userStore.hasPermission(Perms.WORKORDER_CLOSE) &&
          (userStore.hasPermission(Perms.WORKORDER_ACCEPT) || hq)
        )
      if (a.key === 'REVIEW') return userStore.hasPermission(Perms.WORKORDER_REVIEW) || hq
      return true
    })
  }

  /**
   * 派单权限用户：工单已指派给他人（assignedUserId 与当前账号 id 不一致）时仅可查看，不可操作
   */
  const isDispatcherOrderAssignedToOther = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    const aid = order.assignedUserId
    if (aid === undefined || aid === null) return false
    const assigned = Number(aid)
    if (!Number.isFinite(assigned) || assigned <= 0) return false
    const selfId = userStore.userInfo?.id
    if (!Number.isFinite(selfId)) return false
    return assigned !== Number(selfId)
  }

  /**
   * 派单员侧操作条（「总部处理/未转单」一级 Tab）
   * - 有派单权限：待接单/派单、维修中（有转单权时）、已完成（复检/转单按权限）
   * - 仅有转单权限：维修中、已完成仍展示转单按钮
   * @param order 工单
   * @returns 是否展示派单员侧操作条
   */
  const showDispatcherActionBlock = (order: OrderListItem) => {
    if (primaryTab.value !== 'untransferred') return false
    if (isDispatcherOrderAssignedToOther(order)) return false
    const hasAssign = userStore.hasPermission(Perms.WORKORDER_ASSIGN)
    const hasTransfer = userStore.hasPermission(Perms.WORKORDER_TRANSFER)
    if (hasAssign) {
      return (
        isPendingMainStatus(order.status) ||
        (order.status === 'IN_PROGRESS' && hasTransfer) ||
        order.status === 'COMPLETED'
      )
    }
    if (hasTransfer) {
      return order.status === 'IN_PROGRESS' || order.status === 'COMPLETED'
    }
    return false
  }

  /**
   * 是否展示工程师/总部操作条（有可用按钮时才展示）
   * @param order 工单
   * @returns 是否展示工程师/总部操作条（工程师/总部操作条：选此项表示派单给自己，进入「待接单」）
   */
  const showOperatorActions = (order: OrderListItem) => {
    if (primaryTab.value === 'transferred') return false
    if (isDispatcherOrderAssignedToOther(order)) return false
    if (!canOperateOrder.value || !canEngineerOperateTransferredOrder(order)) return false
    // 派单员 pending 仅走上方按钮区：待派单只显示「派单」、待接单只显示「接单」，不与工程师条重复
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && isPendingMainStatus(order.status))
      return false
    return getOperatorActions(order.status).length > 0
  }

  /**
   * 工程师条是否会显示「复检登记」（用于避免与派单条重复）
   * @param order 工单
   * @returns 是否显示「复检登记」（用于避免与派单条重复）
   */
  const operatorShowsRecheck = (order: OrderListItem) => {
    if (order.status !== 'COMPLETED') return false
    if (!showOperatorActions(order)) return false
    return getOperatorActions('COMPLETED').some((a) => a.key === 'REVIEW')
  }

  /** 承修方端暂不展示「上传寄件单号」（与详情页一致；后端仍可能下发 UPLOAD_SEND_EXPRESS） */
  const getOrderAvailableActions = (order: OrderListItem): WorkOrderActionKey[] =>
    normalizeAvailableActions(
      (order as OrderListItem & { availableActions?: unknown }).availableActions
    ).filter((key) => key !== 'UPLOAD_SEND_EXPRESS')

  const getOrderRoleForRegression = (
    order: OrderListItem
  ): 'hq' | 'dispatcher' | 'engineer' | 'unknown' => {
    if (isHqProcessView.value) return 'hq'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return 'dispatcher'
    if (
      userStore.hasPermission(Perms.WORKORDER_ACCEPT) ||
      userStore.hasPermission(Perms.WORKORDER_REPAIR) ||
      userStore.hasPermission(Perms.WORKORDER_REVIEW)
    ) {
      return 'engineer'
    }
    if (order.transferred) return 'engineer'
    return 'unknown'
  }

  const getActionLabel = (actionKey: WorkOrderActionKey) => {
    if (actionKey === 'ASSIGN') return '派单'
    if (actionKey === 'TECH_ACCEPT') return '接单'
    if (actionKey === 'TRANSFER') return '转单'
    if (actionKey === 'REPAIR_FINISH') return '维修登记'
    if (actionKey === 'REVIEW') return '复检登记'
    if (actionKey === 'CLOSE') return '机器返回方式'
    return ''
  }

  const getActionClassName = (actionKey: WorkOrderActionKey): OperatorAction['className'] =>
    actionKey === 'CLOSE' ? 'outline' : 'primary'

  /**
   * 基于权限与业务兜底过滤动作：
   * - 权限二次过滤（防后端误下发）
   * - 派给他人只可查看
   * - 转出网点不可操作
   */
  const isActionAllowed = (order: OrderListItem, actionKey: WorkOrderActionKey) => {
    if (primaryTab.value === 'transferred') return false
    if (!isOrderAcceptedByCurrentCompany(order)) return false
    if (isDispatcherOrderAssignedToOther(order)) return false

    if (actionKey === 'ASSIGN') return userStore.hasPermission(Perms.WORKORDER_ASSIGN)
    if (actionKey === 'TECH_ACCEPT')
      return (
        userStore.hasPermission(Perms.WORKORDER_ACCEPT) &&
        canCurrentSiteOperateTransferredOrder(
          !!order.transferred,
          order.transferFromSite,
          userStore.currentNetworkName
        )
      )
    if (actionKey === 'TRANSFER') return userStore.hasPermission(Perms.WORKORDER_TRANSFER)
    if (actionKey === 'REPAIR_FINISH')
      return (
        (userStore.hasPermission(Perms.WORKORDER_REPAIR) || isHqProcessView.value) &&
        canCurrentSiteOperateTransferredOrder(
          !!order.transferred,
          order.transferFromSite,
          userStore.currentNetworkName
        )
      )
    if (actionKey === 'REVIEW')
      return userStore.hasPermission(Perms.WORKORDER_REVIEW) || isHqProcessView.value
    if (actionKey === 'CLOSE')
      return (
        userStore.hasPermission(Perms.WORKORDER_CLOSE) &&
        (userStore.hasPermission(Perms.WORKORDER_ACCEPT) || isHqProcessView.value)
      )
    return false
  }

  const getActionButtons = (actionKeys: WorkOrderActionKey[]): OperatorAction[] =>
    actionKeys.map((key) => ({
      key,
      label: getActionLabel(key),
      className: getActionClassName(key)
    }))

  /**
   * 状态回退逻辑：当接口未返回 availableActions 时，沿用原先状态驱动按钮。
   * 仅用于过渡期兜底，后续接口稳定后回收（保留审计埋点）。
   */
  const getFallbackStatusActions = (order: OrderListItem): WorkOrderActionKey[] => {
    const actionKeys: WorkOrderActionKey[] = []
    const pushUnique = (actionKey: WorkOrderActionKey) => {
      if (!actionKeys.includes(actionKey)) actionKeys.push(actionKey)
    }

    if (showDispatcherActionBlock(order)) {
      if (isOrderPendingAssign(order)) pushUnique('ASSIGN')
      else if (isOrderPendingTechAccept(order)) pushUnique('TECH_ACCEPT')
      else if (
        order.status === 'IN_PROGRESS' &&
        userStore.hasPermission(Perms.WORKORDER_TRANSFER)
      ) {
        pushUnique('TRANSFER')
      } else if (order.status === 'COMPLETED') {
        if (userStore.hasPermission(Perms.WORKORDER_REVIEW) && !operatorShowsRecheck(order))
          pushUnique('REVIEW')
        if (userStore.hasPermission(Perms.WORKORDER_TRANSFER)) pushUnique('TRANSFER')
      }
    }

    if (showOperatorActions(order)) {
      getOperatorActions(order.status).forEach((action) => {
        pushUnique(action.key)
      })
    }

    return actionKeys
  }

  /**
   * 列表按钮渲染优先级：
   * 1. 有 availableActions 时按后端动作渲染；
   * 2. 前端再做权限二次过滤；
   * 3. 无 availableActions 时才回退旧状态逻辑（过渡期）。
   */
  const getVisibleActions = (order: OrderListItem): OperatorAction[] => {
    const availableActionKeys = getOrderAvailableActions(order)
    let candidateActionKeys = availableActionKeys
    if (!availableActionKeys.length && ENABLE_LEGACY_STATUS_ACTION_FALLBACK) {
      candidateActionKeys = getFallbackStatusActions(order)
      auditLegacyStatusFallbackHit({
        orderId: order.id,
        role: getOrderRoleForRegression(order),
        status: order.status,
        primaryTab: primaryTab.value,
        secondaryTab: secondaryTab.value,
        fallbackActions: candidateActionKeys
      })
    }
    return getActionButtons(candidateActionKeys.filter((key) => isActionAllowed(order, key)))
  }

  /**
   * 接单：进入详情填写故障判定与维修报价，用户提交后再调接单接口（与首页一致）
   * @param orderId 工单ID
   * @returns void
   */
  const onAcceptOrder = (orderId: string) => {
    const id = Number(orderId)
    if (!Number.isFinite(id) || id <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
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
  /** 「无故障」闭环：关闭工单 PUT 需携带刚确认的返回方式 */
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
   */
  const onReturnMethodConfirm = async (data: ReturnMethodConfirmPayload) => {
    currentReturnMethodType.value = data.type
    const id = Number(currentReturnOrderId.value)
    if (!Number.isFinite(id) || id <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
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

      uni.showLoading({ title: '提交中...' })
      try {
        const res = await closeWorkOrder(dto)
        closeOrderReturnMethodPayload.value = null
        uni.showToast({ title: getApiMessage(res, '工单已关闭'), icon: 'none', duration: 1500 })
        setTimeout(() => {
          refreshOrders()
        }, 300)
      } catch {
        // closeWorkOrder 内已 toast
      } finally {
        uni.hideLoading()
      }
      return
    }

    if (judge === '无故障') {
      uni.showToast({
        title: `工单 ${currentReturnOrderId.value} 已选择 ${data.type === 'self' ? '自提' : '回寄'}`,
        icon: 'none'
      })
      closeOrderReturnMethodPayload.value = data
      setTimeout(() => {
        showCloseOrderModal.value = true
      }, 500)
      return
    }

    uni.showToast({ title: '工单状态未就绪，请稍后重试', icon: 'none' })
  }

  /**
   * 确认工单关闭
   * @param reason 关闭原因
   * @returns void
   */
  const onCloseOrderConfirm = async (reason: string) => {
    const id = Number(currentReturnOrderId.value)
    if (!Number.isFinite(id) || id <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    const payload = closeOrderReturnMethodPayload.value
    if (!payload) {
      uni.showToast({ title: '请先完成机器返回方式', icon: 'none' })
      return
    }

    const cr = (reason || '').trim()
    if (!cr) {
      uni.showToast({ title: '请填写关闭原因（无故障必填）', icon: 'none' })
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

    uni.showLoading({ title: '正在关闭...' })
    try {
      const res = await closeWorkOrder(dto)
      closeOrderReturnMethodPayload.value = null
      uni.showToast({ title: getApiMessage(res, '工单已关闭'), icon: 'none', duration: 1500 })
      // 关闭成功后刷新列表（避免本地状态与后端不一致）
      setTimeout(() => {
        refreshOrders()
      }, 300)
    } catch {
      // closeWorkOrder 内已 toast；这里兜底避免 loading 不消失
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 复检登记：进入详情，与维修登记同款表单，顶部状态为已完成
   * @param orderId 工单ID
   * @returns void
   */
  const onRecheck = (orderId: string) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${orderId}&status=COMPLETED&action=recheck`
    })
  }

  /**
   * 动作统一分发：将工单动作语义映射到当前页面既有行为实现。
   */
  const workOrderActionHandlers: Record<
    'ASSIGN' | 'TECH_ACCEPT' | 'TRANSFER' | 'REPAIR_FINISH' | 'REVIEW' | 'CLOSE',
    (orderId: string) => void
  > = {
    ASSIGN: (orderId) => openAssignModal(orderId),
    TECH_ACCEPT: (orderId) => onAcceptOrder(orderId),
    TRANSFER: (orderId) => {
      void openTransferModal(orderId)
    },
    REPAIR_FINISH: (orderId) => onRepairRegister(orderId),
    REVIEW: (orderId) => onRecheck(orderId),
    CLOSE: (orderId) => onReturnMethod(orderId)
  }

  /**
   * 处理工单动作点击。
   * @param actionKey 工单动作 key
   * @param orderId 工单ID
   * @returns void
   */
  const dispatchWorkOrderAction = (actionKey: WorkOrderActionKey, orderId: string | number) => {
    const id = String(orderId ?? '').trim()
    if (!id) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }
    const orderRow = orderList.value.find((o) => o.id === id)
    if (orderRow && !isOrderAcceptedByCurrentCompany(orderRow)) {
      uni.showToast({ title: '受理方非您所在主体，仅可查看', icon: 'none' })
      return
    }
    if (!(actionKey in workOrderActionHandlers)) {
      uni.showToast({ title: '暂不支持该操作', icon: 'none' })
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
