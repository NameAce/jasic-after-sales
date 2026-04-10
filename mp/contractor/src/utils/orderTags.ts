/**
 * 工单类型标签样式类：佳士橙色 / 非佳士灰色
 * @param orderTypeName 工单类型名称
 * @returns 工单类型标签样式类
 */
export function getOrderTypeTagClass(orderTypeName: string): string {
  const name = (orderTypeName || '').trim()
  if (!name || name === '-') return 'tag-value-neutral'
  return /非佳士/.test(name) ? 'tag-order-type-gray' : 'tag-order-type-orange'
}

/**
 * 质保判定标签样式类：保内绿色 / 其余红色
 * @param warrantyClass 质保判定类
 * @returns 质保判定标签样式类
 */
export function getWarrantyTagClass(warrantyClass: string): string {
  const w = (warrantyClass || '').trim()
  if (!w || w === '-') return 'tag-value-neutral'
  return /保内/.test(w) ? 'tag-warranty-green' : 'tag-warranty-red'
}

/**
 * 维修方式标签样式类：邮寄紫色 / 送店黄色
 * @param repairMethod 维修方式
 * @returns 维修方式标签样式类
 */
export function getRepairMethodTagClass(repairMethod: string): string {
  const m = (repairMethod || '').trim()
  if (!m || m === '-') return 'tag-method-neutral'
  if (/邮寄/.test(m)) return 'tag-method-purple'
  if (/送店/.test(m)) return 'tag-method-yellow'
  return 'tag-method-neutral'
}
