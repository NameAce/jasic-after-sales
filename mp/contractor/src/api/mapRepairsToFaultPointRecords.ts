/**
 * 将详情 `repairs` 映射为故障点历史列表，与 aftersale `mapCustomerRepairsToAllFaultPointRecords` 同源逻辑。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import type { FaultPointRecord, WorkOrderFaultVO, WorkOrderRepairVO, SysFileItemVO } from '@/models/order'

/**
 * 作用：接口封装：resolveSysFileItemPreviewUrl。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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

/**
 * 作用：转换/构造：mapSysFileItemsToLabeledImages。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapSysFileItemsToLabeledImages(
  files: SysFileItemVO[] | undefined,
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

/**
 * 作用：接口封装：sortRepairsByCreateTime。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function sortRepairsByCreateTime(repairs: WorkOrderRepairVO[]): WorkOrderRepairVO[] {
  return repairs
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
}

/**
 * 作用：转换/构造：parseRepairPartDesc。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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

/**
 * 作用：接口封装：faultPartsFromFault。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function faultPartsFromFault(f: WorkOrderFaultVO) {
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

/**
 * 单条维修登记 → 若干条故障点历史记录
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapOneRepairToFaultRecords(r: WorkOrderRepairVO): FaultPointRecord[] {
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
 * 与 aftersale `mapCustomerRepairsToAllFaultPointRecords` 保持同源结构。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function mapWorkOrderRepairsToAllFaultPointRecords(
  repairs: WorkOrderRepairVO[] | undefined | null,
): FaultPointRecord[] {
  const list = Array.isArray(repairs) ? repairs : []
  return sortRepairsByCreateTime(list).flatMap(mapOneRepairToFaultRecords)
}
