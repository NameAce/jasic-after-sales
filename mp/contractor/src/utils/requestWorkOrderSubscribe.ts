import { WORKORDER_SUBSCRIBE_TMPL_IDS } from '@/constants/subscribeMessage'

type SubscribeResult = UniNamespace.GeneralCallbackResult

type SubscribeOpts = {
  tmplIds: string[]
  success?: (res: SubscribeResult) => void
  fail?: (err: SubscribeResult) => void
  complete?: (res: SubscribeResult) => void
}

/**
 * 请求工单相关订阅消息授权。
 * contractor 端在「提交报价」「维修完成」点击且校验通过后、`await` 调用（仍在用户点击链路内）。
 * 失败或拒绝不抛错，不阻塞后续业务流程。
 */
export function requestWorkOrderSubscribe(): Promise<SubscribeResult | void> {
  return new Promise((resolve) => {
    const tmplIds = [...WORKORDER_SUBSCRIBE_TMPL_IDS].map((id) => String(id || '').trim()).filter(Boolean)
    if (tmplIds.length === 0) {
      resolve()
      return
    }

    const opts: SubscribeOpts = {
      tmplIds,
      complete: (res) => resolve(res)
    }

    const wxBridge = (globalThis as unknown as { wx?: { requestSubscribeMessage?: (o: SubscribeOpts) => void } })
      .wx
    if (typeof wxBridge?.requestSubscribeMessage === 'function') {
      wxBridge.requestSubscribeMessage(opts)
      return
    }

    const uniAny = uni as UniNamespace.Uni & {
      requestSubscribeMessage?: (o: SubscribeOpts) => void
    }
    const fn = uniAny.requestSubscribeMessage
    if (typeof fn !== 'function') {
      resolve()
      return
    }
    fn.call(uniAny, opts)
  })
}
