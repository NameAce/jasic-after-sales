/** 故障点记录 */
export type FaultPointRecord = {
  /** 维修说明汇总（faultDesc · 维修主文案），旧缓存可能仅有本字段 */
  description: string
  /** 结构化字段（新映射必带，便于历史页按「其它维修说明」规则展示） */
  faultDesc?: string
  repairDesc?: string
  otherDesc?: string
  images: { url: string; label: string }[]
  parts?: { name: string; count: number }[]
  /** repairDesc 非「其它维修说明」时的补充说明 */
  specialInfo?: string
  location: string
  date: string
}
