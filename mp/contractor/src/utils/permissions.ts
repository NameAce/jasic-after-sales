/**
 * 权限标识常量 —— 与后端 sys_menu.perms 一一对应
 * 格式：模块:实体:操作（全端通用，PC 和小程序共用）
 *
 * 用法：import { Perms } from '@/utils/permissions'，
 * 配合 userStore.hasPermission(Perms.XXX)、canAny([...])、canAll([...]) 在页面/组件内控制按钮与区块展示。
 * 路由仅校验登录（见 `routeGuard.ts`），不按权限码拦截页面进入。
 */
export const Perms = {
  /** 工单查询 */
  WORKORDER_LIST: 'workorder:list',
  /** 工单新增 */
  WORKORDER_ADD: 'workorder:add',
  /** 工单派单 */
  WORKORDER_ASSIGN: 'workorder:assign',
  /** 工单接单 */
  WORKORDER_ACCEPT: 'workorder:accept',
  /** 工单转单 */
  WORKORDER_TRANSFER: 'workorder:transfer',
  /** 工单报价 */
  WORKORDER_QUOTE: 'workorder:quote',
  /** 维修登记 */
  WORKORDER_REPAIR: 'workorder:repair',
  /** 复检登记 */
  WORKORDER_REVIEW: 'workorder:review',
  /** 工单关闭 */
  WORKORDER_CLOSE: 'workorder:close',
} as const

/** 后端主体类型 */
export type SubjectType = 'PLATFORM' | 'HQ' | 'SERVICE'

/** 根据 typeCode 推导 subjectType */
export function getSubjectType(typeCode: string | undefined): SubjectType {
  if (!typeCode) return 'SERVICE'
  if (typeCode === 'PLATFORM') return 'PLATFORM'
  if (typeCode.startsWith('HQ')) return 'HQ'
  return 'SERVICE'
}

/** 后端公司简要信息 */
export interface CompanySimple {
  id: number
  companyName: string
  companyCode: string
  typeCode: string
  typeName: string
}

/** 对齐后端 SysUserVO */
export interface SysUserInfo {
  id: number
  username: string
  realName: string
  phone: string
  email?: string
  avatar: string
  sex?: number
  status?: number
  remark?: string
  currentCompanyId: number
  currentCompanyName: string
  currentTypeCode: string
  perms: string[]
  roles?: SysRoleInfo[]
  companies?: CompanySimple[]
}

/** 对齐后端 SysRoleVO（登录返回中 userInfo.roles） */
export interface SysRoleInfo {
  id: number
  roleKey: string
  roleName: string
  roleType?: number
  status?: number
  companyId?: number
  createTime?: string
  dataScope?: string
  isSystem?: number
  menuIds?: number[]
  orderNum?: number
  remark?: string
}

/** 对齐后端 LoginVO */
export interface LoginResult {
  token: string
  userInfo: SysUserInfo
  companies?: CompanySimple[]
  needChooseCompany?: boolean
}

/**
 * 预设角色及其权限配置（Mock 阶段使用）
 * 对接后端后由 /auth/login 接口返回
 */
export const MockRoles = {
  hqAdmin: {
    label: '总部管理员',
    desc: '总部全局管理视图',
    userInfo: {
      id: 1,
      username: 'hq_admin',
      realName: '李总监',
      phone: '13800000001',
      avatar: '',
      currentCompanyId: 10,
      currentCompanyName: '佳士总部A',
      currentTypeCode: 'HQ_A',
      perms: [
        Perms.WORKORDER_LIST,
        Perms.WORKORDER_ASSIGN,
        Perms.WORKORDER_ACCEPT,
        Perms.WORKORDER_REPAIR,
        Perms.WORKORDER_REVIEW,
        Perms.WORKORDER_ADD,
        Perms.WORKORDER_TRANSFER,
        Perms.WORKORDER_CLOSE,
      ],
    } satisfies SysUserInfo,
  },
  dispatcher: {
    label: '网点派单员',
    desc: '派单管理与转单',
    userInfo: {
      id: 2,
      username: 'dispatcher01',
      realName: '王调度',
      phone: '13800000002',
      avatar: '',
      currentCompanyId: 20,
      currentCompanyName: '深圳一级服务网点',
      currentTypeCode: 'FIRST',
      perms: [
        Perms.WORKORDER_LIST,
        Perms.WORKORDER_ASSIGN,
        Perms.WORKORDER_ACCEPT,
        Perms.WORKORDER_REPAIR,
        Perms.WORKORDER_REVIEW,
        Perms.WORKORDER_ADD,
        Perms.WORKORDER_TRANSFER,
        Perms.WORKORDER_CLOSE,
      ],
    } satisfies SysUserInfo,
  },
  engineer: {
    label: '维修工程师',
    desc: '接单、维修与复检',
    userInfo: {
      id: 3,
      username: 'engineer01',
      realName: '张师傅',
      phone: '13800000003',
      avatar: '',
      currentCompanyId: 20,
      currentCompanyName: '深圳一级服务网点',
      currentTypeCode: 'FIRST',
      perms: [
        Perms.WORKORDER_LIST,
        Perms.WORKORDER_ACCEPT,
        Perms.WORKORDER_REPAIR,
        Perms.WORKORDER_REVIEW,
      ],
    } satisfies SysUserInfo,
  },
} as const
