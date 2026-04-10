<template>
  <custom-nav-bar title="服务评价" surface="frosted" :back-icon-size="24" back-icon-color="#666" />
  <view class="evaluate-page">
    <!-- 内容区域 -->
    <scroll-view
      scroll-y
      class="main-content"
      :scroll-into-view="scrollIntoView"
      scroll-with-animation
    >
      <view class="page-content page-padding">
        <!-- 维修员信息卡片 -->
        <view class="section">
          <view class="technician-card">
            <view class="avatar-wrap">
              <image
                class="avatar"
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuD6WAmZjNixQ0DFB7uvPBuqPVdEjienUwTzvUxHj3gzGu-O7sPvnsMMht-aoO4C57OpIbWPMnsXZ0wUs0O6PBCWg57lJv-r_lFtp-J_Af9_Ru_PesWm1Msz6ZYbHO4FFwmXcVfQkYW1Z9ki-zhxlkd14SqgddK7oFK6HfXHeLmNtr5MN8EU6-Emxs4lWlRQDFTlj9mZ3BxfO29CJWDAo8UnTZbt7lpoprofFVDAHdpL4qTgmTE8cT04bubRMUY1Zo2Ae0DIif9gRjo"
                mode="aspectFill"
              ></image>
            </view>
            <view class="info">
              <view class="name-row">
                <text class="name">张师傅</text>
                <text class="badge">高级维修员</text>
              </view>
              <text class="order-no">订单号: {{ orderNoDisplay || '—' }}</text>
            </view>
          </view>
        </view>

        <!-- 评分区域 -->
        <view class="section evaluate-rating-section">
          <FormItemAnchor name="ratings" />
          <view class="rating-box">
            <view class="rating-item">
              <text class="label">服务时效</text>
              <uni-rate
                v-model="formData.efficiencyRating"
                :size="24"
                color="#E5E7EB"
                active-color="#f26604"
                :margin="2"
              ></uni-rate>
            </view>
            <view class="rating-item">
              <text class="label">服务质量</text>
              <uni-rate
                v-model="formData.qualityRating"
                :size="24"
                color="#E5E7EB"
                active-color="#f26604"
                :margin="2"
              ></uni-rate>
            </view>
            <view class="rating-item">
              <text class="label">满意度</text>
              <uni-rate
                v-model="formData.satisfactionRating"
                :size="24"
                color="#E5E7EB"
                active-color="#f26604"
                :margin="2"
              ></uni-rate>
            </view>
          </view>
        </view>

        <!-- 快速标签区域 -->
        <view class="section tags-section">
          <text class="section-title">您对服务满意吗？</text>
          <scroll-view scroll-x class="tags-scroll" :show-scrollbar="false">
            <view class="tags-container">
              <view
                v-for="tag in availableTags"
                :key="tag"
                class="tag-item"
                :class="{ active: formData.tags.includes(tag) }"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 反馈输入区域 -->
        <view class="section feedback-section">
          <view class="textarea-wrap">
            <textarea
              v-model="formData.feedback"
              class="feedback-textarea"
              placeholder="请分享您的维修体验..."
              placeholder-class="textarea-placeholder"
              :maxlength="200"
            ></textarea>
            <text class="word-count">{{ formData.feedback.length }}/200</text>
          </view>
        </view>

        <!-- 照片上传区域 -->
        <view class="section upload-section">
          <MediaUploadField
            v-model="formData.photos"
            label="维修成果上传"
            tip=""
            :limit="9"
            file-mediatype="image"
          />
        </view>
      </view>
    </scroll-view>

    <base-button>
      <view class="btn btn-primary" @click="submit">提交评价</view>
    </base-button>
  </view>
</template>

