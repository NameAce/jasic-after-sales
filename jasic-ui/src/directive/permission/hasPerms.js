import store from '@/store'

export default {
  inserted(el, binding) {
    const { value } = binding
    const perms = store.getters && store.getters.perms

    if (value && value instanceof Array && value.length > 0) {
      const requiredPerms = value
      const hasPermission = perms.some(perm => requiredPerms.includes(perm))

      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('需要权限标识，如 v-hasPerms="[\'system:user:add\']"')
    }
  }
}
