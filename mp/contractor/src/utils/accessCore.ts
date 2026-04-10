/**
 * 纯函数权限判断（不依赖 Pinia，可供路由守卫、单测复用）
 * @param have 已具备的权限列表
 * @param perm 权限
 * @returns 是否具备 perm 权限
 */
export function permsInclude(have: readonly string[], perm: string): boolean {
  return have.includes(perm)
}

/**
 * 判断是否具备 need 中任一权限
 * @param have 已具备的权限列表
 * @param need 需要的权限列表
 * @returns 是否具备 need 中任一权限
 */
export function permsIncludeAny(have: readonly string[], need: readonly string[]): boolean {
  if (need.length === 0) return true
  return need.some((p) => have.includes(p))
}

/**
 * 判断是否具备 need 中全部权限
 * @param have 已具备的权限列表
 * @param need 需要的权限列表
 * @returns 是否具备 need 中全部权限
 */
export function permsIncludeAll(have: readonly string[], need: readonly string[]): boolean {
  if (need.length === 0) return true
  return need.every((p) => have.includes(p))
}
