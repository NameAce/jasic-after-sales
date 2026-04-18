<template>
  <view class="page-index page-padding">
    <!-- 报修入口：代客户填写需填客户手机；报修一级/报修佳士无需手机 -->
    <view class="repair-entry-tabs-wrap">
      <view class="repair-entry-tabs">
        <view
          :class="['tab-item', repairEntryTab === 'proxy' && 'active']"
          @click="setRepairEntryTab('proxy')"
        >
          <text class="text">代客户填写</text>
        </view>
        <view
          :class="['tab-item', repairEntryTab === 'upstream' && 'active']"
          @click="setRepairEntryTab('upstream')"
        >
          <text class="text">{{ upstreamTabLabel }}</text>
        </view>
      </view>
    </view>

    <!-- 商品查询 -->
    <view class="card card-shadow">
      <view class="card-header">
        <view class="icon-box">
          <uni-icons type="vip-filled" size="24" :color="themeColors.primary"></uni-icons>
        </view>
        <view class="header-text">
          <view>商品查询</view>
          <text>请输入或扫描产品条形码查询状态</text>
        </view>
      </view>
      <view class="search-box">
        <view>
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

    <!-- 表单区域 -->
    <uni-forms
      ref="formRef"
      :model-value="formData"
      :rules="formRules"
      label-position="top"
      label-width="auto"
    >
      <!-- 必填信息 -->
      <view>
        <view class="section-header">
          <view>必填信息</view>
          <text class="required-badge">REQUIRED</text>
        </view>

        <view class="card card-shadow form-padding">
          <!-- 客户手机（仅「代客户填写」必填） -->
          <uni-forms-item
            v-if="showContactMobileField"
            label="客户手机号码"
            name="contactMobile"
            required
          >
            <uni-easyinput
              v-model="formData.contactMobile"
              :maxlength="11"
              placeholder="请输入客户手机号码"
            />
          </uni-forms-item>

          <!-- 故障描述：仅当条码查询接口返回非空 faultOptions 时展示（下拉多选） -->
          <uni-forms-item
            v-if="barcodeQueryHasFaultDescription"
            label="故障描述"
            name="faultDescription"
            required
          >
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

          <!-- 故障说明备注：无故障下拉时整段必填；有下拉时仅「其它/其他故障」类选项必填 -->
          <uni-forms-item v-if="showFaultRemark" label="故障说明备注" name="faultRemark" required>
            <uni-easyinput
              v-model="formData.faultRemark"
              type="textarea"
              auto-height
              placeholder="请输入故障说明备注"
            />
          </uni-forms-item>

          <!-- 选择维修路径 -->
          <uni-forms-item label="选择维修路径" name="repairType" required>
            <RepairTypeSelector v-model="formData.repairType" :options="repairTypes" />
          </uni-forms-item>

          <!-- 寄件信息（仅佳士品牌且邮寄；交互对齐售后端） -->
          <uni-forms-item v-if="showShippingInfo" label="寄件信息" name="shippingInfo" required>
            <view class="shipping-address-btn" @click="chooseShippingAddress">
              <text :class="['shipping-address-text', { placeholder: !formData.shippingInfo }]">{{
                shippingInfoDisplay
              }}</text>
              <uni-icons type="right" size="14" :color="themeColors.textMuted" />
            </view>
          </uni-forms-item>
        </view>
      </view>

      <!-- 补充说明 -->
      <view class="section">
        <view class="section-header collapsible" @click="toggleSupplementSection">
          <view>补充说明</view>
          <view class="collapse-toggle">
            <text class="optional-badge">OPTIONAL</text>
            <text class="toggle-text">{{ showSupplementSection ? '收缩' : '展开' }}</text>
          </view>
        </view>

        <view v-show="showSupplementSection" class="card card-shadow form-padding">
          <VoiceInputField v-model="formData.voiceList" />

          <!-- 故障视频/图片 -->
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

          <!-- 寄件快递图片 -->
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

  <!-- 底部按钮 -->
  <base-button>
    <view class="btn btn-secondary" @click="handleSaveDraft">暂存</view>
    <view class="btn btn-secondary" @click="handleResetForm">重置</view>
    <view
      v-if="userStore.hasPermission(Perms.WORKORDER_ADD)"
      class="btn btn-primary"
      @click="handleSubmitClick"
    >
      提交
    </view>
  </base-button>

  <!-- 报修提示弹窗 -->
  <view v-if="showWarrantyModal" class="modal-mask" @click="onWarrantyModalBackdropClick">
    <view class="modal-content" @click.stop>
      <view class="modal-body">
        <view class="modal-icon-box">
          <uni-icons type="info-filled" size="40" :color="themeColors.primary"></uni-icons>
        </view>
        <view class="modal-title">报修提示</view>
        <view class="modal-desc">无条码或无法识别条码，系统默认该机器已过保</view>
        <view class="modal-actions">
          <button class="btn-cancel" @click="onWarrantyModalBackdropClick">取消</button>
          <button
            v-if="userStore.hasPermission(Perms.WORKORDER_ADD)"
            class="btn-confirm"
            @click="confirmSubmit"
          >
            确认提交
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  /**
   * 建维修订单：提交/确认提交需 ORDER_CREATE（按钮级）；主体类型仅影响文案/表单项展示。
   */
  import { ref, computed, watch, nextTick, onMounted, onActivated } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { themeColors } from '@/theme/colors'
  import { useAppStore } from '@/stores'
  import { useUserStore } from '@/stores/modules/user'
  import { Perms } from '@/utils/permissions'
  import { REPAIR_TYPE_OPTIONS } from '@/constants/repairForm'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import RepairTypeSelector from '@/components/RepairTypeSelector/RepairTypeSelector.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import VoiceInputField from '@/components/VoiceInputField/VoiceInputField.vue'
  import {
    createProxyWorkOrder,
    createUpstreamFirstWorkOrder,
    createUpstreamHqWorkOrder,
    fetchProxyBarcodeInfo,
    fetchUpstreamFirstBarcodeInfo,
    type WorkOrderCreateBarcodeInfoVO,
    type WorkOrderProxyCreateDTO,
    type WorkOrderUpstreamCreateDTO
  } from '@/api/order'
  import { getApiMessage } from '@/utils/http'
  import { MOBILE_PATTERN } from '@/utils/validation'
  import { validateFaultMediaSelection } from '@/utils/repairMediaLimits'
  import {
    asUnknownArray,
    collectVoucherFileIds,
    collectVoiceFileIds,
    hasUnuploadedMediaItems,
    partitionFaultMediaFileIds
  } from '@/utils/workOrderFileIds'
  import { takeSelectedShippingAddress, type SelectedShippingAddress } from '@/utils/addressStorage'
  import {
    resolveSendExpressNoForSubmit,
    resolveShippingSubmitFields
  } from '@/utils/shippingSubmitFields'
  import { toastIfMediaUploading } from '@/utils/mediaUploadLock'

  // 用户商店
  const appStore = useAppStore()
  const userStore = useUserStore()

  /**
   * 一级经销商标签
   * @returns 一级经销商标签
   */
  const upstreamTabLabel = computed(() => (userStore.isPrimaryDealer ? '报修佳士' : '报修一级'))

  // 维修入口标签
  const repairEntryTab = ref<'proxy' | 'upstream'>('proxy')
  // 是否显示客户手机号码
  const showContactMobileField = computed(() => repairEntryTab.value === 'proxy')

  // 表单引用
  const formRef = ref(null)

  // 表单类型
  const formType = 'jasic'
  /** 旧版单一草稿键（仅迁移后删除） */
  const DRAFT_KEY_LEGACY = 'jasicRepairDraft'
  const DRAFT_KEY_PROXY = 'jasicRepairDraft_proxy'
  const DRAFT_KEY_UPSTREAM = 'jasicRepairDraft_upstream'
  /** 上次停留的 tab，用于两个 tab 均有暂存时决定首屏 */
  const DRAFT_LAST_TAB_KEY = 'jasicRepairDraft_lastTab'

  const draftStorageKey = (tab: 'proxy' | 'upstream') =>
    tab === 'upstream' ? DRAFT_KEY_UPSTREAM : DRAFT_KEY_PROXY
  // 创建初始表单数据
  const createInitialFormData = () => ({
    // 保修码
    warrantyCode: '',
    // 客户手机（仅代客户填写）
    contactMobile: '',
    // 客户姓名（仅代客户填写）
    customerName: '',
    // 维修路径（STORE=送店 / MAIL=邮寄，与售后端一致）
    repairType: 'STORE' as 'STORE' | 'MAIL',
    // 故障描述
    faultDescription: [] as string[],
    // 故障视频/图片
    images: [],
    // 寄件快递单号
    shippingCode: [] as unknown[],
    // 故障说明备注
    faultRemark: '',
    // 寄件信息
    shippingInfo: '',
    // 语音列表（tempFilePath + duration 毫秒）
    voiceList: [] as { tempFilePath: string; duration: number }[],
    /** 一级报修佳士：目标受理公司 ID（uni-data-select 用字符串） */
    targetCompanyId: ''
  })

  const normalizeRepairType = (r: unknown): 'STORE' | 'MAIL' => {
    if (r === 'MAIL' || r === 'mail') return 'MAIL'
    return 'STORE'
  }

  type FormRef = { clearValidate?: (names?: string[]) => void }
  const getFormRef = () => formRef.value as FormRef | null

  // 状态
  const formData = ref(createInitialFormData())

  /** 最近一次条码查询返回（提交时 faultItems 等可兜底） */
  const lastBarcodeInfo = ref<WorkOrderCreateBarcodeInfoVO | null>(null)
  /** 仅当查询返回非空 faultOptions 时展示故障描述下拉 */
  const barcodeQueryHasFaultDescription = ref(false)
  const faultDescriptionOptionsFromApi = ref<{ text: string; value: string }[]>([])
  /** 有条码但查询失败时仍须展示并必填故障说明备注 */
  const queryFailedWithBarcode = ref(false)
  /** 初始无条码时与售后端一致：展示故障说明备注 */
  const showFaultRemark = ref(true)
  const selectedShippingAddress = ref<SelectedShippingAddress | null>(null)

  const shippingInfoDisplay = computed(() => formData.value.shippingInfo || '请选择寄件信息')

  // 是否显示寄件信息（仅佳士品牌且选择邮寄维修）
  const showShippingInfo = computed(
    () => formType === 'jasic' && formData.value.repairType === 'MAIL'
  )

  const OTHER_FAULT_VALUE = 'other'

  /**
   * 将条码查询返回的 targetCompanyOptions 映射为 uni-data-select 结构。
   */
  const mapTargetCompanyOptionsToSelect = (raw: unknown) => {
    if (!Array.isArray(raw)) return []
    return raw
      .map((item) => {
        const o = item as { id?: number; companyName?: string; companyCode?: string }
        const id = Number(o?.id)
        if (!Number.isFinite(id) || id <= 0) return null
        const name = String(o?.companyName || o?.companyCode || '').trim() || `公司#${id}`
        return { text: name, value: String(id) }
      })
      .filter((x): x is { text: string; value: string } => x != null)
  }

  /**
   * 一级报修佳士：根据条码查询结果同步目标受理公司（多选时保留用户已选且仍合法）。
   */
  const syncUpstreamTargetCompanyFromBarcodeInfo = (
    info: WorkOrderCreateBarcodeInfoVO | null | undefined
  ) => {
    if (repairEntryTab.value !== 'upstream') {
      formData.value.targetCompanyId = ''
      return
    }
    if (!userStore.isPrimaryDealer) {
      formData.value.targetCompanyId = ''
      return
    }
    const opts = mapTargetCompanyOptionsToSelect(info?.targetCompanyOptions)
    const defId = Number(info?.defaultTargetCompanyId)
    const hqId = Number(info?.hqCompanyId)
    const cur = Number(formData.value.targetCompanyId)
    const curInOpts = opts.some((o) => Number(o.value) === cur)

    if (opts.length > 1) {
      if (!Number.isFinite(cur) || cur <= 0 || !curInOpts) {
        const defOk =
          Number.isFinite(defId) && defId > 0 && opts.some((o) => Number(o.value) === defId)
        formData.value.targetCompanyId = defOk ? String(defId) : ''
      }
    } else if (opts.length === 1) {
      formData.value.targetCompanyId = opts[0].value
    } else {
      const fallback = Number.isFinite(defId) && defId > 0 ? defId : hqId
      formData.value.targetCompanyId =
        Number.isFinite(fallback) && fallback > 0 ? String(fallback) : ''
    }
  }

  /**
   * 将后端故障选项映射为 uni-data-select 所需结构。
   * - 其它故障统一归一化为 value=other；
   * - 其余选项使用自身文本作为 value。
   */
  const mapFaultOptionsToSelect = (faultOptions: unknown, otherFaultLabel?: string) => {
    if (!Array.isArray(faultOptions)) return []
    const otherLabel = String(otherFaultLabel || '').trim()
    const list = faultOptions
      .map((item) => {
        if (typeof item === 'string') return item.trim()
        if (item && typeof item === 'object') {
          const o = item as { label?: unknown; text?: unknown; value?: unknown; name?: unknown }
          return String(o.label ?? o.text ?? o.value ?? o.name ?? '').trim()
        }
        return String(item ?? '').trim()
      })
      .filter(Boolean)
      .map((label) => {
        const isOther =
          label === otherLabel ||
          label.includes('其它故障') ||
          label.includes('其他故障') ||
          label.includes('其它') ||
          label.includes('其他')
        return {
          text: label,
          value: isOther ? OTHER_FAULT_VALUE : label
        }
      })
    const dedup = new Map<string, { text: string; value: string }>()
    list.forEach((item) => {
      if (!dedup.has(item.value)) dedup.set(item.value, item)
    })
    return Array.from(dedup.values())
  }

  /** 当前条码是否已完成一次商品查询（含失败/无结果，便于校验「先查询」） */
  const warrantyQueried = ref(false)

  const normalizeFaultDescriptionValue = (val: unknown): string[] => {
    if (Array.isArray(val)) return val.map((x) => String(x)).filter(Boolean)
    const single = String(val ?? '').trim()
    return single ? [single] : []
  }

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
      showFaultRemark.value = normalizeFaultDescriptionValue(formData.value.faultDescription).some((v) =>
        isOtherFaultSelection(v)
      )
      return
    }
    if (lastBarcodeInfo.value || queryFailedWithBarcode.value) {
      showFaultRemark.value = true
      return
    }
    showFaultRemark.value = false
  }

  // 校验规则（「报修一级/报修佳士」不包含手机号校验）
  const formRules = computed(() => {
    const base: Record<
      string,
      { rules: { required?: boolean; pattern?: RegExp; errorMessage: string }[] }
    > = {
      repairType: {
        rules: [{ required: true, errorMessage: '请选择维修路径' }]
      }
    }
    if (barcodeQueryHasFaultDescription.value) {
      base.faultDescription = {
        rules: [{ required: true, errorMessage: '请填写故障描述' }]
      }
    }
    if (showFaultRemark.value) {
      base.faultRemark = {
        rules: [{ required: true, errorMessage: '请填写故障说明备注' }]
      }
    }
    if (repairEntryTab.value === 'proxy') {
      base.contactMobile = {
        rules: [
          { required: true, errorMessage: '请输入客户手机号码' },
          { pattern: MOBILE_PATTERN, errorMessage: '请输入正确的手机号码' }
        ]
      }
    }
    if (formType === 'jasic' && formData.value.repairType === 'MAIL') {
      base.shippingInfo = {
        rules: [{ required: true, errorMessage: '请填写寄件信息' }]
      }
    }
    return base
  })

  // 补充说明折叠状态（默认收起）
  const showSupplementSection = ref(false)

  // 报修提示弹窗显示状态
  const showWarrantyModal = ref(false)

  // 提交中（避免重复点击）
  const submitting = ref(false)

  // 用于避免“回显草稿”时触发 warrantyCode 监听器清空数据
  const isRestoringDraft = ref(false)
  /** 切换入口 tab 恢复快照时避免 warrantyCode 监听误清空 */
  const isRestoringTabSnapshot = ref(false)

  type RepairEntryTabSnapshot = {
    formData: ReturnType<typeof createInitialFormData>
    warrantyQueried: boolean
    lastBarcodeInfo: WorkOrderCreateBarcodeInfoVO | null
    barcodeQueryHasFaultDescription: boolean
    faultDescriptionOptionsFromApi: { text: string; value: string }[]
    queryFailedWithBarcode: boolean
    showFaultRemark: boolean
    selectedShippingAddress: SelectedShippingAddress | null
    showSupplementSection: boolean
  }

  const tabFormSnapshots = ref<Partial<Record<'proxy' | 'upstream', RepairEntryTabSnapshot>>>({})

  /**
   * 将本地存储的一条 tab 草稿解析为切换 tab 用的快照结构。
   */
  const parseTabDraftToSnapshot = (raw: unknown): RepairEntryTabSnapshot | null => {
    if (!raw || typeof raw !== 'object') return null
    const d = raw as {
      formType?: string
      warrantyQueried?: boolean
      barcodeQueryHasFaultDescription?: boolean
      faultDescriptionOptionsFromApi?: { text: string; value: string }[]
      productFaultDescriptionOptions?: { text: string; value: string }[] | null
      lastBarcodeInfo?: WorkOrderCreateBarcodeInfoVO | null
      queryFailedWithBarcode?: boolean
      formData?: Partial<ReturnType<typeof createInitialFormData>>
      showFaultRemark?: boolean
      selectedShippingAddress?: SelectedShippingAddress | null
      showSupplementSection?: boolean
    }
    if (d.formType !== formType) return null

    let faultOpts: { text: string; value: string }[] = []
    let hasFd = false
    if (Array.isArray(d.faultDescriptionOptionsFromApi)) {
      faultOpts = d.faultDescriptionOptionsFromApi
      hasFd = !!d.barcodeQueryHasFaultDescription
    } else if (Array.isArray(d.productFaultDescriptionOptions)) {
      faultOpts = d.productFaultDescriptionOptions
      hasFd = d.productFaultDescriptionOptions.length > 0
    }

    const formMerged = {
      ...createInitialFormData(),
      ...(d.formData || {})
    }
    formMerged.repairType = normalizeRepairType(formMerged.repairType)
    formMerged.faultDescription = normalizeFaultDescriptionValue(formMerged.faultDescription)

    return {
      formData: JSON.parse(JSON.stringify(formMerged)) as ReturnType<typeof createInitialFormData>,
      warrantyQueried: !!d.warrantyQueried,
      lastBarcodeInfo:
        d.lastBarcodeInfo && typeof d.lastBarcodeInfo === 'object'
          ? (JSON.parse(JSON.stringify(d.lastBarcodeInfo)) as WorkOrderCreateBarcodeInfoVO)
          : null,
      barcodeQueryHasFaultDescription: hasFd,
      faultDescriptionOptionsFromApi: [...faultOpts],
      queryFailedWithBarcode: !!d.queryFailedWithBarcode,
      showFaultRemark: typeof d.showFaultRemark === 'boolean' ? d.showFaultRemark : true,
      selectedShippingAddress: d.selectedShippingAddress
        ? ({ ...d.selectedShippingAddress } as SelectedShippingAddress)
        : null,
      showSupplementSection: !!d.showSupplementSection
    }
  }

  const repairTypes = REPAIR_TYPE_OPTIONS

  watch(
    () => formData.value.repairType,
    (val) => {
      if (val === 'STORE') {
        formData.value.shippingInfo = ''
        selectedShippingAddress.value = null
        nextTick(() => getFormRef()?.clearValidate?.(['shippingInfo']))
      }
    }
  )

  /**
   * 扫描条形码
   * @returns void
   */
  const handleScan = () => {
    if (toastIfMediaUploading()) return
    uni.scanCode({
      success: (res) => {
        formData.value.warrantyCode = res.result
      }
    })
  }

  /**
   * 展开/收缩补充说明
   * @returns void
   */
  const toggleSupplementSection = () => {
    if (toastIfMediaUploading()) return
    showSupplementSection.value = !showSupplementSection.value
  }

  /**
   * 查询条码信息（按入口区分：代客户填写 / 报修一级）
   * @param options.silentToast - 进入页自动查询时不弹成功提示
   * @param options.skipClearFaultFieldsWhenNoOptions - 自动查询且接口无故障下拉时不清空已填备注（与暂存恢复配合）
   */
  const checkWarranty = async (options?: {
    silentToast?: boolean
    skipClearFaultFieldsWhenNoOptions?: boolean
    /** 进入页自动查询且有条码故障下拉时保留已填故障项（与暂存恢复一致） */
    preserveFaultFieldsWhenHasOptions?: boolean
  }) => {
    const silentToast = options?.silentToast ?? false
    const skipClearFaultFieldsWhenNoOptions = options?.skipClearFaultFieldsWhenNoOptions ?? false
    const preserveFaultFieldsWhenHasOptions = options?.preserveFaultFieldsWhenHasOptions ?? false
    if (!silentToast && toastIfMediaUploading()) return
    if (!formData.value.warrantyCode) {
      return uni.showToast({ title: '请输入条形码', icon: 'none', duration: 1500 })
    }
    uni.showLoading({ title: '查询中...' })
    const upstreamTid = Number(formData.value.targetCompanyId)
    const request =
      repairEntryTab.value === 'upstream'
        ? fetchUpstreamFirstBarcodeInfo(
            formData.value.warrantyCode,
            userStore.isPrimaryDealer && Number.isFinite(upstreamTid) && upstreamTid > 0
              ? upstreamTid
              : undefined
          )
        : fetchProxyBarcodeInfo(formData.value.warrantyCode)

    try {
      const { data: info, msg } = await request
      uni.hideLoading()
      // 故障描述下拉严格使用条码查询返回的 faultOptions
      const list = mapFaultOptionsToSelect(info?.faultOptions, info?.otherFaultLabel)
      faultDescriptionOptionsFromApi.value = list
      const hasFd = list.length > 0
      barcodeQueryHasFaultDescription.value = hasFd
      queryFailedWithBarcode.value = false
      lastBarcodeInfo.value = info && typeof info === 'object' ? info : null
      warrantyQueried.value = true
      syncUpstreamTargetCompanyFromBarcodeInfo(info)

      if (!hasFd) {
        if (!skipClearFaultFieldsWhenNoOptions) {
          formData.value.faultDescription = []
          formData.value.faultRemark = ''
          nextTick(() => {
            getFormRef()?.clearValidate?.(['faultDescription', 'faultRemark', 'targetCompanyId'])
            syncShowFaultRemarkFromState()
          })
        } else {
          nextTick(() => syncShowFaultRemarkFromState())
        }
      } else {
        if (!preserveFaultFieldsWhenHasOptions) {
          formData.value.faultDescription = []
          formData.value.faultRemark = ''
        }
        nextTick(() => {
          getFormRef()?.clearValidate?.(['faultDescription', 'faultRemark', 'targetCompanyId'])
          syncShowFaultRemarkFromState()
        })
      }

      if (!silentToast) {
        const title = msg.trim() || info?.warrantyStatus || '查询成功'
        uni.showToast({ title, icon: 'success', duration: 1500 })
      }
    } catch {
      uni.hideLoading()
      faultDescriptionOptionsFromApi.value = []
      barcodeQueryHasFaultDescription.value = false
      lastBarcodeInfo.value = null
      queryFailedWithBarcode.value = !!String(formData.value.warrantyCode ?? '').trim()
      warrantyQueried.value = true
      if (userStore.isPrimaryDealer && repairEntryTab.value === 'upstream') {
        formData.value.targetCompanyId = ''
      }
      nextTick(() => syncShowFaultRemarkFromState())
    }
  }

  /**
   * 进入页面且条码已有值时自动查询一次（恢复故障描述下拉与 lastBarcodeInfo）
   */
  const tryAutoQueryBarcodeOnEnter = () => {
    const code = String(formData.value.warrantyCode ?? '').trim()
    if (!code) return
    void checkWarranty({
      silentToast: true,
      skipClearFaultFieldsWhenNoOptions: true,
      preserveFaultFieldsWhenHasOptions: true
    })
  }

  const captureRepairEntryTabSnapshot = (): RepairEntryTabSnapshot => ({
    formData: JSON.parse(JSON.stringify(formData.value)) as ReturnType<
      typeof createInitialFormData
    >,
    warrantyQueried: warrantyQueried.value,
    lastBarcodeInfo: lastBarcodeInfo.value
      ? (JSON.parse(JSON.stringify(lastBarcodeInfo.value)) as WorkOrderCreateBarcodeInfoVO)
      : null,
    barcodeQueryHasFaultDescription: barcodeQueryHasFaultDescription.value,
    faultDescriptionOptionsFromApi: [...faultDescriptionOptionsFromApi.value],
    queryFailedWithBarcode: queryFailedWithBarcode.value,
    showFaultRemark: showFaultRemark.value,
    selectedShippingAddress: selectedShippingAddress.value
      ? ({ ...selectedShippingAddress.value } as SelectedShippingAddress)
      : null,
    showSupplementSection: showSupplementSection.value
  })

  const applyRepairEntryTabSnapshot = (snap: RepairEntryTabSnapshot | null) => {
    isRestoringTabSnapshot.value = true
    try {
      if (!snap) {
        formData.value = createInitialFormData()
        warrantyQueried.value = false
        lastBarcodeInfo.value = null
        barcodeQueryHasFaultDescription.value = false
        faultDescriptionOptionsFromApi.value = []
        queryFailedWithBarcode.value = false
        showFaultRemark.value = true
        selectedShippingAddress.value = null
        showSupplementSection.value = false
        showWarrantyModal.value = false
      } else {
        formData.value = {
          ...createInitialFormData(),
          ...snap.formData
        }
        formData.value.repairType = normalizeRepairType(formData.value.repairType)
        warrantyQueried.value = snap.warrantyQueried
        lastBarcodeInfo.value = snap.lastBarcodeInfo
          ? (JSON.parse(JSON.stringify(snap.lastBarcodeInfo)) as WorkOrderCreateBarcodeInfoVO)
          : null
        barcodeQueryHasFaultDescription.value = snap.barcodeQueryHasFaultDescription
        faultDescriptionOptionsFromApi.value = [...snap.faultDescriptionOptionsFromApi]
        queryFailedWithBarcode.value = snap.queryFailedWithBarcode
        showFaultRemark.value = snap.showFaultRemark
        selectedShippingAddress.value = snap.selectedShippingAddress
          ? ({ ...snap.selectedShippingAddress } as SelectedShippingAddress)
          : null
        showSupplementSection.value = snap.showSupplementSection
        showWarrantyModal.value = false
      }
    } finally {
      nextTick(() => {
        isRestoringTabSnapshot.value = false
        getFormRef()?.clearValidate?.([
          'contactMobile',
          'repairType',
          'faultDescription',
          'faultRemark',
          'shippingInfo',
          'targetCompanyId'
        ])
        syncShowFaultRemarkFromState()
        if (String(formData.value.warrantyCode ?? '').trim()) {
          tryAutoQueryBarcodeOnEnter()
        }
      })
    }
  }

  /**
   * 切换报修入口：离开前保存当前 tab 快照；进入目标 tab 时恢复快照或空表。
   */
  const setRepairEntryTab = (tab: 'proxy' | 'upstream') => {
    if (toastIfMediaUploading()) return
    if (tab === repairEntryTab.value) return
    const from = repairEntryTab.value
    tabFormSnapshots.value = {
      ...tabFormSnapshots.value,
      [from]: captureRepairEntryTabSnapshot()
    }
    repairEntryTab.value = tab
    applyRepairEntryTabSnapshot(tabFormSnapshots.value[tab] ?? null)
  }

  const showFaultDescDropdown = ref(false)
  const draftFaultDesc = ref<string[]>([])

  const selectedFaultDescText = computed(() => {
    const vals = normalizeFaultDescriptionValue(formData.value.faultDescription)
    if (vals.length === 0) return ''
    return vals
      .map((v) => {
        const opt = faultDescriptionOptionsFromApi.value.find((o) => o.value === v)
        return opt?.text || v
      })
      .join('、')
  })

  const openFaultDescDropdown = () => {
    if (toastIfMediaUploading()) return
    draftFaultDesc.value = [...normalizeFaultDescriptionValue(formData.value.faultDescription)]
    showFaultDescDropdown.value = true
  }

  const toggleDraftFaultDesc = (value: string) => {
    if (draftFaultDesc.value.includes(value)) {
      draftFaultDesc.value = draftFaultDesc.value.filter((x) => x !== value)
    } else {
      draftFaultDesc.value = [...draftFaultDesc.value, value]
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
    const nextShow = formData.value.faultDescription.some((v) => isOtherFaultSelection(v))
    showFaultRemark.value = nextShow
    if (!nextShow) {
      formData.value.faultRemark = ''
      nextTick(() => getFormRef()?.clearValidate?.(['faultRemark']))
    }
  }

  watch(
    () => formData.value.warrantyCode,
    (val, oldVal) => {
      if (isRestoringDraft.value || isRestoringTabSnapshot.value) return
      if (val !== oldVal) {
        lastBarcodeInfo.value = null
        barcodeQueryHasFaultDescription.value = false
        faultDescriptionOptionsFromApi.value = []
        queryFailedWithBarcode.value = false
        warrantyQueried.value = false
      }
      if (!val) {
        formData.value.faultDescription = []
        formData.value.targetCompanyId = ''
        showFaultRemark.value = true
      } else {
        nextTick(() => syncShowFaultRemarkFromState())
      }
    }
  )

  type FaultMediaPickEvent = { tempFiles: { fileType?: string }[] }

  const onFaultMediaChange = (e: FaultMediaPickEvent) => {
    validateFaultMediaSelection(e.tempFiles)
  }

  /** 进入「我的地址」选择寄件地址（selectShipping），返回后由 onShow 中 applyShippingPickFromStorage 回填 */
  const chooseShippingAddress = () => {
    if (toastIfMediaUploading()) return
    uni.navigateTo({ url: '/pages/address/index?mode=selectShipping' })
  }

  const getFaultDescriptionTextsForSubmit = () => {
    if (!barcodeQueryHasFaultDescription.value) return [] as string[]
    const selectedValues = normalizeFaultDescriptionValue(formData.value.faultDescription)
    return selectedValues
      .map((selectedValue) => {
        const opt = faultDescriptionOptionsFromApi.value.find((o) => o.value === selectedValue)
        if (isOtherFaultSelection(selectedValue)) {
          return opt?.text || '其它故障'
        }
        return opt?.text || selectedValue || ''
      })
      .filter((x) => String(x).trim().length > 0)
  }

  const resolveFaultItemsForSubmit = (): string[] => {
    const selectedValues = normalizeFaultDescriptionValue(formData.value.faultDescription)
    if (barcodeQueryHasFaultDescription.value && selectedValues.length > 0) {
      const texts = getFaultDescriptionTextsForSubmit()
      return texts.length > 0 ? texts : selectedValues
    }
    const raw = lastBarcodeInfo.value?.faultOptions
    if (Array.isArray(raw) && raw.length > 0) {
      return raw.map((x) => String(x)).filter((s) => s.length > 0)
    }
    return []
  }

  const buildMediaShippingPayload = (customerMobile: string, customerName: string) => {
    const faultMedia = partitionFaultMediaFileIds(asUnknownArray(formData.value.images))
    const senderVoucherFileIds = collectVoucherFileIds(asUnknownArray(formData.value.shippingCode))
    const faultVoiceFileIds = collectVoiceFileIds(asUnknownArray(formData.value.voiceList))
    const isMail = formData.value.repairType === 'MAIL'
    const shippingSubmitFields = resolveShippingSubmitFields(
      selectedShippingAddress.value,
      formData.value.shippingInfo,
      { fallbackName: customerName, fallbackMobile: customerMobile }
    )
    return {
      faultImageFileIds: faultMedia.faultImageFileIds,
      faultVideoFileIds: faultMedia.faultVideoFileIds,
      faultVoiceFileIds,
      senderVoucherFileIds,
      sendExpressNo: isMail ? resolveSendExpressNoForSubmit(formData.value.shippingCode) : '',
      senderAddress: isMail ? shippingSubmitFields.senderAddress : '',
      senderMobile: isMail ? shippingSubmitFields.senderMobile : customerMobile,
      senderName: isMail ? shippingSubmitFields.senderName : customerName,
      serviceMode: (isMail ? 'MAIL' : 'STORE') as 'MAIL' | 'STORE'
    }
  }

  /**
   * 构建「代客户填写」创建工单 DTO
   */
  const buildProxyCreateDto = (): WorkOrderProxyCreateDTO => {
    const api = lastBarcodeInfo.value
    const barcodeTrim = String(formData.value.warrantyCode || '').trim()
    const barcodeFromApi = api?.barcode != null ? String(api.barcode).trim() : ''
    const customerMobile = String(formData.value.contactMobile || '').trim()
    const customerName = String(formData.value.customerName || '').trim()
    const faultRemark = String(formData.value.faultRemark || '').trim()
    const m = buildMediaShippingPayload(customerMobile, customerName)

    return {
      barcode: barcodeFromApi || barcodeTrim,
      customerMobile,
      customerName,
      faultImageFileIds: m.faultImageFileIds,
      faultItems: resolveFaultItemsForSubmit(),
      faultRemark,
      faultVideoFileIds: m.faultVideoFileIds,
      faultVoiceFileIds: m.faultVoiceFileIds,
      sendExpressNo: m.sendExpressNo,
      senderAddress: m.senderAddress,
      senderMobile: m.senderMobile,
      senderName: m.senderName,
      senderVoucherFileIds: m.senderVoucherFileIds,
      serviceMode: m.serviceMode
    }
  }

  /**
   * 构建「二级报修一级 / 报修佳士」创建工单 DTO
   */
  const buildUpstreamFirstCreateDto = (): WorkOrderUpstreamCreateDTO => {
    const api = lastBarcodeInfo.value
    const barcodeTrim = String(formData.value.warrantyCode || '').trim()
    const barcodeFromApi = api?.barcode != null ? String(api.barcode).trim() : ''
    const customerMobile = String(formData.value.contactMobile || '').trim()
    const customerName = String(formData.value.customerName || '').trim()
    const faultRemark = String(formData.value.faultRemark || '').trim()
    const m = buildMediaShippingPayload(customerMobile, customerName)

    const dto: WorkOrderUpstreamCreateDTO = {
      barcode: barcodeFromApi || barcodeTrim,
      customerMobile,
      customerName,
      faultImageFileIds: m.faultImageFileIds,
      faultItems: resolveFaultItemsForSubmit(),
      faultRemark,
      faultVideoFileIds: m.faultVideoFileIds,
      faultVoiceFileIds: m.faultVoiceFileIds,
      sendExpressNo: m.sendExpressNo,
      senderAddress: m.senderAddress,
      senderMobile: m.senderMobile,
      senderName: m.senderName,
      senderVoucherFileIds: m.senderVoucherFileIds,
      serviceMode: m.serviceMode
    }
    const hqTid = Number(formData.value.targetCompanyId)
    if (userStore.isPrimaryDealer && Number.isFinite(hqTid) && hqTid > 0) {
      dto.targetCompanyId = hqTid
    }
    return dto
  }

  /**
   * 重置表单状态
   * @returns void
   */
  const resetFormState = () => {
    repairEntryTab.value = 'proxy'
    tabFormSnapshots.value = {}
    formData.value = createInitialFormData()
    warrantyQueried.value = false
    lastBarcodeInfo.value = null
    barcodeQueryHasFaultDescription.value = false
    faultDescriptionOptionsFromApi.value = []
    queryFailedWithBarcode.value = false
    showFaultRemark.value = true
    selectedShippingAddress.value = null
    showSupplementSection.value = false
    showWarrantyModal.value = false

    nextTick(() => {
      getFormRef()?.clearValidate?.([
        'contactMobile',
        'repairType',
        'faultDescription',
        'faultRemark',
        'shippingInfo',
        'targetCompanyId'
      ])
    })
  }

  const buildTabDraftPayload = () => ({
    formType,
    savedAt: Date.now(),
    warrantyQueried: warrantyQueried.value,
    barcodeQueryHasFaultDescription: barcodeQueryHasFaultDescription.value,
    faultDescriptionOptionsFromApi: [...faultDescriptionOptionsFromApi.value],
    lastBarcodeInfo: lastBarcodeInfo.value
      ? (JSON.parse(JSON.stringify(lastBarcodeInfo.value)) as WorkOrderCreateBarcodeInfoVO)
      : null,
    queryFailedWithBarcode: queryFailedWithBarcode.value,
    showFaultRemark: showFaultRemark.value,
    selectedShippingAddress: selectedShippingAddress.value
      ? ({ ...selectedShippingAddress.value } as SelectedShippingAddress)
      : null,
    showSupplementSection: showSupplementSection.value,
    formData: formData.value
  })

  /**
   * 保存草稿（按当前 tab 分键存储，互不覆盖）
   * @returns void
   */
  const handleSaveDraft = () => {
    if (toastIfMediaUploading()) return
    try {
      const tab = repairEntryTab.value
      uni.setStorageSync(draftStorageKey(tab), buildTabDraftPayload())
      uni.setStorageSync(DRAFT_LAST_TAB_KEY, tab)
      uni.showToast({ title: '暂存成功', icon: 'success', duration: 1500 })
    } catch (_e) {
      void _e
      uni.showToast({ title: '暂存失败', icon: 'none', duration: 1500 })
    }
  }

  /**
   * 重置表单
   * @returns void
   */
  const handleResetForm = () => {
    if (toastIfMediaUploading()) return
    uni.showModal({
      title: '确认重置',
      content: '将清空本次填写内容，是否继续？',
      confirmText: '重置',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          resetFormState()
          uni.showToast({ title: '已重置', icon: 'none', duration: 1500 })
        }
      }
    })
  }

  /**
   * 提交表单
   * @returns void
   */
  const handleSubmitClick = () => {
    if (submitting.value) return
    if (toastIfMediaUploading()) return
    if (!userStore.hasPermission(Perms.WORKORDER_ADD)) {
      uni.showToast({ title: '暂无权限', icon: 'none', duration: 1500 })
      return
    }

    const validatePromise = (
      getFormRef() as { validate?: () => Promise<unknown> } | null
    )?.validate?.()
    if (!validatePromise) {
      uni.showToast({ title: '请完善必填项', icon: 'none', duration: 1500 })
      return
    }
    validatePromise
      .then(() => {
        if (formData.value.warrantyCode && !warrantyQueried.value) {
          uni.showToast({ title: '请先查询商品', icon: 'none', duration: 1500 })
          return
        }

        if (!formData.value.warrantyCode) {
          showWarrantyModal.value = true
          return
        }

        performSubmit()
      })
      .catch(() => {
        uni.showToast({ title: '请完善必填项', icon: 'none', duration: 1500 })
      })
  }

  /**
   * 执行提交
   * @returns void
   */
  const performSubmit = () => {
    if (submitting.value) return
    if (toastIfMediaUploading()) return
    if (
      hasUnuploadedMediaItems(asUnknownArray(formData.value.images)) ||
      hasUnuploadedMediaItems(asUnknownArray(formData.value.shippingCode)) ||
      hasUnuploadedMediaItems(asUnknownArray(formData.value.voiceList))
    ) {
      uni.showToast({ title: '图片/语音正在上传，请稍候再试', icon: 'none', duration: 1500 })
      return
    }
    submitting.value = true

    showWarrantyModal.value = false
    uni.showLoading({ title: '提交中...' })

    const request =
      repairEntryTab.value === 'upstream'
        ? userStore.isPrimaryDealer
          ? createUpstreamHqWorkOrder(buildUpstreamFirstCreateDto())
          : createUpstreamFirstWorkOrder(buildUpstreamFirstCreateDto())
        : createProxyWorkOrder(buildProxyCreateDto())
    request
      .then((res) => {
        uni.hideLoading()
        uni.showToast({ title: getApiMessage(res, '提交成功'), duration: 1500 })

        // 提交成功后仅清理当前 tab 的暂存，并恢复初始填写状态
        const submittedTab = repairEntryTab.value
        try {
          uni.removeStorageSync(draftStorageKey(submittedTab))
          const otherTab: 'proxy' | 'upstream' =
            submittedTab === 'proxy' ? 'upstream' : 'proxy'
          const otherRaw = uni.getStorageSync(draftStorageKey(otherTab)) as unknown
          if (parseTabDraftToSnapshot(otherRaw)) {
            uni.setStorageSync(DRAFT_LAST_TAB_KEY, otherTab)
          } else {
            uni.removeStorageSync(DRAFT_LAST_TAB_KEY)
          }
          uni.removeStorageSync(DRAFT_KEY_LEGACY)
        } catch (_e) {
          void _e
        }
        resetFormState()

        setTimeout(() => {
          appStore.markOrderListScrollRefresherOnNextShow()
          uni.switchTab({ url: '/pages/order/list' })
        }, 1500)
      })
      .catch(() => {
        // http 层和 api 层已统一提示，这里只兜底恢复按钮态
        uni.hideLoading()
      })
      .finally(() => {
        submitting.value = false
      })
  }

  /** 将旧版单一草稿迁移到对应 tab 键并删除旧键 */
  const migrateLegacyJasicDraftIfNeeded = () => {
    try {
      const legacy = uni.getStorageSync(DRAFT_KEY_LEGACY) as unknown
      if (!legacy || typeof legacy !== 'object') return
      const d = legacy as { formType?: string; repairEntryTab?: 'proxy' | 'upstream' }
      if (d.formType !== formType) return
      const snap = parseTabDraftToSnapshot(legacy)
      if (!snap) {
        uni.removeStorageSync(DRAFT_KEY_LEGACY)
        return
      }
      const tab = d.repairEntryTab === 'upstream' ? 'upstream' : 'proxy'
      uni.setStorageSync(draftStorageKey(tab), {
        formType,
        savedAt: Date.now(),
        warrantyQueried: snap.warrantyQueried,
        barcodeQueryHasFaultDescription: snap.barcodeQueryHasFaultDescription,
        faultDescriptionOptionsFromApi: snap.faultDescriptionOptionsFromApi,
        lastBarcodeInfo: snap.lastBarcodeInfo,
        queryFailedWithBarcode: snap.queryFailedWithBarcode,
        showFaultRemark: snap.showFaultRemark,
        selectedShippingAddress: snap.selectedShippingAddress,
        showSupplementSection: snap.showSupplementSection,
        formData: snap.formData
      })
      uni.setStorageSync(DRAFT_LAST_TAB_KEY, tab)
      uni.removeStorageSync(DRAFT_KEY_LEGACY)
    } catch (_e) {
      void _e
    }
  }

  /**
   * 恢复草稿：两个 tab 各自一份，切换 tab 时与内存快照一致
   */
  const restoreDraft = () => {
    try {
      migrateLegacyJasicDraftIfNeeded()

      const snapProxy = parseTabDraftToSnapshot(uni.getStorageSync(DRAFT_KEY_PROXY))
      const snapUpstream = parseTabDraftToSnapshot(uni.getStorageSync(DRAFT_KEY_UPSTREAM))
      if (!snapProxy && !snapUpstream) return

      const lastStored = uni.getStorageSync(DRAFT_LAST_TAB_KEY) as unknown
      let activeTab: 'proxy' | 'upstream'
      if (snapProxy && snapUpstream) {
        activeTab = lastStored === 'upstream' ? 'upstream' : 'proxy'
      } else if (snapUpstream) {
        activeTab = 'upstream'
      } else {
        activeTab = 'proxy'
      }

      isRestoringDraft.value = true
      tabFormSnapshots.value = {}
      if (snapProxy) tabFormSnapshots.value.proxy = snapProxy
      if (snapUpstream) tabFormSnapshots.value.upstream = snapUpstream

      repairEntryTab.value = activeTab
      applyRepairEntryTabSnapshot(tabFormSnapshots.value[activeTab] ?? null)
    } catch (_e) {
      void _e
    } finally {
      isRestoringDraft.value = false
    }
  }

  const applyShippingPickFromStorage = () => {
    const picked = takeSelectedShippingAddress()
    if (!picked) return
    selectedShippingAddress.value = picked
    formData.value.shippingInfo = `${picked.name} ${picked.phone}\n${picked.fullAddress}`
    nextTick(() => getFormRef()?.clearValidate?.(['shippingInfo']))
  }

  onMounted(() => {
    restoreDraft()
  })

  onActivated(() => {
    restoreDraft()
    applyShippingPickFromStorage()
  })

  onShow(() => {
    applyShippingPickFromStorage()
  })

  /**
   * 确认提交按钮点击
   * @returns void
   */
  const onWarrantyModalBackdropClick = () => {
    if (toastIfMediaUploading()) return
    showWarrantyModal.value = false
  }

  const confirmSubmit = () => {
    if (toastIfMediaUploading()) return
    if (!userStore.hasPermission(Perms.WORKORDER_ADD)) {
      uni.showToast({ title: '暂无权限', icon: 'none', duration: 1500 })
      return
    }
    performSubmit()
  }
