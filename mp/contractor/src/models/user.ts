import { permsInclude } from '@/utils/accessCore'
import type { SysUserInfo } from '@/utils/permissions'
import { Perms } from '@/utils/permissions'

export type UserProfile = {
  name: string
  titleTag: string
  idLabel: string
  idValue: string
  /** 登录后持久化在本地用户信息中的手机号 */
  phone: string
  avatar: string
}

/** 根据 SysUserInfo 构建展示用的 UserProfile */
export function buildUserProfile(info: SysUserInfo): UserProfile {
  return {
    name: info.realName,
    titleTag: getRoleTitleTag(info),
    idLabel: '员工ID:',
    idValue: String(info.id),
    phone: info.phone ?? '',
    avatar: info.avatar,
  }
}

function getRoleTitleTag(info: SysUserInfo): string {
  const code = info.currentTypeCode
  if (code?.startsWith('HQ')) return '总部管理员'
  if (permsInclude(info.perms, Perms.WORKORDER_ASSIGN)) return '派单员'
  return '维修工程师'
}

/** 个人中心功能菜单（link 可选，有则点击跳转） */
export type MyMenuItem = {
  icon: string
  label: string
  link?: string
}

export const DEFAULT_MY_MENU: MyMenuItem[] = [
  {
    icon: 'post_add',
    label: '建维修订单',
    link: '/pages/jasicRepair/index',
  },
  {
    icon: 'address',
    label: '地址管理',
    link: '/pages/address/index',
  },
  { icon: 'info', label: '关于我们' },
]

