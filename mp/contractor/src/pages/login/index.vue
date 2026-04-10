<template>
  <view class="page-login">
    <!-- 背景 & 英雄部分 -->
    <view class="bg-wrapper">
      <image class="bg-image" src="/static/images/login-bg.jpg" mode="aspectFill" />
      <view class="hero-vignette"></view>
    </view>

    <!-- 内容覆盖 -->
    <view class="content-overlay">
      <!-- 导航栏 -->
      <CustomNavBar
        title="承修方登录"
        surface="plain"
        tone="light"
        :color="'#ffffff'"
        :auto-back="false"
        @back="handleBack"
      />

      <view class="hero-content">
        <!-- 微标标签 -->
        <view class="badge">
          <view class="badge-dot"></view>
          <text class="badge-text">Technician Portal</text>
        </view>
        <text class="title">欢迎回来</text>
        <text class="subtitle">专业维修服务，高效响应需求</text>

        <!-- 账号密码登录 -->
        <view class="login-form">
          <text class="login-form-title">账号登录</text>
          <view class="input-card">
            <text class="input-label">用户名</text>
            <input
              v-model="username"
              class="input"
              placeholder="请输入用户名"
              placeholder-class="input-placeholder"
              :maxlength="64"
            />
          </view>
          <view class="input-card">
            <text class="input-label">密码</text>
            <input
              v-model="password"
              class="input"
              password
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
              :maxlength="64"
            />
          </view>
        </view>
      </view>

      <!-- 底部操作 -->
      <view class="bottom-actions bottom-glass">
        <view class="action-container">
          <!-- 登录按钮 -->
          <button class="login-btn btn-gradient group" :loading="isLoggingIn" @click="handleLogin">
            <text class="btn-text">{{ isLoggingIn ? '登录中...' : '账号密码登录' }}</text>
            <view v-if="!isLoggingIn" class="btn-icon">
              <uni-icons type="arrow-right" size="20" color="#fff"></uni-icons>
            </view>
            <view class="shine-effect"></view>
          </button>

          <!-- 条款 -->
          <view class="terms-container">
            <view class="checkbox-wrapper">
              <checkbox-group @change="handleTermsChange">
                <checkbox
                  value="1"
                  :checked="isTermsChecked"
                  color="#f26604"
                  style="transform: scale(0.7)"
                />
              </checkbox-group>
            </view>
            <view class="terms-text" :class="{ 'pulse-error': termsError }">
              <text class="terms-label" :class="{ 'text-error': termsError }">我已阅读并同意 </text>
              <text class="terms-link">《用户服务协议》</text>
              <text class="terms-label" :class="{ 'text-error': termsError }">、</text>
              <text class="terms-link">《隐私政策》</text>
              <text class="terms-label" :class="{ 'text-error': termsError }"> 以及 </text>
              <text class="terms-link">《承修方作业规范手册》</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { useUserStore } from '@/stores/modules/user'
  import { login as loginApi } from '@/api/auth'
  import { getApiMessage } from '@/utils/http'
  import type { CompanySimple, SysUserInfo } from '@/utils/permissions'

  // 用户商店
  const userStore = useUserStore()

  // 是否登录中
  const isLoggingIn = ref(false)
  // 是否同意条款
  const isTermsChecked = ref(false)
  // 条款错误
  const termsError = ref(false)
  // 用户名/密码
  const username = ref('')
  const password = ref('')

  /** 与 routeGuard 一致：有 token 则直接进入业务（冷启动首屏为登录页时） */
  onShow(() => {
    if (uni.getStorageSync('token')) {
      uni.switchTab({ url: '/pages/index/index' })
    }
  })

  /**
   * 处理返回
   * @returns void
   */
  const handleBack = () => {
    uni.navigateBack({
      fail: () => {
        // 承修方入口即登录页，无历史栈时退出小程序而非进入业务页
        uni.exitMiniProgram({
          fail: () => {
            /* H5 等环境无 exit 时忽略 */
          }
        })
      }
    })
  }

  /**
   * 处理条款变化
   * @param e 事件
   * @returns void
   */
  const handleTermsChange = (e: any) => {
    isTermsChecked.value = e.detail.value.length > 0
    if (isTermsChecked.value) {
      termsError.value = false
    }
  }

  /**
   * 处理登录
   * @returns void
   */
  const handleLogin = async () => {
    if (!isTermsChecked.value) {
      termsError.value = true
      setTimeout(() => {
        termsError.value = false
      }, 1200)
      return
    }

    if (!username.value.trim() || !password.value) {
      uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }

    isLoggingIn.value = true

    try {
      const loginRes = await loginApi({
        username: username.value.trim(),
        password: password.value
      })
      const result = loginRes.data

      let info = result.userInfo as SysUserInfo
      const companies = (result.companies?.length ? result.companies : info.companies) ?? []

      // 后端要求选择网点时：本地弹窗选择并回填 currentCompany*
      if (result.needChooseCompany && companies.length > 0) {
        const index = await new Promise<number>((resolve, reject) => {
          uni.showActionSheet({
            itemList: companies.map((c) => c.companyName),
            success: (res) => resolve(res.tapIndex),
            fail: (err) => reject(err)
          })
        }).catch(() => -1)

        if (index >= 0) {
          const chosen = companies[index] as CompanySimple
          info = {
            ...info,
            currentCompanyId: chosen.id,
            currentCompanyName: chosen.companyName,
            currentTypeCode: chosen.typeCode
          }
        } else {
          // 用户取消选择：不继续进入业务页
          return
        }
      }

      userStore.login(result.token, info)
      uni.showToast({ title: getApiMessage(loginRes, '登录成功'), icon: 'success' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/index/index' })
      }, 500)
    } catch (err) {
      const message = err instanceof Error ? err.message : '登录失败，请稍后重试'
      uni.showToast({ title: message || '登录失败，请稍后重试', icon: 'none' })
    } finally {
      isLoggingIn.value = false
    }
  }
