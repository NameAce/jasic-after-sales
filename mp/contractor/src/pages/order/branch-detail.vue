<template>
  <!-- 维修工程师/派单员视图 -->
  <CustomNavBar :title="`${branchName}工单详情`" surface="sticky" :shadow="false" />
  <view class="page-container branch-order-page">
    <view class="header">
      <!-- 搜索栏 -->
      <view class="search-wrap">
        <view class="search-box">
          <uni-icons type="search" size="24" color="#cbd5e1"></uni-icons>
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索工单号或故障描述"
            placeholder-class="placeholder-text"
          />
        </view>
      </view>
    </view>

    <!-- 主内容区域 -->
    <scroll-view class="main-content" scroll-y>
      <view class="order-list-scroll">
        <!-- 统计数据 -->
        <view class="stats-dashboard">
          <view v-for="stat in stats" :key="stat.label" class="stat-card">
            <text class="stat-label">{{ stat.label }}</text>
            <text class="stat-value">{{ stat.value }}</text>
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

        <!-- 工单列表 -->
        <OrderCardList
          :orders="filteredOrders"
          :status-text="listStatusText"
          :empty-title="listEmptyTitle"
          :empty-desc="listEmptyDesc"
          brand-label="佳士品牌"
          other-brand-label="非佳士品牌"
          :show-inbound-transfer-tag="(order) => hasInboundTransferFromSite(order.transferFromSite)"
          :show-transferred-tag="() => false"
          card-class="order-card--branch-badges"
          :show-no-more="filteredOrders.length > 0"
          @order-click="onOrderClick"
        >
        </OrderCardList>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import OrderCardList from '@/components/OrderCardList/OrderCardList.vue'
  import TabBar from '@/components/TabBar/TabBar.vue'
  import { fetchBranchList, fetchOrdersByBranch } from '@/api/order'
  import type { OrderListItem } from '@/models/order'
  import { ORDER_STATUS_TEXT_MAP } from '@/utils/orderStatus'
  import { hasInboundTransferFromSite } from '@/utils/orderTransfer'

  // 网点名称
  const branchName = ref('')
  // 搜索关键词
  const searchQuery = ref('')
  // 当前二级Tab
  const activeTab = ref('all')
  // 工单列表
  const orderList = ref<OrderListItem[]>([])
  // 二级Tab栏
  const branchDetailTabs = ['all', 'pending', 'processing', 'completed', 'closed'] as const
  // 二级Tab栏类型
  type BranchDetailTab = (typeof branchDetailTabs)[number]

  // 解析初始二级Tab
  const parseInitialTab = (raw: unknown): BranchDetailTab => {
    const t = raw != null ? String(raw) : ''
    return branchDetailTabs.includes(t as BranchDetailTab) ? (t as BranchDetailTab) : 'all'
  }

  /**
   * 页面加载
   * @param options 页面参数
   */
  onLoad((options) => {
    activeTab.value = parseInitialTab(options?.tab)

    const rawName = options?.name != null ? decodeURIComponent(String(options.name)) : ''
    if (rawName) {
      branchName.value = rawName
      fetchOrdersByBranch(rawName)
        .then((list) => {
          orderList.value = list
        })
        .catch(() => {
          orderList.value = []
        })
      return
    }
    const id = options?.id
    if (id != null) {
      fetchBranchList()
        .then((branches) => {
          const b = branches.find((x) => String(x.id) === String(id))
          if (b) {
            branchName.value = b.name
            return fetchOrdersByBranch(b.name)
          }
          return []
        })
        .then((list) => {
          orderList.value = list
        })
        .catch(() => {
          orderList.value = []
        })
    }
  })

  /**
   * 跳转到工单详情
   * @param order 工单
   */
  const onOrderClick = (order: OrderListItem) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${order.id}&status=${order.status}`
    })
  }

  /**
   * 统计数据
   * @returns 统计数据
   */
  const stats = computed(() => {
    const all = orderList.value
    const pending = all.filter((o) => o.status === 'pending').length
    const processing = all.filter((o) => o.status === 'processing').length
    const completed = all.filter((o) => o.status === 'completed' || o.status === 'closed').length
    return [
      { label: '总工单', value: all.length },
      { label: '待接单', value: pending },
      { label: '维修中', value: processing },
      { label: '已完成', value: completed }
    ]
  })

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
   */
  const setTab = (val: string) => {
    activeTab.value = val
  }

  // 状态文本映射
  const statusTextMap = ORDER_STATUS_TEXT_MAP

  /**
   * 列表卡片状态文案
   * @param order 工单
   * @returns 状态文本
   */
  const listStatusText = (order: OrderListItem) => statusTextMap[order.status]

  /**
   * 过滤工单列表
   * @returns 过滤后的工单列表
   */
  const filteredOrders = computed(() => {
    const q = searchQuery.value?.trim()
    const tab = activeTab.value

    return orderList.value.filter((o) => {
      if (tab !== 'all' && o.status !== tab) return false
      if (!q) return true
      return o.id.includes(q) || o.desc.includes(q)
    })
  })

  /**
   * 空列表标题
   * @returns 空列表标题
   */
  const listEmptyTitle = computed(() => (searchQuery.value?.trim() ? '未找到相关工单' : '暂无工单'))

  /**
   * 空列表描述
   * @returns 空列表描述
   */
  const listEmptyDesc = computed(() => {
    if (searchQuery.value?.trim()) return '试试更换关键词或清空搜索'
    if (orderList.value.length === 0) return '该网点暂无工单'
    return '当前筛选条件下没有工单'
  })
</script>

<style lang="scss" scoped>
  .branch-order-page {
    @include page-column-app;
    gap: $space-lg;

    .order-list-scroll {
      box-sizing: border-box;
      @include flex-column-gap;
    }

    .stats-dashboard {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: $space-md;
      padding: 0 $space-lg;
    }

    .stat-card {
      @include sheet-white($space-lg);
      @include flex-column-center;

      .stat-label {
        font-size: $font-sm;
        color: $text-slate-500;
        margin-bottom: $space-xs;
      }

      .stat-value {
        font-size: $font-xl;
        font-weight: bold;
        color: $primary;
        line-height: 1.2;
      }
    }

    .tabs-wrap {
      background-color: $surface-white;
      border-top: 2rpx solid $surface-slate-100;
      border-bottom: 2rpx solid $surface-slate-100;

      &.sticky-tabs {
        position: sticky;
        top: 0;
        z-index: 10;
      }
    }

    .tabs-scroll {
      width: 100%;
    }

    .tabs-inner {
      @include tabs-track;
    }

    .tab-item {
      @include tab-underline-item(
        $text-color: $text-slate-500,
        $bar-height: 6rpx,
        $bar-radius: 6rpx
      );
      padding: $space-md 0;
    }

    .title-bar--branch {
      padding: $space-sm $space-lg $space-sm;
    }
  }
</style>
