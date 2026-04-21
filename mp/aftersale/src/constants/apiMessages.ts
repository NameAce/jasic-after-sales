/**
 * 统一错误文案字典（三端字面对齐）
 *
 * 真源：jasic-ui 错误提示文案；mp 双端镜像同字面。
 * 变更策略：如需调整文案，必须三端同步改；禁止任一端单边改动。
 */

/** 登录过期 / 401 提示 */
export const API_MSG_AUTH_EXPIRED = '登录已过期，请重新登录'
/** 无操作权限 */
export const API_MSG_NO_PERMISSION = '没有操作权限'
/** 业务失败默认文案（后端未回 msg 时使用） */
export const API_MSG_OPERATION_FAILED = '操作失败'
/** 网络错误默认文案（HTTP 非 2xx / 请求 fail 时使用） */
export const API_MSG_NETWORK_ERROR = '网络错误'
/** 请求超时 */
export const API_MSG_TIMEOUT = '请求超时'
/** 响应结构不合法（非 `{ code, msg, data }`） */
export const API_MSG_BAD_RESPONSE = '响应格式错误'