</script>

<style lang="scss">
  .page-login {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: linear-gradient(180deg, #0f172a 0%, #020617 100%);
    background-color: $login-bg;
    position: relative;
    overflow: hidden;
    font-family:
      'Inter',
      'Noto Sans SC',
      system-ui,
      -apple-system,
      sans-serif;

    /* Background */
    .bg-wrapper {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 0;
      overflow: hidden;
    }

    .bg-image {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      opacity: 0.42;
      transform: scale(1.05);
      filter: contrast(1.18) saturate(1.08);
    }

    .hero-vignette {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: radial-gradient(
        circle at 30% 20%,
        rgba(15, 23, 42, 0.2) 0%,
        rgba(8, 12, 20, 0.9) 80%
      );
    }

    /* Content */
    .content-overlay {
      position: relative;
      z-index: 10;
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    /* Navbar */
    .nav-bar {
      padding-top: var(--status-bar-height, 0px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-left: 24px;
      padding-right: 24px;
      height: 80px;
    }

    .back-btn {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background-color: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: $surface-white;
      transition: all 0.2s;

      &:active {
        transform: scale(0.9);
        background-color: rgba(255, 255, 255, 0.1);
      }
    }

    .back-icon {
      width: 10px;
      height: 10px;
      border-left: 2px solid $surface-white;
      border-bottom: 2px solid $surface-white;
      transform: rotate(45deg);
      margin-left: 4px;
    }

    .nav-title {
      font-size: 15px;
      font-weight: 600;
      letter-spacing: 0.2em;
      color: rgba(255, 255, 255, 0.9);
      text-transform: uppercase;
    }

    .nav-placeholder {
      width: 40px;
    }

    /* Hero Content */
    .hero-content {
      flex-grow: 1;
      padding: 32px 32px 0;
    }

    .badge {
      display: inline-flex;
      align-items: center;
      flex-direction: row;
      gap: 8px;
      padding: 4px 12px;
      border-radius: 6px;
      background-color: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(12px);
      margin-bottom: 24px;
      width: max-content;
    }

    .badge-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: $primary;
      box-shadow: 0 0 8px rgba($primary, 0.8);
    }

    .badge-text {
      font-size: 10px;
      font-weight: bold;
      color: $surface-slate-300;
      text-transform: uppercase;
      letter-spacing: 0.15em;
    }

    .title {
      display: block;
      font-size: 34px;
      font-weight: bold;
      color: $surface-white;
      margin-bottom: 12px;
      letter-spacing: -0.025em;
      line-height: 1.2;
    }

    .subtitle {
      display: block;
      color: $text-slate-400;
      font-size: 14px;
      font-weight: normal;
      line-height: 1.6;
      letter-spacing: 0.025em;
    }

    /* Login Form */
    .login-form {
      margin-top: 90rpx;
      display: flex;
      flex-direction: column;
      gap: 14px;
      max-width: 440px;
      padding: 0 $space-sm;
    }

    .login-form-title {
      display: block;
      font-size: 13px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.5);
      letter-spacing: 0.1em;
      text-transform: uppercase;
      margin-bottom: 2px;
    }

    .input-card {
      padding: 8rpx $space-md;
      border-radius: $radius-md;
      background-color: #ffffff2e;
      border: 1.5px solid rgba(255, 255, 255, 0.08);
      @include flex-row;
      align-items: center;
      gap: 8px;
      transition:
        border-color 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;

      &:focus-within {
        border-color: rgba($primary, 0.68);
        box-shadow: 0 0 0 4px rgba($primary, 0.14);
        transform: translateY(-1px);
      }
    }

    .input-label {
      font-size: 26rpx;
      color: rgb(255 255 255 / 82%);
      letter-spacing: 0.08em;
      min-width: 120rpx;
    }

    .input {
      height: 80rpx;
      flex: 1;
      font-size: 15px;
      color: $text-main;
      background-color: rgb(255 255 255 / 0%);
      padding: 0 $space-md;
      border-radius: $radius-md;
      color: rgb(255 255 255 / 82%);
    }

    .input-placeholder {
      color: rgba(255, 255, 255, 0.5);
    }

    /* Bottom Actions */
    .bottom-actions {
      width: 100%;
      padding: 30px 32px calc(40px + env(safe-area-inset-bottom, 0px));
      margin-top: auto;
      border-top-left-radius: 40px;
      border-top-right-radius: 40px;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
      box-sizing: border-box;
    }

    .bottom-glass {
      background: linear-gradient(
        to bottom,
        rgba(15, 23, 42, 0),
        rgba(15, 23, 42, 0.4) 20%,
        rgba(15, 23, 42, 0.8) 100%
      );
      backdrop-filter: blur(12px);
    }

    .action-container {
      width: 100%;
      max-width: 384px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .login-btn {
      width: 100%;
      color: $surface-white;
      font-weight: bold;
      padding: 0;
      height: 56px;
      border-radius: 16px;
      box-shadow:
        0 10px 25px -5px rgba($primary, 0.4),
        0 8px 10px -6px rgba($primary, 0.4),
        inset 0 1px 1px rgba(255, 255, 255, 0.2);
      transition: all 0.3s;
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      overflow: hidden;
      border: none;

      &::after {
        border: none;
      }

      &:active {
        transform: scale(0.98);
        box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
      }
    }

    .btn-gradient {
      background: $login-gradient;
    }

    .btn-text {
      position: relative;
      z-index: 10;
      letter-spacing: 0.05em;
    }

    .btn-icon {
      position: relative;
      z-index: 10;
      margin-left: 12px;
      transition: transform 0.2s;
      display: flex;
      align-items: center;
    }

    .arrow-svg {
      width: 20px;
      height: 20px;
    }

    .login-btn:hover .btn-icon {
      transform: translateX(4px);
    }

    .shine-effect {
      position: absolute;
      top: 0;
      left: -100%;
      height: 100%;
      width: 50%;
      z-index: 5;
      transform: skewX(-12deg);
      background: linear-gradient(to right, transparent, rgba(255, 255, 255, 0.1), transparent);
      animation: shine 3s infinite ease-in-out;
    }

    @keyframes shine {
      0% {
        left: -100%;
      }
      20% {
        left: 200%;
      }
      100% {
        left: 200%;
      }
    }

    .terms-container {
      display: flex;
      flex-direction: row;
      align-items: flex-start;
      gap: 8px;
    }

    .checkbox-wrapper {
      padding-top: 2px;
    }

    .terms-text {
      flex: 1;
      font-size: 12px;
      line-height: 1.5;
    }

    .terms-label {
      color: $text-slate-400;
      transition: color 0.3s;
    }

    .terms-link {
      color: $primary;
      font-weight: 500;
      transition: color 0.2s;
      word-break: break-all;

      &:active {
        color: $primary-light;
      }
    }

    .text-error {
      color: $red-400 !important;
    }

    .pulse-error {
      animation: pulse 1.2s cubic-bezier(0.4, 0, 0.6, 1);
    }

    @keyframes pulse {
      0%,
      100% {
        opacity: 1;
      }
      50% {
        opacity: 0.5;
      }
    }

    @media (max-width: 375px) {
      .hero-content {
        padding: 22px 24px 0;
      }

      .title {
        font-size: 30px;
        margin-bottom: 10px;
      }

      .login-form {
        margin-top: 22px;
        gap: 12px;
      }

      .bottom-actions {
        padding: 24px 24px calc(34px + env(safe-area-inset-bottom, 0px));
        border-top-left-radius: 30px;
        border-top-right-radius: 30px;
      }
    }
  }
</style>
