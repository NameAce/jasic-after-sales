<template>
  <custom-nav-bar title="非佳士品牌报修" surface="sticky" />
  <view class="repair-form-page page-index">
    <!-- 表单 -->
    <uni-forms
      ref="formRef"
      :model-value="formData"
      :rules="rules"
      label-position="top"
      label-width="auto"
    >
      <view class="form-content page-padding">
        <view class="card card-shadow">
          <view class="card-header">
            <view class="icon-box">
              <uni-icons type="vip-filled" size="24" color="#f26604"></uni-icons>
            </view>
            <view class="header-text">
              <view>商品查询</view>
              <text>请输入或扫描产品条形码查询状态</text>
            </view>
          </view>
          <view class="search-box">
            <view class="search-box-main">
              <FormItemAnchor name="warrantyCode" />
              <uni-easyinput
                v-model="formData.warrantyCode"
                placeholder="输入产品条形码"
                suffix-icon="scan"
                @icon-click="handleScan"
              />
            </view>
            <button class="btn btn-primary mini-btn" @click="checkWarranty">查询</button>
          </view>
        </view>
        <!-- 表单内容 -->
        <!-- 必填信息 -->
        <RepairFormSectionHeader title="必填信息" />
        <!-- 必填信息卡片 -->

        <view class="card card-shadow form-padding">
          <!-- 选择网点 -->
          <ServicePointFormItem :display-text="selectedCenterDisplay" />

          <!-- 故障描述 -->
          <uni-forms-item label="故障描述" name="faultDescription" required>
            <FormItemAnchor name="faultDescription" />
            <view class="fault-desc-picker" @click="openFaultDescDropdown">
              <text :class="['fault-desc-picker-text', { placeholder: !selectedFaultDescText }]">
                {{ selectedFaultDescText || '请选择' }}
              </text>
              <uni-icons type="down" size="15" color="#cbd5e1" />
            </view>
            <view v-if="showFaultDescDropdown" class="fault-desc-dropdown">
              <view
                v-for="option in faultDescriptionOptions"
                :key="option.value"
                class="fault-desc-option"
                @click.stop="toggleDraftFaultDesc(option.value)"
              >
                <checkbox
                  :checked="draftFaultDesc.includes(option.value)"
                  color="#f26604"
                  style="transform: scale(0.8); transform-origin: center"
                />
                <text class="fault-desc-option-text">{{ option.text }}</text>
              </view>
              <view class="fault-desc-dropdown-actions">
                <view class="dropdown-btn dropdown-btn--cancel" @click.stop="cancelFaultDescSelect">
                  取消
                </view>
                <view
                  class="dropdown-btn dropdown-btn--confirm"
                  @click.stop="confirmFaultDescSelect"
                >
                  确定
                </view>
              </view>
            </view>
          </uni-forms-item>

          <!-- 选择维修路径 -->
          <uni-forms-item label="选择维修路径" name="repairType" required>
            <FormItemAnchor name="repairType" />
            <RepairTypeSelector v-model="formData.repairType" :options="repairTypes" />
          </uni-forms-item>

          <!-- 寄件信息 -->
          <uni-forms-item v-if="showShippingInfo" label="寄件信息" name="shippingInfo" required>
            <FormItemAnchor name="shippingInfo" />
            <view class="shipping-address-btn" @click="chooseShippingAddress">
              <text :class="['shipping-address-text', { placeholder: !formData.shippingInfo }]">{{
                shippingInfoDisplay
              }}</text>
              <uni-icons type="right" size="14" color="#94a3b8" />
            </view>
          </uni-forms-item>
        </view>

        <!-- 补充说明 -->
        <RepairFormSectionHeader
          title="补充说明"
          collapsible
          :expanded="showSupplementSection"
          @toggle="toggleSupplementSection"
        />

        <!-- 补充说明卡片 -->
        <view v-show="showSupplementSection" class="card card-shadow form-padding">
          <!-- 语音输入 -->
          <VoiceInputField v-model="formData.voiceList" />
          <!-- 故障视频/图片 -->
          <MediaUploadField
            v-model="formData.images"
            label="故障视频/图片"
            tip="限1个视频、3张图"
            file-mediatype="all"
            :limit="4"
            :del-icon="true"
            @select="handleFaultMediaChange"
            @delete="handleFaultMediaChange"
          />

          <!-- 寄件快递单号 -->
          <MediaUploadField
            v-model="formData.shippingCode"
            label="寄件快递单号"
            tip="限2张图片"
            file-mediatype="image"
            :limit="2"
            :max-file-size="1024 * 1024 * 10"
            :del-icon="true"
          />

          <!-- 品牌名 -->
          <uni-forms-item label="品牌名" name="brandName">
            <uni-easyinput v-model="formData.brandName" auto-height placeholder="请输入品牌名" />
          </uni-forms-item>

          <!-- 型号 -->
          <uni-forms-item label="型号" name="modelName">
            <uni-easyinput v-model="formData.modelName" auto-height placeholder="请输入型号" />
          </uni-forms-item>
        </view>
      </view>
    </uni-forms>
  </view>

  <!-- 按钮 -->
  <base-button>
    <view class="btn btn-secondary" @click="stashForm">暂存</view>
    <view class="btn btn-secondary" @click="resetForm">重置</view>
    <view class="btn btn-primary" @click="submitForm">提交</view>
  </base-button>
