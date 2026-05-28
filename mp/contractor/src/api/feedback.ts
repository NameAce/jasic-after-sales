import { http } from '@/utils/http'

/**
 * 提交投诉与建议入参（对齐后端 `SysFeedbackCreateDTO` 预期字段）
 */
export interface SubmitFeedbackParams {
  /** 反馈正文 */
  content: string
  /** 已上传附件 URL 列表 */
  attachmentUrls?: string[]
}

/**
 * 反馈列表查询入参（按提交时间区间筛选）
 */
export interface FeedbackListQuery {
  /** 页码 */
  pageNum?: number
  /** 每页条数 */
  pageSize?: number
  /** 提交开始时间（`YYYY-MM-DD`，对齐后端 `beginCreateTime`） */
  beginCreateTime?: string
  /** 提交结束时间（`YYYY-MM-DD`，对齐后端 `endCreateTime`） */
  endCreateTime?: string
}

/**
 * 反馈列表项（兼容后端不同字段命名）
 */
export interface FeedbackRecordDTO {
  id?: number | string
  content?: string
  feedbackContent?: string
  createTime?: string
  submitTime?: string
  createdAt?: string
}

/**
 * 反馈分页结构
 */
export interface FeedbackListPageDTO {
  records?: FeedbackRecordDTO[]
  pageNum?: number
  pageSize?: number
  total?: number
}

/**
 * B 端（承修方）提交投诉与建议
 *
 * 对应后端 `POST /api/system/feedback`（待后端落地后可联调）。
 */
export const submitFeedback = (data: SubmitFeedbackParams) => {
  return http<null>({
    url: '/system/feedback',
    method: 'POST',
    data,
  })
}

/**
 * B 端（承修方）反馈分页列表查询
 *
 * 对应后端 `GET /api/system/feedback/my-list`。
 */
export const listFeedback = (params: FeedbackListQuery = {}) => {
  const beginCreateTime = String(params.beginCreateTime ?? '').trim()
  const endCreateTime = String(params.endCreateTime ?? '').trim()
  return http<FeedbackListPageDTO>({
    url: '/system/feedback/my-list',
    method: 'GET',
    data: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
      ...(beginCreateTime ? { beginCreateTime } : {}),
      ...(endCreateTime ? { endCreateTime } : {}),
    },
  })
}
