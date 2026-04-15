<template>
  <!-- 根节点包裹：小程序下插槽内容在子组件内渲染，scoped 易无法命中，样式统一挂在 .return-method-modal 下 -->
  <view class="return-method-modal">
    <CommonModal v-model="visible" title="机器返回方式" animation="slide-up">
      <view class="return-method-content">
        <!-- 选择返回方式 -->
        <view class="radio-group">
          <!-- 自提 -->
          <view
            class="radio-item"
            :class="{ active: returnType === 'self' }"
            @tap="returnType = 'self'"
          >
            <text class="mail-title">自提</text>
            <view class="radio-icon"></view>
          </view>

          <!-- 回寄 -->
          <view
            class="radio-item"
            :class="{ active: returnType === 'mail' }"
            @tap="returnType = 'mail'"
          >
            <text class="mail-title">回寄</text>
            <view class="radio-icon"></view>
          </view>
        </view>

        <!-- 寄件信息：「选择地址」始终保留便于重选；有完整数据时再展示收货人/电话/地址 -->
        <view v-if="returnType === 'mail'" class="mail-section">
          <view class="section-header">
            <view class="section-indicator"></view>
            <text class="section-title">寄件信息<text class="text-red">*</text></text>
            <view v-if="hasCoreMailFilled()" class="edit-btn" @tap="toggleEditAddress">
              <image class="edit-icon" :src="editIcon" mode="aspectFit" />
              <text class="edit-text">{{ isEditingAddress ? '完成' : '编辑' }}</text>
            </view>
          </view>

          <view class="mail-pick-row" @tap="goPickShippingAddress">
            <text class="mail-pick-text">点击选择寄件地址</text>
            <uni-icons type="right" :size="14" color="#f26604"></uni-icons>
          </view>

          <view v-if="detailSendExpressNo" class="express-no-row">
            <text class="info-label">寄件快递单号</text>
            <text class="info-value express-no-text">{{ detailSendExpressNo }}</text>
          </view>

          <view v-if="hasCoreMailFilled()" class="address-card">
            <view class="info-row">
              <text class="info-label">收货人</text>
              <view class="info-value-group">
                <input
                  v-if="isEditingAddress"
                  v-model="receiverName"
                  class="info-input"
                  placeholder-class="info-input-placeholder"
                  placeholder="请输入收货人姓名"
                />
                <text v-else class="info-value">{{ receiverName || '-' }}</text>
              </view>
            </view>
            <view class="info-row">
              <text class="info-label">联系电话</text>
              <view class="info-value-group">
                <input
                  v-if="isEditingAddress"
                  v-model="receiverPhone"
                  class="info-input"
                  type="number"
                  :maxlength="11"
                  placeholder-class="info-input-placeholder"
                  placeholder="请输入联系电话"
                />
                <text v-else class="info-value">{{ receiverPhone || '-' }}</text>
              </view>
            </view>
            <view class="address-row">
              <view class="address-header">
                <text class="info-label">收货地址</text>
              </view>
              <textarea
                v-if="isEditingAddress"
                v-model="receiverAddress"
                class="address-input"
                auto-height
                :maxlength="200"
                placeholder-class="info-input-placeholder"
                placeholder="请输入收货地址"
              ></textarea>
              <text v-else class="address-value">{{ receiverAddress || '-' }}</text>
            </view>
          </view>
        </view>

        <!-- 上传回寄快递单号照片 -->
        <view v-if="returnType === 'mail'" class="upload-section">
          <view class="section-header">
            <view class="section-indicator"></view>
            <text class="section-title">上传回寄快递单号照片<text class="text-red">*</text></text>
          </view>

          <MediaUploadField
            v-model="receiptFileList"
            :show-label-row="false"
            file-mediatype="image"
            mode="grid"
            :limit="1"
          />
        </view>
      </view>
      <!-- 底部按钮 -->
      <template #footer>
        <view class="modal-footer">
          <view class="btns btn-cancel" @tap="onCancel">
            <text class="text">取消</text>
          </view>
          <view class="btns btn-confirm" @tap="onConfirm">
            <text class="text">确认</text>
          </view>
        </view>
      </template>
    </CommonModal>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import CommonModal from '@/components/CommonModal/CommonModal.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import { isValidCnMobile } from '@/utils/validation'
  import { collectVoucherFileIds, hasUnuploadedMediaItems } from '@/utils/workOrderFileIds'
  import { editIcon } from '@/svgs'

  type InitialMailFields = {
    receiverName?: string
    receiverPhone?: string
    receiverAddress?: string
    /** 详情接口 `sendExpressNo`，仅展示 */
    sendExpressNo?: string
    receiptImagePaths?: string[]
  }

  const props = defineProps<{
    modelValue: boolean
    /** 再次打开弹窗时回显已选方式，空则要求用户重新选择 */
    initialType?: '' | 'self' | 'mail'
    /** 选择「回寄」时回显寄件信息与已上传凭证（可继续编辑）；无则空白 */
    initialMail?: InitialMailFields
  }>()

  type ConfirmPayload =
    | { type: 'self' }
    | {
        type: 'mail'
        mail: {
          receiverName: string
          receiverPhone: string
          receiverAddress: string
          receiptImagePaths: string[]
          returnVoucherFileIds: number[]
        }
      }

  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'confirm', data: ConfirmPayload): void
  }>()

  // 是否显示弹窗
  const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  // 返回方式（必填，打开弹窗时由 initialType 回显，无则留空）
  const returnType = ref<'' | 'self' | 'mail'>('')

  // 是否编辑收货地址
  const isEditingAddress = ref(true)
  // 收货人
  const receiverName = ref('')
  // 联系电话
  const receiverPhone = ref('')
  // 收货地址
  const receiverAddress = ref('')
  type ReceiptFileItem = Record<string, unknown>

  /** 与 MediaUploadField / uni-file-picker 一致的结构，确认时抽出 url 作为 receiptImagePaths */
  const receiptFileList = ref<ReceiptFileItem[]>([])

  const detailSendExpressNo = computed(() => (props.initialMail?.sendExpressNo ?? '').trim())

  const receiptUrlsFromFileList = (list: ReceiptFileItem[]): string[] => {
    const out: string[] = []
    for (const item of list) {
      const raw = item.url ?? item.path ?? item.previewUrl ?? item.fileUrl ?? item.tempFilePath
      const s = String(raw ?? '').trim()
      if (s) out.push(s)
    }
    return out
  }

  const receiptFilesFromPathStrings = (paths: string[]): ReceiptFileItem[] =>
    paths
      .map((u) => String(u ?? '').trim())
      .filter(Boolean)
      .map((url) => ({ url, path: url }))

  /**
   * 同步表单数据
   * @returns void
   */
  const syncFormFromInitial = () => {
    const m = props.initialMail
    isEditingAddress.value = true
    if (m) {
      receiverName.value = (m.receiverName ?? '').trim()
      receiverPhone.value = (m.receiverPhone ?? '').trim()
      receiverAddress.value = (m.receiverAddress ?? '').trim()
      receiptFileList.value = m.receiptImagePaths?.length
        ? receiptFilesFromPathStrings(m.receiptImagePaths)
        : []
    } else {
      receiverName.value = ''
      receiverPhone.value = ''
      receiverAddress.value = ''
      receiptFileList.value = []
    }
  }

  const hasCoreMailFilled = () =>
    Boolean(receiverName.value.trim() && receiverPhone.value.trim() && receiverAddress.value.trim())

  /**
   * 跳转地址簿选择寄件信息（与工单详情 onShow + takeSelectedShippingAddress 配合回显）
   */
  const goPickShippingAddress = () => {
    uni.navigateTo({
      url: '/pages/address/index?mode=selectShipping'
    })
  }

  /**
   * 监听弹窗是否显示
   * @param open 是否显示弹窗
   * @returns void
   */
  watch(
    () => props.modelValue,
    (open, wasOpen) => {
      if (open && !wasOpen) {
        const t = props.initialType
        returnType.value = t === 'self' || t === 'mail' ? t : ''
        syncFormFromInitial()
      }
    }
  )

  watch(
    () =>
      [
        props.modelValue,
        (props.initialMail?.receiverName ?? '').trim(),
        (props.initialMail?.receiverPhone ?? '').trim(),
        (props.initialMail?.receiverAddress ?? '').trim()
      ] as const,
    ([open]) => {
      if (!open) return
      const m = props.initialMail
      if (!m) return
      receiverName.value = (m.receiverName ?? '').trim()
      receiverPhone.value = (m.receiverPhone ?? '').trim()
      receiverAddress.value = (m.receiverAddress ?? '').trim()
    }
  )

  watch(returnType, (t, prev) => {
    if (t === 'mail' && prev !== 'mail') {
      syncFormFromInitial()
    }
  })

  /**
   * 切换编辑地址
   * @returns void
   */
  const toggleEditAddress = () => {
    isEditingAddress.value = !isEditingAddress.value
  }

  /**
   * 取消
   * @returns void
   */
  const onCancel = () => {
    visible.value = false
  }

  /**
   * 确认
   * @returns void
   */
  const onConfirm = () => {
    if (returnType.value !== 'self' && returnType.value !== 'mail') {
      uni.showToast({ title: '请选择机器返回方式', icon: 'none' })
      return
    }
    if (returnType.value === 'self') {
      emit('confirm', { type: 'self' })
      visible.value = false
      return
    }

    const name = receiverName.value.trim()
    const phone = receiverPhone.value.trim()
    const addr = receiverAddress.value.trim()
    if (!name) {
      uni.showToast({ title: '请填写收货人', icon: 'none' })
      return
    }
    if (!phone) {
      uni.showToast({ title: '请填写联系电话', icon: 'none' })
      return
    }
    if (!isValidCnMobile(phone)) {
      uni.showToast({ title: '请输入11位手机号', icon: 'none' })
      return
    }
    if (!addr) {
      uni.showToast({ title: '请填写收货地址', icon: 'none' })
      return
    }
    if (hasUnuploadedMediaItems(receiptFileList.value)) {
      uni.showToast({ title: '图片正在上传，请稍候再试', icon: 'none' })
      return
    }
    const returnVoucherFileIds = collectVoucherFileIds(receiptFileList.value)
    if (!returnVoucherFileIds.length) {
      uni.showToast({ title: '请上传回寄快递单号照片', icon: 'none' })
      return
    }
    const receiptImagePaths = receiptUrlsFromFileList(receiptFileList.value)

    emit('confirm', {
      type: 'mail',
      mail: {
        receiverName: name,
        receiverPhone: phone,
        receiverAddress: addr,
        receiptImagePaths,
        returnVoucherFileIds
      }
    })
    visible.value = false
  }
