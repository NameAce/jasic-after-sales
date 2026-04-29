import { request } from '../request';

type IdLike = string | number;
type Query = Record<string, unknown>;

export interface OperLogQuery extends Query {
  /** 与 jasic-ui `views/log/operLog/index.vue` 一致 */
  pageNum?: number;
  pageSize?: number;
  title?: string;
  operType?: number;
  /** jasic 字段名 `operUserName` */
  operUserName?: string;
  status?: 0 | 1;
  beginTime?: string;
  endTime?: string;
}

export interface OperLogVO {
  id: number;
  title?: string;
  operType?: number;
  method?: string;
  requestMethod?: string;
  requestUrl?: string;
  requestParam?: string;
  responseResult?: string;
  userId?: number;
  /** jasic 列表列「操作人」 */
  operUserName?: string;
  /** 兼容旧字段 */
  username?: string;
  companyId?: number;
  ip?: string;
  status?: number;
  errorMsg?: string;
  operTime?: string;
  costTime?: number;
}

export interface OperLogPageResult {
  total: number;
  records: OperLogVO[];
}

export function listOperLog(params?: OperLogQuery) {
  return request<OperLogPageResult>({ url: '/log/oper-log/list', method: 'get', params });
}

export function deleteOperLog(ids: IdLike | IdLike[]) {
  const idText = Array.isArray(ids) ? ids.join(',') : ids;
  return request({ url: `/log/oper-log/${idText}`, method: 'delete' });
}

export function cleanOperLog() {
  return request({ url: '/log/oper-log/clean', method: 'delete' });
}
