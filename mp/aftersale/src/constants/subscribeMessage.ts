/**
 * 微信「客户满意度评价通知」订阅消息模板 ID
 * （与公众平台模板「客户满意度评价通知」一致，后台 `wechat.notify.customer.evaluationInvite.templateId` 需配置同一 ID）
 */
export const WECHAT_TMPL_CUSTOMER_EVALUATION_INVITE =
  '01ZBgiyxkgui_wKWFtYsETnkSySMxeANaK2SoShvXkM'

/** 微信订阅消息模板 ID（新增） */
export const WECHAT_TMPL_EXTRA_NOTICE_1 =
  '_p97aAe9-FJ2c6lCcZjVMQgxDnvBz8q6IRdFnnjIyWg'

/** 微信订阅消息模板 ID（新增） */
export const WECHAT_TMPL_EXTRA_NOTICE_2 =
  '0_vY_Wlie3dIuqmfpPAp_Hpbj-9yCso8yO1WSzWg3og'

/** `uni.requestSubscribeMessage` 的 tmplIds，单次最多 3 个 */
export const EVALUATION_INVITE_SUBSCRIBE_TMPL_IDS = [
  WECHAT_TMPL_CUSTOMER_EVALUATION_INVITE,
  WECHAT_TMPL_EXTRA_NOTICE_1,
  WECHAT_TMPL_EXTRA_NOTICE_2,
] as const
