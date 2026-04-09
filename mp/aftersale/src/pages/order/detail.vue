<template>
  <custom-nav-bar
    title="工单详情"
    surface="sticky"
    color="#ffffff"
    background="#f26604"
    :shadow="false"
  />
  <view class="page-container page-index order-detail-page">
    <!-- 导航栏 -->
    <view class="top-section">
      <!-- 工单状态 -->
      <view class="status-banner">
        <view class="status-top">
          <view class="status-text-wrap">
            <text class="status-title">{{ orderStatus }}</text>
            <text class="status-desc">{{ statusDesc }}</text>
          </view>
          <image v-if="statusIconSrc" class="status-icon" :src="statusIconSrc" mode="aspectFit" />
        </view>

        <!-- 进度条 -->
        <view class="stepper-wrap">
          <view class="stepper-line"></view>
          <view v-for="(step, index) in steps" :key="index" class="step-item">
            <view :class="['step-dot', index <= stepIndex ? 'active' : '']"></view>
            <text :class="['step-text', index <= stepIndex ? 'active' : '']">{{ step }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="main-content page-padding" :class="{ 'has-fixed-bottom-btn': currentTab === 0 }">
      <view class="content-wrap">
        <!-- 标签容器 -->
        <view class="tab-container">
          <!-- 标签栏 -->
          <view class="tab-bar">
            <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">
              <text class="tab-text">申请内容</text>
              <view v-if="currentTab === 0" class="tab-line"></view>
            </view>
            <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">
              <text class="tab-text">维修过程</text>
              <view v-if="currentTab === 1" class="tab-line"></view>
            </view>
            <view
              v-if="showEvaluateTab"
              class="tab-item"
              :class="{ active: currentTab === 2 }"
              @click="currentTab = 2"
            >
              <text class="tab-text">客户评价</text>
              <view v-if="currentTab === 2" class="tab-line"></view>
            </view>
          </view>

          <!-- 申请内容 - 工单信息 -->
          <view v-if="currentTab === 0 && hasBaseInfo" class="section-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">工单信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(order.base.orderNo)" class="info-item">
                <text class="info-label">工单编号</text>
                <text class="info-value">{{ order.base.orderNo }}</text>
              </view>
              <view v-if="hasStr(order.base.orderTypeName)" class="info-item">
                <text class="info-label">工单类型</text>
                <text class="info-value text-primary">{{ order.base.orderTypeName }}</text>
              </view>
              <view v-if="hasStr(order.base.submitTime)" class="info-item">
                <text class="info-label">提交时间</text>
                <text class="info-value">{{ order.base.submitTime }}</text>
              </view>
            </view>
          </view>

          <!-- 维修过程 - 待接单时显示故障信息 -->
          <view
            v-if="currentTab === 1 && orderStatus === '待接单' && hasFaultPanelContent"
            class="section-box"
          >
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">故障信息</text>
            </view>
            <view class="fault-details">
              <view v-if="!isFaultOther && hasStr(order.fault.desc)" class="detail-group">
                <text class="group-title">故障描述</text>
                <text class="group-content">{{ order.fault.desc }}</text>
              </view>
              <view v-else-if="isFaultOther && hasStr(order.fault.remark)" class="detail-group">
                <text class="group-title">故障备注说明</text>
                <text class="group-content">{{ order.fault.remark }}</text>
              </view>
              <view v-if="showFaultVoiceSection" class="detail-group">
                <text class="group-title">语音说明</text>
                <VoicePlaybackList
                  v-if="faultVoicePlaybackItems.length"
                  :items="faultVoicePlaybackItems"
                />
                <text v-else class="group-content">{{ order.fault.voiceDuration }}</text>
              </view>
              <view v-if="hasFaultMedia" class="detail-group">
                <text class="group-title">故障图片</text>
                <view class="image-grid">
                  <image
                    v-for="(img, idx) in order.fault.images"
                    :key="'fi-' + idx"
                    class="grid-img"
                    mode="widthFix"
                    :src="img"
                    @click="previewFaultImages(img)"
                  ></image>
                  <view v-if="hasFaultVideo" class="video-thumbnail" @click="previewFaultVideo">
                    <image class="grid-img" mode="widthFix" :src="videoCoverSrc"></image>
                    <view class="play-overlay">
                      <image class="play-icon" :src="playCircleIcon" mode="aspectFit" />
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <!-- 维修过程 - 非待接单时显示维修信息 -->
          <view
            v-if="currentTab === 1 && orderStatus !== '待接单' && hasRepairTabInfo"
            class="section-box"
          >
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">维修信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(order.repair.faultJudge)" class="info-item">
                <text class="info-label">故障判定</text>
                <text class="info-value">{{ order.repair.faultJudge }}</text>
              </view>
              <view v-if="hasStr(order.repair.quoteAmount)" class="info-item">
                <text class="info-label">维修报价</text>
                <text class="info-value text-primary" style="font-size: 36rpx; font-weight: bold"
                  >¥ {{ order.repair.quoteAmount }}</text
                >
              </view>
              <view v-if="hasStr(order.repair.quoteDesc)" class="info-item-col">
                <text class="info-label" style="margin-bottom: 16rpx; display: block"
                  >报价说明</text
                >
                <view class="desc-box">
                  <text class="desc-text">{{ order.repair.quoteDesc }}</text>
                </view>
              </view>

              <!-- 已完成/已关闭 额外字段 -->
              <template v-if="orderStatus === '已完成' || orderStatus === '已关闭'">
                <view v-if="hasStr(order.repair.repairTime)" class="info-item mt-16">
                  <text class="info-label">维修时间</text>
                  <text class="info-value">{{ order.repair.repairTime }}</text>
                </view>
                <view v-if="hasStr(order.repair.returnMethod)" class="info-item">
                  <text class="info-label">机器返回方式</text>
                  <text class="info-value">{{ order.repair.returnMethod }}</text>
                </view>
                <view
                  v-if="
                    hasStr(order.repair.returnReceiverTitle) || hasStr(order.repair.returnAddress)
                  "
                  class="info-item-col border-top"
                >
                  <text class="info-label" style="margin-bottom: 16rpx; display: block"
                    >回寄信息</text
                  >
                  <view class="desc-box">
                    <text
                      v-if="hasStr(order.repair.returnReceiverTitle)"
                      class="desc-title"
                      style="
                        display: block;
                        font-weight: 500;
                        font-size: 24rpx;
                        color: #0f172a;
                        margin-bottom: 8rpx;
                      "
                      >{{ order.repair.returnReceiverTitle }}</text
                    >
                    <text v-if="hasStr(order.repair.returnAddress)" class="desc-text">{{
                      order.repair.returnAddress
                    }}</text>
                  </view>
                </view>
                <view
                  v-if="
                    hasStr(order.repair.returnExpressNo) ||
                    hasStr(order.repair.returnExpressVoucherImg)
                  "
                  class="info-item-col"
                >
                  <text class="info-label" style="margin-bottom: 16rpx; display: block"
                    >回寄快递单号</text
                  >
                  <view
                    class="shipping-card"
                    @click="previewSingleImage(order.repair.returnExpressVoucherImg)"
                  >
                    <image
                      v-if="hasStr(order.repair.returnExpressVoucherImg)"
                      class="shipping-bg"
                      mode="aspectFill"
                      :src="order.repair.returnExpressVoucherImg"
                    ></image>
                    <view class="shipping-overlay">
                      <image class="icon" :src="localShippingIcon" mode="aspectFit" />
                      <text v-if="hasStr(order.repair.returnExpressNo)" class="number">{{
                        order.repair.returnExpressNo
                      }}</text>
                    </view>
                    <view class="shipping-tag">物流凭证</view>
                  </view>
                </view>
              </template>
            </view>
          </view>

          <!-- 客户评价 -->
          <view v-if="showEvaluateTab && currentTab === 2" class="section-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">客户评价</text>
            </view>
            <view v-if="order.evaluate && hasEvaluateContent" class="eval-list">
              <view v-if="(order.evaluate?.timeliness ?? 0) > 0" class="eval-item">
                <text class="eval-label">服务时效</text>
                <view class="eval-stars">
                  <text
                    v-for="i in 5"
                    :key="'t' + i"
                    :class="['star-char', i <= (order.evaluate?.timeliness || 0) ? 'active' : '']"
                    >★</text
                  >
                </view>
              </view>
              <view v-if="(order.evaluate?.quality ?? 0) > 0" class="eval-item">
                <text class="eval-label">维修质量</text>
                <view class="eval-stars">
                  <text
                    v-for="i in 5"
                    :key="'q' + i"
                    :class="['star-char', i <= (order.evaluate?.quality || 0) ? 'active' : '']"
                    >★</text
                  >
                </view>
              </view>
              <view v-if="(order.evaluate?.satisfaction ?? 0) > 0" class="eval-item">
                <text class="eval-label">服务满意度</text>
                <view class="eval-stars">
                  <text
                    v-for="i in 5"
                    :key="'s' + i"
                    :class="['star-char', i <= (order.evaluate?.satisfaction || 0) ? 'active' : '']"
                    >★</text
                  >
                </view>
              </view>
              <view v-if="hasStr(order.evaluate?.comment)" class="eval-comment">
                <text class="eval-label">评价内容</text>
                <view class="eval-comment-box">
                  <text class="eval-comment-text">{{ order.evaluate.comment }}</text>
                </view>
              </view>
            </view>
            <view v-else class="empty-state">
              <text class="empty-text">暂无评价信息</text>
            </view>
          </view>
        </view>

        <!-- 外部卡片 -->
        <template v-if="currentTab === 0">
          <!-- 商品信息（任一有值则显示整块） -->
          <view
            v-if="
              hasStr(order.product.barcode) ||
              hasStr(order.product.model) ||
              hasStr(order.product.serialNo) ||
              (isNonJasicBrand && hasStr(order.product.brandName))
            "
            class="card-box"
          >
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">商品信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(order.product.barcode)" class="info-item">
                <text class="info-label">条形码</text>
                <text class="info-value">{{ order.product.barcode }}</text>
              </view>
              <view v-if="hasStr(order.product.model)" class="info-item">
                <text class="info-label">机器型号</text>
                <text class="info-value">{{ order.product.model }}</text>
              </view>
              <view v-if="hasStr(order.product.serialNo)" class="info-item">
                <text class="info-label">机器小号</text>
                <text class="info-value">{{ order.product.serialNo }}</text>
              </view>
              <view v-if="isNonJasicBrand && hasStr(order.product.brandName)" class="info-item">
                <text class="info-label">品牌</text>
                <text class="info-value">{{ order.product.brandName }}</text>
              </view>
            </view>
          </view>

          <!-- 服务信息 -->
          <view v-if="hasServiceInfo" class="card-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">服务信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(order.service.sitePhone)" class="info-item">
                <text class="info-label">网点电话</text>
                <text class="info-value">{{ order.service.sitePhone }}</text>
              </view>
              <view v-if="hasStr(serviceModeLabel)" class="info-item">
                <text class="info-label">维修方式</text>
                <view class="tag-primary">{{ serviceModeLabel }}</view>
              </view>
              <!-- 到店时不显示寄件信息和寄件快递单号 -->
              <template v-if="serviceModeLabel !== '到店'">
                <view v-if="hasStr(order.service.senderInfo)" class="info-item align-top">
                  <text class="info-label shrink">寄件信息</text>
                  <text class="info-value text-right">{{ order.service.senderInfo }}</text>
                </view>
                <view v-if="hasStr(order.service.senderVoucherImg)" class="info-item align-center">
                  <text class="info-label">寄件快递单号</text>
                  <image
                    class="shipping-img"
                    mode="aspectFill"
                    :src="order.service.senderVoucherImg"
                    @click="previewSingleImage(order.service.senderVoucherImg)"
                  ></image>
                </view>
              </template>
            </view>
          </view>

          <!-- 联系受理网点 -->
          <base-button>
            <view class="btn btn-primary action-wrap" @click="goToContact">
              <image class="btn-icon" :src="contactPhoneIcon" mode="aspectFit" />联系受理网点
            </view>
          </base-button>
        </template>

        <!-- ===== Tab 1 外部卡片 ===== -->
        <template v-if="currentTab === 1">
          <!-- 受理方信息（非待接单） -->
          <view v-if="orderStatus !== '待接单' && hasAcceptorInfo" class="card-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">受理方信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(order.acceptor.sitePhone)" class="info-item">
                <text class="info-label">网点电话</text>
                <text class="info-value">{{ order.acceptor.sitePhone }}</text>
              </view>
              <view v-if="hasStr(order.acceptor.acceptorName)" class="info-item align-top">
                <text class="info-label shrink">受理方</text>
                <text class="info-value text-right">{{ order.acceptor.acceptorName }}</text>
              </view>
            </view>
          </view>

          <!-- 故障信息（非待接单 — 待接单已在 tab 容器内显示） -->
          <view v-if="orderStatus !== '待接单' && hasFaultPanelContent" class="card-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">故障信息</text>
            </view>
            <view class="fault-details">
              <view v-if="!isFaultOther && hasStr(order.fault.desc)" class="detail-group">
                <text class="group-title">故障描述</text>
                <text class="group-content">{{ order.fault.desc }}</text>
              </view>
              <view v-else-if="isFaultOther && hasStr(order.fault.remark)" class="detail-group">
                <text class="group-title">故障备注说明</text>
                <text class="group-content">{{ order.fault.remark }}</text>
              </view>
              <view v-if="showFaultVoiceSection" class="detail-group">
                <text class="group-title">语音说明</text>
                <VoicePlaybackList
                  v-if="faultVoicePlaybackItems.length"
                  :items="faultVoicePlaybackItems"
                />
                <text v-else class="group-content">{{ order.fault.voiceDuration }}</text>
              </view>
              <view v-if="hasFaultMedia" class="detail-group">
                <text class="group-title">故障图片</text>
                <view class="image-grid">
                  <image
                    v-for="(img, idx) in order.fault.images"
                    :key="'fi2-' + idx"
                    class="grid-img"
                    mode="widthFix"
                    :src="img"
                    @click="previewFaultImages(img)"
                  ></image>
                  <view v-if="hasFaultVideo" class="video-thumbnail" @click="previewFaultVideo">
                    <image class="grid-img" mode="widthFix" :src="videoCoverSrc"></image>
                    <view class="play-overlay">
                      <image class="play-icon" :src="playCircleIcon" mode="aspectFit" />
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <!-- 故障点信息（有数据时显示） -->
          <view v-if="hasFaultPointInfo" class="card-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">故障点信息</text>
            </view>
            <view class="fault-point-info">
              <view class="history-header">
                <text class="history-title">最近维修记录</text>
                <view class="history-btn" @click="repairHistoryRecord">查看历史记录</view>
              </view>
              <view class="history-record">
                <view class="record-top">
                  <text class="record-label">{{ faultPointRecordLabel }}</text>
                  <text v-if="hasStr(order.faultPoint.current.date)" class="record-date">{{
                    order.faultPoint.current.date
                  }}</text>
                </view>
                <text v-if="hasStr(order.faultPoint.current.desc)" class="record-desc">{{
                  order.faultPoint.current.desc
                }}</text>
              </view>
            </view>
          </view>
        </template>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { getOrderDetailAPI, type OrderDetailDTO } from '@/api/order'
  import VoicePlaybackList, {
    type VoicePlaybackItem
  } from '@/components/VoicePlaybackList/VoicePlaybackList.vue'
  import {
    contactPhoneIcon,
    localShippingIcon,
    playCircleIcon,
    statusBuildCircleIcon,
    statusCheckCircleIcon,
    statusPendingActionsIcon,
    statusTaskAltIcon,
    tvGenIcon
  } from '@/svgs'

  /**
   * 安全解码路由参数中的中文状态，避免仍带 % 编码时首屏乱码闪现
   */
  const safeDecodeQueryParam = (value: string) => {
    const s = value.trim()
    if (!s.includes('%')) return s
    try {
      return decodeURIComponent(s.replace(/\+/g, ' '))
    } catch {
      return s
    }
  }

  /** 按状态同步默认 Tab：待接单看申请内容，其余看维修过程 */
  const syncTabByStatus = () => {
    if (orderStatus.value === '待接单') {
      currentTab.value = 0
    } else {
      currentTab.value = 1
    }
  }

  // 当前Tab
  const currentTab = ref(0)
  // 工单状态（勿用占位文案作初值，否则首屏会误显「已关闭」或与 URL 不一致）
  const orderStatus = ref('')
  // 工单ID
  const orderId = ref<string>('')

  // 工单信息
  const order = ref<OrderDetailDTO>({
    status: '已关闭',
    base: { orderNo: '', orderTypeName: '', submitTime: '' },
    product: { barcode: '', model: '', serialNo: '' },
    service: { sitePhone: '', repairMethod: '', senderInfo: '', senderVoucherImg: '' },
    acceptor: { sitePhone: '', acceptorName: '' },
    fault: {
      desc: '',
      remark: '',
      voiceDuration: '',
      voiceUrl: '',
      voiceList: undefined,
      images: [],
      videoThumb: ''
    },
    repair: {
      faultJudge: '',
      quoteAmount: '0.00',
      quoteDesc: '',
      repairTime: '',
      returnMethod: '',
      returnReceiverTitle: '',
      returnAddress: '',
      returnExpressNo: '',
      returnExpressVoucherImg: ''
    },
    faultPoint: { current: { date: '', desc: '' }, records: [] },
    contact: { phone: '' }
  })

  /**
   * 页面加载
   * @param options - 选项
   * @returns void
   */
  onLoad((options: any) => {
    orderId.value = String(options?.id || options?.orderId || '')

    const rawStatus = options?.status
    if (rawStatus != null && String(rawStatus).trim() !== '') {
      orderStatus.value = safeDecodeQueryParam(String(rawStatus))
    }

    syncTabByStatus()

    loadDetail()
  })

  /**
   * 加载工单详情
   * @returns void
   */
  const loadDetail = async () => {
    if (!orderId.value) return
    try {
      const res = await getOrderDetailAPI({ id: orderId.value })
      order.value = res.result
      orderStatus.value = res.result.status || orderStatus.value
      syncTabByStatus()
    } catch {
      /* 失败提示由 http 层使用接口 msg */
    }
  }

  /** 非空字符串（trim 后） */
  const hasStr = (v: unknown) => v != null && String(v).trim().length > 0

  /** 通过工单类型文案判断是否非佳士报修 */
  const isNonJasicBrand = computed(() => {
    const orderTypeName = String(order.value.base.orderTypeName ?? '')
      .trim()
      .toUpperCase()
    return orderTypeName.includes('非佳士') || orderTypeName.includes('NON_JASIC')
  })

  /**
   * 将详情接口的语音时长文案解析为毫秒（与 VoiceInputField 元数据一致）
   */
  const parseVoiceDurationToMs = (raw: string | undefined): number | undefined => {
    if (!raw) return undefined
    const s = String(raw)
      .trim()
      .replace(/[″"′']/g, '')
      .replace(/秒/g, '')
    const colon = s.match(/^(\d+):(\d{1,2})$/)
    if (colon) {
      const min = parseInt(colon[1], 10)
      const sec = parseInt(colon[2], 10)
      return (min * 60 + sec) * 1000
    }
    const num = parseFloat(s.replace(/[^\d.]/g, ''))
    if (!Number.isNaN(num) && num > 0) {
      return num < 60000 ? num * 1000 : num
    }
    return undefined
  }

  /** 工单详情语音条（与报修页 VoiceInputField 列表项对应，用于只读播放） */
  const faultVoicePlaybackItems = computed((): VoicePlaybackItem[] => {
    const f = order.value.fault
    const list = f.voiceList
    if (list?.length) {
      return list
        .filter((x) => hasStr(x.url))
        .map((x) => ({ url: String(x.url).trim(), duration: x.duration }))
    }
    if (hasStr(f.voiceUrl)) {
      return [
        {
          url: String(f.voiceUrl).trim(),
          duration: parseVoiceDurationToMs(f.voiceDuration)
        }
      ]
    }
    return []
  })

  /** 是否展示「语音说明」分组（有时长文案或可播放地址） */
  const showFaultVoiceSection = computed(
    () => faultVoicePlaybackItems.value.length > 0 || hasStr(order.value.fault.voiceDuration)
  )

  /**
   * 是否是其它故障
   * @returns boolean
   */
  const isFaultOther = computed(() => order.value.fault.desc === '其它故障')

  /** 工单基础信息是否有任一字段 */
  const hasBaseInfo = computed(() => {
    const b = order.value.base
    return hasStr(b.orderNo) || hasStr(b.orderTypeName) || hasStr(b.submitTime)
  })

  /** 故障图片/视频是否有内容 */
  const hasFaultMedia = computed(() => {
    const f = order.value.fault
    return (f.images?.length ?? 0) > 0 || hasStr(f.videoUrl) || hasStr(f.videoThumb)
  })

  /** 是否有故障视频 */
  const hasFaultVideo = computed(() => hasStr(order.value.fault.videoUrl))

  /** 判断一个 URL 是否更像“视频链接”而不是图片封面 */
  const isLikelyVideoUrl = (url: string) => /\.(mp4|mov|webm|m3u8|avi|mkv)(\?|#|$)/i.test(url)

  /** 视频封面：优先后端封面，否则使用本地占位图 */
  const videoCoverSrc = computed(() => {
    const thumbRaw = String(order.value.fault.videoThumb ?? '').trim()
    if (thumbRaw && !isLikelyVideoUrl(thumbRaw)) return thumbRaw
    return tvGenIcon
  })

  /** 故障面板（描述/语音/媒体）是否有可展示内容 */
  const hasFaultPanelContent = computed(() => {
    const f = order.value.fault
    const textOk = isFaultOther.value ? hasStr(f.remark) : hasStr(f.desc)
    const voiceOk =
      hasStr(f.voiceDuration) ||
      hasStr(f.voiceUrl) ||
      (f.voiceList?.some((x) => hasStr(x.url)) ?? false)
    return textOk || voiceOk || hasFaultMedia.value
  })

  /** 维修过程 Tab 内「维修信息」是否有可展示字段 */
  const hasRepairTabInfo = computed(() => {
    const r = order.value.repair
    const s = orderStatus.value
    if (hasStr(r.faultJudge)) return true
    if (hasStr(r.quoteAmount)) return true
    if (hasStr(r.quoteDesc)) return true
    if (s === '已完成' || s === '已关闭') {
      if (hasStr(r.repairTime)) return true
      if (hasStr(r.returnMethod)) return true
      if (hasStr(r.returnReceiverTitle) || hasStr(r.returnAddress)) return true
      if (hasStr(r.returnExpressNo) || hasStr(r.returnExpressVoucherImg)) return true
    }
    return false
  })

  /** 服务信息卡片是否有可展示字段 */
  const hasServiceInfo = computed(() => {
    const s = order.value.service
    if (hasStr(s.sitePhone)) return true
    if (hasStr(serviceModeLabel.value)) return true
    if (serviceModeLabel.value !== '到店') {
      if (hasStr(s.senderInfo)) return true
      if (hasStr(s.senderVoucherImg)) return true
    }
    return false
  })

  /** 维修方式展示文案：优先新字段 serviceModeLabel，兼容旧字段 repairMethod */
  const serviceModeLabel = computed(() => {
    const service = order.value.service as { serviceModeLabel?: string; repairMethod?: string }
    return String(service.serviceModeLabel ?? service.repairMethod ?? '').trim()
  })

  /** 受理方信息是否有可展示字段 */
  const hasAcceptorInfo = computed(() => {
    const a = order.value.acceptor
    return hasStr(a.sitePhone) || hasStr(a.acceptorName)
  })

  /** 客户评价是否有可展示内容 */
  const hasEvaluateContent = computed(() => {
    const e = order.value.evaluate
    if (!e) return false
    if ((e.timeliness ?? 0) > 0) return true
    if ((e.quality ?? 0) > 0) return true
    if ((e.satisfaction ?? 0) > 0) return true
    return hasStr(e.comment)
  })

  /**
   * 联系电话
   */
  const contactPhone = computed(() => {
    const s = orderStatus.value
    if (s === '维修中' || s === '已完成') {
      return order.value.acceptor.sitePhone || ''
    }
    return order.value.service.sitePhone || ''
  })

  /**
   * 状态描述
   * @returns string
   */
  const statusDesc = computed(() => {
    switch (orderStatus.value) {
      case '待接单':
        return '工单已提交，等待网点接单'
      case '维修中':
        return '网点已接单，正在为您维修'
      case '已完成':
        return '维修已完成，感谢您的支持'
      case '已关闭':
        return '该工单已关闭，感谢您的配合'
      default:
        return ''
    }
  })

  /**
   * 状态图标
   * @returns string
   */
  const statusIconSrc = computed(() => {
    switch (orderStatus.value) {
      case '待接单':
        return statusPendingActionsIcon
      case '维修中':
        return statusBuildCircleIcon
      case '已完成':
        return statusCheckCircleIcon
      case '已关闭':
        return statusTaskAltIcon
      default:
        return ''
    }
  })

  /**
   * 步骤索引
   * @returns number
   */
  const stepIndex = computed(() => {
    switch (orderStatus.value) {
      case '待接单':
        return 0
      case '维修中':
        return 1
      case '已完成':
        return 2
      case '已关闭':
        return 3
      default:
        return 0
    }
  })

  /**
   * 步骤
   * @returns string[]
   */
  const steps = computed(() => ['待接单', '维修中', '已完成', '已关闭'])

  /**
   * 故障点记录标签
   * @returns string
   */
  const faultPointRecordLabel = computed(() => {
    const s = orderStatus.value
    return s === '待接单' || s === '维修中' ? '上次维修' : '当前维修'
  })

  /**
   * 是否有故障点信息
   * @returns boolean
   */
  const hasFaultPointInfo = computed(() => {
    const c = order.value.faultPoint?.current
    if (!c) return false
    const date = (c.date ?? '').trim()
    const desc = (c.desc ?? '').trim()
    return !!(date || desc)
  })

  /**
   * 是否显示评价Tab
   * @returns boolean
   */
  const showEvaluateTab = computed(() => order.value.canEvaluate ?? orderStatus.value === '已关闭')

  /**
   * 监听评价Tab显示状态
   * @param show - 是否显示
   * @returns void
   */
  watch(showEvaluateTab, (show) => {
    if (!show && currentTab.value === 2) {
      currentTab.value = 1
    }
  })

  /**
   * 跳转到故障点维修记录
   * @returns void
   */
  const repairHistoryRecord = () => {
    const id = orderId.value
    if (!id) {
      uni.showToast({ title: '缺少工单编号', icon: 'none', duration: 1500 })
      return
    }
    uni.navigateTo({
      url: `/pages/historicalRecord/index?orderId=${encodeURIComponent(id)}`
    })
  }

  /**
   * 跳转到联系受理网点
   * @returns void
   */
  const goToContact = () => {
    const phone = contactPhone.value
    if (!phone) {
      uni.showToast({ title: '暂无联系电话', icon: 'none', duration: 1500 })
      return
    }
    uni.makePhoneCall({ phoneNumber: phone })
  }

  /**
   * 预览故障图片
   * @param current - 当前图片地址
   */
  const previewFaultImages = (current: string) => {
    const urls = (order.value.fault.images ?? []).filter((x) => hasStr(x))
    if (!urls.length || !hasStr(current)) return
    uni.previewImage({
      urls,
      current
    })
  }

  /**
   * 预览单张图片
   * @param url - 图片地址
   */
  const previewSingleImage = (url: string) => {
    const current = String(url ?? '').trim()
    if (!current) return
    uni.previewImage({
      urls: [current],
      current
    })
  }

  /**
   * 预览故障视频
   */
  const previewFaultVideo = () => {
    const videoUrl = String(order.value.fault.videoUrl ?? '').trim()
    if (!videoUrl) {
      uni.showToast({ title: '暂无可播放视频', icon: 'none', duration: 1500 })
      return
    }
    if (typeof uni.previewMedia === 'function') {
      uni.previewMedia({
        sources: [{ url: videoUrl, type: 'video' }],
        fail: () => {
          uni.showToast({ title: '无法预览视频', icon: 'none', duration: 1500 })
        }
      })
      return
    }
    uni.showToast({ title: '当前端不支持视频预览', icon: 'none', duration: 1500 })
  }
</script>

<style lang="scss" scoped>
  /* 不继承全局 .page-index 的 min-height:100vh，避免与自定义导航栏叠算后短内容也出现滚动条；内容超出时由页面自然滚动 */
  .page-container.page-index.order-detail-page {
    gap: 0;
    min-height: auto;
  }

  .top-section {
    background-color: $primary;
    color: #fff;
    padding-bottom: 64rpx;
  }

  .status-banner {
    padding: 0 $space-xl $space-lg;

    .status-top {
      @include flex-between;
      margin-bottom: $space-lg;

      .status-text-wrap {
        @include flex-column;

        .status-title {
          font-size: $font-xxl;
          font-weight: bold;
        }

        .status-desc {
          font-size: $font-sm;
          color: rgba(255, 255, 255, 0.8);
          margin-top: $space-xs;
        }
      }

      .status-icon {
        width: 96rpx;
        height: 96rpx;
        opacity: 0.8;
      }
    }

    .stepper-wrap {
      position: relative;
      @include flex-between;
      padding: 0 $space-sm;

      .stepper-line {
        position: absolute;
        top: 12rpx;
        left: $space-sm;
        right: $space-sm;
        height: 4rpx;
        background-color: rgba(255, 255, 255, 0.3);
        z-index: 1;
      }

      .step-item {
        position: relative;
        z-index: 10;
        @include flex-column-center;
        gap: $space-sm;

        .step-dot {
          width: $space-md;
          height: $space-md;
          border-radius: 50%;
          background-color: rgba(255, 255, 255, 0.5);
          box-shadow: 0 0 0 $space-xs rgba(255, 255, 255, 0.1);

          &.active {
            background-color: #fff;
            box-shadow: 0 0 0 $space-xs rgba(255, 255, 255, 0.2);
          }
        }

        .step-text {
          font-size: $font-sm;
          font-weight: 500;
          color: rgba(255, 255, 255, 0.7);

          &.active {
            font-weight: bold;
            color: #fff;
          }
        }
      }
    }
  }

  .main-content.page-padding {
    margin-top: -64rpx;
    position: relative;
    z-index: 10;
    box-sizing: border-box;
  }

  /* tab 下有 BaseButton（fixed）时，给内容留出底部空间避免遮挡 */
  .main-content.page-padding.has-fixed-bottom-btn {
    /* BaseButton 实际占用：24(top)+80(content)+24(bottom)+safe-area，再额外留 32rpx 缓冲 */
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
    padding-bottom: calc(160rpx + constant(safe-area-inset-bottom));
  }

  .content-wrap {
    @include flex-column;
    gap: $space-lg;
  }

  .tab-container {
    background-color: $bg-card;
    border-radius: $radius-lg;
  }

  .tab-bar {
    display: flex;
    justify-content: space-around;
    padding: 0 $space-lg;
    border-bottom: 2rpx solid $border-lighter;

    .tab-item {
      padding: $space-lg 0;
      position: relative;
      flex: 1;
      text-align: center;

      .tab-text {
        font-size: $font-md;
        font-weight: 500;
        color: $text-label;
      }

      &.active .tab-text {
        font-weight: bold;
        color: $primary;
      }

      &.active .tab-line {
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: $space-xl;
        height: 4rpx;
        background-color: $primary;
      }
    }
  }

  .section-box {
    padding: $space-lg;
  }

  .empty-state {
    padding: 64rpx 0;

    .empty-text {
      font-size: $font-md;
      color: $text-muted;
    }
  }

  // info-list 扩展：快递凭证卡片
  .info-list {
    .mt-16 {
      margin-top: $space-sm;
    }

    .info-item .shipping-img {
      width: 128rpx;
      height: 128rpx;
      border-radius: 12rpx;
      border: 2rpx solid $border-light;
    }

    .info-item-col .shipping-card {
      position: relative;
      width: 100%;
      aspect-ratio: 2 / 1;
      border-radius: $radius-md;
      background-color: $bg-hover;
      overflow: hidden;
      border: 2rpx solid #e2e8f0;

      .shipping-bg {
        width: 100%;
        height: 100%;
      }

      .shipping-overlay {
        position: absolute;
        inset: 0;
        background-color: rgba(0, 0, 0, 0.4);
        @include flex-column-center;
        gap: $space-sm;

        .icon {
          width: 64rpx;
          height: 64rpx;
          opacity: 0.9;
        }

        .number {
          color: #fff;
          font-size: $font-xl;
          font-weight: bold;
          letter-spacing: 2rpx;
          text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.3);
        }
      }

      .shipping-tag {
        position: absolute;
        top: $space-sm;
        right: $space-sm;
        background-color: $primary;
        color: #fff;
        padding: 4rpx $space-sm;
        border-radius: $radius-sm;
        font-size: $font-xs;
        font-weight: 500;
      }
    }
  }

  .card-box {
    @include white-card;
    box-shadow: none;
    border: none;
  }

  .action-wrap {
    @include btn-reset;

    .btn-icon {
      width: $font-xxl;
      height: $font-xxl;
      margin-right: $space-sm;
      flex-shrink: 0;
    }

    &:active {
      opacity: 0.9;
    }
  }

  .fault-details {
    @include flex-column;
    gap: $space-lg;

    .detail-group {
      @include flex-column;
      gap: $space-sm;

      .group-title {
        @include info-label;
      }

      .group-content {
        font-size: $font-sm;
        line-height: 1.6;
        color: $text-dark;
      }

      .image-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: $space-sm;

        .grid-img {
          width: 100%;
          height: auto;
          aspect-ratio: 1 / 1;
          border-radius: $radius-md;
          background-color: $bg-hover;
        }

        .video-thumbnail {
          position: relative;
          width: 100%;
          aspect-ratio: 1 / 1;
          border-radius: $radius-md;
          overflow: hidden;

          .play-overlay {
            position: absolute;
            inset: 0;
            background-color: rgba(0, 0, 0, 0.3);
            @include flex-center;

            .play-icon {
              width: 64rpx;
              height: 64rpx;
            }
          }
        }
      }
    }
  }

  .fault-point-info {
    @include flex-column;
    gap: $space-lg;

    .history-header {
      @include flex-between;

      .history-title {
        @include info-label;
      }

      .history-btn {
        padding: $space-xs $space-md;
        background-color: rgba($primary, 0.1);
        color: $primary;
        font-size: $font-sm;
        font-weight: bold;
        border-radius: $radius-round;
      }
    }

    .history-record {
      @include desc-box;
      border-left: 4rpx solid rgba($primary, 0.3);

      .record-top {
        @include flex-between;
        margin-bottom: $space-xs;

        .record-label {
          font-size: $font-sm;
          font-weight: bold;
          color: $text-body;
        }

        .record-date {
          font-size: $font-xs;
          color: $text-muted;
        }
      }

      .record-desc {
        font-size: $font-sm;
        color: $text-desc;
        line-height: 1.5;
      }
    }
  }

  .eval-list {
    @include flex-column;
    gap: $space-lg;
    padding: 0 $space-md;

    .eval-item {
      @include flex-between;

      .eval-label {
        @include info-label;
      }

      .eval-stars {
        display: flex;
        gap: $space-xs;

        .star-char {
          font-size: $font-xl;
          color: #e2e8f0;

          &.active {
            color: #f59e0b;
          }
        }
      }
    }

    .eval-comment {
      @include flex-column;
      gap: $space-sm;

      .eval-label {
        @include info-label;
      }

      .eval-comment-box {
        @include desc-box;

        .eval-comment-text {
          font-size: $font-sm;
          color: $text-desc;
          line-height: 1.6;
        }
      }
    }
  }
</style>
