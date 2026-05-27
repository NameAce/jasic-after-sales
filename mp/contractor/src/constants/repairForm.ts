/**
 * 报修页「维修路径」选项（到店 / 邮寄），与售后端一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface RepairTypeOption {
  label: string
  value: string
  icon: string
}

export const REPAIR_TYPE_OPTIONS: RepairTypeOption[] = [
  { label: '到店维修', value: 'STORE', icon: 'shop-filled' },
  { label: '邮寄维修', value: 'MAIL', icon: 'paperplane-filled' },
]
