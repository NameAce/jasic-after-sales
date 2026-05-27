import { http } from '@/utils/http'

/**
 * 提交投诉与建议入参（对齐后端 `CustomerFeedbackCreateDTO` 预期字段）
 */
export interface SubmitFeedbackParams {
  /** 反馈正文 */
  content: string
  /** 已上传附件 URL 列表 */
  attachmentUrls?: string[]
}

/**
 * C 端提交投诉与建议
 *
 * 对应后端 `POST /api/customer/feedback`（待后端落地后可联调）。
 */
export const submitFeedback = (data: SubmitFeedbackParams) => {
  return http<null>({
    url: '/customer/feedback',
    method: 'POST',
    data,
  })
}
