<template>
  <view class="page-login">
    <!-- 鑳屾櫙 & 鑻遍泟閮ㄥ垎 -->
    <view class="bg-wrapper">
      <image class="bg-image" src="/static/images/login-bg.jpg" mode="aspectFill" />
      <view class="hero-vignette"></view>
    </view>

    <!-- 鍐呭瑕嗙洊 -->
    <view class="content-overlay">
      <!-- 瀵艰埅鏍?-->
      <CustomNavBar
        title="鎵夸慨鏂圭櫥褰?
        surface="plain"
        tone="light"
        :color="'#ffffff'"
        :auto-back="false"
        @back="handleBack"
      />

      <view class="hero-content">
        <!-- 寰爣鏍囩 -->
        <view class="badge">
          <view class="badge-dot"></view>
          <text class="badge-text">Technician Portal</text>
        </view>
        <text class="title">娆㈣繋鍥炴潵</text>
        <text class="subtitle">涓撲笟缁翠慨鏈嶅姟锛岄珮鏁堝搷搴旈渶姹?/text>

        <!-- 瑙掕壊閫夋嫨鍗＄墖 -->
        <view class="role-section">
          <text class="role-section-title">閫夋嫨鐧诲綍瑙掕壊</text>
          <view class="role-cards">
            <view
              v-for="(role, key) in roleOptions"
              :key="key"
              :class="['role-card', selectedRole === key && 'role-card--active']"
              @click="selectedRole = key"
            >
              <view v-if="selectedRole === key" class="role-card-check">
                <image class="role-check-icon" :src="roleCheckCircleIcon" mode="aspectFit" />
              </view>
              <image class="role-card-icon" :src="roleCardIcons[key]" mode="aspectFit" />
              <text class="role-card-label">{{ role.label }}</text>
              <text class="role-card-desc">{{ role.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 搴曢儴鎿嶄綔 -->
      <view class="bottom-actions bottom-glass">
        <view class="action-container">
          <!-- 鐧诲綍鎸夐挳 -->
          <button class="login-btn btn-gradient group" :loading="isLoggingIn" @click="handleLogin">
            <text class="btn-text">{{ isLoggingIn ? '鐧诲綍涓?..' : '鎵嬫満鍙蜂竴閿櫥褰? }}</text>
            <view v-if="!isLoggingIn" class="btn-icon">
              <uni-icons type="arrowright" size="20" color="#fff"></uni-icons>
            </view>
            <view class="shine-effect"></view>
          </button>

          <!-- 鏉℃ -->
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
              <text class="terms-label" :class="{ 'text-error': termsError }">鎴戝凡闃呰骞跺悓鎰?</text>
              <text class="terms-link">銆婄敤鎴锋湇鍔″崗璁€?/text>
              <text class="terms-label" :class="{ 'text-error': termsError }">銆?/text>
              <text class="terms-link">銆婇殣绉佹斂绛栥€?/text>
              <text class="terms-label" :class="{ 'text-error': termsError }"> 浠ュ強 </text>
              <text class="terms-link">銆婃壙淇柟浣滀笟瑙勮寖鎵嬪唽銆?/text>
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
  import {
    roleAssignmentIndIcon,
    roleCheckCircleIcon,
    roleCorporateFareIcon,
    roleEngineeringIcon
  } from '@/svgs'
  import { useUserStore } from '@/stores/modules/user'
  import { MockRoles } from '@/utils/permissions'
  import { mockLogin } from '@/mock/user'

  // 鐢ㄦ埛鍟嗗簵
  const userStore = useUserStore()

  // 鏄惁鐧诲綍涓?  const isLoggingIn = ref(false)
  // 鏄惁鍚屾剰鏉℃
  const isTermsChecked = ref(false)
  // 鏉℃閿欒
  const termsError = ref(false)
  // 閫夋嫨瑙掕壊
  const selectedRole = ref<keyof typeof MockRoles>('engineer')

  // 瑙掕壊閫夐」
  const roleOptions = MockRoles
  // 瑙掕壊鍗＄墖鍥炬爣

  // 瑙掕壊鍗＄墖鍥炬爣
  const roleCardIcons: Record<keyof typeof MockRoles, string> = {
    hqAdmin: roleCorporateFareIcon,
    dispatcher: roleAssignmentIndIcon,
    engineer: roleEngineeringIcon
  }

  /** 涓?routeGuard 涓€鑷达細鏈?token 鍒欑洿鎺ヨ繘鍏ヤ笟鍔★紙鍐峰惎鍔ㄩ灞忎负鐧诲綍椤垫椂锛?*/
  onShow(() => {
    if (uni.getStorageSync('token')) {
      uni.switchTab({ url: '/pages/index/index' })
    }
  })

  /**
   * 澶勭悊杩斿洖
   * @returns void
   */
  const handleBack = () => {
    uni.navigateBack({
      fail: () => {
        // 鎵夸慨鏂瑰叆鍙ｅ嵆鐧诲綍椤碉紝鏃犲巻鍙叉爤鏃堕€€鍑哄皬绋嬪簭鑰岄潪杩涘叆涓氬姟椤?        uni.exitMiniProgram({
          fail: () => {
            /* H5 绛夌幆澧冩棤 exit 鏃跺拷鐣?*/
          }
        })
      }
    })
  }

  /**
   * 澶勭悊鏉℃鍙樺寲
   * @param e 浜嬩欢
   * @returns void
   */
  const handleTermsChange = (e: any) => {
    isTermsChecked.value = e.detail.value.length > 0
    if (isTermsChecked.value) {
      termsError.value = false
    }
  }

  /**
   * 澶勭悊鐧诲綍
   * @returns void
   */
  const handleLogin = () => {
    if (!isTermsChecked.value) {
      termsError.value = true
      setTimeout(() => {
        termsError.value = false
      }, 1200)
      return
    }

    isLoggingIn.value = true

    // Mock 鐧诲綍锛氬鎺ュ悗绔悗鏇挎崲涓虹湡瀹?API 璋冪敤
    setTimeout(() => {
      const result = mockLogin(selectedRole.value)

      userStore.login(result.token, result.userInfo)

      isLoggingIn.value = false
      uni.showToast({ title: '鐧诲綍鎴愬姛', icon: 'success' })

      setTimeout(() => {
        uni.reLaunch({ url: '/pages/index/index' })
      }, 500)
    }, 1000)
  }
</script>

<style lang="scss" scoped>
  .page-login {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background-color: $login-bg;
    position: relative;
    overflow: hidden;
    font-family:
      'Inter',
      'Noto Sans SC',
      system-ui,
      -apple-system,
      sans-serif;
  }

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
    opacity: 0.5;
    transform: scale(1.05);
    filter: contrast(1.25);
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
    padding: 40px 32px 0;
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
    font-size: 36px;
    font-weight: bold;
    color: $surface-white;
    margin-bottom: 12px;
    letter-spacing: -0.025em;
    line-height: 1.2;
  }

  .subtitle {
    display: block;
    color: $text-slate-400;
    font-size: 15px;
    font-weight: normal;
    line-height: 1.6;
    letter-spacing: 0.025em;
  }

  /* Role Selection */
  .role-section {
    margin-top: 32px;
  }

  .role-section-title {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.5);
    letter-spacing: 0.1em;
    text-transform: uppercase;
    margin-bottom: 16px;
  }

  .role-cards {
    display: flex;
    flex-direction: row;
    gap: 12px;
  }

  .role-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 16px 8px;
    border-radius: 14px;
    background-color: rgba(255, 255, 255, 0.04);
    border: 1.5px solid rgba(255, 255, 255, 0.08);
    position: relative;
    transition: all 0.25s;

    &:active {
      transform: scale(0.96);
    }
  }

  .role-card--active {
    border-color: $primary;
    background-color: $primary-alpha-10;
    box-shadow: 0 0 20px rgba($primary, 0.15);
  }

  .role-card-check {
    position: absolute;
    top: 6px;
    right: 6px;
  }

  .role-check-icon {
    width: 18px;
    height: 18px;
    display: block;
  }

  .role-card-icon {
    width: 28px;
    height: 28px;
    display: block;
    margin-bottom: 8px;
    opacity: 0.7;
  }

  .role-card--active .role-card-icon {
    opacity: 1;
    filter: brightness(0) saturate(100%) invert(47%) sepia(98%) saturate(4000%) hue-rotate(359deg)
      brightness(101%) contrast(101%);
  }

  .role-card-label {
    font-size: 13px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.85);
    margin-bottom: 4px;
    text-align: center;
  }

  .role-card-desc {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.4);
    text-align: center;
    line-height: 1.4;
  }

  /* Bottom Actions */
  .bottom-actions {
    width: 100%;
    padding: 36px 32px 56px;
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
    gap: 32px;
  }

  .login-btn {
    width: 100%;
    color: $surface-white;
    font-weight: bold;
    padding: 0;
    height: 60px;
    border-radius: 16px;
    box-shadow:
      0 10px 25px -5px rgba($primary, 0.4),
      0 8px 10px -6px rgba($primary, 0.4),
      inset 0 1px 1px rgba(255, 255, 255, 0.2);
    transition: all 0.3s;
    font-size: 17px;
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
    gap: 10px;
  }

  .checkbox-wrapper {
    padding-top: 0px;
  }

  .terms-text {
    flex: 1;
    font-size: 12px;
    line-height: 1.6;
  }

  .terms-label {
    color: $text-slate-400;
    transition: color 0.3s;
  }

  .terms-link {
    color: $primary;
    font-weight: 500;
    transition: color 0.2s;

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
</style>
