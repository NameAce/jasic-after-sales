import { http, unwrap } from '@/utils/http'
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
  const res = await http<LoginResult>({
    url: '/api/auth/login',
    method: 'POST',
    data: payload,
  })
  const code = String(res.code ?? '')
  if (code !== '00000' && code !== '0' && code !== '200') {
    throw new Error(String(res.msg ?? res.message ?? '登录失败'))
  }
  return {
    ...res,
    data: unwrap(res),
  }
}

/**
 * 微信小程序登录
 * @param payload MpLoginPayload
 * @returns ApiResponse<MpLoginResult>
 */
export async function mpLogin(payload: MpLoginPayload) {
  const res = await http<MpLoginResult>({
    url: '/api/auth/mp-login',
    method: 'POST',
    data: payload,
    header: { 'Content-Type': 'application/json' },
  })
  const code = String(res.code ?? '')
  if (code !== '00000' && code !== '0' && code !== '200') {
    throw new Error(String(res.msg ?? res.message ?? '登录失败'))
  }
  return {
    ...res,
    data: unwrap(res) as MpLoginResult,
  }
}

/**
 * B 端小程序账号认领绑定并登录
 * @param payload MpBindLoginPayload
 * @returns ApiResponse<MpLoginResult>
 */
export async function mpBindLogin(payload: MpBindLoginPayload) {
  const res = await http<MpLoginResult>({
    url: '/api/auth/mp-bind-login',
    method: 'POST',
    data: payload,
    header: { 'Content-Type': 'application/json' },
  })
  const code = String(res.code ?? '')
  if (code !== '00000' && code !== '0' && code !== '200') {
    throw new Error(String(res.msg ?? res.message ?? '绑定失败'))
  }
  return {
    ...res,
    data: unwrap(res) as MpLoginResult,
  }
}

/**
 * PC 账号中心扫码：小程序确认绑定微信（/api/auth/mp-bind-confirm）
 * @param payload MpBindConfirmPayload
 * @returns ApiResponse<MpLoginResult>
 */
export async function mpBindConfirm(payload: MpBindConfirmPayload) {
  const res = await http<MpLoginResult>({
    url: '/api/auth/mp-bind-confirm',
    method: 'POST',
    data: payload,
    header: { 'Content-Type': 'application/json' },
  })
  const code = String(res.code ?? '')
  if (code !== '00000' && code !== '0' && code !== '200') {
    throw new Error(String(res.msg ?? res.message ?? '绑定确认失败'))
  }
  return {
    ...res,
    data: unwrap(res) as MpLoginResult,
  }
}

/**
 * 登出
 * @returns void
 */
export async function logout() {
  await http<void>({
    url: '/api/auth/logout',
    method: 'POST',
  })
}

