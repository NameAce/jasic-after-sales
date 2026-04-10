<template>
  <view class="page-index order-list-page">
    <!-- 维修工程师/派单员视图 -->
    <CustomNavBar title="工单列表" surface="sticky" :shadow="false" :show-back="false">
      <!-- 搜索栏 -->
      <view class="search-wrap">
        <view class="search-box">
          <uni-icons type="search" size="18" color="#94a3b8" class="search-icon"></uni-icons>
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
    <scroll-view v-if="showBranchView" class="main-content" scroll-y>
      <!-- 网点统计概览 -->
      <view class="branch-summary-header">
        <text class="branch-summary-title">网点统计概览</text>
        <text class="branch-summary-count">共 {{ branchList.length }} 个分中心</text>
      </view>
      <!-- 网点列表 -->
      <view class="branch-list-container">
        <view v-for="branch in branchList" :key="branch.id" class="branch-card">
          <view class="branch-card-header" @tap="goToBranchDetail(branch, 'all')">
            <view class="branch-info">
              <view class="branch-icon-wrap">
                <image class="branch-icon" :src="storeIcon" mode="aspectFit" />
              </view>
              <text class="branch-name">{{ branch.name }}</text>
            </view>
            <uni-icons type="right" size="24" color="#cbd5e1"></uni-icons>
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
    </scroll-view>

    <!-- 工单列表视图 -->
    <scroll-view
      v-else
      class="main-content order-list-scroll"
      scroll-y
      lower-threshold="120"
      @scrolltolower="loadMoreOrders"
    >
      <OrderCardList
        :orders="filteredOrders"
        :status-text="listStatusText"
        :empty-title="listEmptyTitle"
        :empty-desc="listEmptyDesc"
        :show-inbound-transfer-tag="showInboundTransferTag"
        :show-transferred-tag="
          (order) => !!order.transferred && !isHqUser
        "
        :show-no-more="filteredOrders.length > 0 && hasLoadedAll"
        @order-click="onOrderClick"
      >
        <!-- 额外信息 -->
        <template #extra-info="{ order }">
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
                v-if="order.status === 'pending' && !isDispatcherAwaitSelfAccept(order)"
                class="btn-action primary"
                @tap.stop="openAssignModal(order.id)"
              >
                派单
              </button>
              <button
                v-else-if="order.status === 'pending' && isDispatcherAwaitSelfAccept(order)"
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
                  v-if="order.isJiashi && userStore.hasPermission(Perms.WORKORDER_TRANSFER)"
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
      :technician-list="technicianList"
      @close="closeAssignModal"
      @confirm="onAssignConfirm"
    />
    <!-- 机器返回方式弹窗 -->
    <ReturnMethodModal
      v-model="showReturnMethodModal"
      :initial-mail="returnMethodInitialMail"
      @confirm="onReturnMethodConfirm"
    />

    <!-- 工单关闭弹窗 -->
    <CloseOrderModal v-model="showCloseOrderModal" @confirm="onCloseOrderConfirm" />
  </view>
</template>

