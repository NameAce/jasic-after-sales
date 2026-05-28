<template>
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
          <text class="required-badge">REQUIRED</text>
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

  /**
   * 提交反馈申请，成功后返回上一页列表。
   */
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
      const res = await submitFeedback({ content })
      await showApiToast(res.msg || '提交成功，感谢您的反馈')
      uni.navigateBack()
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

  .feedback-word-count {
    display: block;
    text-align: right;
    margin-top: $space-sm;
    font-size: $font-xs;
    color: $text-placeholder;
  }
</style>
