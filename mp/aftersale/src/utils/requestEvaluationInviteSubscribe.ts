import { EVALUATION_INVITE_SUBSCRIBE_TMPL_IDS } from '@/constants/subscribeMessage'

type SubscribeResult = UniNamespace.GeneralCallbackResult

/**
 * 在用户可评价场景下请求订阅「客户满意度评价通知」。
 * 须在用户点击等手势回调内调用，否则微信可能拒绝弹窗。
 * 失败或拒绝不抛错，不阻塞后续跳转等业务。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function requestEvaluationInviteSubscribe(): Promise<SubscribeResult | void> {
  return new Promise((resolve) => {
    const uniAny = uni as UniNamespace.Uni & {
      requestSubscribeMessage?: (opts: {
        tmplIds: string[]
        success?: (res: SubscribeResult) => void
        fail?: (err: SubscribeResult) => void
        complete?: (res: SubscribeResult) => void
      }) => void
    }
    const fn = uniAny.requestSubscribeMessage
    if (typeof fn !== 'function') {
      resolve()
      return
    }
    fn.call(uniAny, {
      tmplIds: [...EVALUATION_INVITE_SUBSCRIBE_TMPL_IDS],
      complete: (res) => resolve(res),
    })
  })
}
