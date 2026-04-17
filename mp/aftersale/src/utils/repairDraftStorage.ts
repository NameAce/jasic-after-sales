import type { Ref } from 'vue'
import type { VoiceItem } from '@/components/VoiceInputField/VoiceInputField.vue'
import type { BarcodeInfoDTO } from '@/api/order'

const KEY_OTHER = 'repairFormDraft_other_v1'
const KEY_JASIC = 'repairFormDraft_jasic_v1'

export interface OtherRepairDraftForm {
  centerId: string | number | null
  faultRemark: string
  repairType: string
  shippingInfo: string
  voiceList: VoiceItem[]
  images: unknown[]
  shippingCode: unknown[]
  brandName: string
  modelName: string
}

export interface OtherRepairDraft {
  formData: OtherRepairDraftForm
  selectedCenterDisplay: string
  showSupplementSection: boolean
}

export interface JasicRepairDraftForm {
  warrantyCode: string
  centerId: string | number | null
  repairType: string
  faultDescription: string[]
  images: unknown[]
  shippingCode: unknown[]
  faultRemark: string
  shippingInfo: string
  voiceList: VoiceItem[]
}

export interface JasicRepairDraft {
  formData: JasicRepairDraftForm
  selectedCenterDisplay: string
  showSupplementSection: boolean
  /** 条码查询是否曾返回故障描述（用于恢复下拉是否展示） */
  barcodeQueryHasFaultDescription?: boolean
  /** 条码查询返回的故障描述下拉项 */
  faultDescriptionOptions?: { text: string; value: string }[]
  /** 最近一次条码查询接口 data，用于恢复故障区展示与提交入参（与页面 lastBarcodeInfo 一致） */
  lastBarcodeInfo?: BarcodeInfoDTO | null
  /** 有条码但查询失败时须填故障说明备注 */
  queryFailedWithBarcode?: boolean
}

export function saveOtherRepairDraft(snapshot: OtherRepairDraft): void {
  try {
    uni.setStorageSync(KEY_OTHER, JSON.stringify(snapshot))
  } catch {
    // ignore
  }
}

export function loadOtherRepairDraft(): OtherRepairDraft | null {
  try {
    const raw = uni.getStorageSync(KEY_OTHER)
    if (raw == null || raw === '') return null
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!parsed || typeof parsed !== 'object' || parsed.formData == null) return null
    return parsed as OtherRepairDraft
  } catch {
    return null
  }
}

export function clearOtherRepairDraft(): void {
  try {
    uni.removeStorageSync(KEY_OTHER)
  } catch {
    // ignore
  }
}

export function applyOtherRepairDraft(
  draft: OtherRepairDraft | null,
  formData: Ref<OtherRepairDraftForm>,
  selectedCenterDisplay: Ref<string>,
  showSupplementSection: Ref<boolean>,
  preserveServicePoint: boolean
): void {
  if (!draft?.formData) return
  const fd = draft.formData as Partial<OtherRepairDraftForm> & { faultDescription?: string }
  const normalizedFormData: OtherRepairDraftForm = {
    centerId: fd.centerId ?? null,
    faultRemark:
      String(fd.faultRemark ?? '').trim() ||
      String(fd.faultDescription ?? '').trim(),
    repairType: String(fd.repairType ?? 'STORE'),
    shippingInfo: String(fd.shippingInfo ?? ''),
    voiceList: Array.isArray(fd.voiceList) ? fd.voiceList : [],
    images: Array.isArray(fd.images) ? fd.images : [],
    shippingCode: Array.isArray(fd.shippingCode) ? fd.shippingCode : [],
    brandName: String(fd.brandName ?? ''),
    modelName: String(fd.modelName ?? '')
  }
  if (preserveServicePoint) {
    formData.value = { ...normalizedFormData, centerId: formData.value.centerId }
  } else {
    formData.value = { ...normalizedFormData }
    selectedCenterDisplay.value = draft.selectedCenterDisplay ?? ''
  }
  showSupplementSection.value = !!draft.showSupplementSection
}

export function saveJasicRepairDraft(snapshot: JasicRepairDraft): void {
  try {
    uni.setStorageSync(KEY_JASIC, JSON.stringify(snapshot))
  } catch {
    // ignore
  }
}

export function loadJasicRepairDraft(): JasicRepairDraft | null {
  try {
    const raw = uni.getStorageSync(KEY_JASIC)
    if (raw == null || raw === '') return null
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!parsed || typeof parsed !== 'object' || parsed.formData == null) return null
    return parsed as JasicRepairDraft
  } catch {
    return null
  }
}

export function clearJasicRepairDraft(): void {
  try {
    uni.removeStorageSync(KEY_JASIC)
  } catch {
    // ignore
  }
}

export function applyJasicRepairDraft(
  draft: JasicRepairDraft | null,
  formData: Ref<JasicRepairDraftForm>,
  selectedCenterDisplay: Ref<string>,
  showSupplementSection: Ref<boolean>,
  showFaultRemark: Ref<boolean>,
  preserveServicePoint: boolean,
  barcodeQueryHasFaultDescription?: Ref<boolean>,
  faultDescriptionOptions?: Ref<{ text: string; value: string }[]>,
  lastBarcodeInfo?: Ref<BarcodeInfoDTO | null>,
  queryFailedWithBarcode?: Ref<boolean>
): void {
  if (!draft?.formData) return
  const normalizeFaultDescription = (value: unknown): string[] => {
    if (Array.isArray(value)) {
      return value.map((item) => String(item ?? '').trim()).filter(Boolean)
    }
    const single = String(value ?? '').trim()
    return single ? [single] : []
  }
  const normalizedFormData: JasicRepairDraftForm = {
    ...draft.formData,
    faultDescription: normalizeFaultDescription((draft.formData as { faultDescription?: unknown }).faultDescription)
  }
  if (preserveServicePoint) {
    formData.value = { ...normalizedFormData, centerId: formData.value.centerId }
  } else {
    formData.value = { ...normalizedFormData }
    selectedCenterDisplay.value = draft.selectedCenterDisplay ?? ''
  }
  showSupplementSection.value = !!draft.showSupplementSection
  if (faultDescriptionOptions && Array.isArray(draft.faultDescriptionOptions)) {
    faultDescriptionOptions.value = [...draft.faultDescriptionOptions]
  }
  if (barcodeQueryHasFaultDescription) {
    barcodeQueryHasFaultDescription.value =
      draft.barcodeQueryHasFaultDescription ??
      (!!formData.value.warrantyCode &&
        (formData.value.faultDescription.length > 0 || formData.value.faultRemark !== ''))
  }
  // 须在 formData 赋值之后：watch(条码) 会先清空查询态，这里再写回暂存的查询结果
  if (lastBarcodeInfo) {
    lastBarcodeInfo.value =
      draft.lastBarcodeInfo != null ? { ...draft.lastBarcodeInfo } : null
  }
  if (queryFailedWithBarcode) {
    queryFailedWithBarcode.value = !!draft.queryFailedWithBarcode
  }
  const hasFaultUi = barcodeQueryHasFaultDescription?.value ?? true
  if (!formData.value.warrantyCode) {
    showFaultRemark.value = true
  } else if (!hasFaultUi) {
    // 无故障下拉：须填写故障说明备注（页面加载后会再 sync）
    showFaultRemark.value = true
  } else {
    showFaultRemark.value = false
  }
}
