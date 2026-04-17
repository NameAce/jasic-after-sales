<template>
  <view class="page-index order-list-page">
    <!-- 维修工程师/派单员视图 -->
    <CustomNavBar title="工单列表" surface="sticky" :shadow="false" :show-back="false">
      <!-- 搜索栏 -->
      <view class="search-wrap">
        <view class="search-box">
          <uni-icons type="search" size="18" :color="themeColors.textMuted" class="search-icon"></uni-icons>
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
            <uni-icons type="right" size="24" :color="themeColors.iconSlateLight"></uni-icons>
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
        :show-repair-site-rows="primaryTab === 'transferred'"
        :show-no-more="orderList.length > 0 && hasLoadedAll"
        @order-click="onOrderClick"
      >
        <!-- 额外信息 -->
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
          <!-- 单容器内横向排列：总部管理员等同时有派单条与工程师条时，避免两个 action-wrap 上下堆叠 -->
          <view
            v-if="showDispatcherActionBlock(order) || showOperatorActions(order)"
            class="action-wrap"
          >
            <template v-if="showDispatcherActionBlock(order)">
              <button
                v-if="order.status === 'pending' && isOrderPendingAssign(order)"
                class="btn-action primary"
                @tap.stop="openAssignModal(order.id)"
              >
                派单
              </button>
              <button
                v-else-if="order.status === 'pending' && isOrderPendingTechAccept(order)"
                class="btn-action primary"
                @tap.stop="onAcceptOrder(order.id)"
              >
                接单
              </button>
              <template v-else-if="order.status === 'processing'">
                <button
                  v-if="userStore.hasPermission(Perms.WORKORDER_TRANSFER)"
                  class="btn-action primary"
                  @tap.stop="openTransferModal(order.id)"
                >
                  转单
                </button>
              </template>
              <template v-else-if="order.status === 'completed'">
                <button
                  v-if="
                    userStore.hasPermission(Perms.WORKORDER_REVIEW) && !operatorShowsRecheck(order)
                  "
                  class="btn-action outline"
                  @tap.stop="onRecheck(order.id)"
                >
                  复检登记
                </button>
                <button
                  v-if="userStore.hasPermission(Perms.WORKORDER_TRANSFER)"
                  class="btn-action primary"
                  @tap.stop="openTransferModal(order.id)"
                >
                  转单
                </button>
              </template>
            </template>
            <!-- 操作 -->
            <template v-if="showOperatorActions(order)">
              <button
                v-for="action in getOperatorActions(order.status)"
                :key="`${order.id}-${action.key}`"
                :class="`btn-action ${action.className}`"
                @tap.stop="handleOperatorAction(action.key, order.id)"
              >
                {{ action.label }}
              </button>
            </template>
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
  import { onShow } from '@dcloudio/uni-app'
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
    fetchBranchList,
    fetchAssignUserOptions,
    fetchOrderDetail,
    fetchOrderListPage,
    closeWorkOrder,
    transferWorkOrder,
    fetchTransferTargetOptions,
    WORK_ORDER_FAULT_CLOSE_REASON,
    type OrderListQuery,
    type ReturnMethodConfirmPayload
  } from '@/api/order'
  import {
    getReturnMethodInitialMail,
    type BranchItem,
    type OrderDetail,
    type OrderListItem,
    type OrderStatus
  } from '@/models/order'
  import {
    canCurrentSiteOperateTransferredOrder,
    hasInboundTransferFromSite
  } from '@/utils/orderTransfer'
  import { ORDER_STATUS_TEXT_MAP } from '@/utils/orderStatus'
  import { Perms } from '@/utils/permissions'
  import { getApiMessage } from '@/utils/http'
  import { takeSelectedShippingAddress } from '@/utils/addressStorage'
  import { storeIcon } from '@/svgs'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'
  import { isWorkOrderPendingTechAcceptMainStatus } from '@/utils/workOrderMainStatus'

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
  const isOrderPendingTechAccept = (order: OrderListItem) =>
    order.status === 'pending' && isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)

  /** 待派单：pending 且非 PENDING_TECH_ACCEPT（含 PENDING_ASSIGN、空值等） */
  const isOrderPendingAssign = (order: OrderListItem) =>
    order.status === 'pending' && !isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)

  /**
   * 列表卡片状态文案：派单员视角按接口 mainStatus 区分「待派单 / 待接单」
   */
  const listStatusText = (order: OrderListItem) => {
    const status = order.status
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && status === 'pending') {
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
   * 从其他页面（如"我的"页面）跳转过来时，应用目标 tab；
   * 表单提交等场景若已标记，则走 scroll-view 下拉刷新（refresher）以与手动下拉一致。
   * @returns void
   */
  onShow(async () => {
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

    if (showBranchView.value) {
      baseOrderList.value = []
      if (useScrollRefresherUi) await onBranchViewRefresherRefresh()
      else await refreshBranches()
      return
    }
    if (useScrollRefresherUi) await onOrderListRefresherRefresh()
    else await refreshOrders()
    if (isHqUser.value) await refreshBranches()
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
      const q = searchQuery.value?.trim()
      const primary = primaryTab.value
      const secondary = secondaryTab.value

      const query: OrderListQuery = {
        pageNum: pageNum.value,
        pageSize,
        companyId: userStore.userInfo?.currentCompanyId,
        viewScope: 'ALL',
        hasTransfer: primary === 'transferred' ? 1 : 0
      }

      // 与接口约定一致：含中文→客户姓名模糊；长数字→条码；否则工单号模糊（避免多条件 AND 同时传）
      applyWorkOrderListSearchKeyword(query, q)

      const ms = secondaryTabToMainStatus(secondary)
      if (ms !== undefined) query.mainStatus = ms

      const page = await fetchOrderListPage(query)
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
      const q = searchQuery.value?.trim()
      const primary = primaryTab.value
      const secondary = secondaryTab.value
      const nextPage = pageNum.value + 1
      const query: OrderListQuery = {
        pageNum: nextPage,
        pageSize,
        companyId: userStore.userInfo?.currentCompanyId,
        viewScope: primary === 'transferred' ? 'HISTORY' : 'CURRENT'
      }
      applyWorkOrderListSearchKeyword(query, q)
      const ms = secondaryTabToMainStatus(secondary)
      if (ms !== undefined) query.mainStatus = ms

      const page = await fetchOrderListPage(query)
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
      baseBranchList.value = await fetchBranchList()
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
      const list = await fetchTransferTargetOptions(workOrderId)
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

      uni.showToast({ title: getApiMessage(res, '转单已提交'), icon: 'success' })
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
    fetchAssignUserOptions(workOrderId)
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
        uni.showToast({ title: getApiMessage(res, '派单成功'), icon: 'success' })
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

  type OperatorActionKey = 'accept' | 'repair' | 'returnMethod' | 'recheck'
  // 操作按钮（操作按钮键、操作按钮标签、操作按钮类名）
  type OperatorAction = {
    key: OperatorActionKey
    label: string
    className: 'primary' | 'outline'
  }

  /**
   * 操作按钮映射
   * @returns 操作按钮映射（根据工单状态）
   */
  const operatorActionMap: Record<OrderStatus, OperatorAction[]> = {
    pending: [{ key: 'accept', label: '接单', className: 'primary' }],
    processing: [{ key: 'repair', label: '维修登记', className: 'primary' }],
    completed: [
      { key: 'returnMethod', label: '机器返回方式', className: 'outline' },
      { key: 'recheck', label: '复检登记', className: 'primary' }
    ],
    closed: []
  }

  /**
   * 跳转到工单详情（仅查看；故障点登记需通过「维修登记」「复检登记」按钮进入并带 action）
   * @param order 工单
   * @returns void
   */
  const onOrderClick = (order: OrderListItem) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${order.id}&status=${order.status}`
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
      url: `/pages/order/branch-detail?id=${branch.id}&name=${encodeURIComponent(branch.name)}&tab=${encodeURIComponent(tab)}`
    })
  }

  /**
   * 获取操作按钮（按细粒度权限过滤；总部处理视图保留完整操作能力）
   * @param status 工单状态
   * @returns 操作按钮（操作按钮键、操作按钮标签、操作按钮类名）
   */
  const getOperatorActions = (status: OrderStatus): OperatorAction[] => {
    const base = operatorActionMap[status] ?? []
    const hq = isHqProcessView.value
    return base.filter((a) => {
      if (a.key === 'accept') return userStore.hasPermission(Perms.WORKORDER_ACCEPT)
      if (a.key === 'repair') return userStore.hasPermission(Perms.WORKORDER_REPAIR) || hq
      if (a.key === 'returnMethod')
        return (
          userStore.hasPermission(Perms.WORKORDER_CLOSE) &&
          (userStore.hasPermission(Perms.WORKORDER_ACCEPT) || hq)
        )
      if (a.key === 'recheck') return userStore.hasPermission(Perms.WORKORDER_REVIEW) || hq
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
        order.status === 'pending' ||
        (order.status === 'processing' && hasTransfer) ||
        order.status === 'completed'
      )
    }
    if (hasTransfer) {
      return order.status === 'processing' || order.status === 'completed'
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
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && order.status === 'pending') return false
    return getOperatorActions(order.status).length > 0
  }

  /**
   * 工程师条是否会显示「复检登记」（用于避免与派单条重复）
   * @param order 工单
   * @returns 是否显示「复检登记」（用于避免与派单条重复）
   */
  const operatorShowsRecheck = (order: OrderListItem) => {
    if (order.status !== 'completed') return false
    if (!showOperatorActions(order)) return false
    return getOperatorActions('completed').some((a) => a.key === 'recheck')
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
    fetchOrderDetail(orderId)
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
        uni.showToast({ title: getApiMessage(res, '工单已关闭'), icon: 'success' })
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
      uni.showToast({ title: getApiMessage(res, '工单已关闭'), icon: 'success' })
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
      url: `/pages/order/detail?id=${orderId}&status=completed&action=recheck`
    })
  }

  /**
   * 处理操作按钮点击
   * @param actionKey 操作按钮键
   * @param orderId 工单ID
   * @returns void
   */
  const handleOperatorAction = (actionKey: OperatorActionKey, orderId: string) => {
    if (actionKey === 'accept') {
      onAcceptOrder(orderId)
      return
    }
    if (actionKey === 'repair') {
      onRepairRegister(orderId)
      return
    }
    if (actionKey === 'returnMethod') {
      onReturnMethod(orderId)
      return
    }
    onRecheck(orderId)
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
      padding: $space-sm $space-lg;
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
