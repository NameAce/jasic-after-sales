<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）页面 order / components / OrderDetailServiceCard -->
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
      <view v-if="hasVal(acceptorSiteName)" class="info-item">
        <text class="info-label">网点名称</text>
        <text class="info-value">{{ acceptorSiteName }}</text>
      </view>
      <view v-if="hasVal(serviceModeLabel)" class="info-item">
        <text class="info-label">维修方式</text>
        <view class="tag-primary">{{ serviceModeLabel }}</view>
      </view>
      <!-- 与 mp/aftersale 申请内容一致：到店维修不展示寄件信息、寄件凭证 -->
      <template v-if="!isInStoreRepair">
        <view v-if="hasVal(senderInfoPlain)" class="info-item align-top">
          <text class="info-label shrink">寄件信息</text>
          <text class="info-value text-right sender-info-plain">{{ senderInfoPlain }}</text>
        </view>
        <!-- 与 mp/aftersale：单张凭证 + 横排 label/图（align-center） -->
        <view v-if="hasVal(senderVoucherDisplayUrl)" class="info-item align-center">
          <text class="info-label">寄件快递单号</text>
          <image
            class="shipping-img"
            mode="aspectFill"
            :src="senderVoucherDisplayUrl"
            @click="onPreviewSenderVoucher"
          />
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { resolvePreviewableUrl } from '@/utils/mediaPreview'
  import { hasVal } from '@/utils/value'

  const props = withDefaults(
    defineProps<{
      service: OrderDetail['service']
      /**
 * 与 C 端 `acceptor.acceptorName` 一致：当前受理网点名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      acceptorSiteName?: string
    }>(),
    { acceptorSiteName: '' }
  )

  const acceptorSiteName = computed(() => String(props.acceptorSiteName ?? '').trim())

  /**
 * 与 aftersale：`serviceModeLabel ?? repairMethod`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const serviceModeLabel = computed(() => {
    const s = props.service as { serviceModeLabel?: string; repairMethod?: string }
    return String(s.serviceModeLabel ?? s.repairMethod ?? '').trim()
  })

  /**
 * 到店类维修：与 aftersale `isInStoreRepair` 一致（兼容历史「送店」文案）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const isInStoreRepair = computed(() => {
    const t = serviceModeLabel.value
    if (!t) return false
    const u = t.toUpperCase().replace(/-/g, '_')
    if (u === 'STORE' || u === 'SHOP') return true
    return /到店|送店/.test(t)
  })

  /**
 * 与 C 端映射一致：整段 `senderInfo`（含换行）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const senderInfoPlain = computed(() => String(props.service.senderInfo ?? '').trim())

  /**
 * 与 C 端：优先 `senderVoucherImg`，否则文件列表首图
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const senderVoucherDisplayUrl = computed(() => {
    const img = String(props.service.senderVoucherImg ?? '').trim()
    if (img) return resolvePreviewableUrl(img)
    const files = props.service.senderVoucherFiles
    if (!Array.isArray(files) || !files.length) return ''
    const raw = String(files[0]?.previewUrl ?? '').trim()
    return raw ? resolvePreviewableUrl(raw) : ''
  })

  function onPreviewSenderVoucher() {
    const u = String(senderVoucherDisplayUrl.value ?? '').trim()
    if (!u) return
    uni.previewImage({ urls: [u], current: u })
  }

  const show = computed(() => {
    const s = props.service
    if (hasVal(s.sitePhone)) return true
    if (hasVal(acceptorSiteName.value)) return true
    if (hasVal(serviceModeLabel.value)) return true
    if (!isInStoreRepair.value) {
      if (hasVal(senderInfoPlain.value)) return true
      if (hasVal(senderVoucherDisplayUrl.value)) return true
    }
    return false
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';
</style>
