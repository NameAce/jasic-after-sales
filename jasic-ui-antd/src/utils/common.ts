/**
 * 通用纯函数：如将常量 Record 转为 options、HTML class 切换等页面无关工具。
 */
import { $t } from '@/locales';

/**
 * 将「键为 value、值为 label 文案」的对象转为下拉等组件使用的 options 数组。
 *
 * @param record - 键值对映射
 * @returns {CommonType.Option<keyof T, T[keyof T]>[]} value/label 选项列表
 * @example
 * ```ts
 * const record = { key1: 'label1', key2: 'label2' };
 * transformRecordToOption(record);
 * ```
 */
export function transformRecordToOption<T extends Record<string, string>>(record: T) {
  return Object.entries(record).map(([value, label]) => ({
    value,
    label
  })) as CommonType.Option<keyof T, T[keyof T]>[];
}

/**
 * 将选项中的 label 按 i18n key 翻译为当前语言文案。
 *
 * @param options - 含 i18n key 的选项数组
 * @returns {CommonType.Option<string>[]} 翻译后的选项
 */
export function translateOptions(options: CommonType.Option<string>[]) {
  return options.map(option => ({
    ...option,
    label: $t(option.label as App.I18n.I18nKey)
  }));
}

/**
 * 返回在 document.documentElement 上添加或移除指定 class 的工具方法。
 *
 * @param className - 要切换的 class 名
 * @returns {{ add: () => void; remove: () => void }} 添加与移除函数
 */
export function toggleHtmlClass(className: string) {
  function add() {
    document.documentElement.classList.add(className);
  }

  function remove() {
    document.documentElement.classList.remove(className);
  }

  return {
    add,
    remove
  };
}
