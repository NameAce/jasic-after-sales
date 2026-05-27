<template>
  <view class="page-login">
    <!-- 背景 & 英雄部分 -->
    <view class="bg-wrapper">
      <image class="bg-image" :src="ASSET_IMAGES.loginBg" mode="aspectFill" />
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

        <text class="login-hint">使用微信一键登录承修方账号</text>
      </view>

      <!-- 底部操作 -->
      <view class="bottom-actions bottom-glass">
        <view class="action-container">
          <!-- 登录按钮：微信小程序内先 getPhoneNumber，再 uni.login 取 code，POST /api/auth/mp-login -->
          <!-- #ifdef MP-WEIXIN -->
          <button
            v-if="isTermsChecked"
            class="login-btn btn-gradient group login-btn-native"
            open-type="getPhoneNumber"
            :loading="isLoggingIn"
            @getphonenumber="handleWeixinMpLogin"
          >
            <text class="btn-text">{{ isLoggingIn ? '登录中...' : '手机号一键登录' }}</text>
            <view v-if="!isLoggingIn" class="btn-icon">
              <uni-icons type="phone" size="20" color="#fff"></uni-icons>
            </view>
            <view class="shine-effect"></view>
          </button>
          <button v-else class="login-btn btn-gradient group" @click="promptTermsFirst">
            <text class="btn-text">手机号一键登录</text>
            <view class="btn-icon">
              <uni-icons type="phone" size="20" color="#fff"></uni-icons>
            </view>
            <view class="shine-effect"></view>
          </button>
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <button
            class="login-btn btn-gradient group"
            :loading="isLoggingIn"
            @click="handleNonWeixinLogin"
          >
            <text class="btn-text">手机号一键登录</text>
            <view v-if="!isLoggingIn" class="btn-icon">
              <uni-icons type="phone" size="20" color="#fff"></uni-icons>
            </view>
            <view class="shine-effect"></view>
          </button>
          <!-- #endif -->

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

    <!-- UNBIND：认领绑定（账号密码 + 再次 wx code，带首次 phoneCode）；不可绕过进入业务，仅可绑定成功或退出小程序 -->
    <view v-if="showBindPanel" class="bind-overlay">
      <view class="bind-card" @click.stop>
        <text class="bind-title">绑定承修方账号</text>
        <text class="bind-desc"
          >未绑定已有承修方账号将无法使用本小程序。请输入用户名或手机号与登录密码完成认领。</text
        >
        <view class="bind-input-row">
          <input
            v-model="bindUsername"
            class="bind-input"
            :class="{ 'bind-input--suffix': bindUsername.length > 0 }"
            placeholder="用户名或手机号"
            placeholder-class="bind-placeholder"
          />
          <view v-if="bindUsername.length > 0" class="bind-input-suffix">
            <view class="bind-input-icon-hit" @click.stop="clearBindUsername">
              <uni-icons type="closeempty" size="18" color="rgba(255, 255, 255, 0.45)" />
            </view>
          </view>
        </view>
        <view class="bind-input-row">
          <input
            v-model="bindPassword"
            class="bind-input bind-input--suffix-pwd"
            :password="!showBindPassword"
            placeholder="登录密码"
            placeholder-class="bind-placeholder"
          />
          <view class="bind-input-suffix bind-input-suffix--pwd">
            <view
              v-if="bindPassword.length > 0"
              class="bind-input-icon-hit"
              @click.stop="clearBindPassword"
            >
              <uni-icons type="closeempty" size="18" color="rgba(255, 255, 255, 0.45)" />
            </view>
            <view class="bind-input-icon-hit" @click.stop="toggleBindPasswordVisible">
              <uni-icons
                :type="showBindPassword ? 'eye-filled' : 'eye-slash-filled'"
                size="18"
                color="rgba(255, 255, 255, 0.45)"
              />
            </view>
          </view>
        </view>
        <button
          class="login-btn btn-gradient group login-btn-native bind-submit"
          :loading="isLoggingIn"
          @click="handleBindSubmit"
        >
          <text class="btn-text">{{ isLoggingIn ? '绑定中...' : '确认绑定并登录' }}</text>
        </button>
        <view class="bind-cancel" @click="exitWithoutBind">
          <text>不绑定，退出小程序</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onBackPress, onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { mpBindLogin as mpBindLoginApi, mpLogin as mpLoginApi } from '@/api/auth'
  import { API_MSG_NETWORK_ERROR } from '@/constants/apiMessages'
  import { getApiMessage, type ApiResponse } from '@/utils/http'
  import { showApiToast } from '@/utils/uiFeedback'
  import { finalizeMpLoginSession } from '@/utils/mpSession'
  import type { LoginResult } from '@/utils/permissions'
  import { ASSET_IMAGES } from '@/constants/assets'

  // 是否登录中
  const isLoggingIn = ref(false)
  // 是否同意条款
  const isTermsChecked = ref(false)
  // 条款错误
  const termsError = ref(false)
  /**
 * mp-login 返回 UNBIND 时展示认领表单，并暂存本次 getPhoneNumber 的 phoneCode
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const showBindPanel = ref(false)
  const pendingPhoneCode = ref('')
  const bindUsername = ref('')
  const bindPassword = ref('')
  /**
 * 绑定弹层内密码是否明文展示
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const showBindPassword = ref(false)

  const clearBindUsername = () => {
    bindUsername.value = ''
  }

  const clearBindPassword = () => {
    bindPassword.value = ''
  }

  const toggleBindPasswordVisible = () => {
    showBindPassword.value = !showBindPassword.value
  }

  /**
 * 与 routeGuard 一致：有 token 则直接进入业务（冷启动首屏为登录页时）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  onShow(() => {
    if (uni.getStorageSync('token')) {
      uni.switchTab({ url: '/pages/index/index' })
    }
  })

  /**
 * 待绑定态：拦截系统返回键，与导航返回一致，仅允许确认后退出小程序
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  onBackPress(() => {
    if (showBindPanel.value) {
      exitWithoutBind()
      return true
    }
    return false
  })

  /**
   * 未绑定则无法使用：仅可退出小程序（不关闭绑定层回到可登录业务态）
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const exitWithoutBind = () => {
    uni.showModal({
      title: '提示',
      content: '未绑定承修方账号将无法使用本小程序。是否退出？',
      confirmText: '退出',
      cancelText: '取消',
      success: (res) => {
        if (!res.confirm) return
        closeBindPanel()
        uni.exitMiniProgram({
          fail: () => {
            /* H5 等环境无 exit 时忽略 */
          }
        })
      }
    })
  }

  /**
   * 处理返回
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleBack = () => {
    if (showBindPanel.value) {
      exitWithoutBind()
      return
    }
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleTermsChange = (e: any) => {
    isTermsChecked.value = e.detail.value.length > 0
    if (isTermsChecked.value) {
      termsError.value = false
    }
  }

  /**
   * 未同意条款时提示
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const promptTermsFirst = () => {
    termsError.value = true
    setTimeout(() => {
      termsError.value = false
    }, 1200)
    void showApiToast('请先阅读并同意下方协议')
  }

  /**
   * 非微信端：无法使用手机号授权
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleNonWeixinLogin = () => {
    if (!isTermsChecked.value) {
      promptTermsFirst()
      return
    }
    void showApiToast('请在微信小程序中打开承修方端完成登录')
  }

  const closeBindPanel = () => {
    showBindPanel.value = false
    pendingPhoneCode.value = ''
    bindUsername.value = ''
    bindPassword.value = ''
    showBindPassword.value = false
  }

  /**
   * 展示 mp-bind-login 接口返回的 msg（无 msg 时不弹 toast，避免前端兜底覆盖后端文案）
   * @param res 接口响应或 http reject 体
   * @returns void
   */
  const showMpBindLoginMsg = (res: ApiResponse<unknown> | null | undefined) => {
    const msg = getApiMessage(res, '')
    if (msg) {
      void showApiToast(msg)
    }
  }

  /**
   * 认领绑定并登录（/api/auth/mp-bind-login）
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleBindSubmit = async () => {
    const usernameOrPhone = bindUsername.value.trim()
    if (!usernameOrPhone) {
      void showApiToast('请输入用户名或手机号')
      return
    }
    if (!bindPassword.value) {
      void showApiToast('请输入密码')
      return
    }
    if (!pendingPhoneCode.value) {
      void showApiToast('请重新进行手机号一键登录')
      closeBindPanel()
      return
    }

    isLoggingIn.value = true
    try {
      const wxLogin = await new Promise<{ code: string }>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: resolve, fail: reject })
      })
      if (!wxLogin.code) {
        void showApiToast('获取微信登录凭证失败，请重试')
        return
      }
      // 绑定登录是 POST 写接口，http.ts 自动显示带 mask 的 loading
      const loginRes = await mpBindLoginApi({
        code: wxLogin.code,
        usernameOrPhone,
        password: bindPassword.value,
        phoneCode: pendingPhoneCode.value
      })
      const result = loginRes.data as LoginResult & { status?: string }

      if (!result.token || !result.userInfo) {
        showMpBindLoginMsg(loginRes)
        return
      }

      closeBindPanel()
      await finalizeMpLoginSession(loginRes as ApiResponse<LoginResult>, result as LoginResult)
    } catch (err) {
      const apiErr = err as ApiResponse<LoginResult>
      if (apiErr && typeof apiErr === 'object' && typeof apiErr.code === 'string') {
        showMpBindLoginMsg(apiErr)
        return
      }
      if (err instanceof Error && err.message) {
        void showApiToast(err.message)
        return
      }
      void showApiToast(API_MSG_NETWORK_ERROR)
    } finally {
      isLoggingIn.value = false
    }
  }

  /**
   * 微信小程序：用户授权手机号后取 phoneCode，再 uni.login 取 code，POST /api/auth/mp-login（MpLoginDTO：code + phoneCode）
   * @param e getPhoneNumber 回调事件
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handleWeixinMpLogin = async (e: { detail?: { errMsg?: string; code?: string } }) => {
    if (!isTermsChecked.value) {
      promptTermsFirst()
      return
    }

    const errMsg = e?.detail?.errMsg ?? ''
    if (errMsg !== 'getPhoneNumber:ok') {
      if (errMsg.includes('deny') || errMsg.includes('cancel')) {
        void showApiToast('需要授权手机号才能完成登录')
      }
      return
    }
    const phoneCode = e?.detail?.code
    if (!phoneCode) {
      void showApiToast('未获取到手机号授权，请升级微信后重试')
      return
    }

    isLoggingIn.value = true
    try {
      const wxLogin = await new Promise<{ code: string }>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: resolve, fail: reject })
      })
      if (!wxLogin.code) {
        void showApiToast('获取微信登录凭证失败，请重试')
        return
      }
      // 一键登录是 POST 写接口，http.ts 自动显示带 mask 的 loading
      const loginRes = await mpLoginApi({ code: wxLogin.code, phoneCode })
      const result = loginRes.data

      if (result.status === 'UNBIND') {
        pendingPhoneCode.value = phoneCode
        bindUsername.value = ''
        bindPassword.value = ''
        showBindPanel.value = true
        void showApiToast('请绑定已有承修方账号')
        return
      }

      if (!result.token || !result.userInfo) {
        void showApiToast(getApiMessage(loginRes, '登录失败，请重试'), { duration: 2800 })
        return
      }

      await finalizeMpLoginSession(loginRes as ApiResponse<LoginResult>, result as LoginResult)
    } catch (err) {
      const message = err instanceof Error ? err.message : '登录失败，请稍后重试'
      void showApiToast(message || '登录失败，请稍后重试')
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
      color: $text-bg;
      transition: all 0.2s;

      &:active {
        transform: scale(0.9);
        background-color: rgba(255, 255, 255, 0.1);
      }
    }

    .back-icon {
      width: 10px;
      height: 10px;
      border-left: 2px solid $bg-card;
      border-bottom: 2px solid $bg-card;
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
      color: $icon-slate-light;
      text-transform: uppercase;
      letter-spacing: 0.15em;
    }

    .title {
      display: block;
      font-size: 34px;
      font-weight: bold;
      color: $text-bg;
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

    .login-hint {
      display: block;
      margin-top: 48rpx;
      font-size: 13px;
      line-height: 1.6;
      color: rgba(255, 255, 255, 0.45);
      letter-spacing: 0.02em;
      max-width: 440px;
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
      color: $text-bg;
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

      &.login-btn-native {
        margin: 0;
        line-height: 56px;
        text-align: center;
      }

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

      .login-hint {
        margin-top: 36rpx;
      }

      .bottom-actions {
        padding: 24px 24px calc(34px + env(safe-area-inset-bottom, 0px));
        border-top-left-radius: 30px;
        border-top-right-radius: 30px;
      }
    }

    /* 认领绑定弹层 */
    .bind-overlay {
      position: fixed;
      inset: 0;
      z-index: 200;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      box-sizing: border-box;
      background: rgba(2, 6, 23, 0.75);
      backdrop-filter: blur(8px);
    }

    .bind-card {
      width: 100%;
      max-width: 384px;
      padding: 28px 22px;
      border-radius: 20px;
      background: rgba(15, 23, 42, 0.96);
      border: 1px solid rgba(255, 255, 255, 0.08);
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.45);
    }

    .bind-title {
      display: block;
      font-size: 18px;
      font-weight: 700;
      color: $text-bg;
      margin-bottom: 10px;
      letter-spacing: 0.02em;
    }

    .bind-desc {
      display: block;
      font-size: 13px;
      line-height: 1.55;
      color: $text-slate-400;
      margin-bottom: 20px;
    }

    .bind-input-row {
      position: relative;
      width: 100%;
      margin-bottom: 14px;
    }

    .bind-input {
      width: 100%;
      height: 48px;
      padding: 0 14px;
      box-sizing: border-box;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: $text-bg;
      font-size: 15px;

      &--suffix {
        padding-right: 44px;
      }

      &--suffix-pwd {
        padding-right: 76px;
      }
    }

    .bind-input-suffix {
      position: absolute;
      right: 8px;
      top: 50%;
      transform: translateY(-50%);
      display: flex;
      align-items: center;
      gap: 4px;
      pointer-events: auto;
    }

    .bind-input-suffix--pwd {
      right: 6px;
      gap: 2px;
    }

    .bind-input-icon-hit {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 8px;

      &:active {
        background: rgba(255, 255, 255, 0.08);
      }
    }

    .bind-placeholder {
      color: rgba(255, 255, 255, 0.35);
    }

    .bind-submit {
      margin-top: 8px;
    }

    .bind-cancel {
      margin-top: 16px;
      text-align: center;
      padding: 8px;
    }

    .bind-cancel text {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.45);
    }

    .bind-cancel:active text {
      color: rgba(255, 255, 255, 0.65);
    }
  }
</style>
