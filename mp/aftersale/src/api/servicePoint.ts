import { http } from '@/utils/http'

/**
 * 附近网点接口原始项（`/api/customer/work-order/nearby-service-company-options`）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface NearbyServiceCompanyDTO {
  address: string
  companyCode: string
  companyName: string
  contactPhone: string
  distanceKm: number
  /**
 * 当前客户是否曾在该网点报修
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  hasRepairHistory?: boolean
  id: number
  latitude: number
  longitude: number
  typeCode: string
  typeName: string
}

export interface NearbyServiceCompanyParams {
  latitude: number
  longitude: number
  /**
 * 默认 20
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  limit?: number
}

export interface ServicePointDTO {
  id: number | string
  companyName: string
  address: string
  distance: string
  phone: string
  /**
 * 当前客户是否曾在该网点报修，用于展示「服务过」
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  hasRepairHistory: boolean
  latitude: number
  longitude: number
  companyCode?: string
  typeCode?: string
  typeName?: string
}

/**
 * 作用：转换/构造：formatDistanceKm。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function formatDistanceKm(km: number): string {
  if (!Number.isFinite(km) || km < 0) return '--'
  if (km < 1) return `${Math.round(km * 1000)}m`
  return `${km >= 10 ? Math.round(km) : km.toFixed(1)}km`
}

/**
 * 作用：转换/构造：mapNearbyToServicePoint。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function mapNearbyToServicePoint(item: NearbyServiceCompanyDTO): ServicePointDTO {
  return {
    id: item.id,
    companyName: item.companyName,
    address: item.address,
    distance: formatDistanceKm(item.distanceKm),
    phone: item.contactPhone,
    hasRepairHistory: Boolean(item.hasRepairHistory),
    latitude: item.latitude,
    longitude: item.longitude,
    companyCode: item.companyCode,
    typeCode: item.typeCode,
    typeName: item.typeName,
  }
}

const DEFAULT_NEARBY_LIMIT = 20

/**
 * 作用：加载/请求：listNearbyServiceCompanyOptions。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const listNearbyServiceCompanyOptions = (params: NearbyServiceCompanyParams) => {
  const { latitude, longitude, limit = DEFAULT_NEARBY_LIMIT } = params
  return http<NearbyServiceCompanyDTO[]>({
    url: '/customer/work-order/nearby-service-company-options',
    method: 'GET',
    data: { latitude, longitude, limit },
  })
}
