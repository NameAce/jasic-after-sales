<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>佳士售后管理系统</h2>
        <p>JASIC After-Sales Management</p>
      </div>
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="el-icon-user"
            placeholder="请输入用户名"
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%;"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="login-footer">
      <p>&copy; 2026 佳士科技 版权所有</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        this.$store.dispatch('user/login', this.loginForm)
          .then(data => {
            if (data.needChooseCompany) {
              this.$router.push('/choose-company').catch(() => {})
            } else {
              this.$router.push(this.redirect || '/').catch(() => {})
            }
          })
          .catch(() => {})
          .finally(() => { this.loading = false })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1d3557 0%, #457b9d 50%, #a8dadc 100%);

  .login-card {
    width: 400px;
    padding: 40px 36px 20px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);

    .login-header {
      text-align: center;
      margin-bottom: 30px;

      h2 {
        margin: 0 0 8px;
        font-size: 22px;
        color: #303133;
        letter-spacing: 2px;
      }

      p {
        margin: 0;
        font-size: 12px;
        color: #909399;
        letter-spacing: 1px;
      }
    }
  }

  .login-footer {
    margin-top: 24px;
    p {
      color: rgba(255, 255, 255, 0.7);
      font-size: 12px;
    }
  }
}
</style>
