/**
 * 报修表单相关常量（C 端）。
 * - `REPAIR_TYPE_OPTIONS` 为双端共享的「维修路径」选项，已在 mp/contractor 同名文件镜像。
 * - `JASIC_BRAND_CODE` / `REPAIR_TYPE_TO_SERVICE_MODE` / `CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE`
 *   属于 C 端专有：仅在客户提交 `/api/customer/work-order` 时使用；contractor 走派单-接单
 *   流程提交，不需要此类前端映射，因此不镜像到 contractor。
 */

/** 报修页「维修路径」选项（送店 / 邮寄） */
export interface RepairTypeOption {
  label: string
  value: string
  icon: string
}

/** 报修页「维修路径」选项（送店 / 邮寄） */
export const REPAIR_TYPE_OPTIONS: RepairTypeOption[] = [
  { label: '送店维修', value: 'STORE', icon: 'shop-filled' },
  { label: '邮寄维修', value: 'MAIL', icon: 'paperplane-filled' }
]

/** 佳士报修对接 `/api/customer/work-order` 时的品牌编码（与后端约定一致时可改） */
export const JASIC_BRAND_CODE = 'JASIC'

/** 前端 repairType → 工单 serviceMode */
export const REPAIR_TYPE_TO_SERVICE_MODE: Record<string, string> = {
  shop: 'STORE',
  mail: 'MAIL'
}

/** C 端创建工单 `brandType`，与后端 CustomerWorkOrderCreateDTO 一致 */
export const CUSTOMER_WORK_ORDER_REPORT_BIZ_TYPE = {
  JASIC: 'JASIC',
  NON_JASIC: 'NON_JASIC' 
} as const
