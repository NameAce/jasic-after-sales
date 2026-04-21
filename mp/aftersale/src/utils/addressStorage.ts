/** 本地保存的收货地址（与 uni.chooseAddress 字段对齐便于导入） */
export type SavedAddress = {
  id: string
  name: string
  phone: string
  province: string
  city: string
  county: string
  detail: string
  /** 服务端列表返回的完整地址文案，有则优先展示 */
  fullAddress?: string
  /** 是否默认（1=是，0=否），来自服务端列表时可能有 */
  isDefault?: number
  postalCode?: string
  nationalCode?: string
}

// 保存的收货地址
const STORAGE_KEY = 'user_shipping_addresses'
// 保存的选中的收货地址
const SHIPPING_PICK_STORAGE_KEY = 'selected_shipping_address_for_repair'

export type SelectedShippingAddress = {
  id: string
  name: string
  phone: string
  fullAddress: string
}

/**
 * 生成地址ID
 * @returns 地址ID
 */
export function genAddressId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

/**
 * 加载收货地址
 * @returns 收货地址列表
 */
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

/**
 * 保存收货地址
 * @param list 收货地址列表
 */
export function saveAddresses(list: SavedAddress[]): void {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(list))
}

/**
 * 保存选中的收货地址
 * @param address 收货地址
 */
export function saveSelectedShippingAddress(address: SavedAddress): void {
  const line =
    (address.fullAddress && address.fullAddress.trim()) ||
    `${address.province}${address.city}${address.county}${address.detail}`
  const payload: SelectedShippingAddress = {
    id: address.id,
    name: address.name,
    phone: address.phone,
    fullAddress: line
  }
  uni.setStorageSync(SHIPPING_PICK_STORAGE_KEY, JSON.stringify(payload))
}

/**
 * 获取选中的收货地址
 * @returns 选中的收货地址
 */
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
