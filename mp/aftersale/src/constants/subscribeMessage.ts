/**
 * 微信「客户满意度评价通知」订阅消息模板 ID
 * （小程序端仅负责向微信申请订阅；后台真实发送模板 ID 统一通过通知模板渠道配置维护）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WECHAT_TMPL_CUSTOMER_EVALUATION_INVITE =
  '01ZBgiyxkgui_wKWFtYsETnkSySMxeANaK2SoShvXkM'

/**
 * 微信订阅消息模板 ID（新增）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WECHAT_TMPL_EXTRA_NOTICE_1 =
  '_p97aAe9-FJ2c6lCcZjVMQgxDnvBz8q6IRdFnnjIyWg'

/**
 * 微信订阅消息模板 ID（新增）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WECHAT_TMPL_EXTRA_NOTICE_2 =
  '0_vY_Wlie3dIuqmfpPAp_Hpbj-9yCso8yO1WSzWg3og'

/**
 * `uni.requestSubscribeMessage` 的 tmplIds，单次最多 3 个
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const EVALUATION_INVITE_SUBSCRIBE_TMPL_IDS = [
  WECHAT_TMPL_CUSTOMER_EVALUATION_INVITE,
  WECHAT_TMPL_EXTRA_NOTICE_1,
  WECHAT_TMPL_EXTRA_NOTICE_2,
] as const
