<template>
  <div v-if="!item.hidden">
    <!-- 只有一个子菜单时直接展示 -->
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren)">
      <el-menu-item :index="resolvePath(onlyOneChild.path)">
        <i :class="onlyOneChild.meta && onlyOneChild.meta.icon || item.meta && item.meta.icon || 'el-icon-menu'" />
        <span slot="title">{{ onlyOneChild.meta && onlyOneChild.meta.title }}</span>
      </el-menu-item>
    </template>

    <!-- 多个子菜单 -->
    <el-submenu v-else :index="resolvePath(item.path)">
      <template slot="title">
        <i :class="item.meta && item.meta.icon || 'el-icon-menu'" />
        <span slot="title">{{ item.meta && item.meta.title }}</span>
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(child.path)"
      />
    </el-submenu>
  </div>
</template>

<script>
export default {
  name: 'SidebarItem',
  props: {
    item: { type: Object, required: true },
    basePath: { type: String, default: '' }
  },
  data() {
    return {
      onlyOneChild: null
    }
  },
  methods: {
    hasOneShowingChild(children = [], parent) {
      const showingChildren = children.filter(item => {
        if (item.hidden) return false
        this.onlyOneChild = item
        return true
      })
      if (showingChildren.length === 1) return true
      if (showingChildren.length === 0) {
        this.onlyOneChild = { ...parent, path: '', noShowingChildren: true }
        return true
      }
      return false
    },
    resolvePath(routePath) {
      if (/^(https?:|mailto:|tel:)/.test(routePath)) return routePath
      if (/^(https?:|mailto:|tel:)/.test(this.basePath)) return this.basePath
      if (routePath.startsWith('/')) return routePath
      if (!routePath) return this.basePath
      const base = this.basePath.endsWith('/') ? this.basePath : this.basePath + '/'
      return base + routePath
    }
  }
}
</script>
