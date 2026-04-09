import { http } from '@/utils/http'
import type { UserInfo } from '@/stores/modules/user'

export interface UserInfoDTO {
  id: string
  name: string
  mobile: string
  role: 'dispatcher' | 'engineer' | 'headquarters'
  avatar: string
}

export const getUserInfoAPI = () => {
  return http<UserInfoDTO>({
    url: '/user/info',
    method: 'POST',
  })
}

/** 客户认证登录：短信验证码场景下 code 为验证码，phoneCode 为手机号；一键登录场景下 code 为 wx.login 返回，phoneCode 为 getPhoneNumber 返回 */
export interface CustomerAuthLoginParams {
  code: string
  phoneCode: string
}

/** 登录接口业务体：token + userInfo（后端可能放在 data 字段，由 http 层归一为 result） */
export interface LoginResult {
  token: string
  userInfo: UserInfo
}

/** 
 * 客户认证登录
 * @param data 客户认证登录参数
 * @returns 客户认证登录结果
 */
export const loginAPI = (data: CustomerAuthLoginParams) => {
  return http<LoginResult>({
    url: '/api/customer/auth/login',
    method: 'POST',
    data,
  })
}

/** 
 * 客户认证退出登录（需携带 token，由 http 自动加 Authorization）
 * @returns 空响应
 */
export const logoutAPI = () => {
  return http<null>({
    url: '/api/customer/auth/logout',
    method: 'POST',
  })
}
