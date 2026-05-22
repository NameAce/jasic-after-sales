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
        <!-- 必填信息 -->
        <RepairFormSectionHeader title="必填信息" />
        <!-- 必填信息卡片 -->

        <view class="card card-shadow form-padding">
          <!-- 选择网点 -->
          <ServicePointFormItem :display-text="selectedCenterDisplay" />

          <!-- 故障备注说明 -->
          <uni-forms-item label="故障备注说明" name="faultRemark" required>
            <FormItemAnchor name="faultRemark" />
            <uni-easyinput
              v-model="formData.faultRemark"
              type="textarea"
              auto-height
              placeholder="请输入故障备注说明"
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
              <uni-icons type="right" size="14" :color="themeColor.textMuted" />
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
  import { themeColor } from '@/constants/theme'
  import { useUserStore } from '@/stores/modules/user'
  import { useServicePointSelection } from '@/composables/useServicePointSelection'
  import { useSupplementSection } from '@/composables/useSupplementSection'
  import { createCustomerWorkOrder, type CreateCustomerWorkOrderDTO } from '@/api/workOrder'
  import { requestEvaluationInviteSubscribe } from '@/utils/requestEvaluationInviteSubscribe'
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
  import {
    resolveSendExpressNoForSubmit,
    resolveShippingSubmitFields
  } from '@/utils/shippingSubmitFields'
  // 表单引用
  const formRef = ref(null)
  // 表单数据
  const formData: Ref<OtherRepairDraftForm> = ref({
    centerId: null,
    faultRemark: '',
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const rules = computed(() => {
    const base: Record<string, { rules: { required?: boolean; errorMessage: string }[] }> = {
      centerId: {
        rules: [{ required: true, errorMessage: '请选择附近网点' }]
      },
      faultRemark: {
        rules: [{ required: true, errorMessage: '请填写故障备注说明' }]
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
        fallbackName: String(userStore.userInfo?.nickname ?? ''),
        fallbackMobile: String(userStore.userInfo?.phone ?? '')
      }
    )

    const base: CreateCustomerWorkOrderDTO = {
      barcode: '',
      brandCode: brandNameTrim || 'OTHER',
      brandName: brandNameTrim || undefined,
      brandType: CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE.NON_JASIC,
      customerName: String(userStore.userInfo?.nickname ?? ''),
      faultDesc: '',
      faultItems: [],
      faultRemark: String(formData.value.faultRemark ?? '').trim(),
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
      const res = await createCustomerWorkOrder(buildOtherRepairWorkOrderPayload())
      uni.hideLoading()
      /**
 * 关单后服务端会推「客户满意度评价通知」，需在创建工单时完成订阅授权
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      await requestEvaluationInviteSubscribe()
      uni.showToast({ title: res.msg, icon: 'none', duration: 1500 })
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
      uni.showToast({ title: '已暂存', icon: 'none', duration: 1500 })
    } catch (err: unknown) {
      uni.hideLoading()
      const msg = parseUnknownError(err, '暂存失败')
      uni.showToast({ title: msg, icon: 'none', duration: 1500 })
    }
  }

  /**
   * 重置：清空当前表单与网点展示；保留本地暂存键，再次进入页面（非地图返回）时回显暂存
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const resetForm = () => {
    needReapplyDraftAfterReset.value = true
    clearServicePointSelection()
    showSupplementSection.value = false
    formData.value = {
      centerId: null,
      faultRemark: '',
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const submitForm = () => {
    void executeOtherRepairSubmit({
      validate: true,
      loadingTitle: '提交中...',
      redirect: true
    })
  }
</script>
