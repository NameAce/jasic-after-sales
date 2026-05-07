/**
 * 静态资源（图片）统一管理
 *
 * 资源已上传到 OSS：`${VITE_OSS_BASE}/`
 * - 图标：`${VITE_OSS_BASE}/icon/xxx.png`
 * - 图片：`${VITE_OSS_BASE}/images/xxx.{png,jpg}`
 *
 * 注：微信小程序原生 tabBar 的 iconPath 不支持网络图，仍需保留在
 * `src/static/icon/` 本地，由 `pages.json` 引用。
 */

/** OSS 资源根地址（去除末尾斜杠） */
// 默认值用作兜底，避免新增/修改 .env 后忘记重启 dev server 时图片 404
const DEFAULT_OSS_BASE = 'https://jasic-after.oss-cn-shenzhen.aliyuncs.com/uniapp/contractor'
const OSS_BASE = String(import.meta.env.VITE_OSS_BASE || DEFAULT_OSS_BASE).replace(/\/+$/, '')

/**
 * 拼接 OSS 资源完整 URL
 * @param path 相对路径，可带或不带前导 `/`，例如 `/worker.png`
 */
export const ossAsset = (path: string): string => {
  const p = String(path || '').replace(/^\/+/, '')
  return OSS_BASE ? `${OSS_BASE}/${p}` : `/${p}`
}

/** 常用图片资源 */
export const ASSET_IMAGES = {
  /** 默认维修员形象（派单弹窗） */
  worker: ossAsset('/worker.png'),
  /** 默认用户头像 */
  defaultAvatar: ossAsset('/default-avatar.jpg'),
  /** 登录/绑定页背景 */
  loginBg: ossAsset('/login-bg.jpg')
} as const
