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
            <uni-icons type="vip-filled" size="24" :color="themeColors.primary"></uni-icons>
          </view>
          <view class="header-text">
            <view>商品查询</view>
            <text>输入满22位自动查询，也可扫码或点「查询」</text>
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
              <view class="fault-desc-picker" @click="openFaultDescDropdown">
                <text :class="['fault-desc-picker-text', { placeholder: !selectedFaultDescText }]">
                  {{ selectedFaultDescText || '请选择' }}
                </text>
                <uni-icons type="down" size="15" :color="themeColors.iconSlateLight" />
              </view>
              <view v-if="showFaultDescDropdown" class="fault-desc-dropdown">
                <view
                  v-for="option in faultDescriptionOptionsFromApi"
                  :key="option.value"
                  class="fault-desc-option"
                  @click.stop="toggleDraftFaultDesc(option.value)"
                >
                  <checkbox
                    :checked="draftFaultDesc.includes(option.value)"
                    :color="themeColors.primary"
                    style="transform: scale(0.8); transform-origin: center"
                  />
                  <text class="fault-desc-option-text">{{ option.text }}</text>
                </view>
                <view class="fault-desc-dropdown-actions">
                  <view
                    class="dropdown-btn dropdown-btn--cancel"
                    @click.stop="cancelFaultDescSelect"
                  >
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
                <uni-icons type="right" size="14" :color="themeColors.textMuted" />
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
          <uni-icons type="info-filled" size="40" :color="themeColors.primary"></uni-icons>
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
  import { onLoad, onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import RepairTypeSelector from '@/components/RepairTypeSelector/RepairTypeSelector.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import VoiceInputField, { type VoiceItem } from '@/components/VoiceInputField/VoiceInputField.vue'
  import ServicePointFormItem from '@/components/ServicePointFormItem/ServicePointFormItem.vue'
  import RepairFormSectionHeader from '@/components/RepairFormSectionHeader/RepairFormSectionHeader.vue'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import { themeColors } from '@/constants/theme'
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
    createCustomerWorkOrder,
    getCustomerWorkOrderBarcodeInfo,
    mapBarcodeFaultOptions,
    type BarcodeInfoDTO,
    type CreateCustomerWorkOrderDTO
  } from '@/api/workOrder'
  import { requestEvaluationInviteSubscribe } from '@/utils/requestEvaluationInviteSubscribe'
  import { API_SUCCESS_CODE, getApiMessage } from '@/utils/http'
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
  import { parseUnknownError } from '@/utils/errorMessage'
  import { scanProductBarcode } from '@/utils/scanProductBarcode'
  import { hideLoadingThenShowBarcodeQueryToast } from '@/utils/barcodeQueryToast'
  import {
    resolveSendExpressNoForSubmit,
    resolveShippingSubmitFields
  } from '@/utils/shippingSubmitFields'
  import { hideRequestLoading, showApiToast, showRequestLoading } from '@/utils/uiFeedback'

  const TOAST_DURATION = 1500

  /**
   * 获取表单实例
   * @returns 表单实例
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
    faultDescription: [],
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
  /**
 * 最近一次「查询保修」接口返回的完整 data，提交时优先取其中的 barcode / brand / 机型 / 保修等字段
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const lastBarcodeInfo = ref<BarcodeInfoDTO | null>(null)
  /**
 * 仅当条码「查询」接口返回非空 faultOptions 时为 true，才展示故障描述下拉
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const barcodeQueryHasFaultDescription = ref(false)
  /**
 * 条码查询返回的故障描述下拉（来自接口 data.faultOptions）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const faultDescriptionOptionsFromApi = ref<{ text: string; value: string }[]>([])
  const showFaultDescDropdown = ref(false)
  const draftFaultDesc = ref<string[]>([])
  /**
 * 有条码但查询失败时，仍须展示并必填故障说明备注
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const queryFailedWithBarcode = ref(false)

  /**
 * 是否为「其它 / 其他故障」类选项（展示并必填故障说明备注）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const isOtherFaultSelection = (value: string | string[]) => {
    const values = Array.isArray(value)
      ? value.map((item) => String(item ?? '').trim()).filter(Boolean)
      : [String(value ?? '').trim()].filter(Boolean)
    if (values.length === 0) return false
    const label =
      typeof lastBarcodeInfo.value?.otherFaultLabel === 'string'
        ? lastBarcodeInfo.value.otherFaultLabel.trim()
        : ''
    return values.some((v) => {
      if (/^(other|others)$/i.test(v)) return true
      const opt = faultDescriptionOptionsFromApi.value.find((o) => o.value === v)
      const text = (opt?.text ?? '').trim()
      if (label && (v === label || text === label)) return true
      if (text === '其它' || text === '其他') return true
      if (/(其它|其他)/.test(text) && /故障/.test(text)) return true
      if (/(其它|其他)/.test(v) && /故障/.test(v)) return true
      return false
    })
  }

  /**
 * 按条码 / 查询结果同步是否展示「故障说明备注」
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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

  const normalizeFaultDescSelection = (value: string | string[]) => {
    const values = Array.isArray(value) ? value : [value]
    return values.map((item) => String(item ?? '').trim()).filter(Boolean)
  }

  const selectedFaultDescText = computed(() => {
    const selectedValues = normalizeFaultDescSelection(formData.value.faultDescription)
    const selectedTexts = selectedValues
      .map((value) => {
        const option = faultDescriptionOptionsFromApi.value.find((item) => item.value === value)
        return option?.text || value
      })
      .map((item) => String(item ?? '').trim())
      .filter(Boolean)
    return selectedTexts.join('、')
  })

  const openFaultDescDropdown = () => {
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
    formData.value.faultDescription = [...draftFaultDesc.value]
    showFaultDescDropdown.value = false
    draftFaultDesc.value = []
    getFormRef()?.clearValidate?.(['faultDescription'])
    handleFaultDescriptionChange(formData.value.faultDescription)
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
  /**
 * 重置后仅在实际再次进入页面（非从地图选点返回）时从本地恢复暂存
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const needReapplyDraftAfterReset = ref(false)
  /**
 * 从本地恢复暂存中：避免 warrantyCode 的 watch 先清空条码查询态导致故障描述被卸表单项清空
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const isApplyingJasicRepairDraft = ref(false)
  /**
   * 首页扫码进入时携带的条码：须在暂存恢复后仍保留，并触发一次自动查询
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const entryBarcodeFromHomeScan = ref('')

  /** 佳士商品条码满该长度后自动触发查询（与档案条码位数一致） */
  const BARCODE_AUTO_QUERY_LENGTH = 22
  /** 避免同一条码重复请求、与扫码/进入页查询并发 */
  const barcodeQueryInFlight = ref(false)
  const lastAutoQueriedBarcode = ref('')
  /** 仅最新一次 checkWarranty 可更新 UI / 弹 toast（避免 onShow 静默重查吞掉扫码成功提示） */
  const barcodeQuerySeq = ref(0)

  /**
   * 手动输入条码达到指定长度时自动查询
   * @param raw - 当前输入框条码
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const tryAutoQueryWhenBarcodeLengthReached = (raw: string) => {
    if (isApplyingJasicRepairDraft.value) return
    const code = String(raw ?? '').trim()
    if (code.length < BARCODE_AUTO_QUERY_LENGTH) return
    if (barcodeQueryInFlight.value) return
    if (code === lastAutoQueriedBarcode.value) return
    void checkWarranty()
  }

  /**
   * 应用佳士报修暂存（包装以配合 watch 跳过破坏性重置）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
   * 路由参数：首页扫码带入的条码（进入后自动查询，无需再点「查询」）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  onLoad((options?: Record<string, string>) => {
    const code = String(options?.barcode ?? options?.warrantyCode ?? '').trim()
    if (!code) return
    try {
      entryBarcodeFromHomeScan.value = decodeURIComponent(code)
    } catch {
      entryBarcodeFromHomeScan.value = code
    }
    formData.value.warrantyCode = entryBarcodeFromHomeScan.value
  })

  /**
   * 页面显示：合并暂存恢复与网点回写后的统一收尾（避免重复分支）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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

    // 首页扫码条码优先于暂存中的条码
    const homeScanCode = entryBarcodeFromHomeScan.value.trim()
    if (homeScanCode) {
      formData.value.warrantyCode = homeScanCode
    }

    syncFormCenterIdToUniForms()
    nextTick(() => {
      syncShowFaultRemarkFromState()
      if (homeScanCode) {
        entryBarcodeFromHomeScan.value = ''
        lastAutoQueriedBarcode.value = homeScanCode
        void checkWarranty()
        return
      }
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

  /**
 * 暂存/自动保存草稿时的完整快照（含条码查询结果，便于再次进入恢复 UI 与提交入参）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
   * 扫描条形码：写入条码后立即查询保修（无需再点「查询」）
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const handleScan = async () => {
    const scanRes = await scanProductBarcode({ toastOnCancel: true })
    if (scanRes.status === 'cancel') return
    if (scanRes.status === 'empty') {
      void showApiToast('未识别到条形码')
      return
    }
    const code = scanRes.code.trim()
    // 先标记已处理条码，避免 watch / onShow 静默重查；并作废进行中的查询（iOS 扫码返回常触发 onShow）
    lastAutoQueriedBarcode.value = code
    barcodeQuerySeq.value += 1
    formData.value.warrantyCode = code
    void checkWarranty()
  }

  /**
   * 条码查询失败提示：优先接口 `msg`（如「当前条码未维护档案信息」）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const pickBarcodeQueryErrorMsg = (err: unknown) =>
    getApiMessage(err as { msg?: string } | null | undefined, parseUnknownError(err, '查询失败'))

  /**
   * 查询保修
   * @param options.silentToast - 进入页自动查询时不弹成功提示，避免打扰
   * @param options.skipClearFaultFieldsWhenNoOptions - 自动查询且接口无故障下拉时不清空已填备注（与暂存恢复配合）
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
      void showApiToast('请输入条形码')
      return
    }
    const queryingBarcode = String(formData.value.warrantyCode ?? '').trim()
    const seq = ++barcodeQuerySeq.value
    barcodeQueryInFlight.value = true
    // 条码查询是 GET，需显式给用户「查询中」反馈
    showRequestLoading('查询中...')
    try {
      const res = await getCustomerWorkOrderBarcodeInfo({ barcode: queryingBarcode })
      if (seq !== barcodeQuerySeq.value) return

      if (res.code !== API_SUCCESS_CODE) {
        faultDescriptionOptionsFromApi.value = []
        barcodeQueryHasFaultDescription.value = false
        lastBarcodeInfo.value = null
        queryFailedWithBarcode.value = !!String(formData.value.warrantyCode ?? '').trim()
        nextTick(() => syncShowFaultRemarkFromState())
        const failMsg = getApiMessage(res, '查询失败')
        hideLoadingThenShowBarcodeQueryToast({ title: failMsg, kind: 'fail', icon: 'none' })
        return
      }

      const successMsg = getApiMessage(res, '查询成功')
      if (!silentToast) {
        hideLoadingThenShowBarcodeQueryToast({ title: successMsg, kind: 'success' })
      } else {
        hideRequestLoading()
      }

      const info = res.data
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
          formData.value.faultDescription = []
          formData.value.faultRemark = ''
          nextTick(() => {
            getFormRef()?.clearValidate?.(['faultDescription', 'faultRemark'])
            syncShowFaultRemarkFromState()
          })
        } else {
          nextTick(() => syncShowFaultRemarkFromState())
        }
      } else {
        nextTick(() => syncShowFaultRemarkFromState())
      }
    } catch (err: unknown) {
      if (seq !== barcodeQuerySeq.value) return
      faultDescriptionOptionsFromApi.value = []
      barcodeQueryHasFaultDescription.value = false
      lastBarcodeInfo.value = null
      queryFailedWithBarcode.value = !!String(formData.value.warrantyCode ?? '').trim()
      nextTick(() => syncShowFaultRemarkFromState())
      // 失败一律展示接口 msg（扫码/手动查询均适用）；silentToast 仅抑制成功提示
      const errMsg = pickBarcodeQueryErrorMsg(err)
      hideLoadingThenShowBarcodeQueryToast({ title: errMsg, kind: 'fail', icon: 'none' })
    } finally {
      if (seq === barcodeQuerySeq.value) {
        barcodeQueryInFlight.value = false
        lastAutoQueriedBarcode.value = queryingBarcode
      }
    }
  }

  /**
   * 进入页面且条码已有值时自动查询一次（等同点击「查询」），用于恢复故障描述下拉与 lastBarcodeInfo
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const tryAutoQueryBarcodeOnEnter = (fromMapPick: boolean) => {
    if (fromMapPick) return
    const code = String(formData.value.warrantyCode ?? '').trim()
    if (!code) return
    // 扫码返回会触发 onShow：若条码刚查过/正在查，不再静默重查（第二次 hideLoading 会导致 iOS 成功 toast 不显示）
    if (code === lastAutoQueriedBarcode.value) return
    if (barcodeQueryInFlight.value) return
    void checkWarranty({ silentToast: true, skipClearFaultFieldsWhenNoOptions: true })
  }

  /**
   * 故障描述变化
   * @param e - 故障描述
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleFaultDescriptionChange = (e: string | string[]) => {
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const chooseShippingAddress = () => {
    uni.navigateTo({ url: '/pages/address/index?mode=selectShipping' })
  }

  /**
   * 条形码变化
   * @param val - 条形码
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  watch(
    () => formData.value.warrantyCode,
    (val, oldVal) => {
      if (isApplyingJasicRepairDraft.value) {
        if (!val) {
          formData.value.faultDescription = []
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
        showFaultDescDropdown.value = false
        draftFaultDesc.value = []
        queryFailedWithBarcode.value = false
        const code = String(val ?? '').trim()
        if (code.length < BARCODE_AUTO_QUERY_LENGTH) {
          lastAutoQueriedBarcode.value = ''
        }
      }
      // 如果条形码为空，则清空故障描述
      if (!val) {
        formData.value.faultDescription = []
        showFaultRemark.value = true
      } else {
        nextTick(() => {
          syncShowFaultRemarkFromState()
        })
      }
      tryAutoQueryWhenBarcodeLengthReached(String(val ?? ''))
    },
    { immediate: true }
  )

  /**
   * 维修路径变化
   * @param val - 维修路径
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const getJasicFaultDescriptionText = () => {
    if (!barcodeQueryHasFaultDescription.value) {
      return ''
    }
    const selectedValues = Array.isArray(formData.value.faultDescription)
      ? formData.value.faultDescription
      : [formData.value.faultDescription]
    const selectedTexts = selectedValues
      .map((value) => {
        const option = faultDescriptionOptionsFromApi.value.find((o) => o.value === value)
        return option?.text || value
      })
      .map((item) => String(item ?? '').trim())
      .filter(Boolean)
    if (selectedTexts.length === 0) return ''
    return selectedTexts.join('、')
  }
  /**
   * 从条码查询结果解析保修状态枚举（与后端一致）
   *
   * 真源：`CustomerBarcodeInfoVO.warrantyStatus`，字符串枚举 `IN_WARRANTY / OUT_OF_WARRANTY`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const resolveWarrantyStatusFromBarcodeInfo = (info: BarcodeInfoDTO | null): string => {
    if (!info) return ''
    const ws = info.warrantyStatus
    if (typeof ws === 'string' && ws.trim()) return ws.trim().toUpperCase()
    return ''
  }

  /**
   * 故障描述提交文本：有下拉选项时取用户选择，否则以用户填写的 faultRemark 为准
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const resolveFaultDescForSubmit = (): string => {
    if (barcodeQueryHasFaultDescription.value) return getJasicFaultDescriptionText()
    return String(formData.value.faultRemark ?? '').trim()
  }

  const resolveFaultItemsForSubmit = (): string[] => {
    if (!barcodeQueryHasFaultDescription.value) return []
    const selectedValues = Array.isArray(formData.value.faultDescription)
      ? formData.value.faultDescription
      : [formData.value.faultDescription]
    return selectedValues.map((x) => String(x ?? '').trim()).filter(Boolean)
  }

  /**
   * 构建佳士报修 payload
   * @returns payload
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
    const shippingSubmitFields = resolveShippingSubmitFields(
      selectedShippingAddress.value,
      formData.value.shippingInfo
    )

    const base: CreateCustomerWorkOrderDTO = {
      barcode: barcodeFromApi || barcodeTrim,
      brandCode: JASIC_BRAND_CODE,
      customerName: String(userStore.userInfo?.nickname ?? ''),
      faultDesc: resolveFaultDescForSubmit(),
      faultItems: resolveFaultItemsForSubmit(),
      faultRemark: formData.value.faultRemark,
      productCode: api?.productCode != null ? String(api.productCode) : '',
      productModel: api?.productModel != null ? String(api.productModel) : '',
      brandType,
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

    try {
      // createCustomerWorkOrder 是 POST，http.ts 已自动管理 loading
      const res = await createCustomerWorkOrder(buildJasicWorkOrderPayload())
      /**
 * 关单后服务端会推「客户满意度评价通知」，需在创建工单时完成订阅授权
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      await requestEvaluationInviteSubscribe()
      if (options.redirect) {
        clearJasicRepairDraft()
        // 等 toast 1500ms 阻塞期结束再跳转，确保用户能看完成功提示
        await showApiToast(res.msg || '提交成功')
        uni.redirectTo({ url: `/pages/order/list` })
      } else {
        void showApiToast(res.msg || '提交成功')
        saveJasicRepairDraft(buildJasicRepairDraftSnapshot())
      }
    } catch (err: unknown) {
      void showApiToast(parseUnknownError(err, '提交失败'))
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const stashForm = () => {
    // 暂存只保存本地草稿：不校验、不提交接口
    showRequestLoading('暂存中...')
    try {
      saveJasicRepairDraft(buildJasicRepairDraftSnapshot())
      void showApiToast('已暂存')
    } catch (err: unknown) {
      void showApiToast(parseUnknownError(err, '暂存失败'))
    } finally {
      hideRequestLoading()
    }
  }

  /**
   * 重置表单
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const resetForm = () => {
    // 显示保修提示
    showWarrantyModal.value = false
    lastBarcodeInfo.value = null
    barcodeQueryHasFaultDescription.value = false
    faultDescriptionOptionsFromApi.value = []
    queryFailedWithBarcode.value = false
    lastAutoQueriedBarcode.value = ''
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
      faultDescription: [],
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