</script>

<style lang="scss" scoped>
  .page-index {
    padding-bottom: 200rpx;
  }

  .repair-entry-tabs-wrap {
    padding: 0 $space-xs;

    .repair-entry-tabs {
      @include pill-tabs;
    }
  }

  // 区块标题
  .section {
    margin-top: $space-lg;
  }

  .section-header {
    @include flex-between;
    margin-bottom: $space-md;
    padding: 0 $space-xs;

    view {
      font-size: $font-md;
      font-weight: bold;
      color: $text-main;
    }

    text {
      font-size: $font-sm;
      color: $primary;
      font-weight: bold;
      letter-spacing: 2rpx;
    }

    .required-badge {
      color: $primary;
    }

    .optional-badge {
      color: $text-placeholder;
    }

    &.collapsible {
      cursor: pointer;
    }
  }

  .collapse-toggle {
    @include flex-row;
    gap: $space-sm;
    align-items: center;

    .toggle-text {
      font-size: $font-sm;
      color: $text-secondary;
    }
  }

  // 保修查询卡片头部
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

  // 保修查询搜索框
  .search-box {
    @include flex-row;
    gap: $space-sm;

    view {
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

  // 寄件信息：与表单内 uni-data-select / 售后端选择行一致（灰底、圆角、高度与字号对齐）
  .shipping-address-btn {
    @include flex-row;
    align-items: center;
    justify-content: space-between;
    gap: $space-sm;
    height: 80rpx;
    padding: 0 $space-md;
    border-radius: $radius-input;
    background-color: $bg-light;
    box-sizing: border-box;
  }

  .shipping-address-text {
    flex: 1;
    min-width: 0;
    font-size: $space-input;
    color: $text-main;
    line-height: 1.4;
    white-space: pre-wrap;

    &.placeholder {
      color: $text-placeholder;
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
    border: 2rpx solid $border-slate;
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

  // 报修提示弹窗
  .modal-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 100;
    background-color: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: $space-lg;
  }

  .modal-content {
    width: 100%;
    max-width: 600rpx;
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: $space-xl;
    box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.1);
  }

  .modal-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .modal-icon-box {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      background-color: rgba($primary, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $space-lg;
    }

    .modal-title {
      font-size: $font-lg;
      font-weight: bold;
      color: $text-main;
      margin-bottom: $space-sm;
    }

    .modal-desc {
      font-size: $font-md;
      color: $text-secondary;
      line-height: 1.5;
      margin-bottom: $space-xl * 1.5;
    }

    .modal-actions {
      @include flex-row;
      width: 100%;
      gap: $space-md;

      button {
        flex: 1;
        margin: 0;
        border-radius: $radius-md;
        font-size: $font-md;
        font-weight: bold;
        height: 88rpx;
        line-height: 88rpx;

        &::after {
          border: none;
        }

        &.btn-cancel {
          background-color: $bg-card;
          border: 1px solid $border-color;
          color: $text-secondary;
        }

        &.btn-confirm {
          background-color: $primary;
          color: $text-bg;
          box-shadow: 0 4rpx 12rpx rgba($primary, 0.3);
        }
      }
    }
  }
</style>
