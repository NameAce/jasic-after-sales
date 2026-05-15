/**
 * Day.js 区域：将全局 locale 固定为 `zh-cn`（与界面中文一致）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { locale } from 'dayjs';
import 'dayjs/locale/zh-cn';

/**
 * 作用：将 dayjs 全局语言设为中文（需已引入 `dayjs/locale/zh-cn`）。
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function setDayjsLocale() {
  // 仅保留中文环境的 dayjs locale
  locale('zh-cn');
}
