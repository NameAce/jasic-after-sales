/**
 * 投诉与建议（反馈）后台管理接口：列表查询、受理、修改受理。
 */
import { request } from '../request';

export type FeedbackViewType = 'UNACCEPTED' | 'ACCEPTED' | 'ALL';

export interface FeedbackManageQuery {
  pageNum: number;
  pageSize: number;
  viewType: FeedbackViewType;
  contactPhone?: string;
  submitSourceName?: string;
  beginCreateTime?: string;
  endCreateTime?: string;
  beginAcceptTime?: string;
  endAcceptTime?: string;
}

export interface SysFeedbackVO {
  id: number;
  submitterName?: string;
  contactPhone?: string;
  submitSourceName?: string;
  content?: string;
  createTime?: string;
  acceptReply?: string;
  acceptUserName?: string;
  acceptTime?: string;
  status?: 'UNACCEPTED' | 'ACCEPTED';
}

export interface FeedbackAcceptDTO {
  id: number;
  acceptReply: string;
}

/** 后台管理列表：按视图类型与筛选条件分页查询。 */
export function listFeedbackManage(params: FeedbackManageQuery) {
  return request<PageResult<SysFeedbackVO>>({
    url: '/system/feedback/manage/list',
    method: 'get',
    params
  });
}

/** 首次受理反馈。 */
export function acceptFeedback(data: FeedbackAcceptDTO) {
  return request({
    url: '/system/feedback/manage/accept',
    method: 'post',
    data
  });
}

/** 修改已受理反馈的受理回复。 */
export function updateAcceptFeedback(data: FeedbackAcceptDTO) {
  return request({
    url: '/system/feedback/manage/update-accept',
    method: 'post',
    data
  });
}
