/**
 * 权限标识常量 —— 与后端 `sys_menu.perms` 一一对应
 *
 * 命名约定（与 jasic-ui `v-hasPerms="['system:user:add']"` 风格一致）：
 *   `模块:实体:操作`（全端通用，PC 和小程序共用同一权限字符串）。
 *
 * 工单主线（与后端状态机一致）：待派单(assign) → 待接单(accept) → 维修中(IN_PROGRESS)：
 *   - 无故障：接单后可走机器返回方式 + 填关单原因关单(close)
 *   - 有故障（详情接单页）：接单后机器返回方式确认即关单(close)
 *   - 维修登记(repair)/复检(review) 仍可从列表等入口进入
 *   - 转单(transfer) 仅在维修中、已完成阶段可用
 *
 * 用法：`import { Perms } from '@/utils/permissions'`，
 * 配合 `userStore.hasPermission(Perms.XXX)`、`canAny([...])`、`canAll([...])` 在页面/组件内控制按钮与区块展示。
 * 路由仅校验登录（见 `routeGuard.ts`），不按权限码拦截页面进入。
 *
 * 阶段 4.4（`三端契约统一计划_5eaf1a62.plan.md`）：
 *   需要与后端 `sys_menu.perms` 实际文本 **完全对齐**。当前值按「工单模块」命名空间，
 *   仍沿用 `workorder:xxx` 前缀，待后端给出 `sys_menu.perms` 完整清单后再一次性替换。
 *   替换时只改 `Perms.*` 右值字符串，所有 `userStore.hasPermission(Perms.XXX)` 调用点自动生效。
 *
 * @todo 与后端对齐 `sys_menu.perms` 真实文本后，将下列 `workorder:xxx` 替换为后端字面值。
 *       如命名空间规范改为 `system:work-order:xxx`，也在此处同步更新。
 */
export const Perms = {
  /** 工单查询 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:list`） */
  WORKORDER_LIST: 'workorder:list',
  /** 工单新增 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:add`） */
  WORKORDER_ADD: 'workorder:add',
  /** 工单派单 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:assign`） */
  WORKORDER_ASSIGN: 'workorder:assign',
  /** 工单接单 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:accept`） */
  WORKORDER_ACCEPT: 'workorder:accept',
  /** 工单转单 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:transfer`） */
  WORKORDER_TRANSFER: 'workorder:transfer',
  /** 工单报价 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:quote`） */
  WORKORDER_QUOTE: 'workorder:quote',
  /** 维修登记 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:repair`） */
  WORKORDER_REPAIR: 'workorder:repair',
  /** 复检登记 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:review`） */
  WORKORDER_REVIEW: 'workorder:review',
  /** 工单关闭 · @todo 对齐后端 `sys_menu.perms`（建议 `system:work-order:close`） */
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
