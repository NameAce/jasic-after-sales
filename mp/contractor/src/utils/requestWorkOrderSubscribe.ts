import { WORKORDER_SUBSCRIBE_TMPL_IDS } from '@/constants/subscribeMessage'

type SubscribeResult = UniNamespace.GeneralCallbackResult

type SubscribeOpts = {
  tmplIds: string[]
  success?: (res: SubscribeResult) => void
  fail?: (err: SubscribeResult) => void
  complete?: (res: SubscribeResult) => void
}

const requestSubscribeByTemplateIds = (templateIds: string[]): Promise<SubscribeResult | void> => {
  return new Promise((resolve) => {
    const tmplIds = (Array.isArray(templateIds) ? templateIds : [])
      .map((id) => String(id || '').trim())
      .filter(Boolean)
    if (tmplIds.length === 0) {
      resolve()
      return
    }

    const uniAny = uni as UniNamespace.Uni & {
      requestSubscribeMessage?: (opts: SubscribeOpts) => void
    }
    const fn = uniAny.requestSubscribeMessage
    if (typeof fn !== 'function') {
      resolve()
      return
    }

    fn.call(uniAny, {
      tmplIds,
      complete: (res) => resolve(res)
    })
  })
}

/**
 * 请求工单相关订阅消息授权。
 * contractor 端在「提交报价」「转单确认」「维修完成」等点击且校验通过后、`await` 调用（仍在用户点击链路内）。
 * 失败或拒绝不抛错，不阻塞后续业务流程。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function requestWorkOrderSubscribe(): Promise<SubscribeResult | void> {
  return requestSubscribeByTemplateIds([...WORKORDER_SUBSCRIBE_TMPL_IDS])
}

/**
 * 作用：承修方小程序（网点/总部工单处理、派工）内方法：requestWorkOrderSubscribeWithTemplateIds。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function requestWorkOrderSubscribeWithTemplateIds(
  templateIds: string[]
): Promise<SubscribeResult | void> {
  return requestSubscribeByTemplateIds(templateIds)
}
