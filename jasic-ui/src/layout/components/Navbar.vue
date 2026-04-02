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
          <el-dropdown-item command="bindWechat">
            <i class="el-icon-link" /> 绑定微信
          </el-dropdown-item>
          <el-dropdown-item command="logout">
            <i class="el-icon-switch-button" /> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <el-dialog
      title="绑定微信"
      :visible.sync="bindDialogVisible"
      width="520px"
      append-to-body
      @close="handleBindDialogClose"
    >
      <div v-loading="bindLoading" class="wechat-bind-dialog">
        <template v-if="bindStatus.bound">
          <div class="bind-status bind-status--success">
            当前账号已绑定微信
          </div>
          <div class="bind-info-item">
            <span class="label">微信标识</span>
            <span class="value">{{ bindStatus.maskedOpenid || '-' }}</span>
          </div>
          <div class="bind-info-item">
            <span class="label">微信手机号</span>
            <span class="value">{{ bindStatus.wechatPhone || '-' }}</span>
          </div>
        </template>
        <template v-else>
          <div class="bind-status">
            当前账号尚未绑定微信
          </div>
          <div class="bind-tip">
            请在 B 端小程序未绑定页面输入下方绑定码，完成微信绑定。
          </div>
          <div class="bind-code-panel">
            <div class="bind-code-value">{{ bindStatus.bindCode || '------' }}</div>
            <el-button size="mini" @click="generateBindCode">重新生成</el-button>
          </div>
          <div class="bind-info-item">
            <span class="label">过期时间</span>
            <span class="value">{{ bindStatus.expireAt || '-' }}</span>
          </div>
          <div class="bind-info-item">
            <span class="label">绑定说明</span>
            <span class="value">绑定时不会覆盖当前账号手机号，仅保存微信标识。</span>
          </div>
        </template>
      </div>
      <div slot="footer">
        <el-button @click="bindDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="refreshBindStatus">刷新状态</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { createWechatBindCode, getWechatBindStatus } from '@/api/auth'

export default {
  name: 'Navbar',
  props: {
    isCollapse: { type: Boolean, default: false }
  },
  data() {
    return {
      bindDialogVisible: false,
      bindLoading: false,
      bindPollTimer: null,
      bindSuccessNotified: false,
      bindStatus: {
        bound: false,
        bindCode: '',
        maskedOpenid: '',
        wechatPhone: '',
        expireAt: ''
      }
    }
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
      if (command === 'bindWechat') {
        this.openBindDialog()
        return
      }
      if (command === 'logout') {
        this.$confirm('确认退出登录？', '提示', { type: 'warning' }).then(() => {
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push('/login')
          })
        }).catch(() => {})
      }
    },
    openBindDialog() {
      this.bindDialogVisible = true
      this.bindSuccessNotified = false
      this.loadBindStatus(true)
    },
    refreshBindStatus() {
      this.loadBindStatus(false)
    },
    loadBindStatus(autoGenerate) {
      this.bindLoading = true
      getWechatBindStatus().then(res => {
        if (!res) return
        this.bindStatus = Object.assign({}, this.bindStatus, res.data || {})
        if (!this.bindStatus.bound && !this.bindStatus.bindCode && autoGenerate) {
          return this.generateBindCode()
        }
        this.syncBindPolling()
        if (this.bindStatus.bound) {
          this.handleBindSuccess()
        }
      }).finally(() => {
        this.bindLoading = false
      })
    },
    generateBindCode() {
      this.bindLoading = true
      return createWechatBindCode().then(res => {
        if (!res) return
        this.bindStatus = Object.assign({}, this.bindStatus, res.data || {})
        this.syncBindPolling()
      }).finally(() => {
        this.bindLoading = false
      })
    },
    syncBindPolling() {
      if (this.bindStatus.bound || !this.bindStatus.bindCode || !this.bindDialogVisible) {
        this.clearBindPolling()
        return
      }
      if (this.bindPollTimer) {
        return
      }
      this.bindPollTimer = setInterval(() => {
        if (!this.bindDialogVisible) {
          this.clearBindPolling()
          return
        }
        getWechatBindStatus().then(res => {
          if (!res) return
          this.bindStatus = Object.assign({}, this.bindStatus, res.data || {})
          if (this.bindStatus.bound) {
            this.handleBindSuccess()
          }
          if (this.bindStatus.bound || !this.bindStatus.bindCode) {
            this.clearBindPolling()
          }
        })
      }, 5000)
    },
    handleBindSuccess() {
      this.clearBindPolling()
      this.$store.dispatch('user/getInfo')
      if (!this.bindSuccessNotified) {
        this.bindSuccessNotified = true
        this.$message.success('微信绑定成功')
      }
    },
    handleBindDialogClose() {
      this.clearBindPolling()
    },
    clearBindPolling() {
      if (this.bindPollTimer) {
        clearInterval(this.bindPollTimer)
        this.bindPollTimer = null
      }
    },
    handleSwitchCompany(companyId) {
      this.$confirm('切换公司后需要重新加载页面，确认切换？', '切换公司', { type: 'warning' }).then(() => {
        this.$store.dispatch('user/chooseCompany', companyId).then(() => {
          location.reload()
        })
      }).catch(() => {})
    }
  },
  beforeDestroy() {
    this.clearBindPolling()
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

.wechat-bind-dialog {
  min-height: 160px;

  .bind-status {
    margin-bottom: 12px;
    font-size: 14px;
    color: #606266;

    &.bind-status--success {
      color: #67c23a;
      font-weight: 600;
    }
  }

  .bind-tip {
    margin-bottom: 16px;
    color: #909399;
    line-height: 1.6;
  }

  .bind-code-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 14px;
    margin-bottom: 16px;
    border: 1px dashed #dcdfe6;
    border-radius: 6px;
    background: #f8fafc;
  }

  .bind-code-value {
    font-size: 24px;
    font-weight: 600;
    letter-spacing: 4px;
    color: #303133;
  }

  .bind-info-item {
    display: flex;
    margin-bottom: 10px;
    line-height: 1.6;

    .label {
      width: 86px;
      color: #909399;
      flex-shrink: 0;
    }

    .value {
      color: #303133;
      word-break: break-all;
    }
  }
}
</style>
