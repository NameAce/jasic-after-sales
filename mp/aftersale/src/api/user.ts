/**
 * 用户中心接口（个人资料、偏好设置等 `/api/customer/user/*` 端点的归集入口）
 *
 * 阶段 B：登录相关接口（`login / chooseCompany / getUserInfo / logout`）已迁出至
 * [api/auth.ts](./auth.ts)，以与 jasic-ui `src/api/auth.js` 对齐命名分域；
 * 本文件仅保留真正意义上的「用户中心」业务接口（如头像、昵称、偏好）。
 *
 * 若需新增用户中心相关 DTO / VO，请在此处扩展；
 * 登录相关内容请直接改 `api/auth.ts`。
 */

/** 用户基础展示信息 DTO（用户中心页展示用） */
export interface UserInfoDTO {
  id: string
  name: string
  mobile: string
  role: 'dispatcher' | 'engineer' | 'headquarters'
  avatar: string
}
