<template>
  <custom-nav-bar
    title="服务评价"
    surface="frosted"
    :back-icon-size="24"
    :back-icon-color="themeColors.textSubtle"
  />
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
              <image class="avatar" :src="technicianAvatarSrc" mode="aspectFill" />
            </view>
            <view class="info">
              <view class="name-row">
                <text class="name">{{ technicianDisplayName || '—' }}</text>
                <text v-if="technicianOrgLabel" class="badge">{{ technicianOrgLabel }}</text>
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
                :color="themeColors.borderNeutral"
                :active-color="themeColors.primary"
                :margin="2"
              ></uni-rate>
            </view>
            <view class="rating-item">
              <text class="label">服务质量</text>
              <uni-rate
                v-model="formData.qualityRating"
                :size="24"
                :color="themeColors.borderNeutral"
                :active-color="themeColors.primary"
                :margin="2"
              ></uni-rate>
            </view>
            <view class="rating-item">
              <text class="label">满意度</text>
              <uni-rate
                v-model="formData.satisfactionRating"
                :size="24"
                :color="themeColors.borderNeutral"
                :active-color="themeColors.primary"
                :margin="2"
              ></uni-rate>
            </view>
          </view>
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
        <view v-if="showEvaluatePhotoUpload" class="section upload-section">
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
  import { computed, reactive, ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { evaluateCustomerWorkOrder, getCustomerWorkOrder } from '@/api/workOrder'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { triggerScrollIntoView } from '@/utils/formFieldScrollFocus'
  import { themeColors } from '@/constants/theme'

  const workOrderId = ref(0)
  const orderNoDisplay = ref('')
  const technicianDisplayName = ref('')
  const technicianOrgLabel = ref('')
  const technicianAvatar = ref('')
  const submitting = ref(false)
  const scrollIntoView = ref('')

  const DEFAULT_TECHNICIAN_AVATAR = '/static/images/worker.png'

  const technicianAvatarSrc = computed(() => {
    const u = technicianAvatar.value.trim()
    return u || DEFAULT_TECHNICIAN_AVATAR
  })

  /**
   * 与工单详情同源：拉取详情填充维修员与工单号展示
   */
  const loadDetailForHeader = async () => {
    const id = workOrderId.value
    if (!id) return
    try {
      const res = await getCustomerWorkOrder({ id: String(id) })
      const d = res.data
      if (!d) return
      const no = String(d.base?.orderNo ?? '').trim()
      if (no) orderNoDisplay.value = no
      const t = d.technician
      if (t) {
        technicianDisplayName.value = String(t.name ?? '').trim()
        technicianOrgLabel.value = String(t.orgLabel ?? '').trim()
        technicianAvatar.value = String(t.avatar ?? '').trim()
      }
    } catch {
      /* 失败时保留路由传入的订单号，头像使用默认图 */
    }
  }

  /** 与 `list.vue` 中 `navigateTo` 的 `events` 键名一致，用于返回前通知上一页刷新列表 */
  const WORK_ORDER_EVALUATED_EVENT = 'workOrderEvaluated'

  type OpenerEventChannel = { emit: (eventName: string, ...args: unknown[]) => void }

  let openerEventChannel: OpenerEventChannel | undefined

  onLoad((options?: Record<string, string>) => {
    const pages = getCurrentPages()
    const cur = pages[pages.length - 1] as { getOpenerEventChannel?: () => OpenerEventChannel }
    openerEventChannel = cur.getOpenerEventChannel?.()

    const raw = options?.id ?? options?.workOrderId
    const n = Number(raw)
    if (Number.isFinite(n) && n > 0) {
      workOrderId.value = n
    }
    if (options?.orderNo) {
      orderNoDisplay.value = decodeURIComponent(options.orderNo)
    }
    void loadDetailForHeader()
  })

  // 表单数据
  const formData = reactive({
    efficiencyRating: 5,
    qualityRating: 5,
    satisfactionRating: 5,
    feedback: '',
    photos: []
  })

  /** 为 false 时隐藏维修成果上传 */
  const showEvaluatePhotoUpload = false

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
      const res = await evaluateCustomerWorkOrder({
        qualityScore: formData.qualityRating,
        satisfactionScore: formData.satisfactionRating,
        timelinessScore: formData.efficiencyRating,
        workOrderId: workOrderId.value,
        ...(content ? { content } : {})
      })
      uni.showToast({
        title: res.msg,
        icon: 'none',
        duration: 1500
      })
      openerEventChannel?.emit(WORK_ORDER_EVALUATED_EVENT)
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
          background-color: $primary-tint-bg;
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
      background-color: $bg-card;
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
