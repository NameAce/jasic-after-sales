/**
 * 操作日志：分页查询与详情导出等审计相关接口。
 */
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

/** 作用：分页查询操作日志。 */
export function listOperLog(params?: OperLogQuery) {
  return request<OperLogPageResult>({ url: '/log/oper-log/list', method: 'get', params });
}

/** 作用：按主键删除操作日志（支持批量逗号拼接）。 */
export function deleteOperLog(ids: IdLike | IdLike[]) {
  const idText = Array.isArray(ids) ? ids.join(',') : ids;
  return request({ url: `/log/oper-log/${idText}`, method: 'delete' });
}

/** 作用：清空操作日志。 */
export function cleanOperLog() {
  return request({ url: '/log/oper-log/clean', method: 'delete' });
}
