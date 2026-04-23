/**
 * C 端用户 Model
 *
 * 真源：后端 `CustomerUserInfoVO` 与 `CustomerLoginVO`（`jasic-customer/.../domain/vo/`）。
 *
 * 口径（`mp/MIRROR_FILE_PAIRS.md` 「登录响应 shape」）：
 * - C 端后端接口 `GET /api/customer/auth/user-info` 与 `POST /api/customer/auth/login`
 *   返回体收敛为「客户」最小集，不含 contractor/jasic-ui 的多公司/角色/权限字段。
 * - 因此本模块不再复用 contractor 侧 `SysUserInfo / CompanySimple` 结构，
 *   以避免把后端不存在的字段（`perms / roles / companies / currentCompanyId` 等）
 *   反向渗透到 C 端页面逻辑中。
 */

/**
 * 后端 `CustomerUserInfoVO`
 *
 * 字段与 `jasic-customer/.../CustomerUserInfoVO.java` 严格一一对应。
 */
export interface CustomerUserInfo {
  /** 客户ID */
  userId?: number
  /** 手机号 */
  phone?: string
  /** 昵称 */
  nickname?: string
  /** 头像URL */
  avatar?: string
  /** 是否需要完善资料 */
  needProfileComplete?: boolean
}

/**
 * 后端 `CustomerLoginVO`
 *
 * 字段与 `jasic-customer/.../CustomerLoginVO.java` 严格一一对应。
 */
export interface LoginResult {
  token: string
  userInfo: CustomerUserInfo
}
