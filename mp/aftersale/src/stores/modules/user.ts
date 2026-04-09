import { defineStore } from 'pinia'
import { ref } from 'vue'

export type UserRole = 'dispatcher' | 'engineer' | 'headquarters'

export interface UserInfo {
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
      // 调整为兼容多端的API
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
