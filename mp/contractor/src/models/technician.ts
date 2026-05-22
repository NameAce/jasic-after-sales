/**
 * 派单/工作台可选维修员（与 AssignTechnicianModal 字段一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type Technician = {
  id: number
  name: string
  avatar: string
  isRecommend: boolean
  desc: string
  distance: string
  time: string
  isBusy: boolean
}

