<template>
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

      <!-- 寄件信息 -->
      <view v-if="returnType === 'mail'" class="mail-section">
        <view class="section-header">
          <view class="section-indicator"></view>
          <text class="section-title">寄件信息<text class="text-red">*</text></text>
          <view class="edit-btn" @tap="toggleEditAddress">
            <image class="edit-icon" :src="editIcon" mode="aspectFit" />
            <text class="edit-text">{{ isEditingAddress ? '完成' : '编辑' }}</text>
          </view>
        </view>

        <view class="address-card">
          <view class="info-row">
            <text class="info-label">收货人</text>
            <view class="info-value-group">
              <input
                v-if="isEditingAddress"
                v-model="receiverName"
                class="info-input"
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
                maxlength="11"
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
              :maxlength="200"
              placeholder="请输入收货地址"
            />
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

        <view class="receipt-upload-row">
          <view
            v-for="(path, idx) in receiptImagePaths"
            :key="'rcpt-' + idx"
            class="receipt-thumb-wrap"
          >
            <image class="receipt-thumb" :src="path" mode="aspectFill" />
            <view class="receipt-remove" @tap.stop="removeReceiptImage(idx)">
              <uni-icons type="close" size="24" color="#fff"></uni-icons>
            </view>
          </view>
          <view class="upload-box" @tap="onUpload">
            <view class="upload-icon-wrap">
              <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
            </view>
            <text class="upload-text">上传图片</text>
          </view>
        </view>
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
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import CommonModal from '@/components/CommonModal/CommonModal.vue'
  import { isValidCnMobile } from '@/utils/validation'
  import { addAPhotoIcon, editIcon } from '@/svgs'

  type InitialMailFields = {
    receiverName?: string
    receiverPhone?: string
    receiverAddress?: string
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
  // 回寄快递单号照片
  const receiptImagePaths = ref<string[]>([])

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
      receiptImagePaths.value = m.receiptImagePaths?.length ? [...m.receiptImagePaths] : []
    } else {
      receiverName.value = ''
      receiverPhone.value = ''
      receiverAddress.value = ''
      receiptImagePaths.value = []
    }
  }

  /**
   * 监听弹窗是否显示
   * @param open 是否显示弹窗
   * @returns void
   */
  watch(
    () => props.modelValue,
    (open) => {
      if (open) {
        const t = props.initialType
        returnType.value = t === 'self' || t === 'mail' ? t : ''
        syncFormFromInitial()
      }
    }
  )

  /**
   * 切换编辑地址
   * @returns void
   */
  const toggleEditAddress = () => {
    isEditingAddress.value = !isEditingAddress.value
  }

  /**
   * 删除回寄快递单号照片
   * @param idx 索引
   * @returns void
   */
  const removeReceiptImage = (idx: number) => {
    receiptImagePaths.value = receiptImagePaths.value.filter((_, i) => i !== idx)
  }

  /**
   * 上传回寄快递单号照片
   * @returns void
   */
  const onUpload = () => {
    const remain = 9 - receiptImagePaths.value.length
    if (remain <= 0) {
      uni.showToast({ title: '最多上传9张', icon: 'none' })
      return
    }
    uni.chooseImage({
      count: remain,
      success: (res) => {
        const raw = res.tempFilePaths
        const paths = Array.isArray(raw) ? raw : raw ? [raw] : []
        if (paths.length) {
          receiptImagePaths.value = receiptImagePaths.value.concat(paths)
        }
      }
    })
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
    if (!receiptImagePaths.value.length) {
      uni.showToast({ title: '请上传回寄快递单号照片', icon: 'none' })
      return
    }

    emit('confirm', {
      type: 'mail',
      mail: {
        receiverName: name,
        receiverPhone: phone,
        receiverAddress: addr,
        receiptImagePaths: [...receiptImagePaths.value]
      }
    })
    visible.value = false
  }
</script>

<style lang="scss" scoped>
  .return-method-content {
    padding: 32rpx;
    max-height: 70vh;
    overflow-y: auto;

    display: flex;
    flex-direction: column;
    gap: 24rpx;
  }

  /* Radio Group */
  .radio-group {
    @include flex-between;
    gap: 24rpx;
  }

  .radio-item {
    flex: 1;
    @include flex-between;
    align-items: center;
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

    .radio-icon {
      width: 38rpx;
      height: 38rpx;
      border-radius: 50%;
      border: 4rpx solid $surface-slate-200;
      box-sizing: border-box;
      transition: all 0.3s;
    }
  }

  .radio-item {
    align-items: flex-start;

    .mail-title {
      font-size: 28rpx;
      font-weight: bold;
      color: $text-slate-900;
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

  /* Address Card */
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
          height: 60rpx;
          padding: 0 16rpx;
          font-size: 26rpx;
          color: $text-slate-900;
          background-color: $surface-white;
          border: 2rpx solid $surface-slate-200;
          border-radius: 12rpx;
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
        font-size: 24rpx;
        font-weight: 600;
        color: $text-slate-900;
        line-height: 1.5;
      }

      .address-input {
        width: 100%;
        min-height: 120rpx;
        padding: 16rpx;
        font-size: 24rpx;
        color: $text-slate-900;
        background-color: $surface-white;
        border: 2rpx solid $surface-slate-200;
        border-radius: 12rpx;
        box-sizing: border-box;
      }
    }
  }

  /* Upload Section */
  .upload-section {
    margin-bottom: 16rpx;
  }

  .receipt-upload-row {
    display: flex;
    flex-wrap: wrap;
    gap: 24rpx;
    align-items: flex-start;
  }

  .receipt-thumb-wrap {
    position: relative;
    width: 200rpx;
    height: 200rpx;
    border-radius: 32rpx;
    overflow: hidden;
  }

  .receipt-thumb {
    width: 100%;
    height: 100%;
    display: block;
  }

  .receipt-remove {
    position: absolute;
    top: 8rpx;
    right: 8rpx;
    width: 44rpx;
    height: 44rpx;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .receipt-remove-icon {
    font-size: 28rpx;
    color: $surface-white;
  }

  .upload-box {
    width: 200rpx;
    height: 200rpx;
    border: 4rpx dashed $surface-slate-200;
    border-radius: 32rpx;
    background-color: $surface-slate-50;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 24rpx;

    .upload-icon-wrap {
      width: 80rpx;
      height: 80rpx;
      background-color: $surface-white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

      .upload-icon {
        font-size: 40rpx;
        color: $text-slate-400;
      }
    }

    .upload-text {
      font-size: 24rpx;
      color: $text-slate-400;
    }
  }

  .modal-footer {
    @include modal-footer-bar;
  }
</style>
