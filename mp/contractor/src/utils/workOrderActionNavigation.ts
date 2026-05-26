import type { WorkOrderActionKey } from '@/constants/orderActions'
import { showApiToast } from '@/utils/uiFeedback'

/**
 * 列表/工作台共用的工单动作跳转（具体 API 调用仍在各页弹窗内完成）
 * @修改人 黄碧莲
 * @修改时间 2026-05-26
 */
export const navigateWorkOrderAction = (actionKey: WorkOrderActionKey, orderId: string) => {
  const id = String(orderId ?? '').trim()
  if (!id) {
    void showApiToast('工单ID无效')
    return
  }

  switch (actionKey) {
    case 'TECH_ACCEPT':
      uni.navigateTo({ url: `/pages/order/detail?id=${id}&action=accept` })
      break
    case 'REPAIR_FINISH':
      uni.navigateTo({ url: `/pages/order/detail?id=${id}&action=repair` })
      break
    case 'REVIEW':
      uni.navigateTo({ url: `/pages/order/detail?id=${id}&status=COMPLETED&action=recheck` })
      break
    case 'CLOSE':
      uni.navigateTo({ url: `/pages/order/detail?id=${id}&action=return` })
      break
    default:
      break
  }
}
