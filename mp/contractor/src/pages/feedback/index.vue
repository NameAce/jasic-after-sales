<template>
  <!-- 承修方小程序：投诉与建议（表单样式对齐 jasicRepair） -->
  <custom-nav-bar title="投诉与建议" surface="sticky" />
  <view class="page-index page-padding feedback-form-wrap">
    <uni-forms
      ref="formRef"
      :model-value="formData"
      :rules="rules"
      label-position="top"
      label-width="auto"
    >
      <view>
        <view class="section-header">
          <view>反馈信息</view>
          <view class="section-header-right">
            <text class="required-badge">REQUIRED</text>
            <text class="feedback-list-link" @click="goFeedbackList">查看列表</text>
          </view>
        </view>

        <view class="card card-shadow form-padding">
          <uni-forms-item label="反馈内容" name="content" required>
            <uni-easyinput
              v-model="formData.content"
              type="textarea"
              auto-height
              :maxlength="FEEDBACK_CONTENT_MAX"
              placeholder="请详细描述您的问题或建议，便于我们跟进处理"
            />
            <text class="feedback-word-count">{{ contentLength }}/{{ FEEDBACK_CONTENT_MAX }}</text>
          </uni-forms-item>

        </view>

        <text class="feedback-page-tip">提交后我们将在 1～3 个工作日内处理您的反馈，感谢您的支持。</text>
        <view class="feedback-contact-row">
          <text class="feedback-contact-label">也可直接致电客服：</text>
          <text class="feedback-contact-phone" @click="callServicePhone">400-888-9999</text>
        </view>
      </view>
    </uni-forms>
  </view>

  <base-button>
    <view class="btn btn-primary" @click="submit">提交反馈</view>
  </base-button>
</template>

<script setup lang="ts">
  import { computed, reactive, ref } from 'vue'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import { submitFeedback } from '@/api/feedback'
  import { FEEDBACK_CONTENT_MAX } from '@/constants/feedback'
  import { saveFeedbackRecord } from '@/utils/feedbackHistory'
  import { showApiToast } from '@/utils/uiFeedback'

  const formRef = ref<{ validate?: () => Promise<void> } | null>(null)
  const submitting = ref(false)

  const formData = reactive({
    content: '',
  })

  const rules = {
    content: {
      rules: [{ required: true, errorMessage: '请填写反馈内容' }],
    },
  }

  const contentLength = computed(() => formData.content.length)

  /** 统一客服电话，供投诉建议场景快速联系人工客服 */
  const SERVICE_PHONE = '400-888-9999'

  /** 直接拨打客服电话 */
  const callServicePhone = () => {
    uni.makePhoneCall({ phoneNumber: SERVICE_PHONE })
  }

  /** 进入已提交反馈列表页面 */
  const goFeedbackList = () => {
    uni.navigateTo({ url: '/pages/feedback/list' })
  }

  const submit = async () => {
    if (submitting.value) return
    try {
      await formRef.value?.validate?.()
    } catch {
      return
    }
    const content = formData.content.trim()
    if (content.length < 5) {
      void showApiToast('反馈内容至少 5 个字')
      return
    }
    submitting.value = true
    try {
      const res = await submitFeedback({
        content,
      })
      // 提交成功后将记录落地到本地，供「已提交反馈」页面展示
      saveFeedbackRecord(content)
      await showApiToast(res.msg || '提交成功，感谢您的反馈')
      uni.navigateTo({ url: '/pages/feedback/list' })
    } catch {
      /* 失败提示由 http 层处理 */
    } finally {
      submitting.value = false
    }
  }
</script>

<style lang="scss" scoped>
  @use '@/styles/variables.scss' as *;
  @use '@/styles/mixins.scss' as *;

  .feedback-form-wrap {
    padding-top: $space-lg;
    padding-bottom: 200rpx;
    box-sizing: border-box;
  }

  .section-header {
    @include flex-between;
    margin-bottom: $space-md;
    padding: 0 $space-xs;

    view {
      font-size: $font-md;
      font-weight: bold;
      color: $text-main;
    }

    .required-badge {
      font-size: $font-sm;
      color: $primary;
      font-weight: bold;
      letter-spacing: 2rpx;
    }
  }

  .section-header-right {
    @include flex-row;
    gap: $space-sm;
  }

  .feedback-list-link {
    font-size: $font-sm;
    color: $primary;
    font-weight: 600;
  }

  .feedback-word-count {
    display: block;
    text-align: right;
    margin-top: $space-sm;
    font-size: $font-xs;
    color: $text-placeholder;
  }

  .feedback-page-tip {
    display: block;
    margin-top: $space-lg;
    font-size: $font-sm;
    color: $text-secondary;
    line-height: 1.6;
    padding: 0 $space-xs;
  }

  .feedback-contact-row {
    margin-top: $space-sm;
    padding: 0 $space-xs;
    font-size: $font-sm;
    line-height: 1.6;
  }

  .feedback-contact-label {
    color: $text-secondary;
  }

  .feedback-contact-phone {
    color: $primary;
    font-weight: 600;
    margin-left: $space-xs;
  }
</style>
