/**
 * Day.js 区域：将全局 locale 固定为 `zh-cn`（与界面中文一致）。
 */
import { locale } from 'dayjs';
import 'dayjs/locale/zh-cn';

/**
 * 作用：将 dayjs 全局语言设为中文（需已引入 `dayjs/locale/zh-cn`）。
 * @returns {void}
 */
export function setDayjsLocale() {
  // 仅保留中文环境的 dayjs locale
  locale('zh-cn');
}
