<template>
  <view class="evaluate-page">
    <!-- 技师信息卡片 -->
    <view class="section technician-section">
      <view class="card">
        <view class="avatar-wrapper">
          <image
            class="avatar"
            src="https://lh3.googleusercontent.com/aida-public/AB6AXuD6WAmZjNixQ0DFB7uvPBuqPVdEjienUwTzvUxHj3gzGu-O7sPvnsMMht-aoO4C57OpIbWPMnsXZ0wUs0O6PBCWg57lJv-r_lFtp-J_Af9_Ru_PesWm1Msz6ZYbHO4FFwmXcVfQkYW1Z9ki-zhxlkd14SqgddK7oFK6HfXHeLmNtr5MN8EU6-Emxs4lWlRQDFTlj9mZ3BxfO29CJWDAo8UnTZbt7lpoprofFVDAHdpL4qTgmTE8cT04bubRMUY1Zo2Ae0DIif9gRjo"
            mode="aspectFill"
          ></image>
        </view>
        <view class="info">
          <view class="header">
            <text class="name">张师傅</text>
            <text class="tag">高级维修员</text>
          </view>
          <text class="order-id">订单号: 202308159942</text>
        </view>
      </view>
    </view>

    <!-- 评价星级 -->
    <view class="section rating-section">
      <text class="label">您的评价对我们很重要</text>
      <view class="stars">
        <view v-for="star in 5" :key="star" class="star-btn" @click="setRating(star)">
          <!-- SVG Star -->
          <view class="star-icon" :class="{ active: star <= rating }">
            <svg
              width="40"
              height="40"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M12 17.27L18.18 21L16.54 13.97L22 9.24L14.81 8.63L12 2L9.19 8.63L2 9.24L7.46 13.97L5.82 21L12 17.27Z"
                :fill="star <= rating ? '#f26604' : '#E5E7EB'"
              />
            </svg>
          </view>
        </view>
      </view>
      <text class="rating-text">{{ ratingText }}</text>
    </view>

    <!-- 快速标签 -->
    <view class="section tags-section">
      <text class="section-title">您对服务满意吗？</text>
      <view class="tags">
        <view
          v-for="(tag, index) in tags"
          :key="index"
          class="tag-btn"
          :class="{ active: selectedTags.includes(tag) }"
          @click="toggleTag(tag)"
        >
          <text>{{ tag }}</text>
        </view>
      </view>
    </view>

    <!-- 反馈输入 -->
    <view class="section feedback-section">
      <view class="textarea-wrapper">
        <textarea
          v-model="feedbackText"
          class="feedback-input"
          placeholder="请分享您的维修体验..."
          maxlength="200"
        ></textarea>
        <text class="counter">{{ feedbackText.length }}/200</text>
      </view>
    </view>

    <!-- 照片上传 -->
    <view class="section upload-section">
      <text class="section-title">维修成果上传</text>
      <view class="upload-grid">
        <view v-for="(img, index) in uploadedImages" :key="index" class="image-item">
          <image :src="img" mode="aspectFill" class="uploaded-img"></image>
          <view class="delete-btn" @click="removeImage(index)">
            <uni-icons type="closeempty" size="16" color="#fff"></uni-icons>
          </view>
        </view>

        <view v-if="uploadedImages.length < 3" class="upload-btn" @click="chooseImage">
          <uni-icons type="plusempty" size="30" color="#9ca3af"></uni-icons>
        </view>
      </view>
    </view>

    <!-- 底部提交 -->
    <view class="footer">
      <button class="submit-btn" @click="submitEvaluation">提交评价</button>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed } from 'vue'
  // 评价星级
  const rating = ref(5)
  // 反馈文本
  const feedbackText = ref('')
  // 选择标签
  const selectedTags = ref<string[]>(['准时到达'])
  // 上传图片
  const uploadedImages = ref<string[]>([
    'https://lh3.googleusercontent.com/aida-public/AB6AXuABRIpakNajK09mois0JGGSkTMw1rOuLlzJ6AZ7H7ZEnymbcCMc4Iq8NjZwr7AwbqJfAHjAFaYpo9qI6mZJKW4WE17Rq_Lwepl1mYsK66n09KDjKBn3Rhn2SOgdHTra7CD9GBRCQK5PG5GBUpwwF9F2NB24obq41tUTtyFupM939kyNjBm_qIH9UILAwt7vYqSvgi8ad_Pxs9QbI2baYvcD0Qx9utVMFjuUUuzD38rw2LJztPo44T35Er0CFi3A61333AMFK5Ws3po'
  ])

  // 标签
  const tags = ['专业性强', '准时到达', '维修速度快', '价格公道', '服务态度好']

  /**
   * 评价星级文本
   * @returns 评价星级文本
   */
  const ratingText = computed(() => {
    return `非常好 (${rating.value}/5)`
  })

  /**
   * 设置评价星级
   * @param val 评价星级
   * @returns void
   */
  const setRating = (val: number) => {
    rating.value = val
  }

  /**
   * 切换标签
   * @param tag 标签
   * @returns void
   */
  const toggleTag = (tag: string) => {
    const index = selectedTags.value.indexOf(tag)
    if (index > -1) {
      selectedTags.value.splice(index, 1)
    } else {
      selectedTags.value.push(tag)
    }
  }

  /**
   * 选择图片
   * @returns void
   */
  const chooseImage = () => {
    uni.chooseImage({
      count: 3 - uploadedImages.value.length,
      success: (res) => {
        uploadedImages.value.push(
          ...(Array.isArray(res.tempFilePaths) ? res.tempFilePaths : [res.tempFilePaths])
        )
      }
    })
  }

  /**
   * 删除图片
   * @param index 图片索引
   * @returns void
   */
  const removeImage = (index: number) => {
    uploadedImages.value.splice(index, 1)
  }

  /**
   * 提交评价
   * @returns void
   */
  const submitEvaluation = () => {
    // Submit logic here
    console.log({
      rating: rating.value,
      tags: selectedTags.value,
      feedback: feedbackText.value,
      images: uploadedImages.value
    })
    uni.showToast({
      title: '评价提交成功',
      icon: 'success'
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }
</script>

<style lang="scss" scoped>
  .evaluate-page {
    min-height: 100vh;
    background-color: $surface-white;
    padding-bottom: calc(100rpx + env(safe-area-inset-bottom));
    @include flex-col;
  }

  .section {
    padding: $space-lg;
  }

  .technician-section {
    .card {
      @include flex-row;
      background-color: $surface-white;
      border-radius: $radius-lg;
      padding: $space-lg;
      box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.05);
      border: 1px solid $surface-slate-50;

      .avatar-wrapper {
        position: relative;
        width: 128rpx;
        height: 128rpx;
        border-radius: 50%;
        overflow: hidden;
        border: 4rpx solid $surface-slate-50;

        .avatar {
          width: 100%;
          height: 100%;
        }
      }

      .info {
        margin-left: $space-lg;
        flex: 1;

        .header {
          @include flex-between;
          margin-bottom: $space-xs;

          .name {
            font-size: 36rpx;
            font-weight: bold;
            color: $text-strong;
          }

          .tag {
            font-size: $font-sm;
            background-color: $tag-brand-bg;
            color: $primary;
            padding: 4rpx $space-sm;
            border-radius: $radius-pill;
            font-weight: 500;
          }
        }

        .order-id {
          font-size: $font-md;
          color: $text-slate-500;
        }
      }
    }
  }

  .rating-section {
    text-align: center;
    padding-top: $space-xl;
    padding-bottom: $space-xl;

    .label {
      font-size: $font-lg;
      color: $text-slate-600;
      font-weight: 500;
      display: block;
      margin-bottom: $space-lg;
    }

    .stars {
      @include flex-center;
      gap: $space-md;
      margin-bottom: $space-md;

      .star-btn .star-icon {
        width: 80rpx;
        height: 80rpx;
        @include flex-center;

        &.active {
          filter: drop-shadow(0 4rpx 8rpx rgba($primary, 0.2));
        }
      }
    }

    .rating-text {
      color: $primary;
      font-weight: 600;
      font-size: $font-lg;
    }
  }

  .tags-section {
    .section-title {
      font-size: $font-lg;
      font-weight: 600;
      margin-bottom: $space-lg;
      display: block;
      color: $text-strong;
    }

    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: $space-md;

      .tag-btn {
        padding: $space-sm $space-lg;
        border-radius: $radius-pill;
        border: 1px solid $surface-slate-200;
        background-color: $surface-white;
        transition: all 0.2s;

        text {
          font-size: $font-md;
          color: $text-slate-600;
        }

        &.active {
          background-color: $primary;
          border-color: $primary;
          box-shadow: 0 4rpx 12rpx rgba($primary, 0.2);

          text {
            color: $surface-white;
            font-weight: 500;
          }
        }
      }
    }
  }

  .feedback-section {
    .textarea-wrapper {
      position: relative;
      background-color: $surface-slate-50;
      border-radius: $radius-lg;
      padding: $space-lg;

      .feedback-input {
        width: 100%;
        height: 200rpx;
        font-size: $font-md;
        color: $text-strong;
        background-color: transparent;
      }

      .counter {
        position: absolute;
        bottom: $space-md;
        right: $space-lg;
        font-size: $font-sm;
        color: $text-slate-400;
      }
    }
  }

  .upload-section {
    .section-title {
      font-size: $font-lg;
      font-weight: 600;
      margin-bottom: $space-lg;
      display: block;
      color: $text-strong;
    }

    .upload-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: $space-md;

      .image-item,
      .upload-btn {
        position: relative;
        width: 100%;
        padding-bottom: 100%;
        border-radius: $radius-lg;
        overflow: hidden;
      }

      .image-item {
        .uploaded-img {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
        }

        .delete-btn {
          position: absolute;
          top: $space-xs;
          right: $space-xs;
          background-color: rgba(0, 0, 0, 0.5);
          border-radius: 50%;
          width: 40rpx;
          height: 40rpx;
          @include flex-center;
          backdrop-filter: blur(4px);
          z-index: 10;
        }
      }

      .upload-btn {
        border: 2px dashed $surface-slate-200;
        @include flex-center;
        background-color: $surface-white;

        &::after {
          content: '';
          display: block;
        }
      }
    }
  }

  .footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: $space-lg;
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(20rpx);
    border-top: 1px solid $surface-slate-100;
    padding-bottom: calc(#{$space-lg} + env(safe-area-inset-bottom));
    z-index: 100;

    .submit-btn {
      width: 100%;
      height: 100rpx;
      line-height: 100rpx;
      background-color: $primary;
      color: $surface-white;
      border-radius: $radius-pill;
      font-size: 36rpx;
      font-weight: 600;
      box-shadow: 0 8rpx 24rpx rgba($primary, 0.3);
      border: none;

      &:active {
        transform: scale(0.98);
        opacity: 0.9;
      }
    }
  }
</style>
