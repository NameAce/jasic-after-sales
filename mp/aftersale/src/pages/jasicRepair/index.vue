<template>
  <!-- 导航栏 -->
  <custom-nav-bar title="佳士品牌报修" surface="sticky" />
  <!-- 表单页面 -->
  <view class="repair-form-page page-index">
    <view class="page-padding form-content">
      <view class="card card-shadow">
        <!-- 商品查询 -->
        <view class="card-header">
          <view class="icon-box">
            <uni-icons type="vip-filled" size="24" color="#f26604"></uni-icons>
          </view>
          <view class="header-text">
            <view>商品查询</view>
            <text>请输入或扫描产品条形码查询状态</text>
          </view>
        </view>
        <!-- 搜索框 -->
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
      <!-- 表单 -->
      <uni-forms
        ref="formRef"
        :model-value="formData"
        :rules="rules"
        label-position="top"
        label-width="auto"
      >
        <view class="form-content">
          <!-- 必填信息 -->
          <RepairFormSectionHeader title="必填信息" />

          <view class="card card-shadow form-padding">
            <!-- 选择网点 -->
            <ServicePointFormItem :display-text="selectedCenterDisplay" />

            <!-- 故障描述 -->
            <uni-forms-item
              v-if="barcodeQueryHasFaultDescription"
              label="故障描述"
              name="faultDescription"
              required
            >
              <FormItemAnchor name="faultDescription" />
              <!-- 故障描述选择 -->
              <uni-data-select
                v-model="formData.faultDescription"
                :localdata="faultDescriptionOptionsFromApi"
                placeholder="请选择"
                @change="handleFaultDescriptionChange"
              />
            </uni-forms-item>

            <!-- 故障说明备注 -->
            <uni-forms-item v-if="showFaultRemark" label="故障说明备注" name="faultRemark" required>
              <FormItemAnchor name="faultRemark" />
              <uni-easyinput
                v-model="formData.faultRemark"
                type="textarea"
                auto-height
                placeholder="请输入故障说明备注"
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

          <!-- 补充说明内容 -->
          <view v-show="showSupplementSection" class="card card-shadow form-padding">
            <!-- 语音输入 -->
            <VoiceInputField v-model="formData.voiceList" />
            <!-- 故障图片/视频 -->
            <MediaUploadField
              v-model="formData.images"
              label="故障视频/图片"
              tip="限1个视频、3张图"
              file-mediatype="all"
              :limit="4"
              :del-icon="true"
              @select="onFaultMediaChange"
              @delete="onFaultMediaChange"
            />

            <MediaUploadField
              v-model="formData.shippingCode"
              label="寄件快递单号"
              tip="限2张图片"
              file-mediatype="image"
              :limit="2"
              :max-file-size="1024 * 1024 * 10"
              :del-icon="true"
            />
          </view>
        </view>
      </uni-forms>
    </view>
  </view>

  <!-- 按钮 -->
  <base-button>
    <!-- 暂存 -->
    <view class="btn btn-secondary" @click="stashForm">暂存</view>
    <!-- 重置 -->
    <view class="btn btn-secondary" @click="resetForm">重置</view>
    <!-- 提交 -->
    <view class="btn btn-primary" @click="submitForm">提交</view>
  </base-button>
  <!-- 保修提示 -->
  <view v-if="showWarrantyModal" class="modal-mask" @click="showWarrantyModal = false">
    <!-- 弹窗内容 -->
    <view class="modal-content" @click.stop>
      <view class="modal-body">
        <view class="modal-icon-box">
          <uni-icons type="info-filled" size="40" color="#f26604"></uni-icons>
        </view>
        <view class="modal-title">报修提示</view>
        <view class="modal-desc">无条码或无法识别条码，系统默认该机器已过保</view>
        <view class="modal-actions">
          <button class="btn-cancel" @click="showWarrantyModal = false">取消</button>
          <button class="btn-confirm" @click="confirmSubmit">确认提交</button>
        </view>
      </view>
    </view>
  </view>
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
    JASIC_BRAND_CODE,
    REPAIR_TYPE_OPTIONS,
    REPAIR_TYPE_TO_SERVICE_MODE
  } from '@/constants/repairForm'
  import { useUserStore } from '@/stores/modules/user'
  import { useServicePointSelection } from '@/composables/useServicePointSelection'
  import { useSupplementSection } from '@/composables/useSupplementSection'
  import { validateFaultMediaSelection } from '@/utils/repairMediaLimits'
  import {
    asUnknownArray,
    collectVoucherFileIds,
    collectVoiceFileIds,
    partitionFaultMediaFileIds
  } from '@/utils/workOrderFileIds'
  import {
    createCustomerWorkOrderAPI,
    getBarcodeInfoAPI,
    mapBarcodeFaultOptions,
    type BarcodeInfoDTO,
    type CreateCustomerWorkOrderDTO
  } from '@/api/order'
  import { API_SUCCESS_CODE } from '@/utils/http'
  import {
    scrollPageToFormFieldKey,
    scrollToFirstInvalidUniFormField
  } from '@/utils/formFieldScrollFocus'
  import {
    applyJasicRepairDraft,
    clearJasicRepairDraft,
    loadJasicRepairDraft,
    saveJasicRepairDraft,
    type JasicRepairDraft,
    type JasicRepairDraftForm
  } from '@/utils/repairDraftStorage'
  import { takeSelectedShippingAddress, type SelectedShippingAddress } from '@/utils/addressStorage'

  const TOAST_DURATION = 1500

  /**
   * 解析未知错误
   * @param err - 错误
   * @param fallback - 默认错误信息
   * @returns 错误信息
   */
  const parseUnknownError = (err: unknown, fallback: string) => {
    const e = err as { msg?: unknown; message?: unknown }
    return (
      (typeof e?.msg === 'string' && e.msg) ||
      (typeof e?.message === 'string' && e.message) ||
      fallback
    )
  }

  /**
   * 获取表单实例
   * @returns 表单实例
   */
  type UniFormsInstance = {
    validate?: () => Promise<unknown>
    clearValidate?: (names?: string[]) => void
    setValue?: (name: string, value: unknown) => unknown
  }

  const formRef = ref<UniFormsInstance | null>(null)
  // 获取表单实例
  const getFormRef = () => formRef.value
  // 表单数据
  const formData: Ref<JasicRepairDraftForm> = ref({
    warrantyCode: '',
    centerId: null,
    repairType: 'STORE',
    faultDescription: '',
    images: [] as unknown[],
    shippingCode: [] as unknown[],
    faultRemark: '',
    shippingInfo: '',
    voiceList: [] as VoiceItem[]
  })

  // 是否显示寄件信息
  const showShippingInfo = computed(() => formData.value.repairType === 'MAIL')
  const shippingInfoDisplay = computed(() => formData.value.shippingInfo || '请选择寄件信息')
  const selectedShippingAddress = ref<SelectedShippingAddress | null>(null)

  // 是否显示故障说明备注
  const showFaultRemark = ref(false)
  // 是否显示保修提示
  const showWarrantyModal = ref(false)

  const userStore = useUserStore()
  /** 最近一次「查询保修」接口返回的完整 data，提交时优先取其中的 barcode / brand / 机型 / 保修等字段 */
  const lastBarcodeInfo = ref<BarcodeInfoDTO | null>(null)
  /** 仅当条码「查询」接口返回非空 faultOptions 时为 true，才展示故障描述下拉 */
  const barcodeQueryHasFaultDescription = ref(false)
  /** 条码查询返回的故障描述下拉（来自接口 data.faultOptions） */
  const faultDescriptionOptionsFromApi = ref<{ text: string; value: string }[]>([])
  /** 有条码但查询失败时，仍须展示并必填故障说明备注 */
  const queryFailedWithBarcode = ref(false)

  /** 是否为「其它 / 其他故障」类选项（展示并必填故障说明备注） */
  const isOtherFaultSelection = (value: string) => {
    const v = String(value ?? '').trim()
    if (!v) return false
    if (/^(other|others)$/i.test(v)) return true
    const opt = faultDescriptionOptionsFromApi.value.find((o) => o.value === v)
    const text = (opt?.text ?? '').trim()
    const label =
      typeof lastBarcodeInfo.value?.otherFaultLabel === 'string'
        ? lastBarcodeInfo.value.otherFaultLabel.trim()
        : ''
    if (label && (v === label || text === label)) return true
    if (/(其它|其他)/.test(text) && /故障/.test(text)) return true
    if (/(其它|其他)/.test(v) && /故障/.test(v)) return true
    return false
  }

  /** 按条码 / 查询结果同步是否展示「故障说明备注」 */
  const syncShowFaultRemarkFromState = () => {
    const code = String(formData.value.warrantyCode ?? '').trim()
    if (!code) {
      showFaultRemark.value = true
      return
    }
    if (barcodeQueryHasFaultDescription.value) {
      showFaultRemark.value = isOtherFaultSelection(formData.value.faultDescription)
      return
    }
    if (lastBarcodeInfo.value || queryFailedWithBarcode.value) {
      showFaultRemark.value = true
      return
    }
    showFaultRemark.value = false
  }

  // 表单规则
  const rules = computed(() => {
    // 基础规则
    const base: Record<string, { rules: { required?: boolean; errorMessage: string }[] }> = {
      centerId: {
        rules: [{ required: true, errorMessage: '请选择附近网点' }]
      },
      repairType: {
        rules: [{ required: true, errorMessage: '请选择维修路径' }]
      }
    }
    // 若条码查询返回了故障描述，则故障描述必填
    if (barcodeQueryHasFaultDescription.value) {
      base.faultDescription = {
        rules: [{ required: true, errorMessage: '请填写故障描述' }]
      }
    }
    // 展示故障说明备注时必填（无故障下拉时整段必填；有下拉时仅「其它/其他故障」时展示并必填）
    if (showFaultRemark.value) {
      base.faultRemark = {
        rules: [{ required: true, errorMessage: '请填写故障说明备注' }]
      }
    }
    // 如果选择维修路径为寄件，则寄件信息必填
    if (formData.value.repairType === 'MAIL') {
      base.shippingInfo = {
        rules: [{ required: true, errorMessage: '请填写寄件信息' }]
      }
    }
    return base
  })

  // 网点选择
  const {
    selectedCenterDisplay,
    applyStorageSelection,
    clearServicePointSelection,
    hasPendingServicePointPick
  } = useServicePointSelection(formData)

  // 补充说明
  const { showSupplementSection, toggleSupplementSection } = useSupplementSection(false)

  // 是否已恢复暂存
  const hasRestoredRepairDraft = ref(false)
  // 重置后仅在实际再次进入页面（非从地图选点返回）时从本地恢复暂存
  /** 重置后仅在实际再次进入页面（非从地图选点返回）时从本地恢复暂存 */
  const needReapplyDraftAfterReset = ref(false)
  /** 从本地恢复暂存中：避免 warrantyCode 的 watch 先清空条码查询态导致故障描述被卸表单项清空 */
  const isApplyingJasicRepairDraft = ref(false)

  /**
   * 应用佳士报修暂存（包装以配合 watch 跳过破坏性重置）
   */
  const runApplyJasicRepairDraft = (
    draft: ReturnType<typeof loadJasicRepairDraft>,
    preserveServicePoint: boolean
  ) => {
    isApplyingJasicRepairDraft.value = true
    try {
      applyJasicRepairDraft(
        draft,
        formData,
        selectedCenterDisplay,
        showSupplementSection,
        showFaultRemark,
        preserveServicePoint,
        barcodeQueryHasFaultDescription,
        faultDescriptionOptionsFromApi,
        lastBarcodeInfo,
        queryFailedWithBarcode
      )
    } finally {
      isApplyingJasicRepairDraft.value = false
    }
  }

  /**
   * 同步表单中心ID到uni-forms
   * @returns void
   */
  const syncFormCenterIdToUniForms = () => {
    nextTick(() => {
      const id = formData.value.centerId
      const form = getFormRef()
      if (id != null && form?.setValue) {
        form.setValue('centerId', id)
      } else {
        form?.setValue?.('centerId', null)
      }
    })
  }

  /**
   * 页面显示：合并暂存恢复与网点回写后的统一收尾（避免重复分支）
   */
  onShow(() => {
    const fromMap = hasPendingServicePointPick()
    applyStorageSelection()

    if (needReapplyDraftAfterReset.value && !fromMap) {
      runApplyJasicRepairDraft(loadJasicRepairDraft(), false)
      needReapplyDraftAfterReset.value = false
      hasRestoredRepairDraft.value = true
    } else if (!hasRestoredRepairDraft.value) {
      runApplyJasicRepairDraft(loadJasicRepairDraft(), fromMap)
      hasRestoredRepairDraft.value = true
    }
    const pickedShippingAddress = takeSelectedShippingAddress()
    if (pickedShippingAddress) {
      selectedShippingAddress.value = pickedShippingAddress
      formData.value.shippingInfo =
        `${pickedShippingAddress.name} ${pickedShippingAddress.phone}\n` +
        pickedShippingAddress.fullAddress
      getFormRef()?.clearValidate?.(['shippingInfo'])
    }

    syncFormCenterIdToUniForms()
    nextTick(() => {
      syncShowFaultRemarkFromState()
      tryAutoQueryBarcodeOnEnter(fromMap)
    })
    if (fromMap) {
      nextTick(() => {
        uni.hideKeyboard()
      })
    }
  })

  // 维修路径选项
  const repairTypes = REPAIR_TYPE_OPTIONS

  /** 暂存/自动保存草稿时的完整快照（含条码查询结果，便于再次进入恢复 UI 与提交入参） */
  const buildJasicRepairDraftSnapshot = (): JasicRepairDraft => ({
    formData: JSON.parse(JSON.stringify(formData.value)) as JasicRepairDraftForm,
    selectedCenterDisplay: selectedCenterDisplay.value,
    showSupplementSection: showSupplementSection.value,
    barcodeQueryHasFaultDescription: barcodeQueryHasFaultDescription.value,
    faultDescriptionOptions: [...faultDescriptionOptionsFromApi.value],
    lastBarcodeInfo: lastBarcodeInfo.value
      ? (JSON.parse(JSON.stringify(lastBarcodeInfo.value)) as BarcodeInfoDTO)
      : null,
    queryFailedWithBarcode: queryFailedWithBarcode.value
  })

  /**
   * 扫描条形码
   * @returns void
   */
  const handleScan = () => {
    // 扫描条形码
    uni.scanCode({
      // 成功回调
      success: (res) => {
        // 设置条形码
        formData.value.warrantyCode = res.result
      }
    })
  }

  /**
   * 查询保修
   * @param options.silentToast - 进入页自动查询时不弹成功提示，避免打扰
   * @param options.skipClearFaultFieldsWhenNoOptions - 自动查询且接口无故障下拉时不清空已填备注（与暂存恢复配合）
   * @returns void
   */
  const checkWarranty = async (options?: {
    silentToast?: boolean
    skipClearFaultFieldsWhenNoOptions?: boolean
  }) => {
    const silentToast = options?.silentToast ?? false
    const skipClearFaultFieldsWhenNoOptions = options?.skipClearFaultFieldsWhenNoOptions ?? false
    // 如果条形码为空，则显示提示
    if (!formData.value.warrantyCode) {
      scrollPageToFormFieldKey('warrantyCode')
      return uni.showToast({ title: '请输入条形码', icon: 'none', duration: TOAST_DURATION })
    }
    // 显示加载中
    uni.showLoading({ title: '查询中...' })
    // 尝试查询保修
    try {
      // 查询保修
      const res = await getBarcodeInfoAPI({ barcode: formData.value.warrantyCode })
      // 隐藏加载中
      uni.hideLoading()
      // 获取保修信息（http 层已将 data 归一为 result）
      const info = res.result
      const row = info && typeof info === 'object' ? (info as Record<string, unknown>) : null
      const mapped = mapBarcodeFaultOptions(row?.faultOptions)
      faultDescriptionOptionsFromApi.value = mapped
      const hasFd = mapped.length > 0
      barcodeQueryHasFaultDescription.value = hasFd
      queryFailedWithBarcode.value = false
      if (info && typeof info === 'object') {
        lastBarcodeInfo.value = info as BarcodeInfoDTO
      } else {
        lastBarcodeInfo.value = null
      }
      if (!hasFd) {
        if (!skipClearFaultFieldsWhenNoOptions) {
          formData.value.faultDescription = ''
          formData.value.faultRemark = ''
          nextTick(() => {
            getFormRef()?.clearValidate?.(['faultDescription', 'faultRemark'])
            syncShowFaultRemarkFromState()
          })
        } else {
          nextTick(() => {
            syncShowFaultRemarkFromState()
          })
        }
      } else {
        nextTick(() => {
          syncShowFaultRemarkFromState()
        })
      }
      // 显示提示（自动查询不弹成功 toast）
      if (!silentToast) {
        uni.showToast({
          title: res.msg,
          icon: res.code === API_SUCCESS_CODE ? 'success' : 'none',
          duration: TOAST_DURATION
        })
      }
    } catch (err: unknown) {
      uni.hideLoading()
      faultDescriptionOptionsFromApi.value = []
      barcodeQueryHasFaultDescription.value = false
      lastBarcodeInfo.value = null
      queryFailedWithBarcode.value = !!String(formData.value.warrantyCode ?? '').trim()
      nextTick(() => {
        syncShowFaultRemarkFromState()
      })
      uni.showToast({
        title: parseUnknownError(err, '查询失败'),
        icon: 'none',
        duration: TOAST_DURATION
      })
    }
  }

  /**
   * 进入页面且条码已有值时自动查询一次（等同点击「查询」），用于恢复故障描述下拉与 lastBarcodeInfo
   */
  const tryAutoQueryBarcodeOnEnter = (fromMapPick: boolean) => {
    if (fromMapPick) return
    const code = String(formData.value.warrantyCode ?? '').trim()
    if (!code) return
    void checkWarranty({ silentToast: true, skipClearFaultFieldsWhenNoOptions: true })
  }

  /**
   * 故障描述变化
   * @param e - 故障描述
   * @returns void
   */
  const handleFaultDescriptionChange = (e: string) => {
    const nextShow = isOtherFaultSelection(e)
    showFaultRemark.value = nextShow
    if (!nextShow) {
      formData.value.faultRemark = ''
      getFormRef()?.clearValidate?.(['faultRemark'])
    }
  }

  /**
   * 选择寄件信息
   * @returns void
   */
  const chooseShippingAddress = () => {
    uni.navigateTo({ url: '/pages/address/index?mode=selectShipping' })
  }

  /**
   * 条形码变化
   * @param val - 条形码
   * @returns void
   */
  watch(
    () => formData.value.warrantyCode,
    (val, oldVal) => {
      if (isApplyingJasicRepairDraft.value) {
        if (!val) {
          formData.value.faultDescription = ''
          showFaultRemark.value = true
        } else {
          nextTick(() => {
            syncShowFaultRemarkFromState()
          })
        }
        return
      }
      if (val !== oldVal) {
        lastBarcodeInfo.value = null
        barcodeQueryHasFaultDescription.value = false
        faultDescriptionOptionsFromApi.value = []
        queryFailedWithBarcode.value = false
      }
      // 如果条形码为空，则清空故障描述
      if (!val) {
        formData.value.faultDescription = ''
        showFaultRemark.value = true
      } else {
        nextTick(() => {
          syncShowFaultRemarkFromState()
        })
      }
    },
    { immediate: true }
  )

  /**
   * 维修路径变化
   * @param val - 维修路径
   * @returns void
   */
  watch(
    () => formData.value.repairType,
    (val) => {
      // 如果维修路径为送店，则清空寄件信息
      if (val === 'STORE') {
        // 清空寄件信息
        formData.value.shippingInfo = ''
        selectedShippingAddress.value = null
        // 如果表单引用存在且clearValidate函数存在，则清空寄件信息验证
        getFormRef()?.clearValidate?.(['shippingInfo'])
      }
    }
  )

  type FaultMediaPickEvent = { tempFiles: { fileType?: string }[] }

  const onFaultMediaChange = (e: FaultMediaPickEvent) => {
    validateFaultMediaSelection(e.tempFiles)
  }

  /**
   * 获取故障描述文本
   * @returns 故障描述文本
   */
  const getJasicFaultDescriptionText = () => {
    if (!barcodeQueryHasFaultDescription.value) {
      return ''
    }
    const descOption = faultDescriptionOptionsFromApi.value.find(
      (o) => o.value === formData.value.faultDescription
    )
    if (isOtherFaultSelection(formData.value.faultDescription)) {
      return descOption?.text || '其它故障'
    }
    return descOption?.text || formData.value.faultDescription || ''
  }
  /**
   * 从条码查询结果解析保修状态枚举（与后端一致）
   */
  const resolveWarrantyStatusFromBarcodeInfo = (info: BarcodeInfoDTO | null): string => {
    if (!info) return ''
    const ws = info.warrantyStatus
    if (typeof ws === 'string' && ws.trim()) return ws.trim().toUpperCase()
    if (info.inWarranty === true) return 'IN_WARRANTY'
    if (info.inWarranty === false) return 'OUT_OF_WARRANTY'
    return ''
  }

  /**
   * 无故障下拉时，故障描述/项可从接口 faultDesc、faultDescription、faultItems 兜底
   */
  const resolveFaultDescForSubmit = (): string => {
    if (barcodeQueryHasFaultDescription.value) return getJasicFaultDescriptionText()
    const info = lastBarcodeInfo.value
    const fromApi = info?.faultDescription ?? info?.faultDesc
    if (typeof fromApi === 'string' && fromApi.trim()) return fromApi.trim()
    return String(formData.value.faultRemark ?? '').trim()
  }

  const resolveFaultItemsForSubmit = (): string[] => {
    if (barcodeQueryHasFaultDescription.value && formData.value.faultDescription) {
      return [formData.value.faultDescription]
    }
    const raw = lastBarcodeInfo.value?.faultItems
    if (Array.isArray(raw) && raw.length > 0) {
      return raw.map((x) => String(x)).filter((s) => s.length > 0)
    }
    return []
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
    // 回填旧草稿场景兜底：shippingInfo 第一行格式为「姓名 手机号」
    const raw = String(formData.value.shippingInfo ?? '').trim()
    const lines = raw
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
    const firstLine = lines[0] ?? ''
    const match = firstLine.match(/^(.+?)\s+(1\d{10})$/)
    return {
      senderName: match?.[1] ?? '',
      senderMobile: match?.[2] ?? '',
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
   * 构建佳士报修 payload
   * @returns payload
   */
  const buildJasicWorkOrderPayload = (): CreateCustomerWorkOrderDTO => {
    const rawId = formData.value.centerId
    const sid =
      rawId != null && rawId !== '' ? (typeof rawId === 'number' ? rawId : Number(rawId)) : 0
    const api = lastBarcodeInfo.value
    const barcodeTrim = String(formData.value.warrantyCode ?? '').trim()
    const barcodeFromApi = api?.barcode != null ? String(api.barcode).trim() : ''
    let warrantyStatus = resolveWarrantyStatusFromBarcodeInfo(api)
    if (!barcodeTrim && !warrantyStatus) {
      warrantyStatus = 'OUT_OF_WARRANTY'
    }
    const brandType = CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE.JASIC

    const faultMedia = partitionFaultMediaFileIds(asUnknownArray(formData.value.images))
    const senderVoucherFileIds = collectVoucherFileIds(asUnknownArray(formData.value.shippingCode))
    const faultVoiceFileIds = collectVoiceFileIds(formData.value.voiceList)
    const shippingSubmitFields = resolveShippingSubmitFields()

    const base: CreateCustomerWorkOrderDTO = {
      barcode: barcodeFromApi || barcodeTrim,
      brandCode: JASIC_BRAND_CODE,
      brandName: typeof api?.brandName === 'string' ? api.brandName : undefined,
      customerName: String(userStore.userInfo?.name ?? ''),
      faultDesc: resolveFaultDescForSubmit(),
      faultItems: resolveFaultItemsForSubmit(),
      faultRemark: formData.value.faultRemark,
      productCode: api?.productCode != null ? String(api.productCode) : '',
      productModel: api?.productModel != null ? String(api.productModel) : '',
      brandType,
      sendExpressNo: formData.value.repairType === 'MAIL' ? resolveSendExpressNoForSubmit() : '',
      senderAddress: formData.value.repairType === 'MAIL' ? shippingSubmitFields.senderAddress : '',
      senderMobile: formData.value.repairType === 'MAIL' ? shippingSubmitFields.senderMobile : '',
      senderName: formData.value.repairType === 'MAIL' ? shippingSubmitFields.senderName : '',
      serviceCompanyId: Number.isFinite(sid) ? sid : 0,
      serviceMode:
        REPAIR_TYPE_TO_SERVICE_MODE[formData.value.repairType] ?? formData.value.repairType,
      warrantyStatus
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
   * 执行佳士报修提交
   * @param options - 提交选项
   * @param options.validate - 是否验证表单
   * @param options.loadingTitle - 加载标题
   * @param options.redirect - 是否重定向
   * @returns void
   */
  const executeJasicRepairSubmit = async (options: {
    validate: boolean
    loadingTitle: string
    redirect: boolean
  }) => {
    showWarrantyModal.value = false

    if (options.validate) {
      try {
        await getFormRef()?.validate?.()
      } catch (err: unknown) {
        scrollToFirstInvalidUniFormField(err)
        return
      }
    }

    uni.showLoading({ title: options.loadingTitle })

    try {
      const res = await createCustomerWorkOrderAPI(buildJasicWorkOrderPayload())
      uni.hideLoading()
      uni.showToast({ title: res.msg, icon: 'success', duration: TOAST_DURATION })
      // 如果需要重定向，则清除暂存并重定向
      if (options.redirect) {
        clearJasicRepairDraft()
        setTimeout(() => {
          uni.redirectTo({
            url: `/pages/order/list`
          })
        }, TOAST_DURATION)
      } else {
        // 保存佳士报修暂存
        saveJasicRepairDraft(buildJasicRepairDraftSnapshot())
      }
    } catch (err: unknown) {
      uni.hideLoading()
      uni.showToast({
        title: parseUnknownError(err, '提交失败'),
        icon: 'none',
        duration: TOAST_DURATION
      })
    }
  }

  const performSubmit = () => {
    void executeJasicRepairSubmit({
      validate: true,
      loadingTitle: '提交中...',
      redirect: true
    })
  }

  /**
   * 执行佳士报修暂存
   * @returns void
   */
  const stashForm = () => {
    // 暂存只保存本地草稿：不校验、不提交接口
    uni.showLoading({ title: '暂存中...' })
    try {
      saveJasicRepairDraft(buildJasicRepairDraftSnapshot())
      uni.hideLoading()
      uni.showToast({ title: '已暂存', icon: 'success', duration: TOAST_DURATION })
    } catch (err: unknown) {
      uni.hideLoading()
      uni.showToast({
        title: parseUnknownError(err, '暂存失败'),
        icon: 'none',
        duration: TOAST_DURATION
      })
    }
  }

  /**
   * 重置表单
   * @returns void
   */
  const resetForm = () => {
    // 显示保修提示
    showWarrantyModal.value = false
    lastBarcodeInfo.value = null
    barcodeQueryHasFaultDescription.value = false
    faultDescriptionOptionsFromApi.value = []
    queryFailedWithBarcode.value = false
    // 设置需要重新应用暂存为true
    needReapplyDraftAfterReset.value = true
    // 清除网点选择
    clearServicePointSelection()
    // 设置补充说明为false
    showSupplementSection.value = false
    // 清空表单数据
    formData.value = {
      warrantyCode: '',
      centerId: null,
      repairType: 'STORE',
      faultDescription: '',
      images: [],
      shippingCode: [],
      faultRemark: '',
      shippingInfo: '',
      voiceList: []
    }
    selectedShippingAddress.value = null
    // 显示故障说明备注
    showFaultRemark.value = true
    // 获取表单引用
    getFormRef()?.clearValidate?.()
    nextTick(() => {
      getFormRef()?.setValue?.('centerId', null)
    })
  }

  /**
   * 提交表单
   * @returns void
   */
  const submitForm = () => {
    // 如果条形码不为空，则执行佳士报修提交
    if (formData.value.warrantyCode) {
      performSubmit()
      return
    }
    // 尝试验证表单
    const validatePromise = getFormRef()?.validate?.()
    if (validatePromise) {
      validatePromise
        .then(() => {
          showWarrantyModal.value = true
        })
        .catch((err: unknown) => {
          scrollToFirstInvalidUniFormField(err)
        })
    }
  }

  /**
   * 确认提交
   * @returns void
   */
  const confirmSubmit = () => {
    performSubmit()
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
