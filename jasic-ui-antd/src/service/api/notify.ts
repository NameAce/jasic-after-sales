/**
 * 站内消息与待办：列表、已读、数量统计等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { request } from '../request';

type IdLike = string | number;
type Query = Record<string, unknown>;

interface PageResult<T> {
  total: number;
  records: T[];
}

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

/** 通知记录排障分页查询参数 */
export interface NotifyTraceQuery extends Query {
  pageNum?: number;
  pageSize?: number;
  bizType?: string;
  bizId?: number;
  bizNo?: string;
  sceneCode?: string;
  targetType?: string;
  eventStatus?: string;
  dispatchStatus?: string;
  resultCode?: string;
  beginTime?: string;
  endTime?: string;
}

/** 通知记录人工死信参数 */
export interface NotifyManualDeadDTO {
  reason: string;
}

/** 通知场景配置分页查询参数 */
export interface NotifySceneConfigQuery extends Query {
  pageNum?: number;
  pageSize?: number;
  sceneName?: string;
  sceneCode?: string;
  bizType?: string;
  targetType?: string;
}

/** 通知场景配置保存参数 */
export interface NotifySceneConfigSaveDTO {
  status?: number;
  remark?: string;
  targetConfigs?: NotifySceneTargetConfigDTO[];
}

/** 单条通知目标配置 */
export interface NotifySceneTargetConfigDTO {
  targetType: string;
  enabled?: number;
  titleTemplate?: string;
  contentTemplate?: string;
  routeType?: string;
  routeValueTemplate?: string;
  templateId?: string;
  channelScene?: string;
  pagePathTemplate?: string;
  fieldMapping?: NotifyChannelFieldMappingDTO[];
  remark?: string;
}

/** 小程序订阅消息字段映射 */
export interface NotifyChannelFieldMappingDTO {
  field?: string;
  value?: string;
}

/** 通知场景预览参数 */
export interface NotifyScenePreviewDTO {
  sceneCode: string;
  targetType: string;
  titleTemplate?: string;
  contentTemplate?: string;
  routeType?: string;
  routeValueTemplate?: string;
  templateId?: string;
  channelScene?: string;
  pagePathTemplate?: string;
  fieldMapping?: NotifyChannelFieldMappingDTO[];
  variables?: Record<string, string>;
}

/** 作用：分页查询通知记录（事件维度排障列表）。 */
export function getNotifyTracePage(params?: NotifyTraceQuery) {
  return request<PageResult<Query>>({ url: '/system/notify/trace/page', method: 'get', params });
}

/** 作用：查询通知事件详情（含站内产物与外部分发任务）。 */
export function getNotifyTraceEvent(eventId: IdLike) {
  return request<Query>({ url: `/system/notify/trace/event/${eventId}`, method: 'get' });
}

/** 作用：查询外部分发任务详情。 */
export function getNotifyTraceDispatch(dispatchId: IdLike) {
  return request<Query>({ url: `/system/notify/trace/dispatch/${dispatchId}`, method: 'get' });
}

/** 作用：人工重试通知事件。 */
export function retryNotifyTraceEvent(eventId: IdLike) {
  return request({ url: `/system/notify/trace/event/${eventId}/retry`, method: 'post' });
}

/** 作用：人工重试外部分发任务。 */
export function retryNotifyTraceDispatch(dispatchId: IdLike) {
  return request({ url: `/system/notify/trace/dispatch/${dispatchId}/retry`, method: 'post' });
}

/** 作用：人工将通知事件标记为死信。 */
export function deadNotifyTraceEvent(eventId: IdLike, data: NotifyManualDeadDTO) {
  return request({ url: `/system/notify/trace/event/${eventId}/dead`, method: 'post', data });
}

/** 作用：人工将外部分发任务标记为死信。 */
export function deadNotifyTraceDispatch(dispatchId: IdLike, data: NotifyManualDeadDTO) {
  return request({ url: `/system/notify/trace/dispatch/${dispatchId}/dead`, method: 'post', data });
}

/** 作用：查询通知场景配置页元数据（场景注册表、目标类型等）。 */
export function getNotifySceneOptions() {
  return request<Query>({ url: '/system/notify/scene/options', method: 'get' });
}

/** 作用：分页查询通知场景配置列表。 */
export function listNotifyScene(params?: NotifySceneConfigQuery) {
  return request<PageResult<Query>>({ url: '/system/notify/scene/list', method: 'get', params });
}

/** 作用：查询单个通知场景配置详情。 */
export function getNotifyScene(sceneCode: string) {
  return request<Query>({ url: `/system/notify/scene/${sceneCode}`, method: 'get' });
}

/** 作用：保存整个通知场景下的全部目标配置。 */
export function updateNotifyScene(sceneCode: string, data: NotifySceneConfigSaveDTO) {
  return request({ url: `/system/notify/scene/${sceneCode}`, method: 'put', data });
}

/** 作用：预览指定场景目标的模板渲染结果。 */
export function previewNotifyScene(data: NotifyScenePreviewDTO) {
  return request<Query>({ url: '/system/notify/scene/preview', method: 'post', data });
}
