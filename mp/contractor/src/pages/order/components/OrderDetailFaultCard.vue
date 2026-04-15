<template>
  <view v-if="visible" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">故障信息</text>
    </view>
    <view class="od-fault-details">
      <view v-if="!isOtherFault && hasVal(fault.desc)" class="detail-group">
        <text class="group-title">故障描述</text>
        <text class="group-content">{{ fault.desc }}</text>
      </view>
      <view v-if="isOtherFault && hasVal(fault.faultExplain)" class="detail-group">
        <text class="group-title">故障说明</text>
        <text class="group-content">{{ fault.faultExplain }}</text>
      </view>
      <view v-if="hasVal(fault.voiceDuration)" class="detail-group">
        <text class="group-title">语音说明</text>
        <view class="voice-msg">
          <image class="voice-icon" :src="volumeUpIcon" mode="aspectFit" />
          <view class="voice-waves">
            <view class="wave wave-1"></view>
            <view class="wave wave-2"></view>
            <view class="wave wave-3"></view>
            <view class="wave wave-4"></view>
          </view>
          <text class="voice-duration">{{ fault.voiceDuration }}</text>
        </view>
      </view>
      <view v-if="hasFaultVideoOrImage" class="detail-group">
        <text class="group-title">故障视频/图片</text>
        <view class="fault-media-grid">
          <view
            v-for="(vurl, idx) in faultVideos"
            :key="'fault-vid-' + idx"
            class="fault-media-cell fault-media-tap"
            @tap="onFaultVideoTap(vurl)"
          >
            <video
              class="fault-video fault-video-thumb"
              :src="displayMediaUrl(vurl)"
              object-fit="cover"
              :muted="true"
              :controls="false"
              :show-center-play-btn="false"
              :enable-progress-gesture="false"
            />
            <view class="fault-video-mask">
              <view class="fault-play-btn">
                <view class="fault-play-icon"></view>
              </view>
            </view>
          </view>
          <view
            v-for="(img, idx) in faultImagesResolved"
            :key="'fault-img-' + idx"
            class="fault-media-cell fault-media-tap"
            @tap="onFaultImageTap(idx)"
          >
            <image class="fault-img-thumb" mode="aspectFill" :src="img" />
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { volumeUpIcon } from '@/svgs'
  import { previewImages, previewVideo, resolvePreviewableUrl } from '@/utils/mediaPreview'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    fault: OrderDetail['fault']
  }>()

  const isOtherFault = computed(() => props.fault.desc === '其它故障')

  const faultVideos = computed(() =>
    Array.isArray(props.fault.videos)
      ? props.fault.videos.map((u) => String(u || '').trim()).filter(Boolean)
      : []
  )

  const faultImagesResolved = computed(() =>
    (Array.isArray(props.fault.images) ? props.fault.images : [])
      .map((u) => resolvePreviewableUrl(u))
      .filter(Boolean)
  )

  const displayMediaUrl = (u: string) => resolvePreviewableUrl(u)

  function onFaultImageTap(index: number) {
    const urls = faultImagesResolved.value
    if (!urls.length) return
    previewImages(urls, index)
  }

  function onFaultVideoTap(url: string) {
    previewVideo(url)
  }

  /** 对应详情接口 faultVideoFiles / faultImageFiles 映射到 fault.videos、fault.images */
  const hasFaultVideoOrImage = computed(
    () =>
      faultVideos.value.length > 0 ||
      (Array.isArray(props.fault.images) && props.fault.images.length > 0)
  )

  const visible = computed(() => {
    const f = props.fault
    const other = f.desc === '其它故障'
    const textOk = other ? hasVal(f.faultExplain) : hasVal(f.desc)
    return textOk || hasVal(f.voiceDuration) || hasFaultVideoOrImage.value
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';

  .fault-video-thumb {
    display: block;
    pointer-events: none;
  }

  .fault-img-thumb {
    width: 100%;
    height: 100%;
    display: block;
  }

  .fault-video-mask {
    position: absolute;
    inset: 0;
    z-index: 1;
    @include flex-center;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.08) 0%, rgba(0, 0, 0, 0.38) 100%);
    border-radius: $radius-md;
  }

  .fault-play-btn {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    @include flex-center;
    background-color: rgba(0, 0, 0, 0.45);
    border: 2rpx solid rgba(255, 255, 255, 0.85);
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  }

  .fault-play-icon {
    width: 0;
    height: 0;
    margin-left: 8rpx;
    border-style: solid;
    border-width: 16rpx 0 16rpx 26rpx;
    border-color: transparent transparent transparent #fff;
  }
</style>
