import { ref, type Ref } from 'vue'

const STORAGE_KEY = 'selectedServicePoint'

type StoredPoint = {
  id?: string | number
  centerId?: string | number
  companyName?: string
  distance?: string
}

/**
 * 作用：转换/构造：parseStoredPoint。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function parseStoredPoint(raw: unknown): StoredPoint | null {
  if (raw == null || raw === '') return null
  if (typeof raw === 'object' && raw !== null && !Array.isArray(raw)) {
    return raw as StoredPoint
  }
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw) as unknown
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return parsed as StoredPoint
      }
    } catch {
      return null
    }
  }
  return null
}

/**
 * 网点选择页回写：从缓存同步 centerId 与展示文案；需在页面中 `onShow(applyStorageSelection)`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useServicePointSelection(formData: Ref<{ centerId: string | number | null }>) {
  const selectedCenterDisplay = ref('')

  const applyStorageSelection = () => {
    const selected = parseStoredPoint(uni.getStorageSync(STORAGE_KEY))
    if (!selected) return

    const rawId = selected.id ?? selected.centerId
    if (rawId === undefined || rawId === null || rawId === '') return

    formData.value.centerId = typeof rawId === 'number' ? rawId : String(rawId)
    selectedCenterDisplay.value = `${selected.companyName ?? ''}${
      selected.distance ? ' (' + selected.distance + ')' : ''
    }`
    uni.removeStorageSync(STORAGE_KEY)
  }

  const clearServicePointSelection = () => {
    selectedCenterDisplay.value = ''
    try {
      uni.removeStorageSync(STORAGE_KEY)
    } catch {
      // ignore
    }
  }

  const hasPendingServicePointPick = () => parseStoredPoint(uni.getStorageSync(STORAGE_KEY)) != null

  return {
    selectedCenterDisplay,
    applyStorageSelection,
    clearServicePointSelection,
    hasPendingServicePointPick
  }
}
