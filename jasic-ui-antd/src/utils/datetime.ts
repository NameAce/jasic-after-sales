/**
 * 日期时间展示格式化：统一后端 LocalDateTime、ISO 8601、时间戳等在前端的展示口径。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import type { TableColumnType } from 'ant-design-vue';
import dayjs from 'dayjs';

/**
 * 默认日期时间展示格式
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

/**
 * 仅日期展示格式
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const DATE_FORMAT = 'YYYY-MM-DD';

/**
 * 最近连续 N 个自然日的日期区间（含今天），返回 yyyy-MM-dd。
 * 用于操作日志等「近 7 日」筛选与路由 query 同步。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getRecentDateRange(size = 7) {
  const end = dayjs();
  const start = end.subtract(size - 1, 'day');
  return {
    beginDate: start.format(DATE_FORMAT),
    endDate: end.format(DATE_FORMAT)
  };
}

/**
 * 将日期选择器的 yyyy-MM-dd 转为操作日志接口所需的起止时间。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function toOperLogQueryTime(dateStr: string, boundary: 'start' | 'end') {
  const d = String(dateStr || '').trim();
  if (!d) return undefined;
  if (d.length >= 19) return d.slice(0, 19);
  if (d.length === 10) return boundary === 'end' ? `${d} 23:59:59` : `${d} 00:00:00`;
  return d;
}

/**
 * 不按日期时间格式化的字段（如操作耗时毫秒）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const DATETIME_COLUMN_BLOCKLIST = new Set(['costTime']);

/**
 * 判断表格字段名是否应按日期/时间格式化展示。
 *
 * @param key - 列 key 或 dataIndex
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function isDateTimeColumnKey(key: string): boolean {
  const k = String(key || '').trim();
  if (!k || DATETIME_COLUMN_BLOCKLIST.has(k)) return false;
  return /(?:Time|Date)$/.test(k);
}

/**
 * 判断字符串是否像未格式化的日期时间（ISO、时间戳等）。
 *
 * @param value - 原始字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function looksLikeDateTime(value: string): boolean {
  const s = value.trim();
  if (!s || s === '-') return false;
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(s)) return false;
  if (/^\d{4}-\d{2}-\d{2}[T ]\d{1,2}:\d{2}/.test(s)) return true;
  if (/^\d{10}$/.test(s) || /^\d{13}$/.test(s)) return true;
  return false;
}

/**
 * 格式化为日期（YYYY-MM-DD）。
 *
 * @param value - 原始值
 * @param fallback - 空值占位
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatDate(value: unknown, fallback = '-'): string {
  if (value === null || value === undefined || value === '') return fallback;
  const s = String(value).trim();
  if (!s || s === '-') return fallback;
  const d = dayjs(s);
  if (!d.isValid()) return s;
  return d.format(DATE_FORMAT);
}

/**
 * 格式化为日期时间；若原始值仅含日期则只展示日期部分。
 *
 * @param value - 原始值
 * @param fallback - 空值占位
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatDateTime(value: unknown, fallback = '-'): string {
  if (value === null || value === undefined || value === '') return fallback;
  const s = String(value).trim();
  if (!s || s === '-') return fallback;
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return formatDate(s, fallback);
  const d = dayjs(s);
  if (!d.isValid()) return s;
  return d.format(DATETIME_FORMAT);
}

/**
 * 按字段名选择日期或日期时间格式。
 *
 * @param key - 字段名
 * @param value - 原始值
 * @param fallback - 空值占位
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatDateTimeField(key: string, value: unknown, fallback = '-'): string {
  const k = String(key || '');
  if (k.endsWith('Date') && !k.replace(/Date$/, '').endsWith('Time')) {
    return formatDate(value, fallback);
  }
  return formatDateTime(value, fallback);
}

/**
 * 通用展示格式化：空值占位；疑似日期时间则统一格式化。
 *
 * @param value - 原始值
 * @param fallback - 空值占位
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function formatDisplayValue(value: unknown, fallback = '-'): string {
  const text = String(value ?? '').trim();
  if (!text) return fallback;
  if (looksLikeDateTime(text)) return formatDateTime(text, fallback);
  return text;
}

/**
 * 详情描述行（label + value + key）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type DetailDescriptionRow = {
  key: string;
  label: string;
  value: unknown;
};

/**
 * 批量格式化详情描述行中的日期时间字段。
 *
 * @param rows - 描述行列表
 * @param fallback - 空值占位
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function mapDetailRowsDateTime<T extends DetailDescriptionRow>(rows: T[], fallback = '-'): T[] {
  return rows.map(row => {
    if (!isDateTimeColumnKey(row.key)) return row;
    const raw = row.value === fallback ? undefined : row.value;
    return {
      ...row,
      value: formatDateTimeField(row.key, raw, fallback)
    };
  });
}

/**
 * 为表格列中日期时间字段注入 customRender（已有 customRender 的列不覆盖）。
 *
 * @param columns - 原始列配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function applyDateTimeColumnRender<T = Record<string, unknown>>(
  columns: TableColumnType<T>[]
): TableColumnType<T>[] {
  return columns.map(col => {
    const key = String(col.key ?? col.dataIndex ?? '');
    if (!key || !isDateTimeColumnKey(key) || col.customRender) return col;
    const formatter = key.endsWith('Date') && !key.endsWith('Time') ? formatDate : formatDateTime;
    return {
      ...col,
      customRender: ({ text }: { text: unknown }) => formatter(text)
    };
  });
}
