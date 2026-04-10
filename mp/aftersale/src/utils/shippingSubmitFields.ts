import type { SelectedShippingAddress } from '@/utils/addressStorage'

export type ShippingSubmitFields = {
  senderName: string
  senderMobile: string
  senderAddress: string
}

type ParseShippingInfoOptions = {
  fallbackName?: string
  fallbackMobile?: string
}

export function resolveShippingSubmitFields(
  selectedAddress: SelectedShippingAddress | null,
  shippingInfo: string,
  options?: ParseShippingInfoOptions
): ShippingSubmitFields {
  if (selectedAddress) {
    return {
      senderName: selectedAddress.name,
      senderMobile: selectedAddress.phone,
      senderAddress: selectedAddress.fullAddress
    }
  }
  const raw = String(shippingInfo ?? '').trim()
  const lines = raw
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
  const firstLine = lines[0] ?? ''
  const match = firstLine.match(/^(.+?)\s+(1\d{10})$/)
  return {
    senderName: match?.[1] ?? String(options?.fallbackName ?? ''),
    senderMobile: match?.[2] ?? String(options?.fallbackMobile ?? ''),
    senderAddress: lines.length > 1 ? lines.slice(1).join('') : raw
  }
}

export function resolveSendExpressNoForSubmit(raw: unknown): string {
  if (typeof raw === 'string' || typeof raw === 'number') {
    return String(raw).trim()
  }
  if (Array.isArray(raw)) {
    for (const item of raw) {
      if (!item || typeof item !== 'object') continue
      const row = item as Record<string, unknown>
      const candidate =
        row.sendExpressNo ?? row.expressNo ?? row.shippingNo ?? row.expressCode ?? row.code
      if (typeof candidate === 'string' && candidate.trim()) return candidate.trim()
      if (typeof candidate === 'number') return String(candidate)
    }
  }
  return ''
}