</script>

<style lang="scss">
  /* 非 scoped + 根类限定：避免小程序中插槽节点收不到父组件 scoped 属性导致整段样式失效 */
  .return-method-modal {
    .return-method-content {
      padding: 32rpx;
      max-height: 70vh;
      overflow-y: auto;

      display: flex;
      flex-direction: column;
      gap: 24rpx;
    }

    .radio-group {
      @include flex-between;
      gap: 24rpx;
    }

    .radio-item {
      flex: 1;
      @include flex-between;
      align-items: flex-start;
      padding: $space-md;
      border: 1rpx solid $border-color;
      border-radius: $radius-lg;
      background-color: $surface-white;
      transition: all 0.3s;

      &.active {
        border-color: $primary;
        background-color: rgba($primary, 0.05);

        .radio-icon {
          border: 12rpx solid $primary;
          background-color: $surface-white;
        }
      }

      .mail-title {
        font-size: 28rpx;
        font-weight: bold;
        color: $text-slate-900;
      }

      .radio-icon {
        width: 38rpx;
        height: 38rpx;
        border-radius: 50%;
        border: 4rpx solid $surface-slate-200;
        box-sizing: border-box;
        transition: all 0.3s;
      }
    }

    .section-header {
      @include section-title-bar;

      .edit-btn {
        margin-left: auto;
        @include flex-row;
        gap: $space-xs;
        color: $primary;

        .edit-icon {
          width: 56rpx;
          height: 32rpx;
          flex-shrink: 0;
        }

        .edit-text {
          font-size: $font-md;
          font-weight: 500;
        }
      }
    }

    .address-card {
      background-color: $surface-slate-50;
      border-radius: 32rpx;
      padding: 32rpx;
      display: flex;
      flex-direction: column;
      gap: 24rpx;

      .info-row {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .info-label {
          font-size: 26rpx;
          color: $text-slate-500;
        }

        .info-value-group {
          display: flex;
          align-items: center;
          gap: 16rpx;

          .info-value {
            font-size: 26rpx;
            font-weight: 600;
            color: $text-slate-900;
          }

          .info-input {
            width: 320rpx;
            height: 80rpx;
            padding: 0 $space-lg;
            font-size: 26rpx;
            color: $text-slate-900;
            @include form-field-soft;
            box-sizing: border-box;
          }
        }
      }

      .address-row {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .address-header {
          display: flex;
          align-items: center;
          justify-content: space-between;

          .info-label {
            font-size: 26rpx;
            color: $text-slate-500;
          }
        }

        .address-value {
          display: block;
          width: 100%;
          box-sizing: border-box;
          font-size: 24rpx;
          font-weight: 600;
          color: $text-slate-900;
          line-height: 1.5;
          word-break: break-all;
          white-space: pre-wrap;
        }

        .address-input {
          width: 100%;
          min-height: 100rpx;
          padding: $space-md;
          font-size: 26rpx;
          line-height: 1.5;
          color: $text-slate-900;
          @include form-field-soft;
          box-sizing: border-box;
        }
      }
    }

    .mail-pick-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 24rpx $space-md;
      background-color: rgba($primary, 0.06);
      border: 2rpx solid rgba($primary, 0.35);
      border-radius: $radius-lg;
      margin-bottom: 16rpx;

      &:active {
        opacity: 0.88;
      }
    }

    .mail-pick-text {
      font-size: 28rpx;
      font-weight: 600;
      color: $primary;
    }

    .express-no-row {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16rpx;
      padding: 16rpx $space-md;
      margin-bottom: 16rpx;
      background-color: $surface-slate-50;
      border-radius: $radius-md;

      .info-label {
        flex-shrink: 0;
        font-size: 26rpx;
        color: $text-slate-500;
      }

      .express-no-text {
        flex: 1;
        text-align: right;
        font-size: 26rpx;
        font-weight: 600;
        color: $text-slate-900;
        word-break: break-all;
      }
    }

    .upload-section {
      margin-bottom: 16rpx;
    }

    .modal-footer {
      @include modal-footer-bar;
    }

    .text-red {
      color: $red-500;
      margin-left: 4rpx;
    }

    :deep(.info-input-placeholder) {
      color: $text-slate-400;
    }
  }
</style>
