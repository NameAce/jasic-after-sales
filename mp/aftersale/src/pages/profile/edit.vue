<template>
  <custom-nav-bar title="更换信息" surface="sticky" />
  <view class="page-index page-profile-edit">
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
   * C 端「更换信息」页：编辑昵称（手机号只读展示）
   */
  import { computed, ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { useUserStore } from '@/stores'
  import { updateProfile } from '@/api/auth'
  import { showApiToast } from '@/utils/uiFeedback'

  const userStore = useUserStore()
  const nickname = ref('')

  const phoneDisplay = computed(() => String(userStore.userInfo?.phone || '-'))

  onLoad(() => {
    const current = String(userStore.userInfo?.nickname ?? '').trim()
    nickname.value = current === '佳士用户' ? '' : current
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
      const res = await updateProfile({ nickname: name })
      userStore.setUserInfo(res.data)
      await showApiToast('信息已更新')
      uni.navigateBack()
    } catch {
      /* http 层已提示 */
    }
  }
</script>

<style lang="scss" scoped>
  .page-profile-edit {
    min-height: 100vh;
    background-color: $bg-light;
    display: flex;
    flex-direction: column;
  }

  .main-content.page-padding {
    @include flex-column-gap;
    flex: 1;
    box-sizing: border-box;
    padding-top: $space-lg;
  }

  .form-card {
    @include white-card;
    padding: $space-xs $space-lg $space-lg;
  }

  .form-item {
    padding: $space-md 0;
    border-bottom: 2rpx solid $border-lighter;
    display: flex;
    align-items: center;
    gap: $space-md;

    &:last-child {
      border-bottom: none;
    }

    &--readonly {
      align-items: center;
    }

    .label {
      width: 160rpx;
      flex-shrink: 0;
      font-size: $font-md;
      color: $text-label;
    }

    .input {
      flex: 1;
      font-size: 30rpx;
      color: $text-dark;
    }

    .input-placeholder {
      color: $text-muted;
      font-size: 30rpx;
    }

    .readonly-text {
      flex: 1;
      font-size: 30rpx;
      color: $text-muted;
    }
  }

  .save-btn {
    @include btn-primary-round;
    font-size: $font-lg;
    margin-top: $space-lg;
  }
</style>
