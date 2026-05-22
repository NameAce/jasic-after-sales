<template>
  <!-- 无接口 faults 时不渲染整块（含标题），与详情 hasFaultPoint 一致 -->
  <view v-if="displayRepairFaultRows.length > 0" :class="asCard ? 'od-card-box' : ''">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">故障点信息</text>
    </view>
    <view class="od-fault-point-info">
      <view class="history-header">
        <text class="history-title">{{ historyTitle }}</text>
        <view v-if="showRepairHistoryLink" class="history-btn" @click="openRepairHistory">
          查看历史记录
        </view>
      </view>
      <view
        v-for="(faultRow, idx) in displayRepairFaultRows"
        :key="faultRowKey(faultRow, idx)"
        class="history-record"
      >
        <view class="record-top">
          <text class="record-label">{{ recordLabel }}</text>
          <text v-if="hasVal(faultRowTime(faultRow))" class="record-date">{{
            faultRowTime(faultRow)
          }}</text>
        </view>
        <view class="record-body">
          <text v-if="hasVal(faultRowRepairDesc(faultRow))" class="record-desc">{{
            faultRowRepairDesc(faultRow)
          }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type {
    FaultPointRecord,
    OrderDetailProcessFlowItem,
    WorkOrderFaultVO
  } from '@/models/order'
  import { WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY } from '@/constants/historicalRecord'
  import { hasVal } from '@/utils/value'

  const props = withDefaults(
    defineProps<{
      historyTitle: string
      recordLabel: string
      /**
 * 用于跳转历史记录页
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      orderId: string
      /**
 * 后端 flows → processFlows，用于「流转记录」入口与历史页
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      flowItems?: OrderDetailProcessFlowItem[]
      /**
 * 详情 `repairs` 末条下的 `faults`（map 后为 faultPoint.currentFaults）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      repairFaults?: WorkOrderFaultVO[]
      /**
 * 全部 `repairs[].faults` 映射后的列表，写入 storage 供「查看历史记录」页回显
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      historyRecords?: FaultPointRecord[]
      /**
 * 单条 fault 无 createTime 时，用维修单创建时间兜底
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      repairTimeFallback?: string
      /**
 * false：维修过程 Tab 内嵌（无外层白底 card）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      asCard?: boolean
      /**
 * 列表「已转单」仅查看详情时隐藏「查看历史记录」
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      showRepairHistoryLink?: boolean
    }>(),
    {
      asCard: true,
      showRepairHistoryLink: true,
      flowItems: () => [],
      repairFaults: () => [],
      historyRecords: () => [],
      repairTimeFallback: ''
    }
  )

  /**
 * 仅展示含 repairs.faults.repairDesc / otherDesc 的行，并用于整块卡片显隐
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const displayRepairFaultRows = computed(() =>
    (props.repairFaults || []).filter(
      (row) => hasVal(faultRowRepairDesc(row)) || hasVal(faultRowOtherDesc(row))
    )
  )

  /**
   * 故障点记录键：兼容无 id 的旧接口或其它列表来源
   * @param f 故障点记录对象
   * @param idx 索引
   * @returns 故障点记录键
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  function faultRowKey(f: WorkOrderFaultVO, idx: number) {
    return f.id != null ? String(f.id) : `f-${idx}`
  }

  function faultRowRepairDesc(f: WorkOrderFaultVO) {
    return String(f.repairDesc || '').trim()
  }

  function faultRowOtherDesc(f: WorkOrderFaultVO) {
    return String(f.otherDesc || '').trim()
  }

  function faultRowTime(f: WorkOrderFaultVO) {
    const t = String(f.createTime || '').trim()
    if (t) return t
    return String(props.repairTimeFallback || '').trim()
  }

  /**
   * 打开维修历史记录页
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  function openRepairHistory() {
    const id = (props.orderId || '').trim()
    if (!id) return
    try {
      uni.setStorageSync(
        WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY,
        JSON.stringify(props.historyRecords ?? [])
      )
    } catch {
      /* 存储失败仍跳转，历史页展示空 */
    }
    uni.navigateTo({
      url: `/pages/historicalRecord/index?orderId=${encodeURIComponent(id)}&mode=repairs`
    })
  }
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
