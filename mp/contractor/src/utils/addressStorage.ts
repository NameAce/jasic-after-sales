/**
 * 本地保存的收货地址（与 uni.chooseAddress 字段对齐便于导入）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type SavedAddress = {
  id: string
  name: string
  phone: string
  province: string
  city: string
  county: string
  detail: string
  /**
 * 服务端列表返回的完整地址文案，有则优先展示
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  fullAddress?: string
  /**
 * 是否默认（1=是，0=否），来自服务端列表时可能有
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function genAddressId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

/**
 * 加载收货地址
 * @returns 收货地址列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function saveAddresses(list: SavedAddress[]): void {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(list))
}

/**
 * 保存选中的收货地址
 * @param address 收货地址
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function saveSelectedShippingAddress(address: SavedAddress): void {
  const line =
    (address.fullAddress && String(address.fullAddress).trim()) ||
    `${address.province}${address.city}${address.county}${address.detail}`
  /**
 * 与 `takeSelectedShippingAddress` 一致：统一序列化为 string，避免 JSON 出现未加引号的数字导致读取端 typeof 校验失败
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const payload: SelectedShippingAddress = {
    id: String(address.id ?? '').trim(),
    name: String(address.name ?? '').trim(),
    phone: String(address.phone ?? '').trim(),
    fullAddress: String(line ?? '').trim()
  }
  uni.setStorageSync(SHIPPING_PICK_STORAGE_KEY, JSON.stringify(payload))
}

/**
 * 获取选中的收货地址
 * @returns 选中的收货地址
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function takeSelectedShippingAddress(): SelectedShippingAddress | null {
  try {
    const raw = uni.getStorageSync(SHIPPING_PICK_STORAGE_KEY) as string
    if (!raw || typeof raw !== 'string') return null
    const parsed = JSON.parse(raw) as unknown
    if (!parsed || typeof parsed !== 'object') {
      uni.removeStorageSync(SHIPPING_PICK_STORAGE_KEY)
      return null
    }
    const item = parsed as Record<string, unknown>
    const id = String(item.id ?? '').trim()
    const name = String(item.name ?? '').trim()
    const phone = String(item.phone ?? '').trim()
    const fullAddress = String(item.fullAddress ?? '').trim()
    if (!id || !name || !phone || !fullAddress) {
      uni.removeStorageSync(SHIPPING_PICK_STORAGE_KEY)
      return null
    }
    uni.removeStorageSync(SHIPPING_PICK_STORAGE_KEY)
    return { id, name, phone, fullAddress }
  } catch {
    return null
  }
}
