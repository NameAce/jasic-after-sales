import { http } from '@/utils/http'
import type { CustomerUserInfo } from '@/models/user'

/**
 * 客户认证登录入参（对齐后端 `CustomerWechatLoginDTO`）
 *
 * - `code`：`wx.login` 返回的 js_code（微信会话码），@NotBlank
 * - `phoneCode`：`getPhoneNumber` 授权回调的 code；若缺省则后端不绑定手机号
 *
 * C 端专属白名单：对应 jasic-ui `login`（PC 侧 `/api/auth/login`）；
 * C 端走 `/api/customer/auth/login`，与 contractor `/api/auth/mp-login` 并列为端侧专属登录端点。
 */
export interface CustomerAuthLoginParams {
  /** 微信 js_code；后端必填 */
  code: string
  /** 手机号授权 code；可空（空时跳过手机号绑定） */
  phoneCode?: string
}

/**
 * 登录接口业务体（对齐后端 `CustomerLoginVO`）
 *
 * 后端 VO 仅包含 `token / userInfo`，不存在 contractor/jasic-ui 的
 * `needChooseCompany / companies` 多公司分支，故 C 端登录结果严格收敛为双字段。
 */
export interface LoginResult {
  token: string
  userInfo: CustomerUserInfo
}

/**
 * 客户认证登录
 *
 * 对应 jasic-ui `login`（C 端专属接口 `/api/customer/auth/login`）。
 * 与 contractor `/api/auth/mp-login` 并列为端侧专属登录端点，
 * 故函数名与 jasic-ui 保持一致（`login`），但 URL 字面保留 C 端命名空间。
 *
 * @param data 客户认证登录参数
 * @returns 客户认证登录结果
 */
export const login = (data: CustomerAuthLoginParams) => {
  return http<LoginResult>({
    url: '/customer/auth/login',
    method: 'POST',
    data,
  })
}

/**
 * 拉取当前登录用户信息（C 端专属接口 `/api/customer/auth/user-info`）
 *
 * 对应 jasic-ui `getUserInfo` 的 C 端实现。真源：
 * `jasic-customer/.../CustomerAuthController#getUserInfo`（返回 `CustomerUserInfoVO`）。
 * 仅返回 `userId / phone / nickname / avatar / needProfileComplete`，
 * 不含 contractor/jasic-ui 的 `perms / companies / roles` 等字段。
 *
 * @returns 用户信息
 */
export const getUserInfo = () => {
  return http<CustomerUserInfo>({
    url: '/customer/auth/user-info',
    method: 'GET',
  })
}

/**
 * 退出登录（C 端专属接口 `/api/customer/auth/logout`）
 *
 * 对应 jasic-ui `logout` 的 C 端实现。真源：
 * `jasic-customer/.../CustomerAuthController#logout`。
 *
 * @returns 空响应
 */
export const logout = () => {
  return http<null>({
    url: '/customer/auth/logout',
    method: 'POST',
  })
}
