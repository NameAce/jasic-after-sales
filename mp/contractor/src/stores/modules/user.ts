import { defineStore } from 'pinia'
import type { PiniaPluginContext } from 'pinia'
import { ref, computed } from 'vue'
import { permsInclude, permsIncludeAll, permsIncludeAny } from '@/utils/accessCore'
import { getSubjectType } from '@/utils/permissions'
import type { SysUserInfo, SubjectType } from '@/utils/permissions'
import { logout as logoutApi } from '@/api/auth'

// 经销商层级
export type DealerLevel = 'primary' | 'secondary'

// 用户商店
export const useUserStore = defineStore(
  'user',
  () => {
    // ========== 核心状态 ==========
    // Token
    const token = ref('')
    // 用户信息
    const userInfo = ref<SysUserInfo>()
    // 权限
    const permissions = ref<string[]>([])
    // 是否登录
    const isLoggedIn = computed(() => !!token.value)
    // 设置Token
    const setToken = (val: string) => {
      token.value = val
      uni.setStorageSync('token', val)
    }

    // ========== 权限判断（组件内配合 @/utils/permissions 的 Perms 使用，控制按钮等）==========
    // 是否具备指定权限码（与后端 sys_menu.perms 一致）
    const hasPermission = (perm: string) => permsInclude(permissions.value, perm)
    // 是否具备 required 中任一权限
    const canAny = (required: readonly string[]) => permsIncludeAny(permissions.value, required)
    // 是否具备 required 中全部权限
    const canAll = (required: readonly string[]) => permsIncludeAll(permissions.value, required)

    // ========== 主体类型 & 经销商层级 ==========
    // 主体类型
    const subjectType = computed<SubjectType>(() =>
      getSubjectType(userInfo.value?.currentTypeCode),
    )
    // 经销商层级（后端 SysAuthServiceImpl 写入 currentTypeCode 值为 SITE_FIRST / SITE_SECOND）
    const dealerLevel = computed<DealerLevel>(() => {
      const code = userInfo.value?.currentTypeCode
      if (code === 'SITE_SECOND') return 'secondary'
      return 'primary'
    })
    // 是否为主经销商
    const isPrimaryDealer = computed(() => dealerLevel.value === 'primary')
    // 是否为副经销商
    const isSecondaryDealer = computed(() => dealerLevel.value === 'secondary')

    // ========== 网点名称 ==========
    // 当前网点名称
    const currentNetworkName = computed(() => {
      return userInfo.value?.currentCompanyName ?? ''
    })

    // ========== 操作方法 ==========
    /**
     * 设置用户信息
     * @param val 用户信息
     * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    const setUserInfo = (val: SysUserInfo) => {
      userInfo.value = val
      if (val.perms) {
        permissions.value = [...val.perms]
      }
    }
    /**
     * 设置权限
     * @param val 权限
     * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    const setPermissions = (val: string[]) => {
      permissions.value = val
    }
    /**
     * 清除用户信息
     * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    const clearUserInfo = () => {
      token.value = ''
      userInfo.value = undefined
      permissions.value = []
      uni.removeStorageSync('token')
    }
    /**
     * 完整登录：设置 token + 用户信息 + 权限
     * @param loginToken 登录 token
     * @param info 用户信息
     * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    const login = (loginToken: string, info: SysUserInfo) => {
      setToken(loginToken)
      setUserInfo(info)
    }
    /**
     * 退出登录
     * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    const logout = async () => {
      try {
        await logoutApi()
      } finally {
        clearUserInfo()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }

    return {
      token,
      userInfo,
      permissions,
      isLoggedIn,
      hasPermission,
      canAny,
      canAll,
      subjectType,
      dealerLevel,
      isPrimaryDealer,
      isSecondaryDealer,
      currentNetworkName,
      setToken,
      setUserInfo,
      setPermissions,
      clearUserInfo,
      login,
      logout,
    }
  },
  {
    // 持久化存储
    persist: {
      // 阶段 5.4：新增的 B 端缓存统一走 `jasic_*` 前缀
      key: 'jasic_user_info',
      storage: {
        setItem(key, value) {
          uni.setStorageSync(key, value)
        },
        getItem(key) {
          return uni.getStorageSync(key)
        },
      },
      /**
 * 持久化恢复后对齐 permissions，避免仅有 userInfo.perms 时首页权限判断失败
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      afterRestore: (ctx: PiniaPluginContext) => {
        const store = ctx.store as {
          permissions?: string[]
          userInfo?: SysUserInfo
        }
        const perms = store.userInfo?.perms
        if (perms?.length && (!store.permissions || store.permissions.length === 0)) {
          store.permissions = [...perms]
        }
      },
    },
  },
)
