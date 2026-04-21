import { useUserStore } from '@/stores/modules/user'
import { getApiMessage, type ApiResponse } from '@/utils/http'
import type { CompanySimple, LoginResult, SysUserInfo } from '@/utils/permissions'

/**
 * 小程序登录成功后的统一收尾：选公司、写 token、进首页（与登录页逻辑一致）
 */
export async function finalizeMpLoginSession(
  loginRes: ApiResponse<LoginResult>,
  result: LoginResult
) {
  const userStore = useUserStore()
  let info = result.userInfo as SysUserInfo
  const companies = (result.companies?.length ? result.companies : info.companies) ?? []

  if (result.needChooseCompany && companies.length > 0) {
    const index = await new Promise<number>((resolve, reject) => {
      uni.showActionSheet({
        itemList: companies.map((c) => c.companyName),
        success: (res) => resolve(res.tapIndex),
        fail: (err) => reject(err)
      })
    }).catch(() => -1)

    if (index >= 0) {
      const chosen = companies[index] as CompanySimple
      info = {
        ...info,
        currentCompanyId: chosen.id,
        currentCompanyName: chosen.companyName,
        currentTypeCode: chosen.typeCode
      }
    } else {
      return
    }
  }

  userStore.login(result.token, info)
  uni.showToast({ title: getApiMessage(loginRes, '登录成功'), icon: 'none', duration: 1500 })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/index/index' })
  }, 500)
}
