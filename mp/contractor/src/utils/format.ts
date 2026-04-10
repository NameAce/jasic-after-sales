/**
 * 格式化时间为 HH:MM 格式
 * @param date 日期
 * @returns 格式化后的时间
 */
export function formatTimeHHMM(date: Date = new Date()): string {
  return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}
