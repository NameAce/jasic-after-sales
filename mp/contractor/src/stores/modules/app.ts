import { defineStore } from 'pinia'
import { ref } from 'vue'

export type OrderListNavTarget = {
  primaryTab: 'untransferred' | 'transferred'
  secondaryTab: 'all' | 'pending' | 'pending_accept' | 'processing' | 'completed' | 'closed'
} | null

export const useAppStore = defineStore('app', () => {
  // 状态栏高度
  const statusBarHeight = ref(0)

  // switchTab 不支持传参，用此字段在页面间传递工单列表的目标 tab
  const orderListNavTarget = ref<OrderListNavTarget>(null)
  /**
   * 设置工单列表导航目标
   * @param target 工单列表导航目标
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const setOrderListNavTarget = (target: OrderListNavTarget) => {
    orderListNavTarget.value = target
  }

  /**
   * 消费工单列表导航目标
   * @returns 工单列表导航目标
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const consumeOrderListNavTarget = () => {
    const target = orderListNavTarget.value
    orderListNavTarget.value = null
    return target
  }

  /**
 * 表单提交等场景进入工单库 tab 时，触发与 scroll-view 下拉刷新一致的加载反馈
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const orderListScrollRefresherOnNextShow = ref(false)
  const markOrderListScrollRefresherOnNextShow = () => {
    orderListScrollRefresherOnNextShow.value = true
  }
  const consumeOrderListScrollRefresherOnNextShow = () => {
    const v = orderListScrollRefresherOnNextShow.value
    orderListScrollRefresherOnNextShow.value = false
    return v
  }

  /**
   * 初始化应用/系统信息
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const initAppInfo = () => {
    try {
      const info = uni.getSystemInfoSync()
      statusBarHeight.value = info.statusBarHeight || 0
    } catch (error) {
      console.error('获取系统信息失败:', error)
    }
  }

  // 实例化时默认获取一次
  initAppInfo()

  return {
    statusBarHeight,
    orderListNavTarget,
    orderListScrollRefresherOnNextShow,
    initAppInfo,
    setOrderListNavTarget,
    consumeOrderListNavTarget,
    markOrderListScrollRefresherOnNextShow,
    consumeOrderListScrollRefresherOnNextShow
  }
})
