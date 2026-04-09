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
      <!-- 表单内容 -->
      <view class="form-content page-padding">
        <!-- 必填信息 -->
        <RepairFormSectionHeader title="必填信息" />
        <!-- 必填信息卡片 -->

        <view class="card card-shadow form-padding">
          <!-- 选择网点 -->
          <ServicePointFormItem :display-text="selectedCenterDisplay" />

          <!-- 故障描述 -->
          <uni-forms-item label="故障描述" name="faultDescription" required>
            <FormItemAnchor name="faultDescription" />
            <uni-easyinput
              v-model="formData.faultDescription"
              type="textarea"
              auto-height
              placeholder="请详细描述产品故障现象，以便我们更快为您处理..."
            />
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
            @select="handleFaultMediaSelect"
            @delete="handleFaultMediaDelete"
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
  import { createCustomerWorkOrderAPI, type CreateCustomerWorkOrderDTO } from '@/api/order'
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
  // 表单引用
  const formRef = ref(null)
  // 表单数据
  const formData: Ref<OtherRepairDraftForm> = ref({
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

  /**
   * 故障图片/视频选择
   * @param e - 选择事件
   * @returns void
   */
  const handleFaultMediaSelect = (e: { tempFiles: { fileType?: string }[] }) => {
    validateFaultMediaSelection(e.tempFiles)
  }

  /**
   * 故障图片/视频删除
   * @param e - 删除事件
   * @returns void
   */
  const handleFaultMediaDelete = (e: { tempFiles: { fileType?: string }[] }) => {
    validateFaultMediaSelection(e.tempFiles)
  }

  const resolveShippingSubmitFields = () => {
    const addr = selectedShippingAddress.value
    if (addr) {
      return {
        senderName: addr.name,
        senderMobile: addr.phone,
        senderAddress: addr.fullAddress
      }
    }
    const raw = String(formData.value.shippingInfo ?? '').trim()
    const lines = raw
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
    const firstLine = lines[0] ?? ''
    const match = firstLine.match(/^(.+?)\s+(1\d{10})$/)
    return {
      senderName: match?.[1] ?? String(userStore.userInfo?.name ?? ''),
      senderMobile: match?.[2] ?? String(userStore.userInfo?.mobile ?? ''),
      senderAddress: lines.length > 1 ? lines.slice(1).join('') : raw
    }
  }

  /**
   * 解析寄件快递单号（兼容字符串或对象字段）
   */
  const resolveSendExpressNoForSubmit = (): string => {
    const raw = formData.value.shippingCode as unknown
    if (typeof raw === 'string' || typeof raw === 'number') {
      return String(raw).trim()
    }
    if (Array.isArray(raw)) {
      for (const item of raw) {
        if (!item || typeof item !== 'object') continue
        const row = item as Record<string, unknown>
        const candidate =
          row.sendExpressNo ?? row.expressNo ?? row.shippingNo ?? row.expressCode ?? row.code
        if (typeof candidate === 'string' && candidate.trim()) return candidate.trim()
        if (typeof candidate === 'number') return String(candidate)
      }
    }
    return ''
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
    const shippingSubmitFields = resolveShippingSubmitFields()

    const base: CreateCustomerWorkOrderDTO = {
      barcode: '',
      brandCode: brandNameTrim || 'OTHER',
      brandName: brandNameTrim || undefined,
      brandType: CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE.NON_JASIC,
      customerName: String(userStore.userInfo?.name ?? ''),
      faultDesc: formData.value.faultDescription,
      faultItems: [],
      faultRemark: '',
      productCode: '',
      productModel: String(formData.value.modelName || '').trim(),
      sendExpressNo: formData.value.repairType === 'MAIL' ? resolveSendExpressNoForSubmit() : '',
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
      const e = err as { msg?: unknown; message?: unknown }
      const msg =
        (typeof e?.msg === 'string' && e.msg) ||
        (typeof e?.message === 'string' && e.message) ||
        '暂存失败'
      uni.showToast({ title: msg, icon: 'none', duration: 1500 })
    }
  }

  /**
   * 重置：清空当前表单与网点展示；保留本地暂存键，再次进入页面（非地图返回）时回显暂存
   * @returns void
   */
  const resetForm = () => {
    needReapplyDraftAfterReset.value = true
    clearServicePointSelection()
    showSupplementSection.value = false
    formData.value = {
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
  .shipping-address-btn {
    @include flex-row;
    justify-content: space-between;
    align-items: center;
    border: 1px solid #f0f0f0;
    border-radius: $radius-md;
    padding: 18rpx 24rpx;
    min-height: 80rpx;
    box-sizing: border-box;
    background-color: #f8fafc;
  }

  .shipping-address-text {
    flex: 1;
    font-size: $font-sm;
    color: $text-body;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-all;

    &.placeholder {
      color: #909399;
    }
  }
</style>
