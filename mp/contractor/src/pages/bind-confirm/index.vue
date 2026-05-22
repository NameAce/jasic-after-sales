<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）页面 bind-confirm / index -->
  <view class="page-bind-confirm">
    <view class="bg-wrapper">
      <image class="bg-image" :src="ASSET_IMAGES.loginBg" mode="aspectFill" />
      <view class="hero-vignette"></view>
    </view>

    <view class="content-overlay">
      <CustomNavBar
        title="绑定微信"
        surface="plain"
        tone="light"
        :color="'#ffffff'"
        :auto-back="false"
        @back="handleBack"
      />

      <view class="hero-content">
        <view class="badge">
          <view class="badge-dot"></view>
          <text class="badge-text">PC 扫码绑定</text>
        </view>
        <text class="title">确认绑定微信</text>
        <text class="subtitle">请在 10 分钟内完成；需授权手机号以核对与 PC 账号一致</text>

        <view v-if="!bindTicket" class="error-box">
          <text class="error-text">无效或已过期的绑定链接，请返回 PC 账号中心重新生成二维码</text>
        </view>
      </view>

      <view class="bottom-actions bottom-glass">
        <view class="action-container">
          <!-- #ifdef MP-WEIXIN -->
          <button
            v-if="bindTicket"
            class="login-btn btn-gradient group login-btn-native"
            open-type="getPhoneNumber"
            :loading="isSubmitting"
            :disabled="isSubmitting"
            @getphonenumber="handleBindConfirm"
          >
            <text class="btn-text">{{ isSubmitting ? '提交中...' : '授权手机号并确认绑定' }}</text>
            <view v-if="!isSubmitting" class="btn-icon">
              <uni-icons type="phone" size="20" color="#fff"></uni-icons>
            </view>
            <view class="shine-effect"></view>
          </button>
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <button class="login-btn btn-gradient group" @click="handleNonWeixin">
            <text class="btn-text">授权手机号并确认绑定</text>
          </button>
          <!-- #endif -->

          <text class="hint">绑定成功后自动登录承修方小程序</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 承修方小程序（网点/总部工单处理、派工）：index。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { mpBindConfirm as mpBindConfirmApi } from '@/api/auth'
  import { getApiMessage, type ApiResponse } from '@/utils/http'
  import { finalizeMpLoginSession } from '@/utils/mpSession'
  import type { LoginResult } from '@/utils/permissions'
  import { ASSET_IMAGES } from '@/constants/assets'

  const bindTicket = ref('')
  const isSubmitting = ref(false)

  function resolveBindTicket(q: Record<string, any>) {
    const raw = q.bindTicket ?? q.ticket ?? q.scene
    if (typeof raw !== 'string') return ''
    try {
      return decodeURIComponent(raw).trim()
    } catch {
      return String(raw).trim()
    }
  }

  onLoad((options) => {
    bindTicket.value = resolveBindTicket(options || {})
  })

  const handleBack = () => {
    uni.navigateBack({
      fail: () => {
        uni.reLaunch({ url: '/pages/login/index' })
      }
    })
  }

  const handleNonWeixin = () => {
    uni.showToast({ title: '请在微信小程序中完成扫码绑定', icon: 'none' })
  }

  const handleBindConfirm = async (e: { detail?: { errMsg?: string; code?: string } }) => {
    if (!bindTicket.value) {
      uni.showToast({ title: '绑定凭证无效', icon: 'none' })
      return
    }

    const errMsg = e?.detail?.errMsg ?? ''
    if (errMsg !== 'getPhoneNumber:ok') {
      if (errMsg.includes('deny') || errMsg.includes('cancel')) {
        uni.showToast({ title: '需要授权手机号才能完成绑定', icon: 'none' })
      }
      return
    }

    const phoneCode = e?.detail?.code
    if (!phoneCode) {
      uni.showToast({ title: '未获取到手机号授权，请升级微信后重试', icon: 'none' })
      return
    }

    isSubmitting.value = true
    try {
      const wxLogin = await new Promise<{ code: string }>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: resolve, fail: reject })
      })
      if (!wxLogin.code) {
        uni.showToast({ title: '获取微信登录凭证失败，请重试', icon: 'none' })
        return
      }

      const loginRes = await mpBindConfirmApi({
        bindTicket: bindTicket.value,
        code: wxLogin.code,
        phoneCode
      })
      const result = loginRes.data

      if (result.status === 'UNBIND' || !result.token || !result.userInfo) {
        uni.showToast({
          title: getApiMessage(loginRes, '绑定失败，请核对 PC 账号或重新扫码'),
          icon: 'none',
          duration: 2800
        })
        return
      }

      await finalizeMpLoginSession(loginRes as ApiResponse<LoginResult>, result as LoginResult)
    } catch (err) {
      const message = err instanceof Error ? err.message : '绑定失败，请稍后重试'
      uni.showToast({ title: message || '绑定失败，请稍后重试', icon: 'none' })
    } finally {
      isSubmitting.value = false
    }
  }
</script>

<style lang="scss">
  .page-bind-confirm {
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

    .content-overlay {
      position: relative;
      z-index: 10;
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

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
      font-size: 28px;
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
    }

    .error-box {
      margin-top: 24px;
      padding: 14px;
      border-radius: 12px;
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.35);
    }

    .error-text {
      font-size: 13px;
      line-height: 1.55;
      color: #fecaca;
    }

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
      gap: 16px;
    }

    .hint {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.45);
      text-align: center;
      line-height: 1.5;
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
      display: flex;
      align-items: center;
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
  }
</style>
