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
  /** 限定扫码类型；不传则使用系统默认（条码 + 二维码） */
  scanType?: Array<'barCode' | 'qrCode' | 'datamatrix' | 'pdf417'>
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
  const scanType = options?.scanType
  return new Promise((resolve) => {
    uni.scanCode({
      ...(scanType?.length ? { scanType } : {}),
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
