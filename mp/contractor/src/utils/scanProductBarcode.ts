/**
 * 调起小程序扫码并返回产品条码（佳士报修商品查询共用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-26
 */

import { showApiToast } from '@/utils/uiFeedback'

export type ScanProductBarcodeResult =
  | { status: 'ok'; code: string }
  | { status: 'empty' }
  | { status: 'cancel' }

export type ScanProductBarcodeOptions = {
  /** 用户取消扫码时是否提示，默认 false */
  toastOnCancel?: boolean
}

/**
 * 扫码获取条码
 * @param options - 可选配置
 * @returns 扫码结果
 */
export function scanProductBarcode(
  options?: ScanProductBarcodeOptions
): Promise<ScanProductBarcodeResult> {
  const toastOnCancel = options?.toastOnCancel === true
  return new Promise((resolve) => {
    uni.scanCode({
      success: (res) => {
        const code = String(res.result ?? '').trim()
        if (code) {
          resolve({ status: 'ok', code })
          return
        }
        resolve({ status: 'empty' })
      },
      fail: () => {
        if (toastOnCancel) {
          void showApiToast('扫码已取消')
        }
        resolve({ status: 'cancel' })
      }
    })
  })
}
