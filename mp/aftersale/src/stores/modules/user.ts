import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CustomerUserInfo } from '@/models/user'

/**
 * C 端 `userInfo` 运行时类型
 *
 * 真源：后端 `CustomerUserInfoVO`（见 `@/models/user` `CustomerUserInfo`）。
 * C 端不存在 contractor/jasic-ui 的 `perms / roles / companies` 等字段，
 * 故此处直接复用 `CustomerUserInfo`，不再保留历史 `SysUserInfo` 兼容别名
 * （`name / mobile / role` 等已在页面统一迁移为 `nickname / phone`）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type UserInfo = CustomerUserInfo

export const useUserStore = defineStore(
  'user',
  () => {
    const userInfo = ref<UserInfo>()

    const setUserInfo = (val?: UserInfo) => {
      userInfo.value = val
    }

    const clearUserInfo = () => {
      userInfo.value = undefined
    }

    return {
      userInfo,
      setUserInfo,
      clearUserInfo,
    }
  },
  {
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
