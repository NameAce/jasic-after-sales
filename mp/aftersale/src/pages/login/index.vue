<template>
  <view class="page-container page-index">
    <custom-nav-bar title="佳士服务" surface="plain" fixed :show-back="false" />
    <!-- 内容区域 -->
    <view class="content-wrapper page-padding">
      <!-- 顶部区域 -->
      <view class="avatar-section">
        <view class="avatar-container">
          <view class="avatar-glow"></view>
          <image class="avatar-img" mode="aspectFill" src="/static/images/login-avatar.jpg" />
        </view>
        <view class="welcome-text">
          <text class="greeting">欢迎回来</text>
          <text class="subtitle">登录以访问您的售后服务仪表盘</text>
        </view>
      </view>

      <!-- 登录区域 -->
      <view class="login-card">
        <!-- 登录按钮 -->
        <view class="btn-group">
          <!-- #ifdef MP-WEIXIN -->
          <button
            v-if="agreed"
            class="login-btn primary login-btn-native"
            hover-class="btn-hover"
            :hover-stay-time="100"
            open-type="getPhoneNumber"
            @getphonenumber="onGetPhoneNumber"
          >
            <image class="login-btn-phone-icon" :src="smartphoneIcon" mode="aspectFit" />
            <text>手机号一键登录</text>
          </button>
          <view
            v-else
            class="login-btn primary"
            hover-class="btn-hover"
            :hover-stay-time="100"
            @click="promptAgreementFirst"
          >
            <image class="login-btn-phone-icon" :src="smartphoneIcon" mode="aspectFit" />
            <text>手机号一键登录</text>
          </view>
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <view
            class="login-btn primary"
            hover-class="btn-hover"
            :hover-stay-time="100"
            @click="handlePhoneLogin"
          >
            <image class="login-btn-phone-icon" :src="smartphoneIcon" mode="aspectFit" />
            <text>手机号一键登录</text>
          </view>
          <!-- #endif -->
        </view>
      </view>

      <!-- 用户协议 -->
      <view class="agreement-section agreement-section-anchor">
        <FormItemAnchor name="loginAgreement" />
        <view class="agreement-label">
          <view class="checkbox-wrapper">
            <checkbox-group @change="toggleAgreement">
              <checkbox
                :checked="agreed"
                value="1"
                :color="themeColor.primary"
                style="transform: scale(0.7)"
              />
            </checkbox-group>
          </view>
          <view class="agreement-text">
            <text @click="toggleAgreement">我已阅读并同意 </text>
            <text class="link" @click.stop="openAgreement">用户协议</text>
            <text @click="toggleAgreement"> 和 </text>
            <text class="link" @click.stop="openPrivacy">隐私政策</text>
            <text @click="toggleAgreement">。</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 背景装饰元素 -->
    <view class="bg-blobs">
      <view class="blob blob-bottom-right"></view>
      <view class="blob blob-top-left"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { themeColor } from '@/constants/theme'
  import { smartphoneIcon } from '@/svgs'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import { useUserStore } from '@/stores'
  import { login } from '@/api/auth'
  import { scrollPageToFormFieldKey } from '@/utils/formFieldScrollFocus'
  import { showToastThen, TAB_HOME } from '@/utils/toastNavigate'
  // 是否同意用户协议
  const agreed = ref(false)

  // 用户商店
  const userStore = useUserStore()

  /**
   * 切换用户协议
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const toggleAgreement = () => {
    agreed.value = !agreed.value
  }
  /**
   * 提示用户先阅读并同意用户协议和隐私政策
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const promptAgreementFirst = () => {
    scrollPageToFormFieldKey('loginAgreement')
    uni.showToast({ title: '请先阅读并同意用户协议和隐私政策', icon: 'none', duration: 1500 })
  }

  /**
   * 微信小程序：授权手机号回调里用 e.detail.code 作为 phoneCode，再搭配 uni.login 的 code 请求后端
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const onGetPhoneNumber = async (e: { detail: { code?: string } }) => {
    const phoneCode = e.detail?.code ?? ''
    if (!phoneCode) {
      uni.showToast({ title: '需要授权手机号才能登录', icon: 'none', duration: 1500 })
      return
    }
    uni.showLoading({ title: '登录中...' })
    try {
      const loginRes = await new Promise<{ code: string }>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: resolve, fail: reject })
      })
      const res = await login({ code: loginRes.code, phoneCode })
      userStore.setUserInfo(res.data.userInfo)
      uni.setStorageSync('token', res.data.token)
      uni.hideLoading()
      showToastThen(TAB_HOME, { title: res.msg || '登录成功', duration: 1500 })
    } catch {
      uni.hideLoading()
      /* 失败提示由 http 层使用接口 msg */
    }
  }

  /**
   * 非微信小程序端兜底：C 端登录强依赖微信 `wx.login` 返回的 js_code，
   * 后端 `CustomerWechatLoginDTO.code` 标注 `@NotBlank`，传空值必然 400。
   * 故非微信环境直接提示用户切换至微信小程序，不再调用接口空转。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const handlePhoneLogin = () => {
    if (!agreed.value) {
      promptAgreementFirst()
      return
    }
    uni.showToast({
      title: '请在微信小程序内完成登录',
      icon: 'none',
      duration: 1800,
    })
  }

  /**
   * 打开用户协议
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const openAgreement = () => {
    uni.showToast({ title: '查看用户协议', icon: 'none', duration: 1500 })
  }

  /**
   * 打开隐私政策
   * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const openPrivacy = () => {
    uni.showToast({ title: '查看隐私政策', icon: 'none', duration: 1500 })
  }
</script>

<style lang="scss" scoped>
  .page-container.page-index {
    position: relative;
    min-height: 100vh;
    width: 100%;
    padding: 0;
    box-sizing: border-box;
  }

  .content-wrapper.page-padding {
    flex: 1;
    width: 100%;
    max-width: 896rpx;
    margin: 0 auto;
    @include flex-column-center;
    justify-content: center;
    gap: 64rpx;
    z-index: 1;
    box-sizing: border-box;
  }

  .avatar-section {
    @include flex-column-center;
    gap: $space-xl;
  }

  .avatar-container {
    position: relative;
    width: 256rpx;
    height: 256rpx;

    .avatar-glow {
      position: absolute;
      inset: 0;
      background-color: rgba($primary, 0.2);
      border-radius: $radius-round;
      filter: blur(48rpx);
      transition: all 0.3s;
    }

    .avatar-img {
      position: relative;
      width: 100%;
      height: 100%;
      border-radius: $radius-round;
      border: 8rpx solid $primary-contrast;
      box-shadow:
        0 40rpx 50rpx -10rpx rgba(0, 0, 0, 0.1),
        0 16rpx 20rpx -12rpx rgba(0, 0, 0, 0.1);
      overflow: hidden;
      box-sizing: border-box;
    }
  }

  .welcome-text {
    @include flex-column-center;
    gap: $space-sm;

    .greeting {
      color: $text-dark;
      font-size: 60rpx;
      font-weight: 700;
    }

    .subtitle {
      color: $text-label;
      font-size: $font-lg;
    }
  }

  .login-card {
    width: 80%;
    background-color: $bg-card;
    border-radius: $radius-xxl;
    box-shadow: 0 50rpx 100rpx -24rpx rgba(226, 232, 240, 0.5);
    padding: 64rpx 40rpx;
    border: 2rpx solid $border-light;

    .btn-group {
      @include flex-column;
      gap: $space-lg;
    }

    .login-btn {
      width: 100%;
      @include flex-center;
      border-radius: $radius-round;
      padding: 26rpx $space-xl;
      box-sizing: border-box;
      transition: all 0.2s;

      .login-btn-phone-icon {
        width: 40rpx;
        height: 40rpx;
        margin-right: 8rpx;
        flex-shrink: 0;
      }

      &.primary {
        background-color: $primary;
        color: $primary-contrast;
        font-size: $font-lg;
        font-weight: 700;
      }

      &.secondary {
        background-color: rgba($primary, 0.1);
        color: $primary;
        font-size: $font-lg;
        font-weight: 600;
      }

      &.login-btn-native {
        border: none;
        margin: 0;
        line-height: inherit;
        text-align: center;

        &::after {
          border: none;
        }
      }
    }
  }

  .btn-hover {
    transform: scale(0.98);
    opacity: 0.9;
  }

  .btn-hover-sec {
    background-color: rgba($primary, 0.2);
  }

  .agreement-section-anchor {
    position: relative;
  }

  .agreement-section {
    .agreement-label {
      display: flex;
      align-items: flex-start;
      gap: 12rpx;

      .checkbox-wrapper {
        @include flex-row;
        height: 44rpx;
      }

      .agreement-text {
        color: $text-label;
        font-size: $font-md;
        line-height: 1.625;
        flex: 1;

        .link {
          color: $primary;
          font-weight: 500;
          display: inline;
        }
      }
    }
  }

  .bg-blobs {
    position: absolute;
    inset: 0;
    z-index: 0;
    pointer-events: none;
    overflow: hidden;

    .blob {
      position: absolute;
      width: 768rpx;
      height: 768rpx;
      border-radius: $radius-round;
      filter: blur(128rpx);

      &.blob-bottom-right {
        bottom: -192rpx;
        right: -192rpx;
        background-color: rgba($primary, 0.05);
      }

      &.blob-top-left {
        top: -192rpx;
        left: -192rpx;
        background-color: rgba($primary, 0.1);
      }
    }
  }
</style>
