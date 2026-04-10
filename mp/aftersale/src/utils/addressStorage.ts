/** 本地保存的收货地址（与 uni.chooseAddress 字段对齐便于导入） */
export type SavedAddress = {
  id: string
  name: string
  phone: string
  province: string
  city: string
  county: string
  detail: string
  /** 是否默认（1=是，0=否），来自服务端列表时可能有 */
  isDefault?: number
  postalCode?: string
  nationalCode?: string
}

const STORAGE_KEY = 'user_shipping_addresses'
const SHIPPING_PICK_STORAGE_KEY = 'selected_shipping_address_for_repair'

export type SelectedShippingAddress = {
  id: string
  name: string
  phone: string
  fullAddress: string
}

export function genAddressId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

export function loadAddresses(): SavedAddress[] {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY) as string
    if (!raw || typeof raw !== 'string') return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed.filter(
      (x): x is SavedAddress =>
        x &&
        typeof x === 'object' &&
        typeof (x as SavedAddress).id === 'string' &&
        typeof (x as SavedAddress).name === 'string'
    )
  } catch {
    return []
  }
}

export function saveAddresses(list: SavedAddress[]): void {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(list))
}

export function saveSelectedShippingAddress(address: SavedAddress): void {
  const payload: SelectedShippingAddress = {
    id: address.id,
    name: address.name,
    phone: address.phone,
    fullAddress: `${address.province}${address.city}${address.county}${address.detail}`
  }
  uni.setStorageSync(SHIPPING_PICK_STORAGE_KEY, JSON.stringify(payload))
}

export function takeSelectedShippingAddress(): SelectedShippingAddress | null {
  try {
    const raw = uni.getStorageSync(SHIPPING_PICK_STORAGE_KEY) as string
    if (!raw || typeof raw !== 'string') return null
    uni.removeStorageSync(SHIPPING_PICK_STORAGE_KEY)
    const parsed = JSON.parse(raw) as unknown
    if (!parsed || typeof parsed !== 'object') return null
    const item = parsed as Partial<SelectedShippingAddress>
    if (
      typeof item.id !== 'string' ||
      typeof item.name !== 'string' ||
      typeof item.phone !== 'string' ||
      typeof item.fullAddress !== 'string'
    ) {
      return null
    }
    return {
      id: item.id,
      name: item.name,
      phone: item.phone,
      fullAddress: item.fullAddress
    }
  } catch {
    return null
  }
}
