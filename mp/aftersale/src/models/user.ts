/**
 * C 端用户 Model（与 contractor `mp/contractor/src/utils/permissions.ts`
 * 的 `CompanySimple / SysUserInfo / LoginResult` 同形，字面镜像）
 *
 * 口径（`mp/MIRROR_FILE_PAIRS.md` 「登录响应 shape」L118）：
 * - 真源：后端 `GET /api/auth/user-info` 与 `POST /api/auth/mp-login-*` 返回体；
 *   jasic-ui `store/modules/user.js` 解包 `token / needChooseCompany / companies / userInfo`。
 * - 即使 C 端不消费 `perms / companies / roles`，字段也必须保留为 `optional`，
 *   防止后端扩字段时解包出错，并与 contractor 同形便于三端 grep。
 */

/** 后端公司简要信息（对齐 contractor `CompanySimple`） */
export interface CompanySimple {
  id: number
  companyName: string
  companyCode: string
  typeCode: string
  typeName: string
}

/** 后端 `SysRoleVO`（登录返回 `userInfo.roles`，对齐 contractor `SysRoleInfo`） */
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

/**
 * 后端 `SysUserVO`（对齐 contractor `SysUserInfo`，C 端不消费的字段保留为 optional）
 *
 * - `perms / roles / companies / currentCompanyId / currentCompanyName / currentTypeCode`
 *   C 端通常不消费，但保留字段形状以匹配后端真源与 contractor 双端。
 */
export interface SysUserInfo {
  id?: number
  username?: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  sex?: number
  status?: number
  remark?: string
  currentCompanyId?: number
  currentCompanyName?: string
  currentTypeCode?: string
  perms?: string[]
  roles?: SysRoleInfo[]
  companies?: CompanySimple[]
}

/** 后端 `LoginVO`（对齐 contractor `LoginResult`） */
export interface LoginResult {
  token: string
  userInfo: SysUserInfo
  companies?: CompanySimple[]
  needChooseCompany?: boolean
}
