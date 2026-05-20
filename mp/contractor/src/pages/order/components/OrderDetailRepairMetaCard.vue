<template>
  <view v-if="show" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">维修信息</text>
    </view>
    <view class="od-info-list">
      <view v-if="hasVal(order.repair.faultJudge)" class="info-item align-center">
        <text class="info-label">故障判定</text>
        <view :class="['tag-value', faultJudgeTagClass]">{{ order.repair.faultJudge }}</view>
      </view>
      <!-- 维修登记：在维修信息内编辑报价（复检不提交报价字段） -->
      <template v-if="quoteEditable">
        <view class="meta-quote-field">
          <text class="info-label">维修报价</text>
          <view class="meta-quote-input-wrap">
            <text class="meta-quote-currency">¥</text>
            <input
              v-model="repairQuote"
              class="meta-quote-input"
              type="digit"
              placeholder="请输入维修报价"
              placeholder-class="meta-quote-placeholder"
            />
          </view>
        </view>
        <view class="meta-quote-field">
          <text class="info-label">维修报价说明</text>
          <textarea
            v-model="quoteDesc"
            class="meta-quote-textarea"
            placeholder="请输入维修报价说明"
            placeholder-class="meta-quote-placeholder"
            :maxlength="-1"
            auto-height
          />
        </view>
      </template>
      <template v-else>
        <view v-if="hasMeaningfulRepairQuoteAmount(order.repair.quoteAmount)" class="info-item">
          <text class="info-label">维修报价</text>
          <text class="info-value repair-quote-readonly-value">
            ¥ {{ order.repair.quoteAmount }}
          </text>
        </view>
        <view v-if="hasVal(order.repair.quoteDesc)" class="info-item-col">
          <text class="info-label" style="margin-bottom: 16rpx; display: block">维修报价说明</text>
          <view class="desc-box">
            <text class="desc-text repair-quote-readonly-desc">{{ order.repair.quoteDesc }}</text>
          </view>
        </view>
      </template>
      <view v-if="hasVal(repairTime)" class="info-item">
        <text class="info-label">维修时间</text>
        <text class="info-value">{{ repairTime }}</text>
      </view>
      <view v-if="hasVal(order.service.returnMethod)" class="info-item">
        <text class="info-label">机器返回方式</text>
        <text class="info-value">{{ order.service.returnMethod }}</text>
      </view>

      <view v-if="showMailReturnInfo" class="info-item-col">
        <text class="info-label" style="margin-bottom: 16rpx; display: block">回寄信息</text>
        <view class="desc-box">
          <text class="desc-text">
            {{ mailReceiverLine }}
          </text>
          <text
            v-if="hasVal(mailReceiverAddress)"
            class="desc-text"
            style="margin-top: 8rpx; display: block"
          >
            {{ mailReceiverAddress }}
          </text>
        </view>
      </view>

      <view v-if="showMailReturnExpress" class="info-item-col">
        <text class="info-label" style="margin-bottom: 16rpx; display: block">回寄快递单号</text>
        <view
          v-if="hasVal(order.service.returnExpressNo)"
          class="desc-box"
          style="margin-bottom: 16rpx"
        >
          <text class="desc-text">{{ order.service.returnExpressNo }}</text>
        </view>
        <view v-if="receiptImageUrls.length" class="od-receipt-grid">
          <image
            v-for="(u, idx) in receiptImageUrls"
            :key="`${idx}-${u}`"
            class="od-receipt-img"
            :src="u"
            mode="aspectFill"
            @click="onPreviewReceipt(idx)"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { getFaultJudgeTagClass } from '@/utils/orderTags'
  import { hasMeaningfulRepairQuoteAmount, hasVal } from '@/utils/value'
  import { previewImages, resolvePreviewableUrl } from '@/utils/mediaPreview'

  const props = withDefaults(
    defineProps<{
      order: OrderDetail
      /** 维修登记：在「维修信息」内展示可编辑报价（与 detail 中 repair 提交共用） */
      quoteEditable?: boolean
    }>(),
    { quoteEditable: false }
  )

  const repairQuote = defineModel<string>('repairQuote', { default: '' })
  const quoteDesc = defineModel<string>('quoteDesc', { default: '' })

  const faultJudgeTagClass = computed(() =>
    getFaultJudgeTagClass(String(props.order?.repair?.faultJudge ?? ''))
  )

  const repairTime = computed(() => String(props.order?.faultPoint?.current?.date ?? '').trim())

  const returnMethodText = computed(() => String(props.order?.service?.returnMethod ?? '').trim())

  const isSelfPickupReturn = computed(() => /自提|到店|自取|^SELF/i.test(returnMethodText.value))

  const isMailReturnContext = computed(() => {
    if (isSelfPickupReturn.value) return false
    const rm = returnMethodText.value
    return /回寄|邮寄|快递|物流|^MAIL$/i.test(rm)
  })

  const showMailReturnInfo = computed(() => {
    if (!isMailReturnContext.value) return false
    return (
      hasVal(mailReceiverName.value) ||
      hasVal(mailReceiverPhone.value) ||
      hasVal(mailReceiverAddress.value)
    )
  })

  const showMailReturnExpress = computed(() => {
    if (!isMailReturnContext.value) return false
    return (
      hasVal(props.order?.service?.returnExpressNo) ||
      (props.order?.service?.mailReturnForm?.receiptImagePaths?.length ?? 0) > 0 ||
      receiptImageUrls.value.length > 0
    )
  })

  const mailReceiverName = computed(() => {
    const m = props.order?.service?.mailReturnForm
    const fromForm = String(m?.receiverName ?? '').trim()
    if (fromForm) return fromForm
    return String(props.order?.service?.senderName ?? '').trim()
  })
  const mailReceiverPhone = computed(() => {
    const m = props.order?.service?.mailReturnForm
    const fromForm = String(m?.receiverPhone ?? '').trim()
    if (fromForm) return fromForm
    return String(props.order?.service?.senderMobile ?? '').trim()
  })
  const mailReceiverAddress = computed(() => {
    const m = props.order?.service?.mailReturnForm
    const fromForm = String(m?.receiverAddress ?? '').trim()
    if (fromForm) return fromForm
    return String(props.order?.service?.senderAddress ?? '').trim()
  })
  const mailReceiverLine = computed(() =>
    [mailReceiverName.value, mailReceiverPhone.value].filter(Boolean).join('  ')
  )

  const receiptImageUrls = computed(() => {
    const list = props.order?.service?.mailReturnForm?.receiptImagePaths ?? []
    if (!Array.isArray(list) || !list.length) return []
    return list.map((u) => resolvePreviewableUrl(u)).filter(Boolean)
  })

  const onPreviewReceipt = (idx: number) => {
    const raw = props.order?.service?.mailReturnForm?.receiptImagePaths ?? []
    if (!Array.isArray(raw) || !raw.length) return
    previewImages(raw as string[], idx)
  }

  const show = computed(() => {
    const o = props.order
    const r = o?.repair
    const s = o?.service
    const hasRepairMeta =
      !!r &&
      (hasVal(r.faultJudge) || hasMeaningfulRepairQuoteAmount(r.quoteAmount) || hasVal(r.quoteDesc))
    const hasTime = hasVal(repairTime.value)
    const hasReturnMethod = !!s && hasVal(s.returnMethod)
    const hasMailInfo = showMailReturnInfo.value || showMailReturnExpress.value
    return hasRepairMeta || hasTime || hasReturnMethod || hasMailInfo || props.quoteEditable
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
  @use '@/styles/variables.scss' as *;
  @use '@/styles/mixins.scss' as *;

  /* 与 orderDetailCardStyles 中 .od-info-list .info-item 下规则一致；顶层声明避免 scoped + @use 嵌套过深时故障判定标签色不生效 */
  .tag-value {
    flex-shrink: 0;
    padding: 4rpx $space-sm;
    font-size: $font-sm;
    font-weight: 500;
    border-radius: $radius-sm;
    line-height: 1.4;
  }

  .tag-fault-judge-green {
    background-color: rgba($status-completed-text, 0.14);
    color: $status-completed-text;
  }

  .tag-fault-judge-red {
    background-color: rgba($red-600, 0.14);
    color: $red-600;
  }

  .tag-fault-judge-neutral {
    @include surface-muted;
    color: $text-slate-500;
  }

  .od-receipt-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
  }

  .od-receipt-img {
    width: 180rpx;
    height: 180rpx;
    border-radius: 12rpx;
    background: #f2f2f2;
    overflow: hidden;
  }

  /* 维修登记：与 FaultJudgeForm 一致的输入框风格 */
  .meta-quote-field {
    @include flex-col;
    gap: $space-sm;

    .info-label {
      font-size: 26rpx;
      font-weight: 400;
      color: $text-slate-900;
    }
  }

  .meta-quote-input-wrap {
    position: relative;
    @include flex-row;
    align-items: center;
  }

  .meta-quote-currency {
    position: absolute;
    left: $space-lg;
    z-index: 1;
    font-size: 26rpx;
    color: $text-slate-500;
    line-height: 1;
  }

  .meta-quote-input,
  .meta-quote-textarea {
    width: 100%;
    font-size: 26rpx;
    color: $text-slate-900;
    @include form-field-soft;
    box-sizing: border-box;
  }

  .meta-quote-input {
    height: 80rpx;
    padding: 0 $space-lg 0 64rpx;

    &::placeholder {
      color: $text-slate-400;
    }
  }

  .meta-quote-textarea {
    min-height: 100rpx;
    padding: $space-md;

    &::placeholder {
      color: $text-slate-400;
    }
  }

  :deep(.meta-quote-placeholder) {
    color: $text-slate-400;
  }

  /* 与维修说明输入态视觉保持一致 */
  .repair-quote-readonly-value {
    font-size: 36rpx;
    font-weight: 700;
    color: #ef4444 !important;
  }

  .repair-quote-readonly-desc {
    font-size: 26rpx;
    color: $text-slate-900;
  }
</style>
