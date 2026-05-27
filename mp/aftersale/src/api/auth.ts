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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CustomerAuthLoginParams {
  /**
 * 微信 js_code；后端必填
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  code: string
  /**
 * 手机号授权 code；可空（空时跳过手机号绑定）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  phoneCode?: string
}

/**
 * 登录接口业务体（对齐后端 `CustomerLoginVO`）
 *
 * 后端 VO 仅包含 `token / userInfo`，不存在 contractor/jasic-ui 的
 * `needChooseCompany / companies` 多公司分支，故 C 端登录结果严格收敛为双字段。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const getUserInfo = () => {
  return http<CustomerUserInfo>({
    url: '/customer/auth/user-info',
    method: 'GET',
  })
}

/**
 * 客户资料修改入参（对齐后端 `CustomerProfileUpdateDTO`）
 *
 * 字段均可选；传 null 的字段后端不更新。小程序「我的」页保存时通常同时提交昵称与头像 URL。
 */
export interface CustomerProfileUpdateParams {
  nickname?: string
  avatar?: string
}

/**
 * 修改当前客户资料（C 端专属 `PUT /api/customer/auth/profile`）
 *
 * 成功后返回最新 `CustomerUserInfo`，调用方应写入 userStore。
 */
export const updateProfile = (data: CustomerProfileUpdateParams) => {
  return http<CustomerUserInfo>({
    url: '/customer/auth/profile',
    method: 'PUT',
    data,
  })
}

/**
 * 退出登录（C 端专属接口 `/api/customer/auth/logout`）
 *
 * 对应 jasic-ui `logout` 的 C 端实现。真源：
 * `jasic-customer/.../CustomerAuthController#logout`。
 *
 * @returns 空响应
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const logout = () => {
  return http<null>({
    url: '/customer/auth/logout',
    method: 'POST',
  })
}
