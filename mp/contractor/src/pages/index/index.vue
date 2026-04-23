<template>
  <view class="index-page-root" :class="{ 'index-page-root--hq': isHqView }">
    <CustomNavBar
      class="workbench-nav"
      :class="{ 'workbench-nav--hq': isHqView }"
      :title="pageTitle"
      title-align="left"
      surface="sticky"
      :show-back="false"
    >
      <template #left>
        <view v-if="!isHqView" class="avatar-wrap">
          <image class="icon-primary" :src="personPinCircleIcon" mode="aspectFit" />
        </view>
        <view v-else class="avatar-wrap avatar-wrap--hq">
          <image :src="hqMenuIcon" mode="aspectFit" />
        </view>
      </template>
    </CustomNavBar>
    <view class="dashboard-page" :class="{ 'dashboard-page--hq': isHqView }">
      <!-- 网点工作台 -->
      <SiteWorkbench
        v-if="!isHqView"
        :primary-pending-stat="sitePrimaryPendingStat"
        :site-workbench-stats="siteWorkbenchStats"
        :workbench-list-title="workbenchListTitle"
        :workbench-empty-title="workbenchEmptyTitle"
        :workbench-empty-desc="workbenchEmptyDesc"
        :show-no-more="showSiteWorkbenchNoMore"
        :order-list="orderList"
        :get-order-list-status-text="getOrderListStatusText"
        :show-accept-order-button="showAcceptOrderButton"
        :show-dispatch-order-button="showDispatchOrderButton"
        :show-inbound-transfer-tag="showInboundTransferTag"
        :show-transferred-tag="showTransferredTag"
        @stat-tap="goToOrderListTab"
        @order-click="onOrderClick"
        @accept-order="onAcceptOrder"
        @dispatch-order="openAssignModal"
      />

      <!-- 总部工作台 -->
      <HqWorkbench
        v-if="isHqView"
        :hq-updated-at="hqUpdatedAt"
        :hq-network-stats="hqNetworkStats"
        :hq-transferred-count="hqTransferredCount"
        @stat-tap="goToOrderListTab"
      />

      <!-- 派单模态框 -->
      <AssignTechnicianModal
        v-if="userStore.hasPermission(Perms.WORKORDER_ASSIGN)"
        v-model="showAssignModal"
        v-model:selected-tech-id="selectedTechId"
        :assign-work-order-id="currentOrderId"
        :technician-list="technicianList"
        @close="closeAssignModal"
        @confirm="onAssignConfirm"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, nextTick } from 'vue'
  import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
  import { useAppStore, useUserStore } from '@/stores'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import AssignTechnicianModal, {
    type Technician
  } from '@/components/AssignTechnicianModal/AssignTechnicianModal.vue'
  import SiteWorkbench from './components/SiteWorkbench.vue'
  import HqWorkbench from './components/HqWorkbench.vue'
  import type { OrderListItem } from '@/models/order'
  import { formatTimeHHMM } from '@/utils/format'
  import { Perms } from '@/utils/permissions'
  import { hqMenuIcon, personPinCircleIcon } from '@/svgs'
  import { useIndexWorkbench } from './useIndexWorkbench'
  import { assignWorkOrder, listAssignUserOptions } from '@/api/workOrder'
  import { getApiMessage } from '@/utils/http'

  const appStore = useAppStore()
  const userStore = useUserStore()
  /** 是否是总部工作台 */
  /** 总部首页：显式权限或主体为总部（与 currentTypeCode 以 HQ 开头一致，避免权限数组未同步时整页空白） */
  const isHqView = computed(() => {
    const code = userStore.userInfo?.currentTypeCode
    return !!code?.startsWith('HQ')
  })

  /** 工作台数据 */
  const {
    siteWorkbenchStats,
    orderList,
    refreshSiteWorkbench,
    refreshHqWorkbench,
    hqNetworkStats,
    hqTransferredCount,
    getOrderListStatusText,
    showDispatchOrderButton,
    showAcceptOrderButton,
    workbenchListTitle,
    workbenchEmptyTitle,
    workbenchEmptyDesc,
    sitePrimaryPendingStat,
    showInboundTransferTag,
    showTransferredTag,
    loadMoreSiteWorkbench,
    showSiteWorkbenchNoMore
  } = useIndexWorkbench()

  /** 总部更新时间 */
  const hqUpdatedAt = ref(formatTimeHHMM())
  /** 显示时间 */
  onShow(() => {
    hqUpdatedAt.value = formatTimeHHMM()
    if (!isHqView.value) {
      refreshSiteWorkbench()
    } else {
      refreshHqWorkbench()
    }
  })

  onPullDownRefresh(async () => {
    hqUpdatedAt.value = formatTimeHHMM()
    try {
      if (!isHqView.value) {
        await refreshSiteWorkbench()
      } else {
        await refreshHqWorkbench()
      }
    } finally {
      uni.stopPullDownRefresh()
    }
  })

  onReachBottom(() => {
    if (!isHqView.value) {
      loadMoreSiteWorkbench()
    }
  })

  /** 页面标题 */
  const pageTitle = computed(() => {
    if (isHqView.value) return '总部管理工作台'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '派单工作台'
    return '接单工作台'
  })

  /** 可派单人员列表 */
  const technicianList = ref<Technician[]>([])

  /** 当前订单ID */
  const currentOrderId = ref('')
  /** 选中技术人员ID */
  const selectedTechId = ref<number | string | null>(null)
  /** 是否显示派单模态框 */
  const showAssignModal = ref(false)

  /**
   * 打开派单模态框
   * @param orderId 订单ID
   * @returns void
   */
  const openAssignModal = async (orderId: string | number) => {
    const openedFor = String(orderId ?? '').trim()
    currentOrderId.value = openedFor
    showAssignModal.value = true
    selectedTechId.value = null
    technicianList.value = []

    const workOrderId = Number(openedFor)
    if (!Number.isFinite(workOrderId) || workOrderId <= 0) return
    try {
      uni.showLoading({ title: '加载可派单人员...' })
      const list = await listAssignUserOptions(workOrderId)
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
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 跳转到订单详情
   * @param order 订单
   * @returns void
   */
  const onOrderClick = (order: OrderListItem) => {
    uni.navigateTo({
      url: `/pages/order/detail?id=${order.id}&status=${order.status}`
    })
  }

  /**
   * 关闭派单模态框
   * @returns void
   */
  const closeAssignModal = () => {
    showAssignModal.value = false
    currentOrderId.value = ''
    selectedTechId.value = null
    technicianList.value = []
  }

  /**
   * 确认派单
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
        uni.showToast({ title: '已派单给自己，可在「待接单」中接单', icon: 'none', duration: 1500 })
      } else {
        uni.showToast({ title: getApiMessage(res, '派单成功'), icon: 'none', duration: 1500 })
      }
      closeAssignModal()
      await nextTick()
      await refreshSiteWorkbench(true)
    } catch {
      // assignWorkOrder / http 内已 toast
    }
  }

  /**
   * 跳转到订单列表
   * @param secondaryTab 二级Tab
   * @returns void
   */
  const goToOrderListTab = (
    secondaryTab: 'all' | 'pending' | 'pending_accept' | 'processing' | 'completed'
  ) => {
    appStore.setOrderListNavTarget({
      primaryTab: 'untransferred',
      secondaryTab
    })
    uni.switchTab({ url: '/pages/order/list' })
  }

  /**
   * 接单：进入详情填写故障判定与维修报价，用户提交后再调接单接口（与工单列表一致）
   * @param orderId 订单ID
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
</script>

<style lang="scss" scoped>
  .index-page-root {
    width: 100%;
    min-height: 100vh;
    box-sizing: border-box;
  }

  .index-page-root--hq {
    min-height: 100vh;
    box-sizing: border-box;
    @include flex-col;

    :deep(.workbench-nav) {
      flex-shrink: 0;
    }
  }

  .dashboard-page {
    font-family: 'Inter', sans-serif;
    min-height: 100vh;
    width: 100%;
    background-color: $bg-page;
    @include flex-col;
    box-sizing: border-box;
  }

  .avatar-wrap {
    width: 60rpx;
    height: 60rpx;
    flex-shrink: 0;
    border-radius: 50%;
    background-color: $primary-alpha-10;
    margin-right: $space-sm;
    @include flex-center;

    .icon-primary {
      width: 60rpx;
      height: 60rpx;
      display: block;
    }
  }

  .dashboard-page--hq {
    width: 100%;
    flex-shrink: 0;
    background-color: $bg-page;
  }

  .avatar-wrap--hq {
    background-color: transparent;
  }

  :deep(.workbench-nav--hq .custom-nav-bar__title) {
    font-weight: 700;
    letter-spacing: -0.02em;
  }
</style>
