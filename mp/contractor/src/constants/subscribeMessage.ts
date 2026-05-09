/**
 * 微信工单相关订阅消息模板 ID
 */
export const WECHAT_TMPL_WORKORDER_NOTICE_1 =
  'aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q'

export const WECHAT_TMPL_WORKORDER_NOTICE_2 =
  'JEO-zVGuWBQPIhU0ck7e3I97Tlr1tNk1ouxbbLovCCE'

export const WECHAT_TMPL_WORKORDER_NOTICE_3 =
  'mw7ebqsdXbJxdQf-A_9161z0CdEVRGSi_I-gQY3dONw'

/** 微信「派单通知」订阅消息模板 ID */
export const WECHAT_TMPL_WORKORDER_ASSIGN_NOTICE =
  'hhXhuNSWE4r98FbVMX8MfveAzBq3h7-QtfAMVOB2fTg'

/** `uni.requestSubscribeMessage` 的 tmplIds，单次最多 3 个 */
export const WORKORDER_SUBSCRIBE_TMPL_IDS = [
  WECHAT_TMPL_WORKORDER_NOTICE_1,
  WECHAT_TMPL_WORKORDER_NOTICE_2,
  WECHAT_TMPL_WORKORDER_NOTICE_3
] as const
