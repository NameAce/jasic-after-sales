/**
 * Day.js：注册 localeData 等插件并与项目 i18n 区域同步。
 */
import { extend } from 'dayjs';
import localeData from 'dayjs/plugin/localeData';
import { setDayjsLocale } from '../locales/dayjs';

/**
 * 作用：注册 dayjs 插件并同步为中文区域设置。
 * @returns {void}
 */
export function setupDayjs() {
  extend(localeData);

  setDayjsLocale();
}
