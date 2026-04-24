<template>
  <view v-if="visible" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">故障信息</text>
    </view>
    <view class="od-fault-details">
      <view v-if="showFaultDescGroup" class="detail-group">
        <text class="group-title">故障描述</text>
        <text class="group-content">{{ fault.desc }}</text>
      </view>
      <view v-if="showFaultRemarkGroup" class="od-info-list fault-remark-as-quote">
        <view class="info-item-col">
          <text class="info-label">故障说明备注</text>
          <view class="desc-box">
            <text class="desc-text">{{ faultRemarkDisplayText }}</text>
          </view>
        </view>
      </view>
      <view v-if="hasFaultVoiceSection" class="detail-group">
        <text class="group-title">语音说明</text>
        <VoicePlaybackList
          v-if="faultVoicePlaybackItems.length"
          :items="faultVoicePlaybackItems"
        />
        <view v-else class="voice-msg">
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
        <text class="group-title">故障图片/视频</text>
        <view class="image-grid">
          <image
            v-for="(img, idx) in faultImagesResolved"
            :key="'fi-' + idx"
            class="grid-img"
            mode="aspectFill"
            :src="img"
            @tap="onFaultImageTap(idx)"
          />
          <view
            v-for="(vurl, idx) in faultVideos"
            :key="'fv-' + idx"
            class="video-thumbnail"
            @tap="onFaultVideoTap(vurl)"
          >
            <video
              class="grid-img fault-grid-video"
              :src="displayMediaUrl(vurl)"
              object-fit="cover"
              :muted="true"
              :controls="false"
              :show-center-play-btn="false"
              :enable-progress-gesture="false"
            />
            <view class="play-overlay">
              <view class="play-icon-fallback"></view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import VoicePlaybackList from '@/components/VoicePlaybackList/VoicePlaybackList.vue'
  import type { VoicePlaybackItem } from '@/components/VoicePlaybackList/VoicePlaybackList.vue'
  import { volumeUpIcon } from '@/svgs'
  import { previewImages, previewVideo, resolvePreviewableUrl } from '@/utils/mediaPreview'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    fault: OrderDetail['fault']
  }>()

  const faultDescTrimmed = computed(() => String(props.fault.desc ?? '').trim())

  const isFaultOtherExact = computed(() => {
    const d = faultDescTrimmed.value
    return d === '其它故障' || d === '其他故障'
  })

  /** 与 mp/aftersale `detail.vue` 一致：含「其它故障；3」等 */
  const faultDescContainsOtherFault = computed(() => {
    const raw = faultDescTrimmed.value
    if (!raw) return false
    if (raw.includes('其它故障') || raw.includes('其他故障')) return true
    const parts = raw
      .split(/[；;]+/)
      .map((s) => s.trim())
      .filter(Boolean)
    if (parts.some((p) => p === '其它故障' || p === '其他故障')) return true
    if (parts.some((p) => /(其它|其他)/.test(p) && /故障/.test(p))) return true
    return /(其它|其他)/.test(raw) && /故障/.test(raw)
  })

  const faultRemarkTrimmed = computed(() => String(props.fault.faultExplain ?? '').trim())
  const faultRemarkDisplayText = computed(() => faultRemarkTrimmed.value || '—')

  const showFaultDescGroup = computed(
    () => !isFaultOtherExact.value && hasVal(faultDescTrimmed.value)
  )

  const showFaultRemarkGroup = computed(
    () => faultDescContainsOtherFault.value || hasVal(faultRemarkTrimmed.value)
  )

  const faultVoicePlaybackItems = computed((): VoicePlaybackItem[] => {
    const list = props.fault.voiceList
    if (!Array.isArray(list) || !list.length) return []
    return list
      .filter((x) => hasVal(x?.url))
      .map((x) => ({ url: String(x.url).trim(), duration: x.duration }))
  })

  const hasFaultVoiceSection = computed(
    () => faultVoicePlaybackItems.value.length > 0 || hasVal(props.fault.voiceDuration)
  )

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

  const hasFaultVideoOrImage = computed(
    () =>
      faultVideos.value.length > 0 ||
      (Array.isArray(props.fault.images) && props.fault.images.length > 0)
  )

  const visible = computed(() => {
    const textOk =
      hasVal(faultDescTrimmed.value) ||
      hasVal(faultRemarkTrimmed.value) ||
      faultDescContainsOtherFault.value
    return textOk || hasFaultVoiceSection.value || hasFaultVideoOrImage.value
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
