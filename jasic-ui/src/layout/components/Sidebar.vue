<template>
  <div class="sidebar-container">
    <div class="logo-container" :class="{ collapse: isCollapse }">
      <h1 v-if="!isCollapse">佳士售后</h1>
      <h1 v-else>佳</h1>
    </div>
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        unique-opened
        router
      >
        <sidebar-item
          v-for="route in routes"
          :key="route.path"
          :item="route"
          :base-path="route.path.startsWith('/') ? route.path : '/' + route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import SidebarItem from './SidebarItem.vue'

export default {
  name: 'Sidebar',
  components: { SidebarItem },
  props: {
    isCollapse: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    routes() {
      return this.$store.state.permission.routes.filter(r => !r.hidden)
    },
    activeMenu() {
      const { path } = this.$route
      return path
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  .logo-container {
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #263445;

    h1 {
      color: #fff;
      font-size: 16px;
      margin: 0;
      white-space: nowrap;
      letter-spacing: 2px;
    }

    &.collapse h1 {
      font-size: 18px;
      letter-spacing: 0;
    }
  }

  .el-scrollbar {
    flex: 1;
    overflow: hidden;
  }
}

::v-deep .el-menu {
  border-right: none;
}
</style>
