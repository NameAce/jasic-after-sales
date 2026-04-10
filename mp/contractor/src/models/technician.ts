/** 派单/工作台可选维修员（与 AssignTechnicianModal 字段一致） */
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

