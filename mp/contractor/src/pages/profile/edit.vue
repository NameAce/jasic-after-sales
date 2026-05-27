<template>
  <custom-nav-bar title="更换信息" surface="sticky" />
  <view class="page-profile-edit">
    <scroll-view class="main-content page-padding" scroll-y>
      <view class="form-card">
        <view class="form-item">
          <text class="label">昵称</text>
          <!-- #ifdef MP-WEIXIN -->
          <input
            class="input"
            type="nickname"
            :value="nickname"
            placeholder="点击使用微信昵称"
            placeholder-class="input-placeholder"
            @input="onNicknameInput"
          />
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <input
            v-model="nickname"
            class="input"
            type="text"
            placeholder="请输入昵称"
            placeholder-class="input-placeholder"
          />
          <!-- #endif -->
        </view>
        <view class="form-item form-item--readonly">
          <text class="label">手机号</text>
          <text class="readonly-text">{{ phoneDisplay }}</text>
        </view>
      </view>
      <view class="save-btn" @click="onSave">保存</view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  /**
   * B 端「更换信息」页：编辑展示昵称（同步至 realName），手机号只读
   */
  import { computed, ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { useUserStore } from '@/stores/modules/user'
  import { updateMpProfile } from '@/api/auth'
  import { showApiToast } from '@/utils/uiFeedback'

  const userStore = useUserStore()
  const nickname = ref('')

  const phoneDisplay = computed(() => String(userStore.userInfo?.phone || '-'))

  onLoad(() => {
    nickname.value = String(userStore.userInfo?.realName ?? '').trim()
  })

  const onNicknameInput = (e: { detail?: { value?: string } }) => {
    nickname.value = String(e.detail?.value ?? '')
  }

  const onSave = async () => {
    const name = nickname.value.trim()
    if (!name) {
      void showApiToast('请填写昵称')
      return
    }
    try {
      const res = await updateMpProfile({ realName: name })
      userStore.setUserInfo(res.data)
      await showApiToast('信息已更新')
      uni.navigateBack()
    } catch {
      /* http 层已提示 */
    }
  }
</script>

<style lang="scss" scoped>
  @use '@/styles/variables.scss' as *;

  .page-profile-edit {
    min-height: 100vh;
    background-color: $bg-light;
    display: flex;
    flex-direction: column;
  }

  .main-content.page-padding {
    flex: 1;
    box-sizing: border-box;
    padding: 32rpx 40rpx;
    display: flex;
    flex-direction: column;
    gap: 24rpx;
  }

  .form-card {
    background-color: $bg-card;
    border-radius: 24rpx;
    padding: 8rpx 32rpx 32rpx;
    box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.05);
  }

  .form-item {
    padding: 28rpx 0;
    border-bottom: 2rpx solid $border-lighter;
    display: flex;
    align-items: center;
    gap: 24rpx;

    &:last-child {
      border-bottom: none;
    }

    .label {
      width: 160rpx;
      flex-shrink: 0;
      font-size: 28rpx;
      color: #64748b;
    }

    .input {
      flex: 1;
      font-size: 30rpx;
      color: #0f172a;
    }

    .input-placeholder {
      color: #94a3b8;
      font-size: 30rpx;
    }

    .readonly-text {
      flex: 1;
      font-size: 30rpx;
      color: #94a3b8;
    }
  }

  .save-btn {
    margin-top: 16rpx;
    padding: 28rpx 0;
    border-radius: 48rpx;
    background-color: $primary;
    color: $primary-contrast;
    font-size: 32rpx;
    font-weight: 600;
    text-align: center;
  }
</style>
