/**
 * 承修方小程序：投诉与建议本地历史记录。
 * 说明：后端列表接口尚未落地前，先使用本地缓存支撑「已提交反馈」查看能力。
 */

/** 本地缓存 key（统一使用 jasic_ 前缀） */
const FEEDBACK_HISTORY_KEY = 'jasic_feedback_history'
/** 最多保留条数，避免本地缓存无限增长 */
const FEEDBACK_HISTORY_LIMIT = 50

/** 反馈历史条目 */
export interface FeedbackHistoryItem {
  id: string
  content: string
  createdAt: number
}

/**
 * 读取本地反馈历史，异常时返回空数组保证页面可用。
 */
export const loadFeedbackHistory = (): FeedbackHistoryItem[] => {
  try {
    const raw = uni.getStorageSync(FEEDBACK_HISTORY_KEY)
    if (!raw) return []
    const list = Array.isArray(raw) ? raw : []
    return list
      .map((item) => {
        const row = item as Partial<FeedbackHistoryItem>
        return {
          id: String(row.id ?? ''),
          content: String(row.content ?? ''),
          createdAt: Number(row.createdAt) || 0,
        }
      })
      .filter((item) => item.id && item.content && item.createdAt > 0)
  } catch {
    return []
  }
}

/**
 * 保存本地反馈历史。
 * @param list 历史记录数组
 */
export const saveFeedbackHistory = (list: FeedbackHistoryItem[]) => {
  uni.setStorageSync(FEEDBACK_HISTORY_KEY, list)
}

/**
 * 追加一条反馈记录（新记录置顶）。
 * @param content 反馈内容
 */
export const saveFeedbackRecord = (content: string) => {
  const text = String(content || '').trim()
  if (!text) return
  const current = loadFeedbackHistory()
  const next: FeedbackHistoryItem[] = [
    {
      id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      content: text,
      createdAt: Date.now(),
    },
    ...current,
  ].slice(0, FEEDBACK_HISTORY_LIMIT)
  saveFeedbackHistory(next)
}
