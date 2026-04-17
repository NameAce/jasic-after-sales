/**
 * 将详情 `repairs` 映射为故障点历史列表，与 contractor `mapWorkOrderRepairsToAllFaultPointRecords` 同源逻辑。
 */
import type { FaultPointRecord } from '@/models/order'

/** 与详情附件结构兼容（独立类型，避免与 `order.ts` 环依赖） */
type RepairHistoryFileItem = {
  previewUrl?: string
  preview_url?: string
  url?: string
  fileUrl?: string
  file_url?: string
  sortNum?: number
}

/** 与 contractor `WorkOrderFaultPartVO` 对齐 */
type CustomerFaultPartLike = {
  partName?: string
  partQty?: number
  sortNum?: number
}

/** 与 contractor `WorkOrderFaultVO` 映射所需字段对齐 */
export type CustomerRepairFaultForHistory = {
  faultDesc?: string
  repairDesc?: string
  otherDesc?: string
  imageUrls?: string
  partDesc?: string
  partList?: CustomerFaultPartLike[]
  createTime?: string
  sortNum?: number
}

/** 与 contractor `WorkOrderRepairVO` 映射所需字段对齐 */
export type CustomerRepairForHistory = {
  companyName?: string
  createTime?: string
  faults?: CustomerRepairFaultForHistory[]
  faultOldImageFiles?: RepairHistoryFileItem[]
  faultNewImageFiles?: RepairHistoryFileItem[]
  machineImageFiles?: RepairHistoryFileItem[]
  machineBarcodeImageFiles?: RepairHistoryFileItem[]
  otherImageFiles?: RepairHistoryFileItem[]
}

function resolveSysFileItemPreviewUrl(item: unknown): string {
  if (item == null) return ''
  if (typeof item === 'string') return item.trim()
  if (typeof item !== 'object') return ''
  const o = item as Record<string, unknown>
  for (const k of ['previewUrl', 'preview_url', 'url', 'fileUrl', 'file_url'] as const) {
    const v = o[k]
    if (v != null && String(v).trim()) return String(v).trim()
  }
  return ''
}

function mapSysFileItemsToLabeledImages(
  files: RepairHistoryFileItem[] | undefined,
  labelPrefix: string,
): { url: string; label: string }[] {
  if (!Array.isArray(files) || !files.length) return []
  return [...files]
    .sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
    .map((file, i) => {
      const url = String(resolveSysFileItemPreviewUrl(file) || '').trim()
      if (!url) return null
      return { url, label: `${labelPrefix}${i + 1}` }
    })
    .filter((x): x is { url: string; label: string } => x != null)
}

function sortRepairsByCreateTime(repairs: CustomerRepairForHistory[]): CustomerRepairForHistory[] {
  return repairs
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
}

function parseRepairPartDesc(partDesc: string): { name: string; count: number }[] {
  const raw = String(partDesc || '').trim()
  if (!raw) return []
  const out: { name: string; count: number }[] = []
  for (const seg of raw.split(/[；;]/)) {
    const s = seg.trim()
    if (!s) continue
    const m = s.match(/^(.+?)\s*[×xX＊*]\s*(\d+)\s*$/)
    if (m) {
      const n = Number(m[2])
      out.push({ name: m[1].trim(), count: Number.isFinite(n) && n > 0 ? n : 1 })
    } else {
      out.push({ name: s, count: 1 })
    }
  }
  return out
}

function faultPartsFromFault(f: CustomerRepairFaultForHistory) {
  const list = Array.isArray(f.partList) ? f.partList : []
  return list
    .slice()
    .sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
    .map((p) => {
      const name = String(p.partName || '').trim()
      const n = Number(p.partQty)
      const count = Number.isFinite(n) && n > 0 ? n : 1
      return name ? { name, count } : null
    })
    .filter((x): x is { name: string; count: number } => x != null)
}

/** 单条维修登记 → 若干条故障点历史记录（与 contractor `mapOneWorkOrderRepairToFaultRecords` 一致） */
function mapOneRepairToFaultRecords(r: CustomerRepairForHistory): FaultPointRecord[] {
  const faults = Array.isArray(r.faults) ? r.faults : []
  const when = String(r.createTime || '')
  const site = String(r.companyName || '').trim()
  const repairLevelImages = [
    ...mapSysFileItemsToLabeledImages(r.faultOldImageFiles, '旧件图'),
    ...mapSysFileItemsToLabeledImages(r.faultNewImageFiles, '新件图'),
    ...mapSysFileItemsToLabeledImages(r.machineImageFiles, '整机图'),
    ...mapSysFileItemsToLabeledImages(r.machineBarcodeImageFiles, '条码图'),
    ...mapSysFileItemsToLabeledImages(r.otherImageFiles, '其它图'),
  ]
  return faults.map((f) => {
    const faultDesc = String(f.faultDesc || '').trim()
    const repairDesc = String(f.repairDesc || '').trim()
    const otherDesc = String(f.otherDesc || '').trim()
    const repairMain = repairDesc === '其它维修说明' ? otherDesc : repairDesc
    const description = [faultDesc, repairMain].filter(Boolean).join(' · ')
    const specialInfo = otherDesc && repairDesc !== '其它维修说明' ? otherDesc : undefined
    const fromLegacyUrls = (String(f.imageUrls || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean) as string[]).map((url, i) => ({ url, label: `图${i + 1}` }))
    const partsFromList = faultPartsFromFault(f)
    const parts =
      partsFromList.length > 0 ? partsFromList : parseRepairPartDesc(String(f.partDesc || ''))
    return {
      description,
      faultDesc,
      repairDesc,
      otherDesc,
      images: [...fromLegacyUrls, ...repairLevelImages],
      parts,
      specialInfo,
      location: site,
      date: String(f.createTime || when || '').trim(),
    }
  })
}

/**
 * 将详情接口 `repairs` 映射为故障点历史列表。
 * 与 contractor `mapWorkOrderRepairsToAllFaultPointRecords` 一致。
 */
export function mapCustomerRepairsToAllFaultPointRecords(
  repairs: CustomerRepairForHistory[] | undefined | null,
): FaultPointRecord[] {
  const list = Array.isArray(repairs) ? repairs : []
  return sortRepairsByCreateTime(list).flatMap(mapOneRepairToFaultRecords)
}
