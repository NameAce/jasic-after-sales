<template>
  <!-- 售后客户端小程序：投诉与建议（表单样式对齐 jasicRepair / otherRepair） -->
  <custom-nav-bar title="投诉与建议" surface="sticky" />
  <view class="repair-form-page page-index">
    <uni-forms
      ref="formRef"
      :model-value="formData"
      :rules="rules"
      label-position="top"
      label-width="auto"
    >
      <view class="form-content page-padding">
        <RepairFormSectionHeader title="反馈信息" />

        <view class="card card-shadow form-padding">
          <uni-forms-item label="反馈内容" name="content" required>
            <FormItemAnchor name="content" />
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
  import RepairFormSectionHeader from '@/components/RepairFormSectionHeader/RepairFormSectionHeader.vue'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import { submitFeedback } from '@/api/feedback'
  import { FEEDBACK_CONTENT_MAX } from '@/constants/feedback'
  import { scrollToFirstInvalidUniFormField } from '@/utils/formFieldScrollFocus'
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

  const submit = async () => {
    if (submitting.value) return
    try {
      await formRef.value?.validate?.()
    } catch (err) {
      scrollToFirstInvalidUniFormField(err)
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
      await showApiToast(res.msg || '提交成功，感谢您的反馈')
      uni.navigateBack()
    } catch {
      /* 失败提示由 http 层处理 */
    } finally {
      submitting.value = false
    }
  }
</script>

<style lang="scss">
  .feedback-word-count {
    display: block;
    text-align: right;
    margin-top: $space-sm;
    font-size: $font-xs;
    color: $text-placeholder;
  }

  .feedback-page-tip {
    display: block;
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
