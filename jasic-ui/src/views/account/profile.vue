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
              <el-button size="mini" type="danger" :loading="bindLoading" @click="handleUnbindWechat">解绑微信</el-button>
            </template>
            <template v-else>
              <el-alert
                title="当前账号尚未绑定微信"
                type="info"
                :closable="false"
                show-icon
              />
              <div class="bind-tip">
                请使用微信扫一扫下方二维码，在 B 端小程序中确认绑定。绑定时不会覆盖当前账号手机号，仅保存微信标识。
              </div>
              <div class="bind-qrcode-panel">
                <div v-if="bindQrImage" class="bind-qrcode-box">
                  <img :src="bindQrImage" alt="微信绑定二维码" class="bind-qrcode-image">
                </div>
                <div v-else class="bind-qrcode-placeholder">
                  {{ bindStatus.hasActiveTicket ? '当前存在有效二维码，如需继续绑定请重新生成。' : '点击下方按钮生成绑定二维码。' }}
                </div>
                <el-button size="mini" type="primary" @click="generateBindQrcode">
                  {{ bindStatus.hasActiveTicket ? '重新生成二维码' : '生成二维码' }}
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
  createWechatBindQrcode,
  getWechatBindStatus,
  unbindWechat,
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
      bindQrImage: '',
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
        maskedOpenid: '',
        wechatPhone: '',
        expireAt: '',
        hasActiveTicket: false
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
        const wasBound = this.bindStatus.bound
        this.bindStatus = Object.assign({}, this.bindStatus, res.data || {})
        if (this.bindStatus.bound || !this.bindStatus.hasActiveTicket) {
          this.bindQrImage = ''
        }
        if (!wasBound && this.bindStatus.bound) {
          this.$store.dispatch('user/getInfo')
          this.$message.success('微信绑定成功')
        }
      }).finally(() => {
        this.bindLoading = false
      })
    },
    generateBindQrcode() {
      this.bindLoading = true
      createWechatBindQrcode().then(res => {
        if (!res) return
        const wasBound = this.bindStatus.bound
        const data = res.data || {}
        this.bindStatus = Object.assign({}, this.bindStatus, data)
        this.bindQrImage = data.qrImageBase64 || ''
        if (this.bindStatus.bound) {
          this.$store.dispatch('user/getInfo')
          this.$message.success(wasBound ? '当前账号已绑定微信' : '微信绑定成功')
          return
        }
        if (this.bindQrImage) {
          this.$message.success('二维码已生成，请使用微信扫一扫')
        }
      }).finally(() => {
        this.bindLoading = false
      })
    },
    handleUnbindWechat() {
      const message = [
        '解绑后当前微信将无法登录 B 端小程序。',
        '解绑后当前账号将退出登录，如需继续使用请重新完成绑定。',
        '请输入当前密码确认解绑。'
      ].join('<br/>')
      this.$prompt(message, '解绑微信', {
        confirmButtonText: '确认解绑',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入当前密码',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入当前密码',
        dangerouslyUseHTMLString: true
      }).then(({ value }) => {
        this.bindLoading = true
        return unbindWechat({
          currentPassword: String(value || '').trim()
        }).then(res => {
          if (!res) return
          this.$message.success('微信解绑成功，请重新登录')
          return this.$store.dispatch('user/resetToken').then(() => {
            this.$router.push('/login')
          })
        }).finally(() => {
          this.bindLoading = false
        })
      }).catch(() => {})
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

.bind-qrcode-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 14px;
  margin: 18px 0 16px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #f8fafc;
  gap: 14px;
}

.bind-qrcode-box {
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #ebeef5;
}

.bind-qrcode-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.bind-qrcode-placeholder {
  width: 220px;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  text-align: center;
  line-height: 1.7;
  color: #909399;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: inset 0 0 0 1px #ebeef5;
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
