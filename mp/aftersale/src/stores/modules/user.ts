import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CompanySimple, SysUserInfo } from '@/models/user'

export type UserRole = 'dispatcher' | 'engineer' | 'headquarters'

/**
 * C 端公司简要信息（`CompanySimple` 的历史别名，保留用于 `api/auth.ts` 的向后兼容）
 *
 * 阶段 C：本类型已迁至 `@/models/user` 的 `CompanySimple`（与 contractor 同形），
 * 此处仅保留 `type` 别名以免破坏已有引用点；后续 PR 将引用点逐步改为 `CompanySimple`。
 */
export type CustomerCompanySimple = CompanySimple

/**
 * C 端 `userInfo` 运行时类型
 *
 * 阶段 C（三层化）：主体形对齐后端 `SysUserVO`（见 `@/models/user` `SysUserInfo`，与
 * contractor 同形）；C 端继续附加若干兼容字段：
 * - `role / name / mobile`：C 端页面在历史实现中使用的别名字段，待页面统一迁移为
 *   `realName / phone` 后移除；保留 `optional` 不影响后端解包。
 * - `[key: string]: unknown`：防止后端扩字段时 C 端解包出错
 *   （见 `mp/MIRROR_FILE_PAIRS.md` L118「登录响应 shape」口径）。
 */
export type UserInfo = SysUserInfo & {
  role?: UserRole
  name?: string
  mobile?: string
  [key: string]: unknown
}

// 定义 Store
export const useUserStore = defineStore(
  'user',
  () => {
    // 用户信息
    const userInfo = ref<UserInfo>()

    // 保存用户信息，登录时使用
    const setUserInfo = (val?: UserInfo) => {
      userInfo.value = val
    }

    // 清理用户信息，退出时使用
    const clearUserInfo = () => {
      userInfo.value = undefined
    }

    return {
      userInfo,
      setUserInfo,
      clearUserInfo,
    }
  },
  // TODO: 持久化
  {
    // 配置持久化
    persist: {
      // 阶段 5.4：新增的 C 端缓存统一走 `jasic_*` 前缀
      key: 'jasic_user_info',
      storage: {
        setItem(key, value) {
          uni.setStorageSync(key, value)
        },
        getItem(key) {
          return uni.getStorageSync(key)
        },
      },
    },
  },
)
