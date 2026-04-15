/** 报修页「维修路径」选项（送店 / 邮寄），与售后端一致 */
export interface RepairTypeOption {
  label: string
  value: string
  icon: string
}

export const REPAIR_TYPE_OPTIONS: RepairTypeOption[] = [
  { label: '送店维修', value: 'STORE', icon: 'shop-filled' },
  { label: '邮寄维修', value: 'MAIL', icon: 'paperplane-filled' },
]
