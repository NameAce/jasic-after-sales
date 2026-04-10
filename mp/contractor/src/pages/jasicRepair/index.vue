<template>
  <view class="page-index">
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
          <uni-icons type="vip-filled" size="24" color="#f26604"></uni-icons>
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

          <uni-forms-item
            v-if="showContactMobileField"
            label="客户姓名"
            name="customerName"
            required
          >
            <uni-easyinput v-model="formData.customerName" placeholder="请输入客户姓名" />
          </uni-forms-item>

          <!-- 故障描述：默认显示；商品查询无结果/无预设列表时隐藏，改填故障说明备注 -->
          <uni-forms-item
            v-if="showFaultDescriptionSelect"
            label="故障描述"
            name="faultDescription"
            required
          >
            <uni-data-select
              v-model="formData.faultDescription"
              :localdata="resolvedFaultDescriptionOptions"
              placeholder="请选择"
              @change="handleFaultDescriptionChange"
            />
          </uni-forms-item>

          <!-- 故障说明备注：无预设列表时必填；有列表且选「其它故障」时必填 -->
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

          <!-- 寄件信息（仅佳士品牌显示） -->
          <uni-forms-item v-if="showShippingInfo" label="寄件信息" name="shippingInfo" required>
            <uni-easyinput
              v-model="formData.shippingInfo"
              type="textarea"
              auto-height
              placeholder="请输入寄件信息"
            />
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
          <!-- 故障语音说明：录音条（VoiceInputField）+ 列表播放（VoicePlaybackList，与售后页组件拆分一致） -->
          <VoiceInputField v-model="formData.voiceList" :show-recorded-list="false" />
          <view v-if="!formData.voiceList.length" class="voice-empty-hint">
            <text>暂无录音</text>
          </view>
          <VoicePlaybackList
            v-else
            :items="voicePlaybackItems"
            deletable
            @remove="onVoicePlaybackRemove"
          />

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

          <!-- 寄件快递图片 -->
          <MediaUploadField
            v-model="formData.shippingCode"
            label="寄件快递单号"
            tip="限2张图片"
            file-mediatype="image"
            :limit="2"
            :max-file-size="1024 * 1024 * 10"
            :del-icon="true"
            @select="handleExpressImageSelect"
            @delete="handleExpressImageDelete"
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
  <view v-if="showWarrantyModal" class="modal-mask" @click="showWarrantyModal = false">
    <view class="modal-content" @click.stop>
      <view class="modal-body">
        <view class="modal-icon-box">
          <uni-icons type="info-filled" size="40" color="#f26604"></uni-icons>
        </view>
        <view class="modal-title">报修提示</view>
        <view class="modal-desc">无条码或无法识别条码，系统默认该机器已过保</view>
        <view class="modal-actions">
          <button class="btn-cancel" @click="showWarrantyModal = false">取消</button>
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
  import { useUserStore } from '@/stores/modules/user'
  import { Perms } from '@/utils/permissions'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import RepairTypeSelector from '@/components/RepairTypeSelector/RepairTypeSelector.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import VoiceInputField from '@/components/VoiceInputField/VoiceInputField.vue'
  import VoicePlaybackList from '@/components/VoicePlaybackList/VoicePlaybackList.vue'
  import type { VoicePlaybackItem } from '@/components/VoicePlaybackList/VoicePlaybackList.vue'
  import {
    createProxyWorkOrder,
    createUpstreamFirstWorkOrder,
    createUpstreamHqWorkOrder,
    fetchProxyBarcodeInfo,
    fetchUpstreamFirstBarcodeInfo,
    type WorkOrderProxyCreateDTO,
    type WorkOrderUpstreamCreateDTO
  } from '@/api/order'
  import { getApiMessage } from '@/utils/http'
  import { MOBILE_PATTERN } from '@/utils/validation'

  // 用户商店
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
  // 设置维修入口标签
  const setRepairEntryTab = (tab: 'proxy' | 'upstream') => {
    repairEntryTab.value = tab
    nextTick(() => {
      const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
      fr?.clearValidate?.(['contactMobile'])
    })
  }

  // 表单引用
  const formRef = ref(null)

  // 表单类型
  const formType = 'jasic'
  // 草稿存储键
  const DRAFT_KEY = 'jasicRepairDraft'
  // 创建初始表单数据
  const createInitialFormData = () => ({
    // 保修码
    warrantyCode: '',
    // 客户手机（仅代客户填写）
    contactMobile: '',
    // 客户姓名（仅代客户填写）
    customerName: '',
    // 维修路径（仅送店 / 邮寄）
    repairType: 'shop',
    // 故障描述
    faultDescription: '',
    // 故障视频/图片
    images: [],
    // 寄件快递单号
    shippingCode: [] as unknown[],
    // 故障说明备注
    faultRemark: '',
    // 寄件信息
    shippingInfo: '',
    // 语音列表（tempFilePath + duration 毫秒，与 VoiceInputField / VoicePlaybackList 一致）
    voiceList: [] as { tempFilePath: string; duration: number }[]
  })

  // 状态
  const formData = ref(createInitialFormData())

  /** 供 VoicePlaybackList 使用：url + duration（毫秒） */
  const voicePlaybackItems = computed<VoicePlaybackItem[]>(() =>
    (formData.value.voiceList || []).map((v) => ({
      url: v.tempFilePath,
      duration: v.duration
    }))
  )

  /**
   * 从播放列表删除一条（与表单 voiceList 同步）
   * @param index - 索引
   */
  const onVoicePlaybackRemove = (index: number) => {
    const list = [...(formData.value.voiceList || [])]
    if (index < 0 || index >= list.length) return
    list.splice(index, 1)
    formData.value.voiceList = list
  }

  // 是否显示寄件信息（仅佳士品牌且选择邮寄维修）
  const showShippingInfo = computed(
    () => formType === 'jasic' && formData.value.repairType === 'mail'
  )

  const OTHER_FAULT_VALUE = 'other'

  /** 预设故障选项（未查询时兜底） */
  const DEFAULT_FAULT_DESCRIPTION_OPTIONS = [
    { text: '其它故障', value: OTHER_FAULT_VALUE },
    { text: '无法开机', value: 'no_power' },
    { text: '无法焊接', value: 'no_weld' },
    { text: '显示异常', value: 'display_error' },
    { text: '按键失灵', value: 'key_failure' }
  ]

  /**
   * 将后端故障选项映射为 uni-data-select 所需结构。
   * - 其它故障统一归一化为 value=other，便于沿用现有「备注必填」逻辑；
   * - 其余选项使用自身文本作为 value，避免与后端定义不一致。
   */
  const mapFaultOptionsToSelect = (faultOptions: unknown, otherFaultLabel?: string) => {
    if (!Array.isArray(faultOptions)) return []
    const otherLabel = String(otherFaultLabel || '').trim()
    const list = faultOptions
      .map((item) => String(item || '').trim())
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
  /**
   * 查询返回的预设故障列表：
   * null — 尚未根据当前条码查询，下拉使用本地默认项；
   * [] — 已查询但无商品/无预设/查询失败 → 隐藏下拉，仅填备注；
   * 非空 — 使用接口返回项。
   */
  const productFaultDescriptionOptions = ref<{ text: string; value: string }[] | null>(null)
  // 是否显示故障描述选择
  const showFaultDescriptionSelect = computed(() => {
    const opts = productFaultDescriptionOptions.value
    if (opts === null) return true
    return opts.length > 0
  })

  // 是否显示故障说明备注
  const showFaultRemark = computed(() => {
    const opts = productFaultDescriptionOptions.value
    if (opts === null) {
      return formType === 'jasic' && formData.value.faultDescription === OTHER_FAULT_VALUE
    }
    if (!warrantyQueried.value) return false
    if (opts.length === 0) return true
    return formType === 'jasic' && formData.value.faultDescription === OTHER_FAULT_VALUE
  })

  // 解析故障描述选项
  const resolvedFaultDescriptionOptions = computed(() => {
    const opts = productFaultDescriptionOptions.value
    if (opts === null) return DEFAULT_FAULT_DESCRIPTION_OPTIONS
    return opts
  })

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
    if (showFaultDescriptionSelect.value) {
      base.faultDescription = {
        rules: [{ required: true, errorMessage: '请选择故障描述' }]
      }
    }
    if (showFaultRemark.value) {
      base.faultRemark = {
        rules: [{ required: true, errorMessage: '请输入故障说明备注' }]
      }
    }
    if (repairEntryTab.value === 'proxy') {
      base.contactMobile = {
        rules: [
          { required: true, errorMessage: '请输入客户手机号码' },
          { pattern: MOBILE_PATTERN, errorMessage: '请输入正确的手机号码' }
        ]
      }
      base.customerName = {
        rules: [{ required: true, errorMessage: '请输入客户姓名' }]
      }
    }
    if (formType === 'jasic' && formData.value.repairType === 'mail') {
      base.shippingInfo = {
        rules: [{ required: true, errorMessage: '请输入寄件信息' }]
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

  // 维修路径选项
  const repairTypes = [
    { label: '送店维修', value: 'shop', icon: 'shop-filled' },
    { label: '邮寄维修', value: 'mail', icon: 'paperplane-filled' }
  ]

  /**
   * 监听维修路径变化
   * @param val 维修路径
   */
  watch(
    () => formData.value.repairType,
    (val) => {
      if (val !== 'mail') {
        nextTick(() => {
          const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
          fr?.clearValidate?.(['shippingInfo'])
        })
      }
    }
  )

  /**
   * 扫描条形码
   * @returns void
   */
  const handleScan = () => {
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
    showSupplementSection.value = !showSupplementSection.value
  }

  /**
   * 查询条码信息（按入口区分：代客户填写 / 报修一级）
   * @returns void
   */
  const checkWarranty = () => {
    if (!formData.value.warrantyCode) return uni.showToast({ title: '请输入条形码', icon: 'none' })
    uni.showLoading({ title: '查询中...' })
    const request =
      repairEntryTab.value === 'upstream'
        ? fetchUpstreamFirstBarcodeInfo(formData.value.warrantyCode)
        : fetchProxyBarcodeInfo(formData.value.warrantyCode)

    request
      .then((info) => {
        uni.hideLoading()
        const list = mapFaultOptionsToSelect(info?.faultOptions, info?.otherFaultLabel)
        uni.showToast({ title: info?.warrantyStatus || '查询成功', icon: 'success' })
        warrantyQueried.value = true
        productFaultDescriptionOptions.value = list
        if (list.length === 0) {
          formData.value.faultDescription = OTHER_FAULT_VALUE
          formData.value.faultRemark = ''
        } else {
          formData.value.faultDescription = ''
          formData.value.faultRemark = ''
        }
        nextTick(() => {
          const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
          fr?.clearValidate?.(['faultDescription', 'faultRemark'])
        })
      })
      .catch((err: unknown) => {
        uni.hideLoading()
        if (err instanceof Error && err.message) {
          // api 层已提示，这里不重复弹错误文案
        }
        warrantyQueried.value = true
        productFaultDescriptionOptions.value = []
        formData.value.faultDescription = OTHER_FAULT_VALUE
        formData.value.faultRemark = ''
        // 清空表单校验
        nextTick(() => {
          const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
          fr?.clearValidate?.(['faultDescription', 'faultRemark'])
        })
      })
  }

  /**
   * 故障描述变化
   * @param e 事件
   * @returns void
   */
  const handleFaultDescriptionChange = (e: unknown) => {
    const val = typeof e === 'string' ? e : (e as { detail?: { value?: string } })?.detail?.value
    if (formType !== 'jasic' || !showFaultDescriptionSelect.value) return
    if (val !== OTHER_FAULT_VALUE) {
      formData.value.faultRemark = ''
      nextTick(() => {
        // 清空表单校验
        const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
        fr?.clearValidate?.(['faultRemark'])
      })
    }
  }

  /**
   * 监听条码变化
   * @returns void
   */
  watch(
    () => formData.value.warrantyCode,
    () => {
      if (isRestoringDraft.value) return
      warrantyQueried.value = false
      productFaultDescriptionOptions.value = null
      formData.value.faultDescription = ''
      formData.value.faultRemark = ''
    }
  )

  /**
   * 选择故障视频/图片
   * @param e 事件
   * @returns void
   */
  const handleFaultMediaSelect = (e) => {
    const files = e.tempFiles
    let videoCount = 0
    let imageCount = 0
    // 统计视频和图片数量
    files.forEach((file) => {
      if (file.fileType === 'video') {
        videoCount++
      } else if (file.fileType === 'image') {
        imageCount++
      }
    })

    // 校验视频数量
    if (videoCount > 1) {
      uni.showToast({
        title: '最多只能上传1个视频',
        icon: 'none'
      })
      return
    }

    // 校验图片数量
    if (imageCount > 3) {
      uni.showToast({
        title: '最多只能上传3张图片',
        icon: 'none'
      })
      return
    }
  }

  /**
   * 删除故障视频/图片
   * @returns void
   */
  const handleFaultMediaDelete = () => {
    // 删除处理
  }

  /**
   * 选择寄件快递图片
   * @returns void
   */
  const handleExpressImageSelect = () => {
    // 选择处理
  }

  /**
   * 删除寄件快递图片
   * @returns void
   */
  const handleExpressImageDelete = () => {
    // 删除处理
  }

  /**
   * 从上传文件列表中提取 fileId/id 数组
   * @param files 文件列表
   * @returns 文件ID数组
   */
  const extractFileIds = (files: unknown): number[] => {
    if (!Array.isArray(files)) return []
    return files
      .map((item) => {
        if (!item || typeof item !== 'object') return undefined
        const obj = item as Record<string, unknown>
        const candidates = [obj.fileId, obj.id, obj.value]
        for (const c of candidates) {
          const n = Number(c)
          if (Number.isFinite(n) && n > 0) return n
        }
        return undefined
      })
      .filter((id): id is number => typeof id === 'number')
  }

  /**
   * 构建「代客户填写」创建工单 DTO
   * @returns DTO
   */
  const buildProxyCreateDto = (): WorkOrderProxyCreateDTO => {
    const barcode = String(formData.value.warrantyCode || '').trim()
    const customerMobile = String(formData.value.contactMobile || '').trim()
    const customerName = String(formData.value.customerName || '').trim()
    const faultDescription = String(formData.value.faultDescription || '').trim()
    const faultRemark = String(formData.value.faultRemark || '').trim()
    const serviceMode = formData.value.repairType === 'mail' ? 'MAIL' : 'STORE'

    return {
      barcode,
      customerMobile,
      customerName,
      faultImageFileIds: extractFileIds(formData.value.images),
      faultItems: faultDescription ? [faultDescription] : [],
      faultRemark,
      faultVideoFileIds: [],
      faultVoiceFileIds: [],
      sendExpressNo: '',
      senderAddress: serviceMode === 'MAIL' ? String(formData.value.shippingInfo || '').trim() : '',
      senderMobile: customerMobile,
      senderName: customerName,
      senderVoucherFileIds: extractFileIds(formData.value.shippingCode),
      serviceMode
    }
  }

  /**
   * 构建「二级报修一级」创建工单 DTO
   * @returns DTO
   */
  const buildUpstreamFirstCreateDto = (): WorkOrderUpstreamCreateDTO => {
    const barcode = String(formData.value.warrantyCode || '').trim()
    const customerMobile = String(formData.value.contactMobile || '').trim()
    const customerName = String(formData.value.customerName || '').trim()
    const faultDescription = String(formData.value.faultDescription || '').trim()
    const faultRemark = String(formData.value.faultRemark || '').trim()
    const serviceMode = formData.value.repairType === 'mail' ? 'MAIL' : 'STORE'

    return {
      barcode,
      customerMobile,
      customerName,
      faultImageFileIds: extractFileIds(formData.value.images),
      faultItems: faultDescription ? [faultDescription] : [],
      faultRemark,
      faultVideoFileIds: [],
      faultVoiceFileIds: [],
      sendExpressNo: '',
      senderAddress: serviceMode === 'MAIL' ? String(formData.value.shippingInfo || '').trim() : '',
      senderMobile: customerMobile,
      senderName: customerName,
      senderVoucherFileIds: extractFileIds(formData.value.shippingCode),
      serviceMode
    }
  }

  /**
   * 重置表单状态
   * @returns void
   */
  const resetFormState = () => {
    repairEntryTab.value = 'proxy'
    // 重置表单数据
    formData.value = createInitialFormData()
    warrantyQueried.value = false
    productFaultDescriptionOptions.value = null
    showSupplementSection.value = false
    showWarrantyModal.value = false

    // 清空表单校验
    nextTick(() => {
      const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
      fr?.clearValidate?.([
        'contactMobile',
        'customerName',
        'repairType',
        'faultDescription',
        'faultRemark',
        'shippingInfo'
      ])
    })
  }

  /**
   * 保存草稿
   * @returns void
   */
  const handleSaveDraft = () => {
    // 保存草稿
    try {
      uni.setStorageSync(DRAFT_KEY, {
        formType,
        repairEntryTab: repairEntryTab.value,
        warrantyQueried: warrantyQueried.value,
        // 仅用于恢复「故障描述下拉是否显示/选项内容」
        productFaultDescriptionOptions: productFaultDescriptionOptions.value,
        savedAt: Date.now(),
        formData: formData.value
      })
      uni.showToast({ title: '暂存成功', icon: 'success' })
    } catch (_e) {
      void _e
      uni.showToast({ title: '暂存失败', icon: 'none' })
    }
  }

  /**
   * 重置表单
   * @returns void
   */
  const handleResetForm = () => {
    uni.showModal({
      title: '确认重置',
      content: '将清空本次填写内容，是否继续？',
      confirmText: '重置',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          resetFormState()
          uni.showToast({ title: '已重置', icon: 'none' })
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
    if (!userStore.hasPermission(Perms.WORKORDER_ADD)) {
      uni.showToast({ title: '暂无权限', icon: 'none' })
      return
    }

    // 先做校验，避免上传/提交逻辑里再处理错误
    formRef.value
      ?.validate()
      .then(() => {
        if (formData.value.warrantyCode && !warrantyQueried.value) {
          uni.showToast({ title: '请先查询商品', icon: 'none' })
          return
        }

        // 如果没有条形码，显示报修提示弹窗（默认该机器已过保）
        if (!formData.value.warrantyCode) {
          showWarrantyModal.value = true
          return
        }

        // 执行提交
        performSubmit()
      })
      .catch(() => {
        // uni-forms 内部会展示对应的校验提示
        uni.showToast({ title: '请完善必填项', icon: 'none' })
      })
  }

  /**
   * 执行提交
   * @returns void
   */
  const performSubmit = () => {
    if (submitting.value) return
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
        uni.showToast({ title: getApiMessage(res, '提交成功') })

        // 提交成功后清理草稿，并恢复初始填写状态
        try {
          uni.removeStorageSync(DRAFT_KEY)
        } catch (_e) {
          void _e
        }
        resetFormState()
      })
      .catch(() => {
        // http 层和 api 层已统一提示，这里只兜底恢复按钮态
        uni.hideLoading()
      })
      .finally(() => {
        submitting.value = false
      })
  }

  /**
   * 恢复草稿
   * @returns void
   */
  const restoreDraft = () => {
    // 恢复草稿
    try {
      const draft = uni.getStorageSync(DRAFT_KEY) as unknown
      if (!draft || typeof draft !== 'object') return

      // 解析草稿数据
      const d = draft as {
        formType?: string
        repairEntryTab?: 'proxy' | 'upstream'
        warrantyQueried?: boolean
        productFaultDescriptionOptions?: { text: string; value: string }[] | null
        formData?: Partial<ReturnType<typeof createInitialFormData>>
      }

      // 校验表单类型
      if (d.formType !== formType) return

      isRestoringDraft.value = true

      // tabs / 查询状态 / 下拉选项
      repairEntryTab.value = d.repairEntryTab === 'upstream' ? 'upstream' : 'proxy'
      warrantyQueried.value = !!d.warrantyQueried

      if (Array.isArray(d.productFaultDescriptionOptions)) {
        productFaultDescriptionOptions.value = d.productFaultDescriptionOptions
      } else {
        productFaultDescriptionOptions.value = null
      }

      // 表单数据（合并初始值，避免缺字段导致 UI 异常）
      formData.value = {
        ...createInitialFormData(),
        ...(d.formData || {})
      }

      // 额外 UI 状态不从草稿恢复，保持默认更可控
      showSupplementSection.value = false
      showWarrantyModal.value = false

      nextTick(() => {
        const fr = formRef.value as { clearValidate?: (names?: string[]) => void } | null
        fr?.clearValidate?.([
          'contactMobile',
          'customerName',
          'repairType',
          'faultDescription',
          'faultRemark',
          'shippingInfo'
        ])
      })
    } catch (_e) {
      // 静默失败：读取草稿不是必须动作
      void _e
    } finally {
      isRestoringDraft.value = false
    }
  }

  onMounted(() => {
    restoreDraft()
  })

  onActivated(() => {
    restoreDraft()
  })

  /**
   * 确认提交按钮点击
   * @returns void
   */
  const confirmSubmit = () => {
    if (!userStore.hasPermission(Perms.WORKORDER_ADD)) {
      uni.showToast({ title: '暂无权限', icon: 'none' })
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
    background-color: $surface-white;
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
          background-color: $surface-white;
          border: 1px solid $border-color;
          color: $text-secondary;
        }

        &.btn-confirm {
          background-color: $primary;
          color: $surface-white;
          box-shadow: 0 4rpx 12rpx rgba($primary, 0.3);
        }
      }
    }
  }

  /* 与 VoiceInputField 占位一致：无录音时仅展示提示（录音条在上方组件内） */
  .voice-empty-hint {
    padding: $space-md;
    text-align: center;
    color: $text-placeholder;
    font-size: $font-sm;
    background-color: #f9f9f9;
    border-radius: $radius-md;
    margin-bottom: $space-sm;
  }
</style>
