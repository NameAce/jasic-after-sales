import { request } from '../request';

/**
 * 鉴权接口分层说明：
 * - PC 主链路：`/auth/login`、`/auth/user-info`、`/auth/menus`、`/auth/choose-company`、`/auth/logout`
 * - mp-* 接口：兼容保留链路，默认不由 PC 登录入口触发
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

/**
 * 作用：用户名密码登录（PC 主链路），请求体对齐 `SysAuthController` 所接 `LoginDTO`（字段 `username` / `password`）。
 * @param userName - 用户名或手机号，提交前会与后端一致做 trim
 * @param password - 明文密码（后端仅校验非空，不做固定格式限制）
 * @returns {Promise} 请求封装结果
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchLogin(userName: string, password: string) {
  return request<Api.Auth.LoginResponse>({
    url: '/auth/login',
    method: 'post',
    data: {
      username: userName.trim(),
      password
    }
  });
}

/** 小程序登录（兼容保留；默认不由 PC 登录入口触发）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchMpLogin(data: Api.Auth.MpLoginParams) {
  return request<Api.Auth.LoginResponse>({
    url: '/auth/mp-login',
    method: 'post',
    data
  });
}

/** 小程序账号认领绑定并登录（兼容保留；默认不由 PC 登录入口触发）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchMpBindLogin(data: Api.Auth.MpBindLoginParams) {
  return request<Api.Auth.LoginResponse>({
    url: '/auth/mp-bind-login',
    method: 'post',
    data
  });
}

/** 小程序侧确认绑定并登录（兼容保留；默认不由 PC 登录入口触发）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchMpBindConfirm(data: Api.Auth.MpBindConfirmParams) {
  return request<Api.Auth.LoginResponse>({
    url: '/auth/mp-bind-confirm',
    method: 'post',
    data
  });
}

/** 获取当前用户信息（与 jasic-ui `GET /auth/user-info`、后端 `SysAuthController#getUserInfo` 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchGetUserInfo() {
  return request<Api.Auth.BackendUserInfo>({ url: '/auth/user-info', method: 'get' });
}

/** 拉取侧边鉴权菜单树（动态路由数据源）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchGetMenus() {
  return request<Api.Route.AuthMenusResponse>({ url: '/auth/menus', method: 'get' });
}

/** 后端要求选择公司主体时提交选择
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchChooseCompany(data: Api.Auth.ChooseCompanyParams) {
  return request<Api.Auth.BackendUserInfo>({
    url: '/auth/choose-company',
    method: 'post',
    data
  });
}

/** 与 jasic-ui `PUT /auth/profile`（updateProfile）一致；成功后返回最新用户信息（与 getUserInfo 结构相同）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchUpdateProfile(data: Partial<Api.Auth.BackendUserInfo>) {
  return request<Api.Auth.BackendUserInfo>({ url: '/auth/profile', method: 'put', data });
}

/** 修改当前用户密码
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchChangePassword(data: Api.Auth.ChangePasswordParams) {
  return request<null>({
    url: '/auth/change-password',
    method: 'put',
    data
  });
}

/** 与 jasic-ui `GET /auth/wechat-bind/status` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchGetWechatBindStatus() {
  return request<Api.Auth.WechatBindStatus>({ url: '/auth/wechat-bind/status', method: 'get' });
}

/** 与 jasic-ui `POST /auth/wechat-bind/qrcode` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchCreateWechatBindQrcode() {
  return request<Api.Auth.WechatBindQrcode>({ url: '/auth/wechat-bind/qrcode', method: 'post' });
}

/** 与 jasic-ui `POST /auth/wechat-bind/unbind` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchUnbindWechat(data?: Api.Auth.UnbindWechatParams) {
  return request<null>({ url: '/auth/wechat-bind/unbind', method: 'post', data });
}

/** 退出登录
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchLogout() {
  return request<null>({
    url: '/auth/logout',
    method: 'post'
  });
}

/**
 * Refresh token — `POST /auth/refresh-token`
 *
 * jasic-ui 侧无刷新链路；本仓库仅在 `VITE_AUTH_REFRESH_TOKEN=Y` 时由 `handleExpiredRequest` 调用，避免无接口环境误请求。
 *
 * @param refreshToken Refresh token
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchRefreshToken(refreshToken: string) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/refresh-token',
    method: 'post',
    data: {
      refreshToken
    }
  });
}

/**
 * 作用：按后端约定返回自定义错误（联调/测试用）。
 * @param code 错误码
 * @param msg 错误信息
 * @returns {Promise}
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function fetchCustomBackendError(code: string, msg: string) {
  return request({ url: '/auth/error', params: { code, msg } });
}
