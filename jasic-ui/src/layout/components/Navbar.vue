<template>
  <div class="navbar">
    <div class="left-menu">
      <i
        :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
        class="hamburger"
        @click="$emit('toggle-sidebar')"
      />
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          {{ item.meta && item.meta.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="right-menu">
      <el-dropdown v-if="companies.length > 1" trigger="click" @command="handleSwitchCompany" style="margin-right: 16px;">
        <span class="el-dropdown-link company-switcher">
          {{ currentCompanyName }}<i class="el-icon-arrow-down el-icon--right" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item
            v-for="c in companies"
            :key="c.id"
            :command="c.id"
            :disabled="c.id === currentCompanyId"
          >
            {{ c.companyName }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="el-dropdown-link avatar-wrapper">
          <i class="el-icon-user-solid" />
          {{ userInfo.realName || userInfo.username || '用户' }}
          <i class="el-icon-arrow-down el-icon--right" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="logout">
            <i class="el-icon-switch-button" /> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Navbar',
  props: {
    isCollapse: { type: Boolean, default: false }
  },
  computed: {
    ...mapGetters(['userInfo', 'companies', 'currentCompanyId']),
    breadcrumbs() {
      return this.$route.matched.filter(item => item.meta && item.meta.title)
    },
    currentCompanyName() {
      const found = this.companies.find(c => c.id === this.currentCompanyId)
      return found ? found.companyName : '当前公司'
    }
  },
  methods: {
    handleCommand(command) {
      if (command === 'logout') {
        this.$confirm('确认退出登录？', '提示', { type: 'warning' }).then(() => {
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push('/login')
          })
        }).catch(() => {})
      }
    },
    handleSwitchCompany(companyId) {
      this.$confirm('切换公司后需要重新加载页面，确认切换？', '切换公司', { type: 'warning' }).then(() => {
        this.$store.dispatch('user/chooseCompany', companyId).then(() => {
          location.reload()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;

  .left-menu {
    display: flex;
    align-items: center;

    .hamburger {
      font-size: 20px;
      cursor: pointer;
      margin-right: 12px;
      color: #5a5e66;
      &:hover { color: #409EFF; }
    }
  }

  .right-menu {
    display: flex;
    align-items: center;

    .el-dropdown-link {
      cursor: pointer;
      color: #5a5e66;
      font-size: 14px;
      &:hover { color: #409EFF; }
    }

    .company-switcher {
      padding: 4px 8px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      font-size: 13px;
    }
  }
}
</style>