<script setup lang="ts">
  /**
   * 工单库：路由仅要求登录；列表 Tab 与操作按钮用 Perms + userStore.hasPermission / canAny / canAll。
   */
  import { ref, computed, nextTick, watch } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { useAppStore, useUserStore } from '@/stores'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import TabBar from '@/components/TabBar/TabBar.vue'
  import OrderCardList from '@/components/OrderCardList/OrderCardList.vue'
  import TransferModal from '@/components/TransferModal/TransferModal.vue'
  import AssignTechnicianModal, {
    type Technician
  } from '@/components/AssignTechnicianModal/AssignTechnicianModal.vue'
  import ReturnMethodModal from '@/components/ReturnMethodModal/ReturnMethodModal.vue'
  import CloseOrderModal from '@/components/CloseOrderModal/CloseOrderModal.vue'
  import {
    fetchBranchList,
    fetchAssignUserOptions,
    fetchOrderDetail,
    fetchOrderListPage,
    closeWorkOrder,
    techAcceptWorkOrder,
    transferWorkOrder,
    fetchTransferTargetOptions,
    type OrderListQuery
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
  import { storeIcon } from '@/svgs'

  type PrimaryTab = 'untransferred' | 'transferred'
  type SecondaryTab = 'all' | 'pending' | 'pending_accept' | 'processing' | 'completed' | 'closed'

  /** 派单弹窗：选此项表示派单给自己，进入「待接单」 */
  const DISPATCHER_SELF_TECH_ID = 'dispatcher_self'
  // 派单员侧操作条：选此项表示派单给自己，进入「待接单」
  const isDispatcherAwaitSelfAccept = (order: OrderListItem) =>
    order.dispatcherPendingSubState === 'await_self_accept'

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
  const showBranchView = computed(
    () => isHqUser.value && primaryTab.value === 'transferred'
  )

  // 是否为总部"总部处理"视图
  const isHqProcessView = computed(
    () => isHqUser.value && primaryTab.value === 'untransferred'
  )
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

  /**
   * 列表卡片状态文案：派单员视角区分「待派单 / 待接单」
   * @param order 工单
   * @returns 状态文本
   */
  const listStatusText = (order: OrderListItem) => {
    const status = order.status
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && status === 'pending') {
      return isDispatcherAwaitSelfAccept(order) ? '待接单' : '待派单'
    }
    return statusTextMap[status]
  }

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

  /**
   * 从其他页面（如"我的"页面）跳转过来时，应用目标 tab
   * @returns void
   */
  onShow(() => {
    const target = appStore.consumeOrderListNavTarget()
    if (target) {
      primaryTab.value = target.primaryTab
      let sec = target.secondaryTab
      if (sec === 'pending_accept' && !userStore.hasPermission(Perms.WORKORDER_ASSIGN))
        sec = 'pending'
      secondaryTab.value = sec
      scrollSecondaryTabIntoView(sec)
    }

    if (showBranchView.value) {
      baseOrderList.value = []
      refreshBranches()
      return
    }
    refreshOrders()
    if (isHqUser.value) refreshBranches()
  })

  // ==================== 工单列表 ====================
  // 派单员在弹窗中选择「本人」后，本地标记为待本人接单（演示用，对接接口后可删）
  const selfAssignedPendingOrderIds = ref<Set<string>>(new Set())

  const baseOrderList = ref<OrderListItem[]>([])
  const pageNum = ref(1)
  const pageSize = 20
  const totalOrders = ref(0)
  const loadingMore = ref(false)
  const requestVersion = ref(0)
  const hasLoadedAll = computed(
    () => baseOrderList.value.length >= totalOrders.value && totalOrders.value > 0
  )

  /**
   * 二级 Tab → 接口 mainStatus（与后端字典一致时可再调整映射）
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
        companyId: userStore.userInfo?.currentCompanyId
      }

      // 与占位「工单号或故障描述」对齐：接口无故障描述字段时用工单号模糊；条码可改为传 barcode
      if (q) query.orderNo = q

      // 一级 Tab：未转单/已转单由接口字段 hasTransfer 区分
      query.hasTransfer = primary === 'transferred' ? 1 : 0

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
        companyId: userStore.userInfo?.currentCompanyId
      }
      if (q) query.orderNo = q
      query.hasTransfer = primary === 'transferred' ? 1 : 0
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

  /**
   * 与 mock 全量工单同源，并合并派单给自己的本地状态
   * @returns 工单列表（包含派单给自己后的本地状态）
   */
  const orderList = computed<OrderListItem[]>(() => {
    const base = baseOrderList.value
    const local = selfAssignedPendingOrderIds.value
    return base.map((o) => {
      if (local.has(o.id)) {
        return { ...o, dispatcherPendingSubState: 'await_self_accept' as const }
      }
      return o
    })
  })

  /**
   * 过滤工单列表
   * @returns 过滤后的工单列表（根据一级Tab、二级Tab、搜索关键词过滤）
   */
  const filteredOrders = computed(() => {
    const q = searchQuery.value?.trim()
    const primary = primaryTab.value
    const secondary = secondaryTab.value
    // 过滤工单列表
    return orderList.value.filter((o) => {
      // 总部处理视图：已转单时不展示
      if (!isHqUser.value) {
        const isTransferred = !!o.transferred
        if (primary === 'untransferred' && isTransferred) return false
        if (primary === 'transferred' && !isTransferred) return false
      }
      // 二级Tab：全部时，不进行过滤
      if (secondary === 'all') {
        // pass
      } else if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && secondary === 'pending') {
        if (o.status !== 'pending' || isDispatcherAwaitSelfAccept(o)) return false
      } else if (userStore.hasPermission(Perms.WORKORDER_ASSIGN) && secondary === 'pending_accept') {
        if (o.status !== 'pending' || !isDispatcherAwaitSelfAccept(o)) return false
      } else if (o.status !== secondary) {
        return false
      }
      // 搜索关键词：不进行过滤
      if (!q) return true
      return (
        o.id.includes(q) ||
        (o.orderNo?.includes(q) ?? false) ||
        (o.barcode?.includes(q) ?? false) ||
        o.desc.includes(q)
      )
    })
  })

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

  const refreshBranches = async () => {
    try {
      baseBranchList.value = await fetchBranchList()
    } catch {
      baseBranchList.value = []
    }
  }

  // 网点列表（总部-网点工单）
  const branchList = computed<BranchItem[]>(() => baseBranchList.value)

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
  const selectedTechId = ref<number | string | null>(1)

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

      uni.showToast({ title: getApiMessage(res, '转单已提交'), icon: 'success' })
      // 提交成功后刷新列表（避免本地状态与后端不一致）
      setTimeout(() => {
        refreshOrders()
      }, 300)
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
  const openAssignModal = (orderId: string) => {
    currentOrderId.value = orderId
    showAssignModal.value = true
    selectedTechId.value = null

    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) {
      technicianList.value = []
      return
    }

    const base: Technician[] = [
      {
        id: DISPATCHER_SELF_TECH_ID,
        name: '本人（派单员）',
        avatar: '',
        isRecommend: true,
        desc: '派单给自己后，在「待接单」中接单维修',
        distance: '',
        time: '',
        isBusy: false
      }
    ]
    technicianList.value = base

    const workOrderId = Number(orderId)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) return
    fetchAssignUserOptions(workOrderId)
      .then((list) => {
        const mapped: Technician[] = list.map((u) => ({
          id: u.id,
          name: u.realName || u.phone || `用户${u.id}`,
          phone: u.phone || '',
          avatar: '',
          desc: u.phone || '',
          isRecommend: false,
          distance: '',
          time: '',
          isBusy: false
        }))
        technicianList.value = base.concat(mapped)
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
    technicianList.value = []
  }

  /**
   * 确认派单
   * @returns void
   */
  const onAssignConfirm = () => {
    const oid = currentOrderId.value
    if (selectedTechId.value === DISPATCHER_SELF_TECH_ID && oid) {
      selfAssignedPendingOrderIds.value = new Set(selfAssignedPendingOrderIds.value).add(oid)
      uni.showToast({ title: '已派单给自己，可在「待接单」中接单', icon: 'none' })
    } else {
      uni.showToast({ title: '派单成功', icon: 'success' })
    }
    closeAssignModal()
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
      if (a.key === 'returnMethod') return userStore.hasPermission(Perms.WORKORDER_ACCEPT) || hq
      if (a.key === 'recheck') return userStore.hasPermission(Perms.WORKORDER_REVIEW) || hq
      return true
    })
  }

  /**
   * 派单员侧操作条（与「总部处理」一级 Tab 及状态组合条件，与模板原 v-if 一致）
   * @param order 工单
   * @returns 是否展示派单员侧操作条（派单员侧操作条：选此项表示派单给自己，进入「待接单」）
   */
  const showDispatcherActionBlock = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN) || primaryTab.value !== 'untransferred')
      return false
    return (
      order.status === 'pending' ||
      (order.status === 'processing' && order.isJiashi) ||
      order.status === 'completed'
    )
  }

  /**
   * 是否展示工程师/总部操作条（有可用按钮时才展示）
   * @param order 工单
   * @returns 是否展示工程师/总部操作条（工程师/总部操作条：选此项表示派单给自己，进入「待接单」）
   */
  const showOperatorActions = (order: OrderListItem) => {
    if (primaryTab.value === 'transferred') return false
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
   * 接单
   * @param orderId 工单ID
   * @returns void
   */
  const onAcceptOrder = async (orderId: string) => {
    const id = Number(orderId)
    if (!Number.isFinite(id) || id <= 0) {
      uni.showToast({ title: '工单ID无效', icon: 'none' })
      return
    }

    uni.showLoading({ title: '正在接单...' })
    try {
      const res = await techAcceptWorkOrder({ workOrderId: id })
      uni.showToast({ title: getApiMessage(res, '接单成功'), icon: 'success' })
      setTimeout(() => {
        refreshOrders()
      }, 300)
    } catch {
      // http.ts / api 内已 toast；这里兜底避免 loading 不消失
    } finally {
      uni.hideLoading()
    }
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
  // 当前选择的机器返回方式（用于关闭工单入参）
  const currentReturnMethodType = ref<'' | 'self' | 'mail'>('')
  // 工单关闭弹窗
  const showCloseOrderModal = ref(false)

  /**
   * 机器返回方式初始邮件
   * @returns 机器返回方式初始邮件
   */
  const returnMethodInitialMail = computed(() =>
    getReturnMethodInitialMail(currentReturnOrderDetail.value)
  )

  /**
   * 打开机器返回方式弹窗
   * @param orderId 工单ID
   * @returns void
   */
  const onReturnMethod = (orderId: string) => {
    currentReturnOrderId.value = orderId
    currentReturnOrderDetail.value = undefined
    currentReturnMethodType.value = ''
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
   * 确认机器返回方式
   * @param data 机器返回方式数据
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
    currentReturnMethodType.value = data.type
    uni.showToast({
      title: `工单 ${currentReturnOrderId.value} 已选择 ${data.type === 'self' ? '自提' : '回寄'}`,
      icon: 'none'
    })
    // 与详情页一致：仅「无故障」维修完成流程需在返回方式后填写关闭原因；有故障不弹关闭工单
    const detail = currentReturnOrderDetail.value
    if (detail?.repair?.faultJudge === '无故障') {
      setTimeout(() => {
        showCloseOrderModal.value = true
      }, 500)
    }
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
    const method = currentReturnMethodType.value
    if (method !== 'self' && method !== 'mail') {
      uni.showToast({ title: '请先选择机器返回方式', icon: 'none' })
      return
    }

    uni.showLoading({ title: '正在关闭...' })
    try {
      const res = await closeWorkOrder({
        workOrderId: id,
        closeReason: (reason || '').trim(),
        returnMethod: method === 'self' ? 'SELF' : 'MAIL',
        returnExpressNo: '',
        returnVoucherFileIds: []
      })
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
      border-bottom: 2rpx solid $surface-slate-100;

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
      background-color: $surface-slate-50;

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
      background-color: $surface-slate-50;
    }

    .branch-card {
      @include sheet-white($space-lg);
      border: 2rpx solid $surface-slate-100;

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
          color: $surface-slate-300;
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
