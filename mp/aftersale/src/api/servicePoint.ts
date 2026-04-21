import { http } from '@/utils/http'

/** 附近网点接口原始项（`/api/customer/work-order/nearby-service-company-options`） */
export interface NearbyServiceCompanyDTO {
  address: string
  companyCode: string
  companyName: string
  contactPhone: string
  distanceKm: number
  id: number
  latitude: number
  longitude: number
  typeCode: string
  typeName: string
}

export interface NearbyServiceCompanyParams {
  latitude: number
  longitude: number
  /** 默认 20 */
  limit?: number
}

export interface ServicePointDTO {
  id: number | string
  companyName: string
  address: string
  distance: string
  phone: string
  serviced: boolean
  latitude: number
  longitude: number
  companyCode?: string
  typeCode?: string
  typeName?: string
}

function formatDistanceKm(km: number): string {
  if (!Number.isFinite(km) || km < 0) return '--'
  if (km < 1) return `${Math.round(km * 1000)}m`
  return `${km >= 10 ? Math.round(km) : km.toFixed(1)}km`
}

export function mapNearbyToServicePoint(item: NearbyServiceCompanyDTO): ServicePointDTO {
  return {
    id: item.id,
    companyName: item.companyName,
    address: item.address,
    distance: formatDistanceKm(item.distanceKm),
    phone: item.contactPhone,
    serviced: false,
    latitude: item.latitude,
    longitude: item.longitude,
    companyCode: item.companyCode,
    typeCode: item.typeCode,
    typeName: item.typeName,
  }
}

const DEFAULT_NEARBY_LIMIT = 20

export const listNearbyServiceCompanyOptions = (params: NearbyServiceCompanyParams) => {
  const { latitude, longitude, limit = DEFAULT_NEARBY_LIMIT } = params
  return http<NearbyServiceCompanyDTO[]>({
    url: '/customer/work-order/nearby-service-company-options',
    method: 'GET',
    data: { latitude, longitude, limit },
  })
}