<script setup lang="ts">
  import { reactive, ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { evaluateCustomerWorkOrderAPI } from '@/api/order'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { triggerScrollIntoView } from '@/utils/formFieldScrollFocus'

  const workOrderId = ref(0)
  const orderNoDisplay = ref('')
  const submitting = ref(false)
  const scrollIntoView = ref('')

  onLoad((options?: Record<string, string>) => {
    const raw = options?.id ?? options?.workOrderId
    const n = Number(raw)
    if (Number.isFinite(n) && n > 0) {
      workOrderId.value = n
    }
    if (options?.orderNo) {
      orderNoDisplay.value = decodeURIComponent(options.orderNo)
    }
  })

  // 表单数据
  const formData = reactive({
    efficiencyRating: 5,
    qualityRating: 5,
    satisfactionRating: 5,
    tags: [] as string[],
    feedback: '',
    photos: []
  })

  // 可用标签
  const availableTags = ['专业性强', '准时到达', '维修速度快', '价格公道', '服务态度好']

  /**
   * 将标签追加到反馈内容末尾，保证用户手动输入内容不被覆盖
   * @param tag 标签
   */
  const appendTagToFeedback = (tag: string) => {
    const current = formData.feedback.trimEnd()
    if (!current) {
      formData.feedback = tag
      return
    }

    const needsSeparator = !/[、，,\s]$/.test(current)
    formData.feedback = `${current}${needsSeparator ? '、' : ''}${tag}`
  }

  /**
   * 切换标签
   * @param tag 标签
   */
  const toggleTag = (tag: string) => {
    const index = formData.tags.indexOf(tag)
    if (index > -1) {
      formData.tags.splice(index, 1)
    } else {
      formData.tags.push(tag)
      appendTagToFeedback(tag)
    }
  }

  /**
   * 表单校验（与提交评价接口一致：仅三项评分 + 工单 ID 为必填）
   */
  const validateForm = () => {
    if (!formData.efficiencyRating || !formData.qualityRating || !formData.satisfactionRating) {
      uni.showToast({
        title: '请完成评分',
        icon: 'none',
        duration: 1500
      })
      triggerScrollIntoView(scrollIntoView, 'ratings')
      return false
    }

    return true
  }

  /**
   * 提交评价
   */
  const submit = async () => {
    if (!validateForm()) return
    if (!workOrderId.value) {
      uni.showToast({
        title: '工单参数无效',
        icon: 'none',
        duration: 1500
      })
      return
    }
    if (submitting.value) return
    submitting.value = true
    uni.showLoading({ title: '提交中...', mask: true })
    try {
      const content = formData.feedback.trim()
      const tagsStr = formData.tags.length ? formData.tags.join(',') : ''
      const res = await evaluateCustomerWorkOrderAPI({
        qualityScore: formData.qualityRating,
        satisfactionScore: formData.satisfactionRating,
        timelinessScore: formData.efficiencyRating,
        workOrderId: workOrderId.value,
        ...(content ? { content } : {}),
        ...(tagsStr ? { tags: tagsStr } : {})
      })
      uni.showToast({
        title: res.msg,
        icon: 'success',
        duration: 1500
      })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    } catch {
      /* 失败提示由 http 层处理 */
    } finally {
      uni.hideLoading()
      submitting.value = false
    }
  }
</script>

<style lang="scss">
  .page-content {
    @include flex-column-gap;
  }

  .evaluate-page {
    padding-top: $space-lg;
  }

  .evaluate-rating-section {
    position: relative;
  }

  .evaluate-page .main-content.page-padding {
    flex: 1;
    @include flex-column-gap;
    box-sizing: border-box;
    background-color: $bg-page;
  }

  .evaluate-page .section {
    padding: 0;
  }

  .technician-card {
    @include white-card($radius-xl, $space-lg);
    @include flex-row;

    .avatar-wrap {
      width: 144rpx;
      height: 144rpx;
      border-radius: 50%;
      overflow: hidden;
      background-color: $bg-light;
      flex-shrink: 0;

      .avatar {
        width: 100%;
        height: 100%;
      }
    }

    .info {
      margin-left: $space-lg;
      flex: 1;

      .name-row {
        @include flex-between;

        .name {
          font-size: $font-lg;
          font-weight: 700;
          color: $text-dark;
        }

        .badge {
          font-size: $font-sm;
          background-color: #fff7ed;
          color: $primary;
          padding: $space-xs 20rpx;
          border-radius: $radius-round;
          font-weight: 700;
        }
      }

      .order-no {
        font-size: $font-md;
        color: $text-muted;
        margin-top: $space-xs;
        font-weight: 500;
        display: block;
      }
    }
  }

  .rating-box {
    @include white-card($radius-xl, $space-lg);
    @include flex-column;
    gap: $space-lg;

    .rating-item {
      @include flex-between;

      .label {
        font-size: 26rpx;
        font-weight: 600;
        color: $text-body;
      }
    }
  }

  .tags-section {
    .section-title {
      font-size: $font-md;
      font-weight: 700;
      color: $text-dark;
      margin-bottom: $space-lg;
      display: block;
    }

    .tags-scroll {
      width: 100%;
      white-space: nowrap;

      .tags-container {
        display: inline-flex;
        gap: $space-sm;
      }
    }

    .tag-item {
      padding: 12rpx 24rpx;
      border-radius: $radius-round;
      font-size: $font-sm;
      font-weight: 500;
      color: $text-body;
      background-color: $bg-card;
      border: 2rpx solid $border-light;
      transition: all 0.2s;

      &.active {
        background-color: $primary;
        color: #ffffff;
        border-color: $primary;
        font-weight: 600;
        box-shadow: 0 4rpx 12rpx rgba($primary, 0.15);
      }
    }
  }

  .feedback-section {
    padding: 0;
  }

  .textarea-wrap {
    position: relative;
    background-color: $bg-card;
    border: 2rpx solid $bg-card;
    border-radius: $radius-xl;
    overflow: hidden;
    transition: all 0.2s;

    &:focus-within {
      background-color: #ffffff;
      border-color: rgba($primary, 0.3);
    }

    .feedback-textarea {
      width: 100%;
      height: 288rpx;
      padding: $space-lg;
      background-color: transparent;
      border: none;
      border-radius: 0;
      font-size: 26rpx;
      line-height: 1.6;
      box-sizing: border-box;

      &:focus {
        outline: none;
      }
    }

    .textarea-placeholder {
      color: $text-muted;
    }

    .word-count {
      position: absolute;
      bottom: $space-sm;
      right: $space-md;
      font-size: $font-xs;
      font-weight: 500;
      color: $text-muted;
    }
  }

  .upload-section {
    :deep(.shipping-label) {
      margin-bottom: 20rpx;
      font-size: $font-md;
      font-weight: 700;
      color: $text-dark;
    }
  }
</style>
