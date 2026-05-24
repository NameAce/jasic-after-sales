<template>
  <!-- 与工单列表页一致：固定视口高度 + scroll-view 内滚动，保证触底加载与下拉刷新 -->
  <view class="page-index order-list-page">
    <CustomNavBar :title="`${branchName}`" surface="sticky" :shadow="false" />
    <view class="page-container branch-order-page">
      <view class="header">
        <!-- 搜索栏 -->
        <view class="search-wrap">
          <view class="search-box">
            <uni-icons type="search" size="18" color="#cbd5e1"></uni-icons>
            <input
              v-model="searchQuery"
              class="search-input"
              placeholder="搜索工单号或故障描述"
              placeholder-class="placeholder-text"
            />
          </view>
        </view>
        <!-- 二级Tab栏 -->
        <TabBar
          variant="underline"
          tone="sheet"
          underline="rounded"
          padding="md"
          :sticky="true"
          :tabs="tabs"
          :model-value="activeTab"
          :scrollable="true"
          @change="setTab"
        />
      </view>

      <!-- 主内容区域 -->
      <scroll-view
        class="main-content order-list-scroll"
        scroll-y
        lower-threshold="120"
        refresher-enabled
        :refresher-triggered="refresherTriggered"
        @refresherrefresh="onRefresherRefresh"
        @scrolltolower="loadMoreBranchOrders"
      >
        <!-- 与 list.vue 一致：scroll-view 内直接挂 OrderCardList，卡片间距由 order-pages 中 .list-container 承担 -->
        <OrderCardList
          :orders="orderList"
          :status-text="listStatusText"
          :empty-title="listEmptyTitle"
          :empty-desc="listEmptyDesc"
          :show-inbound-transfer-tag="showInboundTransferTag"
          :show-transferred-tag="showTransferredTag"
          show-repair-site-rows
          :show-no-more="orderList.length > 0 && hasLoadedAll"
          @order-click="onOrderClick"
        />
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import OrderCardList from '@/components/OrderCardList/OrderCardList.vue'
  import TabBar from '@/components/TabBar/TabBar.vue'
  import {
    applyWorkOrderListSearchKeyword,
    listHqSiteOrders,
    type HqSiteOrdersDisplayStatus
  } from '@/api/workOrder'
  import type { OrderListItem } from '@/models/order'
  import { ORDER_STATUS_TEXT_MAP } from '@/utils/orderStatus'
  import { hasInboundTransferFromSite } from '@/utils/orderTransfer'
  import { useScrollRefresher } from '@/utils/useScrollRefresher'
  import { useUserStore } from '@/stores'

  const userStore = useUserStore()
  /**
   * 与 list.vue 一致：总部用户不在卡片上展示「已转单」角标
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isHqUser = computed(() => {
    const code = userStore.userInfo?.currentTypeCode
    return !!code?.startsWith('HQ')
  })

  /**
   * 由其他网点转入本网点时展示「转单」标记（与 list.vue 未转单 Tab 逻辑同源，网点明细无一级 Tab 故始终按转入判断）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const showInboundTransferTag = (order: OrderListItem) =>
    hasInboundTransferFromSite(order.transferFromSite)

  const showTransferredTag = (order: OrderListItem) => !!order.transferred && !isHqUser.value
  // 网点名称
  const branchName = ref('')
  // 搜索关键词
  const searchQuery = ref('')
  // 当前二级Tab
  const activeTab = ref<BranchDetailTab>('all')
  // 工单列表
  const orderList = ref<OrderListItem[]>([])
  const pageNum = ref(1)
  const pageSize = 10
  const totalOrders = ref(0)
  const loadingMore = ref(false)
  const requestVersion = ref(0)
  const siteCompanyId = ref<number>(0)
  const searchDebounceTimer = ref<ReturnType<typeof setTimeout> | null>(null)
  // 二级Tab栏
  const branchDetailTabs = ['all', 'pending', 'processing', 'completed', 'closed'] as const
  // 二级Tab栏类型
  type BranchDetailTab = (typeof branchDetailTabs)[number]

  // 解析初始二级Tab
  const parseInitialTab = (raw: unknown): BranchDetailTab => {
    const t = raw != null ? String(raw) : ''
    return branchDetailTabs.includes(t as BranchDetailTab) ? (t as BranchDetailTab) : 'all'
  }

  const hasLoadedAll = computed(
    () => orderList.value.length >= totalOrders.value && totalOrders.value > 0
  )

  const tabToDisplayStatus: Record<BranchDetailTab, HqSiteOrdersDisplayStatus> = {
    all: 'ALL',
    pending: 'WAIT_ACCEPT',
    processing: 'IN_PROGRESS',
    completed: 'COMPLETED',
    closed: 'CLOSED'
  }

  const buildBranchQuery = (targetPage: number) => {
    const q = searchQuery.value?.trim()
    const query: Parameters<typeof listHqSiteOrders>[0] = {
      siteCompanyId: siteCompanyId.value,
      displayStatus: tabToDisplayStatus[activeTab.value],
      pageNum: targetPage,
      pageSize
    }
    applyWorkOrderListSearchKeyword(query, q)
    return query
  }

  const refreshBranchOrders = async () => {
    const currentVersion = ++requestVersion.value
    if (!siteCompanyId.value) {
      orderList.value = []
      totalOrders.value = 0
      return
    }
    pageNum.value = 1
    try {
      const page = await listHqSiteOrders(buildBranchQuery(1))
      if (currentVersion !== requestVersion.value) return
      orderList.value = page.records
      totalOrders.value = page.total
    } catch {
      if (currentVersion !== requestVersion.value) return
      orderList.value = []
      totalOrders.value = 0
    }
  }

  /**
   * 页面加载
   * @param options 页面参数
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  onLoad((options) => {
    activeTab.value = parseInitialTab(options?.tab)
    const id = Number(options?.id)
    siteCompanyId.value = Number.isFinite(id) && id > 0 ? id : 0
    const rawName = options?.name != null ? decodeURIComponent(String(options.name)) : ''
    branchName.value = rawName
    refreshBranchOrders()
  })

  const { refresherTriggered, onRefresherRefresh } = useScrollRefresher(async () => {
    await refreshBranchOrders()
  })

  watch(
    () => activeTab.value,
    () => {
      refreshBranchOrders()
    }
  )

  watch(
    () => searchQuery.value,
    () => {
      if (searchDebounceTimer.value) clearTimeout(searchDebounceTimer.value)
      searchDebounceTimer.value = setTimeout(() => {
        refreshBranchOrders()
      }, 300)
    }
  )

  /**
   * 跳转到工单详情
   * @param order 工单
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const onOrderClick = (order: OrderListItem) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${order.id}&status=${order.status}`
    })
  }

  // 二级Tab栏
  const tabs = [
    { label: '全部', value: 'all' },
    { label: '待接单', value: 'pending' },
    { label: '维修中', value: 'processing' },
    { label: '已完成', value: 'completed' },
    { label: '已关闭', value: 'closed' }
  ]

  /**
   * 设置二级Tab栏
   * @param val 二级Tab栏值
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const setTab = (val: string) => {
    activeTab.value = parseInitialTab(val)
  }

  // 状态文本映射
  const statusTextMap = ORDER_STATUS_TEXT_MAP

  /**
   * 列表卡片状态文案
   * @param order 工单
   * @returns 状态文本
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const listStatusText = (order: OrderListItem) => {
    if (order.status === 'PENDING_TECH_ACCEPT') return '待接单'
    if (order.status === 'PENDING_ASSIGN') return '待派单'
    return statusTextMap[order.status]
  }

  /**
   * 加载更多工单
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const loadMoreBranchOrders = async () => {
    if (!siteCompanyId.value) return
    if (loadingMore.value || hasLoadedAll.value) return
    // 与 list.vue 一致：首屏尚未拉到数据时不触发翻页，避免并发/空列表误请求
    if (!orderList.value.length && pageNum.value === 1) return
    loadingMore.value = true
    const currentVersion = requestVersion.value
    try {
      const nextPage = pageNum.value + 1
      const page = await listHqSiteOrders(buildBranchQuery(nextPage))
      if (currentVersion !== requestVersion.value) return
      pageNum.value = nextPage
      totalOrders.value = page.total
      if (page.records.length) {
        orderList.value = orderList.value.concat(page.records)
      }
    } finally {
      loadingMore.value = false
    }
  }

  /**
   * 空列表标题
   * @returns 空列表标题
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const listEmptyTitle = computed(() => (searchQuery.value?.trim() ? '未找到相关工单' : '暂无工单'))

  /**
   * 空列表描述
   * @returns 空列表描述
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  /**
   * 与 list.vue 工单列表空状态描述一致
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const listEmptyDesc = computed(() =>
    searchQuery.value?.trim() ? '试试更换关键词或清空搜索' : '当前筛选条件下没有工单'
  )
</script>

<style lang="scss" scoped>
  .branch-order-page {
    /* 与 list.vue 工单列表 .order-list-scroll 一致 */
    .order-list-scroll {
      box-sizing: border-box;
    }
  }
</style>
