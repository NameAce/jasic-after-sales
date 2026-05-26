import { useUserStore } from '@/stores/modules/user'
import { chooseCompany, logout as logoutApi } from '@/api/auth'
import { getApiMessage, type ApiResponse } from '@/utils/http'
import { showApiToast } from '@/utils/uiFeedback'
import type { CompanySimple, LoginResult, SysUserInfo } from '@/utils/permissions'

/**
 * 小程序登录成功后的统一收尾：选公司、写 token、进首页（与登录页逻辑一致）
 *
 * 关键契约（与后端 `SysAuthServiceImpl.doLogin` 对齐）：
 * - 单公司：后端已在 doLogin 里主动调了 `chooseCompany`，sa-token session
 *   里 `currentCompanyId / currentSubjectType / currentTypeCode / regionIds /
 *   effectiveDataScope / perms` 均已写入，前端只需把 token + userInfo 存本地。
 * - 多公司：后端返回 `needChooseCompany=true`，session 里上述字段**全部为空**，
 *   必须由前端显式调 `/auth/choose-company` 才会写入。否则后端所有依赖
 *   `SecurityContext.getCurrentCompanyId()` 的接口（工单详情/派单/维修登记/
 *   企业地址/文件上传等）都会因为拿到 null 走进"无权限/空结果"分支。
 *   因此本函数在多公司分支里：
 *     1) 先把 token 写本地（`http.ts` 从 storage 取 Authorization）
 *     2) 调 `chooseCompany({ companyId })` 让后端写入 session 上下文
 *     3) 用返回的 `SysUserInfo` 覆盖本地 info（含最新 perms 与 typeCode）
 *   任何一步失败都清掉本地 token，避免留下"半登录态"。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

    if (index < 0) {
      /**
       * 用户取消选公司：后端已 `StpUtil.login` 建了 session 但无公司上下文。
       * 临时写入 token 调一次 logout 让后端释放 session，再清理本地；
       * 任一步失败都忽略，保证用户能留在登录页重试。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      userStore.setToken(result.token)
      try {
        await logoutApi()
      } catch {
        /* 忽略登出失败，后端 session 到期自释放 */
      }
      userStore.clearUserInfo()
      return
    }

    const chosen = companies[index] as CompanySimple

    /**
 * http.ts 从 uni.getStorageSync('token') 取鉴权头，必须先写入
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    userStore.setToken(result.token)

    try {
      const chooseRes = await chooseCompany({ companyId: chosen.id })
      if (!chooseRes?.data) {
        /**
 * http 层对非 00000 已 toast；这里仅清理本地态即可
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
        userStore.clearUserInfo()
        return
      }
      info = chooseRes.data
    } catch {
      /**
 * reject 分支 http 层已展示 toast，避免重复提示；清 token 让用户重登
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      userStore.clearUserInfo()
      return
    }
  }

  userStore.login(result.token, info)
  // 等 toast 完整展示完再 reLaunch，避免提示还没看清页面就被替换
  await showApiToast(getApiMessage(loginRes, '登录成功'))
  uni.reLaunch({ url: '/pages/index/index' })
}
