import { request } from '../request';

type IdLike = string | number;
type Query = Record<string, unknown>;

export interface NotifyTodoCountVO {
  count: number;
}

/** 与 jasic-ui notify 列表一致：分页为 pageNum/pageSize，box 区分待办与历史 */
export interface NotifyMessageQuery extends Query {
  pageNum?: number;
  pageSize?: number;
  box?: 'TODO' | 'HISTORY';
  bizType?: string;
  bizId?: number;
}

export interface NotifyMessagePageVO {
  id: number;
  title: string;
  summary?: string;
  bizType?: string;
  bizId?: number;
  bizNo?: string;
  routeType?: string;
  routeValue?: string;
  todoStatus?: string;
  invalidReason?: string;
  createTime?: string;
}

export interface NotifyMessagePageResultVO {
  total: number;
  records: NotifyMessagePageVO[];
}

export interface NotifyReadByBizDTO {
  bizType: string;
  bizId: number;
}

export function getNotifyTodoCount() {
  return request<NotifyTodoCountVO>({ url: '/system/notify/todo/count', method: 'get' });
}

export function getNotifyTodoPage(params?: NotifyMessageQuery) {
  return request<NotifyMessagePageResultVO>({ url: '/system/notify/todo/page', method: 'get', params });
}

export function markNotifyMessageRead(messageId: IdLike) {
  return request({ url: `/system/notify/message/${messageId}/read`, method: 'post' });
}

export function markNotifyMessageReadByBiz(data: NotifyReadByBizDTO) {
  return request({ url: '/system/notify/message/read-by-biz', method: 'post', data });
}
