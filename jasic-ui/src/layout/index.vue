<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '210px'" class="layout-aside">
      <sidebar :is-collapse="isCollapse" />
    </el-aside>
    <el-container>
      <el-header class="layout-header" height="50px">
        <navbar :is-collapse="isCollapse" @toggle-sidebar="isCollapse = !isCollapse" />
      </el-header>
      <el-main class="layout-main">
        <router-view :key="$route.fullPath" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'

export default {
  name: 'Layout',
  components: { Sidebar, Navbar },
  data() {
    return {
      isCollapse: false
    }
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  .layout-aside {
    background: #304156;
    transition: width 0.3s;
    overflow: hidden;
  }
  .layout-header {
    padding: 0;
    border-bottom: 1px solid #e6e6e6;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  }
  .layout-main {
    background: #f0f2f5;
    padding: 16px;
    overflow-y: auto;
  }
}
</style>
