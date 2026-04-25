/**
 * 微信「客户满意度评价通知」订阅消息模板 ID
 * （与公众平台模板「客户满意度评价通知」一致，后台 `wechat.notify.customer.evaluationInvite.templateId` 需配置同一 ID）
 */
export const WECHAT_TMPL_CUSTOMER_EVALUATION_INVITE =
  '01ZBgiyxkgui_wKWFtYsETnkSySMxeANaK2SoShvXkM'

/** `uni.requestSubscribeMessage` 的 tmplIds，单次最多 3 个 */
export const EVALUATION_INVITE_SUBSCRIBE_TMPL_IDS = [WECHAT_TMPL_CUSTOMER_EVALUATION_INVITE] as const
