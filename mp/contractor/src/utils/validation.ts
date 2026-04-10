/**
 * 中国大陆手机号正则
 * @returns 中国大陆手机号正则
 */ 
export const MOBILE_PATTERN = /^1[3-9]\d{9}$/

/**
 * 校验是否为合法中国大陆手机号（11位）
 * @param phone 手机号
 * @returns 是否为合法中国大陆手机号
 */
export function isValidCnMobile(phone: string): boolean {
  return /^1\d{10}$/.test(phone.trim())
}
