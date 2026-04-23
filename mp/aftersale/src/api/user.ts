/**
 * 用户中心接口（个人资料、偏好设置等 `/api/customer/user/*` 端点的归集入口）
 *
 * 登录相关接口（`login / getUserInfo / logout`）位于 [api/auth.ts](./auth.ts)，
 * 对齐后端 `CustomerAuthController`（`/api/customer/auth/*`）。
 * 本文件仅保留真正意义上的「用户中心」业务接口（如资料完善、偏好）。
 */
