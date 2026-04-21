import { http } from '@/utils/http'
import type { CustomerCompanySimple, UserInfo } from '@/stores/modules/user'

/**
 * 客户认证登录入参
 *
 * 短信验证码场景下 `code` 为验证码，`phoneCode` 为手机号；
 * 一键登录场景下 `code` 为 `wx.login` 返回，`phoneCode` 为 `getPhoneNumber` 返回。
 *
 * C 端专属白名单：对应 jasic-ui `login`（PC 侧 `/api/auth/login`）；
 * C 端改走 `/api/customer/auth/login`，与 contractor `/api/auth/mp-login` 并列为端侧专属登录端点。
 */
export interface CustomerAuthLoginParams {
  code: string
  phoneCode: string
}

/**
 * 登录接口业务体（对齐 jasic-ui `LoginVO` 的三端同形结构）
 *
 * - `token`：后端下发的 JWT
 * - `userInfo`：同 `/api/auth/user-info` 返回体，字段全部 optional 以兼容 C 端
 * - `needChooseCompany` / `companies`：用于多公司选择分支（C 端目前不使用但保留字段）
 *
 * 真源：[jasic-ui/src/store/modules/user.js](../../../../jasic-ui/src/store/modules/user.js) L50-67。
 */
export interface LoginResult {
  token: string
  userInfo: UserInfo
  needChooseCompany?: boolean
  companies?: CustomerCompanySimple[]
}

/** 选择公司入参（对齐 jasic-ui `chooseCompany` 与后端 `ChooseCompanyDTO`） */
export interface ChooseCompanyPayload {
  companyId: number
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
 * 登录后选择公司（当 `login` 返回 `needChooseCompany=true` 时调用）
 *
 * 对应 jasic-ui `chooseCompany`。真源：
 * [jasic-ui/src/api/auth.js](../../../../jasic-ui/src/api/auth.js) `chooseCompany`。
 * C 端目前不消费多公司分支，但仍保留该端点以与三端登录流同形。
 *
 * @param data 公司 id
 * @returns 用户信息（与 `LoginResult.userInfo` 同形）
 */
export const chooseCompany = (data: ChooseCompanyPayload) => {
  return http<UserInfo>({
    url: '/auth/choose-company',
    method: 'POST',
    data,
  })
}

/**
 * 拉取当前登录用户信息（与 contractor / jasic-ui 同路径）
 *
 * 对应 jasic-ui `getUserInfo`。真源：
 * [jasic-ui/src/api/auth.js](../../../../jasic-ui/src/api/auth.js) `getUserInfo`。
 * 典型调用点：`onLaunch` 时若本地有 token，先用该接口刷新最新 `perms` 与公司上下文；
 * C 端目前主要使用其中 `name / mobile / avatar` 字段，但仍保留 `perms / currentCompanyId` 等 optional 字段。
 *
 * @returns 用户信息（与 `LoginResult.userInfo` 同形）
 */
export const getUserInfo = () => {
  return http<UserInfo>({
    url: '/auth/user-info',
    method: 'GET',
  })
}

/**
 * 退出登录（对齐 jasic-ui `/api/auth/logout`，与 contractor 同路径）
 *
 * 对应 jasic-ui `logout`。真源：
 * [jasic-ui/src/api/auth.js](../../../../jasic-ui/src/api/auth.js) `logout`。
 * 该接口不依赖 `/api/customer/*` 客户专属命名空间。
 *
 * @returns 空响应
 */
export const logout = () => {
  return http<null>({
    url: '/auth/logout',
    method: 'POST',
  })
}
