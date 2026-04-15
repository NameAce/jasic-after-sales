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

/** 维修方式展示文案是否属于寄修/邮寄类（兼容空格、大小写、全角 MAIL、常见同义写法） */
function isMailRepairMethodLabel(raw: string): boolean {
  const collapsed = String(raw ?? '')
    .trim()
    .replace(/\s+/g, '')
  const m = collapsed.replace(/[ＭＡＩＬｍａｉｌ]/g, (ch) =>
    String.fromCharCode(ch.charCodeAt(0) - 0xfee0),
  )
  if (!m || m === '-') return false
  if (/mail/i.test(m)) return true
  if (/寄修|邮寄|郵寄|邮修|寄件维修|邮寄维修/.test(m)) return true
  return false
}

/**
 * 维修方式标签样式类：寄修/邮寄/MAIL 主色橙 / 送店黄色
 * @param repairMethod 维修方式展示文案
 * @param serviceMode 后端枚举 MAIL / STORE，优先于文案解析
 */
export function getRepairMethodTagClass(
  repairMethod: string,
  serviceMode: 'MAIL' | 'STORE' | '' = '',
): string {
  const mode = String(serviceMode || '').trim().toUpperCase()
  if (mode === 'MAIL') return 'tag-method-mail-orange'
  if (mode === 'STORE') return 'tag-method-yellow'
  const m = (repairMethod || '').trim()
  if (!m || m === '-') return 'tag-method-neutral'
  if (isMailRepairMethodLabel(m)) return 'tag-method-mail-orange'
  if (/送店|到店/.test(m) || /^STORE$/i.test(m)) return 'tag-method-yellow'
  return 'tag-method-neutral'
}
