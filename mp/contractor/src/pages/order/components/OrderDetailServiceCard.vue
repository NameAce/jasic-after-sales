<template>
  <view v-if="show" class="od-apply-card">
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">服务信息</text>
    </view>
    <view class="od-apply-info-list">
      <view v-if="hasVal(service.sitePhone)" class="info-item">
        <text class="info-label">网点电话</text>
        <text class="info-value">{{ service.sitePhone }}</text>
      </view>
      <view v-if="hasVal(service.repairMethod)" class="info-item">
        <text class="info-label">维修方式</text>
        <view :class="['tag-method', repairMethodTagClass]">{{ service.repairMethod }}</view>
      </view>
      <view v-if="hasVal(service.source)" class="info-item">
        <text class="info-label">申请来源</text>
        <text class="info-value">{{ service.source }}</text>
      </view>
      <view v-if="showServiceSenderInfo && hasSenderDetail" class="info-item align-top sender-info-row">
        <text class="info-label shrink">寄件信息</text>
        <view class="sender-info-value">
          <view class="sender-info-inner">
            <text v-if="senderInfoDisplay.line1" class="sender-info-line">{{ senderInfoDisplay.line1 }}</text>
            <text v-if="senderInfoDisplay.line2" class="sender-info-line sender-info-line--addr">{{
              senderInfoDisplay.line2
            }}</text>
          </view>
        </view>
      </view>
      <view
        v-if="showServiceSenderInfo && senderVoucherFilesForView.length"
        class="info-item align-top voucher-row"
      >
        <text class="info-label shrink">寄件快递单号</text>
        <view class="shipping-voucher-grid">
          <image
            v-for="(file, idx) in senderVoucherFilesForView"
            :key="'sender-voucher-' + idx"
            class="shipping-img"
            mode="aspectFill"
            :src="file.previewUrl"
            @click="previewVoucher(idx)"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { previewImages } from '@/utils/mediaPreview'
  import { getRepairMethodTagClass } from '@/utils/orderTags'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    service: OrderDetail['service']
  }>()
  // 与详情映射一致：中文「邮寄」或枚举 MAIL；避免仅有 serviceMode 时整块寄件信息不展示
  const repairMethodTagClass = computed(() =>
    getRepairMethodTagClass(props.service.repairMethod, props.service.serviceMode)
  )

  /** 是否有寄件人/地址可展示（兼容仅有 senderInfo 拼接串） */
  const hasSenderDetail = computed(() => {
    const s = props.service
    if (hasVal(s.senderInfo)) return true
    return hasVal(s.senderName) || hasVal(s.senderMobile) || hasVal(s.senderAddress)
  })

  /**
   * 第一行：姓名 + 手机；第二行：地址（与 UI 稿一致，优先用结构化字段）
   */
  const senderInfoDisplay = computed(() => {
    const s = props.service
    const name = String(s.senderName || '').trim()
    const mobile = String(s.senderMobile || '').trim()
    const addr = String(s.senderAddress || '').trim()
    if (name || mobile || addr) {
      return {
        line1: [name, mobile].filter(Boolean).join(' '),
        line2: addr
      }
    }
    const raw = String(s.senderInfo || '').trim()
    if (!raw) return { line1: '', line2: '' }
    const parts = raw.split(/\s*\/\s*/).map((p) => p.trim()).filter(Boolean)
    if (parts.length >= 3) {
      return {
        line1: `${parts[0]} ${parts[1]}`.trim(),
        line2: parts.slice(2).join(' ')
      }
    }
    if (parts.length === 2) {
      return { line1: `${parts[0]} ${parts[1]}`.trim(), line2: '' }
    }
    return { line1: parts[0] || raw, line2: '' }
  })

  /** 接口 senderVoucherFiles：仅展示有效预览地址 */
  const senderVoucherFilesForView = computed(() => {
    const files = props.service.senderVoucherFiles
    if (!Array.isArray(files) || !files.length) return []
    return files
      .map((f) => ({ previewUrl: String(f.previewUrl || '').trim() }))
      .filter((f) => f.previewUrl)
  })

  function previewVoucher(index: number) {
    const list = senderVoucherFilesForView.value
    if (!list.length) return
    const urls = list.map((f) => f.previewUrl)
    previewImages(urls, index)
  }

  /** 寄件信息/凭证：以接口 serviceMode 为准；未返回枚举时再看展示文案 */
  const showServiceSenderInfo = computed(() => {
    const mode = (props.service.serviceMode || '').toUpperCase()
    if (mode === 'STORE') return false
    if (mode === 'MAIL') return true
    const method = (props.service.repairMethod || '').trim()
    return /寄修|邮寄|郵寄|mail/i.test(method.replace(/\s+/g, ''))
  })

  /** 与原 hasServiceInfoCard 一致 */
  const show = computed(() => {
    const s = props.service
    if (hasVal(s.sitePhone) || hasVal(s.repairMethod) || hasVal(s.source)) return true
    if (!showServiceSenderInfo.value) return false
    return hasSenderDetail.value || senderVoucherFilesForView.value.length > 0
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';
  @use '@/styles/variables.scss' as *;

  /* 与 orderDetailApplyCards 中样式一致；顶层声明避免部分端上 scoped + @use 嵌套过深时标签色不生效 */
  .tag-method-mail-orange {
    background-color: rgba(242, 102, 4, 0.1);
    color: #f26604;
  }

  /* 寄件信息：右侧信息块靠右，块内两行左对齐（与设计稿一致） */
  .sender-info-row {
    .sender-info-value {
      flex: 1;
      min-width: 0;
      display: flex;
      justify-content: flex-end;
    }

    .sender-info-inner {
      max-width: 420rpx;
      text-align: left;
    }

    .sender-info-line {
      display: block;
      font-size: $font-sm;
      font-weight: 500;
      color: $text-strong;
      line-height: 1.4;
    }

    .sender-info-line--addr {
      margin-top: 4rpx;
    }
  }
</style>
