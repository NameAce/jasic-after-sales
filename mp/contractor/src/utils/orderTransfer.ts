/**
 * 由其他网点转入（存在转出方网点名）时，列表在质保标签后展示「转单」角标
 * @param transferFromSite 转出网点名称（接口返回；缺省时无法判断，不拦截）
 * @returns 是否由其他网点转入
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function hasInboundTransferFromSite(transferFromSite: string | undefined): boolean {
  return !!(transferFromSite && String(transferFromSite).trim())
}

/**
 * 已转单工单：当前登录网点为「转出网点」时不可进行接单/登记等操作；
 * 「被转单网点」（接收方）可操作。
 *
 * @param transferFromSite 转出网点名称（接口返回；缺省时无法判断，不拦截）
 * @param currentSiteName 当前登录网点名称（userInfo / 本地配置）
 * @returns 是否可以操作转单工单
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function canCurrentSiteOperateTransferredOrder(
  transferred: boolean,
  transferFromSite: string | undefined,
  currentSiteName: string | undefined
): boolean {
  if (!transferred) return true
  const from = (transferFromSite || '').trim()
  if (!from) return true
  const me = (currentSiteName || '').trim()
  if (!me) return true
  return me !== from
}
