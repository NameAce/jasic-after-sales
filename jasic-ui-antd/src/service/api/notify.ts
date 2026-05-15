/**
 * 站内消息与待办：列表、已读、数量统计等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { request } from '../request';

type IdLike = string | number;
type Query = Record<string, unknown>;

export interface NotifyTodoCountVO {
  count: number;
}

/** 与 jasic-ui notify 列表一致：分页为 pageNum/pageSize，box 区分待办与历史
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
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

/** 作用：查询待办消息数量角标。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getNotifyTodoCount() {
  return request<NotifyTodoCountVO>({ url: '/system/notify/todo/count', method: 'get' });
}

/** 作用：待办/历史消息分页列表。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getNotifyTodoPage(params?: NotifyMessageQuery) {
  return request<NotifyMessagePageResultVO>({ url: '/system/notify/todo/page', method: 'get', params });
}

/** 作用：将单条消息标记为已读。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function markNotifyMessageRead(messageId: IdLike) {
  return request({ url: `/system/notify/message/${messageId}/read`, method: 'post' });
}

/** 作用：按业务维度批量标记已读。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function markNotifyMessageReadByBiz(data: NotifyReadByBizDTO) {
  return request({ url: '/system/notify/message/read-by-biz', method: 'post', data });
}
