<template>
  <div class="app-container account-profile">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">基本信息</div>
          <el-form ref="profileForm" :model="profileForm" :rules="profileRules" label-width="88px">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="当前公司">
              <el-input :value="userInfo.currentCompanyName || '-'" disabled />
            </el-form-item>
            <el-form-item label="当前角色">
              <el-input :value="roleText" disabled />
            </el-form-item>
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input
                v-model="profileForm.currentPassword"
                type="password"
                show-password
                placeholder="请输入当前密码以确认修改"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="submitProfile">保存资料</el-button>
              <el-button @click="resetProfileForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">修改密码</div>
          <el-form ref="passwordForm" :model="passwordForm" :rules="passwordRules" label-width="88px">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="submitPassword">确认修改</el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header card-header--between">
            <span>微信绑定</span>
            <el-button size="mini" @click="refreshBindStatus">刷新状态</el-button>
          </div>
          <div v-loading="bindLoading" class="wechat-bind-panel">
            <template v-if="bindStatus.bound">
              <el-alert
                title="当前账号已绑定微信"
                type="success"
                :closable="false"
                show-icon
              />
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
              <el-alert
                title="当前账号尚未绑定微信"
                type="info"
                :closable="false"
                show-icon
              />
              <div class="bind-tip">
                请在 B 端小程序未绑定页面输入下方绑定码，完成微信绑定。绑定时不会覆盖当前账号手机号，仅保存微信标识。
              </div>
              <div class="bind-code-panel">
                <div class="bind-code-value">{{ bindStatus.bindCode || '------' }}</div>
                <el-button size="mini" type="primary" @click="generateBindCode">
                  {{ bindStatus.bindCode ? '重新生成' : '生成绑定码' }}
                </el-button>
              </div>
              <div class="bind-info-item">
                <span class="label">过期时间</span>
                <span class="value">{{ bindStatus.expireAt || '-' }}</span>
              </div>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  changePassword,
  createWechatBindCode,
  getWechatBindStatus,
  updateProfile
} from '@/api/auth'

export default {
  name: 'AccountProfile',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入新密码'))
        return
      }
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
        return
      }
      callback()
    }

    return {
      profileLoading: false,
      passwordLoading: false,
      bindLoading: false,
      bindPollTimer: null,
      bindSuccessNotified: false,
      profileForm: {
        username: '',
        realName: '',
        phone: '',
        email: '',
        currentPassword: ''
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      bindStatus: {
        bound: false,
        bindCode: '',
        maskedOpenid: '',
        wechatPhone: '',
        expireAt: ''
      },
      profileRules: {
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
        currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }]
      },
      passwordRules: {
        currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
        confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
      }
    }
  },
  computed: {
    ...mapGetters(['userInfo']),
    roleText() {
      const roles = (this.userInfo.roles || []).map(item => item.roleName).filter(Boolean)
      return roles.length ? roles.join('、') : '-'
    }
  },
  created() {
    this.initPage()
  },
  beforeDestroy() {
    this.clearBindPolling()
  },
  methods: {
    initPage() {
      this.loadUserInfo()
      this.refreshBindStatus()
    },
    loadUserInfo() {
      this.$store.dispatch('user/getInfo').then(info => {
        this.profileForm.username = info.username || ''
        this.profileForm.realName = info.realName || ''
        this.profileForm.phone = info.phone || ''
        this.profileForm.email = info.email || ''
        this.profileForm.currentPassword = ''
      })
    },
    resetProfileForm() {
      this.loadUserInfo()
      this.$nextTick(() => this.$refs.profileForm && this.$refs.profileForm.clearValidate())
    },
    submitProfile() {
      this.$refs.profileForm.validate(valid => {
        if (!valid) return
        this.profileLoading = true
        updateProfile({
          realName: this.profileForm.realName,
          phone: this.profileForm.phone,
          email: this.profileForm.email,
          currentPassword: this.profileForm.currentPassword
        }).then(res => {
          if (!res) return
          this.$message.success('资料修改成功')
          this.$store.commit('user/SET_USER_INFO', res.data || {})
          this.loadUserInfo()
        }).finally(() => {
          this.profileLoading = false
        })
      })
    },
    resetPasswordForm() {
      this.passwordForm = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      this.$nextTick(() => this.$refs.passwordForm && this.$refs.passwordForm.clearValidate())
    },
    submitPassword() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) return
        this.passwordLoading = true
        changePassword({
          currentPassword: this.passwordForm.currentPassword,
          newPassword: this.passwordForm.newPassword
        }).then(res => {
          if (!res) return
          this.$message.success('密码修改成功，请重新登录')
          this.$store.dispatch('user/resetToken').then(() => {
            this.$router.push('/login')
          })
        }).finally(() => {
          this.passwordLoading = false
        })
      })
    },
    refreshBindStatus() {
      this.bindLoading = true
      getWechatBindStatus().then(res => {
        if (!res) return
        this.bindStatus = Object.assign({}, this.bindStatus, res.data || {})
        if (this.bindStatus.bound) {
          this.handleBindSuccess()
        }
        this.syncBindPolling()
      }).finally(() => {
        this.bindLoading = false
      })
    },
    generateBindCode() {
      this.bindLoading = true
      createWechatBindCode().then(res => {
        if (!res) return
        this.bindStatus = Object.assign({}, this.bindStatus, res.data || {})
        this.bindSuccessNotified = false
        this.syncBindPolling()
      }).finally(() => {
        this.bindLoading = false
      })
    },
    syncBindPolling() {
      if (this.bindStatus.bound || !this.bindStatus.bindCode) {
        this.clearBindPolling()
        return
      }
      if (this.bindPollTimer) {
        return
      }
      this.bindPollTimer = setInterval(() => {
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
    clearBindPolling() {
      if (this.bindPollTimer) {
        clearInterval(this.bindPollTimer)
        this.bindPollTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.account-profile {
  padding: 0;
}

.section-card {
  margin-bottom: 16px;
}

.card-header {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-header--between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wechat-bind-panel {
  min-height: 220px;
}

.bind-tip {
  margin-top: 16px;
  color: #606266;
  line-height: 1.7;
}

.bind-code-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin: 18px 0 16px;
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
</style>