</template>

<script setup lang="ts">
  import { ref, computed, watch, nextTick, type Ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import RepairTypeSelector from '@/components/RepairTypeSelector/RepairTypeSelector.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import VoiceInputField, { type VoiceItem } from '@/components/VoiceInputField/VoiceInputField.vue'
  import ServicePointFormItem from '@/components/ServicePointFormItem/ServicePointFormItem.vue'
  import RepairFormSectionHeader from '@/components/RepairFormSectionHeader/RepairFormSectionHeader.vue'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import {
    CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE,
    REPAIR_TYPE_OPTIONS,
    REPAIR_TYPE_TO_SERVICE_MODE
  } from '@/constants/repairForm'
  import { useUserStore } from '@/stores/modules/user'
  import { useServicePointSelection } from '@/composables/useServicePointSelection'
  import { useSupplementSection } from '@/composables/useSupplementSection'
  import {
    createCustomerWorkOrderAPI,
    getBarcodeInfoAPI,
    mapBarcodeFaultOptions,
    type CreateCustomerWorkOrderDTO
  } from '@/api/order'
  import { scrollToFirstInvalidUniFormField } from '@/utils/formFieldScrollFocus'
  import { validateFaultMediaSelection } from '@/utils/repairMediaLimits'
  import {
    asUnknownArray,
    collectVoucherFileIds,
    collectVoiceFileIds,
    partitionFaultMediaFileIds
  } from '@/utils/workOrderFileIds'
  import {
    applyOtherRepairDraft,
    clearOtherRepairDraft,
    loadOtherRepairDraft,
    saveOtherRepairDraft,
    type OtherRepairDraftForm
  } from '@/utils/repairDraftStorage'
  import { takeSelectedShippingAddress, type SelectedShippingAddress } from '@/utils/addressStorage'
  import { parseUnknownError } from '@/utils/errorMessage'
  import { API_SUCCESS_CODE } from '@/utils/http'
  import {
    resolveSendExpressNoForSubmit,
    resolveShippingSubmitFields
  } from '@/utils/shippingSubmitFields'
  // 表单引用
  const formRef = ref(null)
  // 表单数据
  const formData: Ref<OtherRepairDraftForm> = ref({
    warrantyCode: '',
    centerId: null,
    faultDescription: '',
    repairType: 'STORE',
    shippingInfo: '',
    voiceList: [] as VoiceItem[],
    images: [] as unknown[],
    shippingCode: [] as unknown[],
    brandName: '',
    modelName: ''
  })

  // 是否显示寄件信息
  const showShippingInfo = computed(() => formData.value.repairType === 'MAIL')
  const shippingInfoDisplay = computed(() => formData.value.shippingInfo || '请选择寄件信息')
  const selectedShippingAddress = ref<SelectedShippingAddress | null>(null)
  // 使用服务点选择器
  const {
    selectedCenterDisplay,
    applyStorageSelection,
    clearServicePointSelection,
    hasPendingServicePointPick
  } = useServicePointSelection(formData)
  // 使用补充说明
  const { showSupplementSection, toggleSupplementSection } = useSupplementSection(false)
  const userStore = useUserStore()
  const faultDescriptionOptions = ref<{ text: string; value: string }[]>([])
  const showFaultDescDropdown = ref(false)
  const draftFaultDesc = ref<string[]>([])
  const TOAST_DURATION = 1500
  const selectedFaultDescText = computed(() => {
    const selectedValues = normalizeFaultDescSelection(formData.value.faultDescription)
    const selectedTexts = selectedValues
      .map(
        (value) => faultDescriptionOptions.value.find((item) => item.value === value)?.text || value
      )
      .map((item) => String(item ?? '').trim())
      .filter(Boolean)
    return selectedTexts.join('、')
  })

  const normalizeFaultDescSelection = (value: string | string[]) => {
    if (Array.isArray(value)) {
      return value.map((item) => String(item ?? '').trim()).filter(Boolean)
    }
    return String(value ?? '')
      .split(/[、，,]/)
      .map((item) => item.trim())
      .filter(Boolean)
      .map((item) => {
        const matched = faultDescriptionOptions.value.find((option) => option.text === item)
        return matched?.value ?? item
      })
  }

  const openFaultDescDropdown = () => {
    if (faultDescriptionOptions.value.length === 0) {
      uni.showToast({ title: '请先查询条码获取故障描述', icon: 'none', duration: TOAST_DURATION })
      return
    }
    draftFaultDesc.value = normalizeFaultDescSelection(formData.value.faultDescription)
    showFaultDescDropdown.value = true
  }

  const toggleDraftFaultDesc = (value: string) => {
    const val = String(value ?? '').trim()
    if (!val) return
    if (draftFaultDesc.value.includes(val)) {
      draftFaultDesc.value = draftFaultDesc.value.filter((x) => x !== val)
    } else {
      draftFaultDesc.value = [...draftFaultDesc.value, val]
    }
  }

  const cancelFaultDescSelect = () => {
    showFaultDescDropdown.value = false
    draftFaultDesc.value = []
  }

  const confirmFaultDescSelect = () => {
    const selectedTexts = draftFaultDesc.value
      .map(
        (value) => faultDescriptionOptions.value.find((item) => item.value === value)?.text || value
      )
      .map((item) => String(item ?? '').trim())
      .filter(Boolean)
    formData.value.faultDescription = selectedTexts.join('、')
    showFaultDescDropdown.value = false
    draftFaultDesc.value = []
    const form = formRef.value as { clearValidate?: (names?: string[]) => void } | null
    form?.clearValidate?.(['faultDescription'])
  }

  const handleScan = () => {
    uni.scanCode({
      success: (res) => {
        formData.value.warrantyCode = res.result
      }
    })
  }

  const checkWarranty = async () => {
    const barcode = String(formData.value.warrantyCode ?? '').trim()
    if (!barcode) {
      uni.showToast({ title: '请输入条形码', icon: 'none', duration: TOAST_DURATION })
      return
    }
    uni.showLoading({ title: '查询中...' })
    try {
      const res = await getBarcodeInfoAPI({ barcode })
      const mapped = mapBarcodeFaultOptions(res.result?.faultOptions)
      faultDescriptionOptions.value = mapped
      formData.value.faultDescription = ''
      showFaultDescDropdown.value = false
      draftFaultDesc.value = []
      const form = formRef.value as { clearValidate?: (names?: string[]) => void } | null
      form?.clearValidate?.(['faultDescription'])
      uni.hideLoading()
      uni.showToast({
        title: mapped.length > 0 ? res.msg : '未查询到故障描述选项',
        icon: res.code === API_SUCCESS_CODE && mapped.length > 0 ? 'success' : 'none',
        duration: TOAST_DURATION
      })
    } catch (err: unknown) {
      uni.hideLoading()
      faultDescriptionOptions.value = []
      formData.value.faultDescription = ''
      uni.showToast({
        title: parseUnknownError(err, '查询失败'),
        icon: 'none',
        duration: TOAST_DURATION
      })
    }
  }

  // 是否恢复暂存
  const hasRestoredRepairDraft = ref(false)
  // 重置后仅在实际再次进入页面（非从地图选点返回）时从本地恢复暂存
  const needReapplyDraftAfterReset = ref(false)
  // 同步表单中心ID到UniForms
  /**
   * 同步表单中心ID到UniForms
   * @returns void
   */
  const syncFormCenterIdToUniForms = () => {
    nextTick(() => {
      const id = formData.value.centerId
      const form = formRef.value as { setValue?: (name: string, value: unknown) => unknown } | null
      if (id != null && form?.setValue) {
        form.setValue('centerId', id)
      } else {
        form?.setValue?.('centerId', null)
      }
    })
  }

  /**
   * 页面显示
   * @returns void
   */
  onShow(() => {
    // 是否从地图选点返回
    const fromMap = hasPendingServicePointPick()
    // 应用存储选择
    applyStorageSelection()
    // 如果需要重新应用暂存，并且不是从地图选点返回
    if (needReapplyDraftAfterReset.value && !fromMap) {
      // 应用暂存
      applyOtherRepairDraft(
        loadOtherRepairDraft(),
        formData,
        selectedCenterDisplay,
        showSupplementSection,
        false
      )
      needReapplyDraftAfterReset.value = false
      hasRestoredRepairDraft.value = true
      syncFormCenterIdToUniForms()
      return
    }
    // 如果还没有恢复暂存
    if (!hasRestoredRepairDraft.value) {
      // 应用暂存
      applyOtherRepairDraft(
        loadOtherRepairDraft(),
        formData,
        selectedCenterDisplay,
        showSupplementSection,
        fromMap
      )
      hasRestoredRepairDraft.value = true
    }
    const pickedShippingAddress = takeSelectedShippingAddress()
    if (pickedShippingAddress) {
      selectedShippingAddress.value = pickedShippingAddress
      formData.value.shippingInfo =
        `${pickedShippingAddress.name} ${pickedShippingAddress.phone}\n` +
        pickedShippingAddress.fullAddress
      const form = formRef.value as { clearValidate?: (names?: string[]) => void } | null
      form?.clearValidate?.(['shippingInfo'])
    }
    syncFormCenterIdToUniForms()
    // 如果从地图选点返回
    if (fromMap) {
      nextTick(() => {
        uni.hideKeyboard()
      })
    }
  })

  /**
   * 表单规则
   * @returns 表单规则
   */
  const rules = computed(() => {
    const base: Record<string, { rules: { required?: boolean; errorMessage: string }[] }> = {
      centerId: {
        rules: [{ required: true, errorMessage: '请选择附近网点' }]
      },
      faultDescription: {
        rules: [{ required: true, errorMessage: '请填写故障描述' }]
      },
      repairType: {
        rules: [{ required: true, errorMessage: '请选择维修路径' }]
      }
    }
    if (formData.value.repairType === 'MAIL') {
      base.shippingInfo = {
        rules: [{ required: true, errorMessage: '请填写寄件信息' }]
      }
    }
    return base
  })

  /**
   * 维修类型变化
   * @param val - 维修类型
   * @returns void
   */
  watch(
    () => formData.value.repairType,
    (val) => {
      if (val === 'STORE') {
        formData.value.shippingInfo = ''
        selectedShippingAddress.value = null
        if (formRef.value && typeof formRef.value.clearValidate === 'function') {
          formRef.value.clearValidate(['shippingInfo'])
        }
      }
    }
  )

  /**
   * 选择寄件信息
   * @returns void
   */
  const chooseShippingAddress = () => {
    uni.navigateTo({ url: '/pages/address/index?mode=selectShipping' })
  }

  // 维修类型选项
  const repairTypes = REPAIR_TYPE_OPTIONS

  type FaultMediaPickEvent = { tempFiles: { fileType?: string }[] }

  const handleFaultMediaChange = (e: FaultMediaPickEvent) => {
    validateFaultMediaSelection(e.tempFiles)
  }

  /**
   * 构建非佳士品牌报修 payload（与佳士页同接口、同文件 ID 组装规则）
   * @returns payload
   */
  const buildOtherRepairWorkOrderPayload = (): CreateCustomerWorkOrderDTO => {
    const rawId = formData.value.centerId
    const sid =
      rawId != null && rawId !== '' ? (typeof rawId === 'number' ? rawId : Number(rawId)) : 0

    const brandNameTrim = String(formData.value.brandName ?? '').trim()
    const faultMedia = partitionFaultMediaFileIds(asUnknownArray(formData.value.images))
    const senderVoucherFileIds = collectVoucherFileIds(asUnknownArray(formData.value.shippingCode))
    const faultVoiceFileIds = collectVoiceFileIds(formData.value.voiceList)
    const shippingSubmitFields = resolveShippingSubmitFields(
      selectedShippingAddress.value,
      formData.value.shippingInfo,
      {
        fallbackName: String(userStore.userInfo?.name ?? ''),
        fallbackMobile: String(userStore.userInfo?.mobile ?? '')
      }
    )

    const base: CreateCustomerWorkOrderDTO = {
      barcode: String(formData.value.warrantyCode ?? '').trim(),
      brandCode: brandNameTrim || 'OTHER',
      brandName: brandNameTrim || undefined,
      brandType: CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE.NON_JASIC,
      customerName: String(userStore.userInfo?.name ?? ''),
      faultDesc: formData.value.faultDescription,
      faultItems: [],
      faultRemark: '',
      productCode: '',
      productModel: String(formData.value.modelName || '').trim(),
      sendExpressNo:
        formData.value.repairType === 'MAIL'
          ? resolveSendExpressNoForSubmit(formData.value.shippingCode)
          : '',
      senderAddress: formData.value.repairType === 'MAIL' ? shippingSubmitFields.senderAddress : '',
      senderMobile: formData.value.repairType === 'MAIL' ? shippingSubmitFields.senderMobile : '',
      senderName: formData.value.repairType === 'MAIL' ? shippingSubmitFields.senderName : '',
      serviceCompanyId: Number.isFinite(sid) ? sid : 0,
      serviceMode:
        REPAIR_TYPE_TO_SERVICE_MODE[formData.value.repairType] ?? formData.value.repairType,
      warrantyStatus: 'OUT_OF_WARRANTY'
    }
    if (faultMedia.faultImageFileIds.length) {
      base.faultImageFileIds = faultMedia.faultImageFileIds
    }
    if (faultMedia.faultVideoFileIds.length) {
      base.faultVideoFileIds = faultMedia.faultVideoFileIds
    }
    if (faultVoiceFileIds.length) {
      base.faultVoiceFileIds = faultVoiceFileIds
    }
    if (senderVoucherFileIds.length) {
      base.senderVoucherFileIds = senderVoucherFileIds
    }
    return base
  }
  /**
   * 执行非佳士品牌报修提交
   * @param options - 提交选项
   * @param options.validate - 是否验证表单
   * @param options.loadingTitle - 加载标题
   * @param options.redirect - 是否重定向
   * @returns void
   */
  const executeOtherRepairSubmit = async (options: {
    validate: boolean
    loadingTitle: string
    redirect: boolean
  }) => {
    if (options.validate) {
      const form = formRef.value as { validate?: () => Promise<unknown> } | null
      try {
        await form?.validate?.()
      } catch (err: unknown) {
        scrollToFirstInvalidUniFormField(err)
        return
      }
    }

    uni.showLoading({ title: options.loadingTitle })
    try {
      const res = await createCustomerWorkOrderAPI(buildOtherRepairWorkOrderPayload())
      uni.hideLoading()
      uni.showToast({ title: res.msg, icon: 'success', duration: 1500 })
      if (options.redirect) {
        clearOtherRepairDraft()
        setTimeout(() => {
          uni.redirectTo({ url: `/pages/order/list` })
        }, 1500)
      } else {
        saveOtherRepairDraft({
          formData: JSON.parse(JSON.stringify(formData.value)) as OtherRepairDraftForm,
          selectedCenterDisplay: selectedCenterDisplay.value,
          showSupplementSection: showSupplementSection.value
        })
      }
    } catch {
      uni.hideLoading()
      /* 失败提示由 http 层使用接口 msg */
    }
  }

  /**
   * 暂存非佳士品牌报修
   * @returns void
   */
  const stashForm = () => {
    // 暂存只保存本地草稿：不校验、不提交接口
    uni.showLoading({ title: '暂存中...' })
    try {
      saveOtherRepairDraft({
        formData: JSON.parse(JSON.stringify(formData.value)) as OtherRepairDraftForm,
        selectedCenterDisplay: selectedCenterDisplay.value,
        showSupplementSection: showSupplementSection.value
      })
      uni.hideLoading()
      uni.showToast({ title: '已暂存', icon: 'success', duration: 1500 })
    } catch (err: unknown) {
      uni.hideLoading()
      const msg = parseUnknownError(err, '暂存失败')
      uni.showToast({ title: msg, icon: 'none', duration: 1500 })
    }
  }

  /**
   * 重置：清空当前表单与网点展示；保留本地暂存键，再次进入页面（非地图返回）时回显暂存
   * @returns void
   */
  const resetForm = () => {
    showFaultDescDropdown.value = false
    draftFaultDesc.value = []
    needReapplyDraftAfterReset.value = true
    clearServicePointSelection()
    showSupplementSection.value = false
    formData.value = {
      warrantyCode: '',
      centerId: null,
      faultDescription: '',
      repairType: 'STORE',
      shippingInfo: '',
      voiceList: [],
      images: [],
      shippingCode: [],
      brandName: '',
      modelName: ''
    }
    selectedShippingAddress.value = null
    const form = formRef.value as { clearValidate?: (names?: string[]) => void } | null
    form?.clearValidate?.()
    nextTick(() => {
      const f = formRef.value as { setValue?: (name: string, value: unknown) => unknown } | null
      f?.setValue?.('centerId', null)
    })
  }

  /**
   * 提交非佳士品牌报修
   * @returns void
   */
  const submitForm = () => {
    void executeOtherRepairSubmit({
      validate: true,
      loadingTitle: '提交中...',
      redirect: true
    })
  }
</script>

<style lang="scss">
  .card-header {
    @include flex-row;
    justify-content: flex-start;
    gap: $space-md;

    .icon-box {
      padding: $space-sm;
      background-color: rgba($primary, 0.1);
      border-radius: $radius-md;
    }

    .header-text {
      view {
        font-size: $font-md;
        font-weight: bold;
      }
      text {
        font-size: $font-sm;
        color: $text-secondary;
      }
    }
  }

  .search-box {
    @include flex-row;
    gap: $space-sm;

    .search-box-main {
      position: relative;
      flex: 1;
    }

    .btn {
      flex: none;

      &.mini-btn {
        width: auto;
        height: 88rpx;
        margin: 0;
      }

      &:active {
        opacity: 0.9;
        transform: scale(0.98);
      }
    }
  }

  .fault-desc-picker {
    @include flex-row;
    align-items: center;
    justify-content: space-between;
    gap: $space-sm;
    height: 80rpx;
    padding: 0 $space-md;
    border: 1rpx solid $border-color;
    border-radius: $radius-input;
    background-color: $bg-light;
    box-sizing: border-box;
  }

  .fault-desc-picker-text {
    flex: 1;
    min-width: 0;
    font-size: $space-input;
    color: $text-main;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.placeholder {
      color: $text-placeholder;
    }
  }

  .fault-desc-dropdown {
    margin-top: $space-sm;
    border: 2rpx solid $border-light;
    border-radius: $radius-md;
    background: $bg-card;
    padding: $space-sm;
  }

  .fault-desc-option {
    @include flex-row;
    align-items: center;
    gap: $space-xs;
    padding: 12rpx 8rpx;
  }

  .fault-desc-option-text {
    font-size: 26rpx;
    color: $text-main;
  }

  .fault-desc-dropdown-actions {
    @include flex-row;
    justify-content: flex-end;
    gap: $space-sm;
    padding-top: $space-sm;
  }

  .dropdown-btn {
    font-size: 24rpx;
    padding: 10rpx 20rpx;
    border-radius: $radius-sm;
  }

  .dropdown-btn--cancel {
    color: $text-secondary;
    background: $bg-light;
  }

  .dropdown-btn--confirm {
    color: $text-bg;
    background: $primary;
  }

  :deep(.uni-data-checklist .checklist-box.is--default.is-checked .checkbox__inner-icon),
  :deep(.uni-data-checklist .checklist-box.is--default.is-checked .radio__inner-icon),
  :deep(.uni-data-checklist .checklist-box.is--default.is-checked .uni-icons) {
    color: $primary !important;
  }

  :deep(.uni-data-checklist .checklist-box.is--default .checkbox__inner) {
    width: 26rpx;
    height: 26rpx;
  }
</style>
