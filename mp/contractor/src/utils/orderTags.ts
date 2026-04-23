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
 * 故障判定标签样式类：有故障红色 / 无故障绿色
 * @param faultJudge 故障判定文案（与接单表单选项一致）
 */
export function getFaultJudgeTagClass(faultJudge: string): string {
  const t = (faultJudge || '').trim()
  if (!t || t === '-') return 'tag-fault-judge-neutral'
  if (t === '无故障') return 'tag-fault-judge-green'
  if (t === '有故障') return 'tag-fault-judge-red'
  return 'tag-fault-judge-neutral'
}

