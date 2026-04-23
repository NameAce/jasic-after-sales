import { http } from '@/utils/http'
import type { CompanySimple, LoginResult, SysUserInfo } from '@/utils/permissions'

export type LoginPayload = {
  username: string
  password: string
}

/** B 端小程序登录（与后端 MpLoginVO 对齐） */
export type MpLoginResult = {
  status?: string
  token?: string
  userInfo?: SysUserInfo
  companies?: CompanySimple[]
  needChooseCompany?: boolean
}

/** 与后端 MpLoginDTO 一致：微信 login 凭证 code + 可选手机号授权码 phoneCode（getPhoneNumber） */
export type MpLoginPayload = {
  code: string
  phoneCode?: string
}

/** 与后端 MpBindLoginDTO 一致：认领绑定并登录 */
export type MpBindLoginPayload = {
  code: string
  password: string
  phoneCode?: string
  usernameOrPhone: string
}

/** PC 扫码绑定确认（与后端 WechatBindConfirmDTO 一致） */
export type MpBindConfirmPayload = {
  bindTicket: string
  code: string
  phoneCode?: string
}

/**
 * 账号密码登录
 * @param payload LoginPayload
 * @returns ApiResponse<LoginResult>
 */
export async function login(payload: LoginPayload) {
  return http<LoginResult>({
    url: '/auth/login',
    method: 'POST',
    data: payload,
  })
}

/**
 * 微信小程序登录
 * @param payload MpLoginPayload
 * @returns ApiResponse<MpLoginResult>
 */
export async function mpLogin(payload: MpLoginPayload) {
  return http<MpLoginResult>({
    url: '/auth/mp-login',
    method: 'POST',
    data: payload,
    header: { 'Content-Type': 'application/json' },
  })
}

/**
 * B 端小程序账号认领绑定并登录
 * @param payload MpBindLoginPayload
 * @returns ApiResponse<MpLoginResult>
 */
export async function mpBindLogin(payload: MpBindLoginPayload) {
  return http<MpLoginResult>({
    url: '/auth/mp-bind-login',
    method: 'POST',
    data: payload,
    header: { 'Content-Type': 'application/json' },
  })
}

/**
 * PC 账号中心扫码：小程序确认绑定微信（/api/auth/mp-bind-confirm）
 * @param payload MpBindConfirmPayload
 * @returns ApiResponse<MpLoginResult>
 */
export async function mpBindConfirm(payload: MpBindConfirmPayload) {
  return http<MpLoginResult>({
    url: '/auth/mp-bind-confirm',
    method: 'POST',
    data: payload,
    header: { 'Content-Type': 'application/json' },
  })
}

/**
 * 登出
 * @returns void
 */
export async function logout() {
  await http<void>({
    url: '/auth/logout',
    method: 'POST',
  })
}

/** 选择公司入参（与后端 ChooseCompanyDTO 对齐） */
export type ChooseCompanyPayload = {
  companyId: number
}

/**
 * 登录后选择公司（当 `login`/`mpLogin` 返回 `needChooseCompany=true` 时调用）
 *
 * 真源：[jasic-ui/src/api/auth.js](../../../jasic-ui/src/api/auth.js) `chooseCompany`。
 * 返回的 `SysUserInfo` 与登录成功分支里 `LoginResult.userInfo` 同形，调用方应把 `data`
 * 存到 userStore，并以 `data.perms` 覆盖权限数组。
 *
 * @param payload 公司 id
 * @returns ApiResponse<SysUserInfo>
 */
export async function chooseCompany(payload: ChooseCompanyPayload) {
  return http<SysUserInfo>({
    url: '/auth/choose-company',
    method: 'POST',
    data: payload,
  })
}

/**
 * 拉取当前登录用户信息（含 perms / currentCompanyId / companies）
 *
 * 真源：[jasic-ui/src/api/auth.js](../../../jasic-ui/src/api/auth.js) `getUserInfo`。
 * 典型调用点：`onLaunch` 时若本地有 token，先用该接口刷新最新 perms 与公司上下文。
 *
 * @returns ApiResponse<SysUserInfo>
 */
export async function getUserInfo() {
  return http<SysUserInfo>({
    url: '/auth/user-info',
    method: 'GET',
  })
}
