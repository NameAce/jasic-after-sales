<template>
  <!-- 售后客户端小程序（报修、工单、地址）页面 order / detail -->
  <custom-nav-bar
    title="工单详情"
    surface="sticky"
    :color="themeColors.primaryContrast"
    :background="themeColors.primary"
    :shadow="false"
  />
  <view
    class="page-container page-index order-detail-page"
    :class="{ 'order-detail-page--ios': isIOSPlatform }"
  >
    <!-- 导航栏 -->
    <view class="top-section">
      <!-- 工单状态 -->
      <view class="status-banner">
        <view class="status-top">
          <view class="status-text-wrap">
            <text class="status-title">{{ orderStatusBannerText }}</text>
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

    <view class="main-content page-padding">
      <view class="content-wrap">
        <!-- 标签容器 -->
        <view class="tab-container">
          <!-- 标签栏 -->
          <view class="tab-bar">
            <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">
              <text class="tab-text">申请内容</text>
            </view>
            <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">
              <text class="tab-text">维修过程</text>
            </view>
            <view
              v-if="showEvaluateTab"
              class="tab-item"
              :class="{ active: currentTab === 2 }"
              @click="currentTab = 2"
            >
              <text class="tab-text">客户评价</text>
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
                <text :class="['info-value', 'tag', order.isJasic ? 'tag-jasic' : 'tag-non-jasic']">
                  {{ order.base.orderTypeName }}
                </text>
              </view>
              <view v-if="hasStr(order.base.submitTime)" class="info-item">
                <text class="info-label">提交时间</text>
                <text class="info-value">{{ formatIsoDateTime(order.base.submitTime) }}</text>
              </view>
              <view v-if="hasStr(order.customerName)" class="info-item">
                <text class="info-label">客户姓名</text>
                <text class="info-value">{{ order.customerName }}</text>
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
              <view v-if="showFaultDescGroup" class="detail-group">
                <text class="group-title">故障描述</text>
                <text class="group-content">{{ faultDescTrimmed }}</text>
              </view>
              <view v-if="showFaultRemarkGroup" class="info-item-col">
                <text class="info-label" style="margin-bottom: 16rpx; display: block"
                  >故障备注说明</text
                >
                <view class="desc-box">
                  <text class="desc-text">{{ order.fault.remark }}</text>
                </view>
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
                    v-for="(img, idx) in faultImagePreviewUrls"
                    :key="'fi-' + idx"
                    class="grid-img"
                    mode="aspectFill"
                    :src="img"
                    @click="previewFaultImages(img)"
                  ></image>
                  <view v-if="hasFaultVideo" class="video-thumbnail" @click="previewFaultVideo">
                    <image class="grid-img" mode="aspectFill" :src="videoCoverSrc"></image>
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
              <view v-if="hasStr(order.repair.faultJudge)" class="info-item align-center">
                <text class="info-label">故障判定</text>
                <view :class="['warranty-judge-tag', faultJudgeTagClass]">
                  <text class="warranty-judge-tag-text">{{ order.repair.faultJudge }}</text>
                </view>
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
                  <text class="info-value">{{ formatIsoDateTime(order.repair.repairTime) }}</text>
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
                      class="return-receiver-title"
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
                  <text
                    v-if="hasStr(order.repair.returnExpressNo)"
                    class="return-express-no-text"
                    >{{ order.repair.returnExpressNo }}</text
                  >
                  <image
                    v-if="hasStr(order.repair.returnExpressVoucherImg)"
                    class="return-express-voucher-img"
                    mode="widthFix"
                    :src="order.repair.returnExpressVoucherImg"
                    @click="previewSingleImage(order.repair.returnExpressVoucherImg)"
                  />
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
          <!-- 商品信息（任一有值则显示整块；C 端申请内容不展示品牌与质保判定） -->
          <view v-if="hasProductInfoCard" class="card-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">商品信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(order.product.model)" class="info-item">
                <text class="info-label">机器型号</text>
                <text class="info-value">{{ order.product.model }}</text>
              </view>
              <view v-if="hasStr(order.product.barcode)" class="info-item">
                <text class="info-label">条形码</text>
                <text class="info-value">{{ order.product.barcode }}</text>
              </view>
              <view v-if="hasStr(order.product.serialNo)" class="info-item">
                <text class="info-label">机器小号</text>
                <text class="info-value">{{ order.product.serialNo }}</text>
              </view>
              <view v-if="hasStr(order.product.lastOutDate)" class="info-item">
                <text class="info-label">最后出库日期</text>
                <text class="info-value">{{ order.product.lastOutDate }}</text>
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
              <view v-if="hasStr(order.customerMobile)" class="info-item">
                <text class="info-label">客户联系方式</text>
                <text class="info-value">{{ order.customerMobile }}</text>
              </view>
              <view v-if="hasStr(order.service.applySourceLabel)" class="info-item">
                <text class="info-label">申请来源</text>
                <text class="info-value">{{ order.service.applySourceLabel }}</text>
              </view>
              <view v-if="hasStr(order.service.acceptingParty)" class="info-item">
                <text class="info-label">受理方</text>
                <text class="info-value">{{ order.service.acceptingParty }}</text>
              </view>
              <view v-if="hasStr(order.service.sitePhone)" class="info-item">
                <text class="info-label">网点电话</text>
                <text class="info-value">{{ order.service.sitePhone }}</text>
              </view>
              <view v-if="hasStr(order.acceptor.acceptorName)" class="info-item">
                <text class="info-label">网点名称</text>
                <text class="info-value">{{ order.acceptor.acceptorName }}</text>
              </view>
              <view v-if="hasStr(serviceModeLabel)" class="info-item">
                <text class="info-label">维修方式</text>
                <view class="tag-primary">{{ serviceModeLabel }}</view>
              </view>
              <!-- 到店维修不显示寄件信息与寄件快递单号（兼容历史「送店」文案与 STORE 编码） -->
              <template v-if="!isInStoreRepair">
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
        </template>

        <!-- ===== Tab 1 外部卡片（单容器：避免多根节点各自吃 content-wrap 的 gap；Tab 内无 section 时与 Tab 白底衔接） ===== -->
        <view
          v-if="currentTab === 1"
          class="tab-repair-extras"
          :class="{ 'tab-repair-extras--flush': !hasRepairProcessTabInner }"
        >
          <!-- 受理方信息（非待接单） -->
          <view v-if="orderStatus !== '待接单' && hasAcceptorInfo" class="card-box">
            <view class="section-header">
              <view class="section-mark"></view>
              <text class="section-title">受理方信息</text>
            </view>
            <view class="info-list">
              <view v-if="hasStr(acceptorOutletPhoneDisplay)" class="info-item">
                <text class="info-label">网点电话</text>
                <text class="info-value">{{ acceptorOutletPhoneDisplay }}</text>
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
              <view v-if="showFaultDescGroup" class="detail-group">
                <text class="group-title">故障描述</text>
                <text class="group-content">{{ faultDescTrimmed }}</text>
              </view>
              <view v-if="showFaultRemarkGroup" class="info-item-col">
                <text class="info-label" style="margin-bottom: 16rpx; display: block"
                  >故障备注说明</text
                >
                <view class="desc-box">
                  <text class="desc-text">{{ order.fault.remark }}</text>
                </view>
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
                    v-for="(img, idx) in faultImagePreviewUrls"
                    :key="'fi2-' + idx"
                    class="grid-img"
                    mode="aspectFill"
                    :src="img"
                    @click="previewFaultImages(img)"
                  ></image>
                  <view v-if="hasFaultVideo" class="video-thumbnail" @click="previewFaultVideo">
                    <image class="grid-img" mode="aspectFill" :src="videoCoverSrc"></image>
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
              </view>
              <view class="history-record">
                <view class="record-top">
                  <text class="record-label">{{ faultPointRecordLabel }}</text>
                  <text v-if="hasStr(order.faultPoint.current.date)" class="record-date">{{
                    formatIsoDateTime(order.faultPoint.current.date)
                  }}</text>
                </view>
                <text v-if="hasStr(order.faultPoint.current.desc)" class="record-desc">{{
                  order.faultPoint.current.desc
                }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { onLoad, onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import { getCustomerWorkOrder, type OrderDetail } from '@/api/workOrder'
  import { showApiToast } from '@/utils/uiFeedback'
  import { WORK_ORDER_MAIN_STATUS } from '@/models/order'
  import { getStatusDesc } from '@/utils/orderStatus'
  import VoicePlaybackList, {
    type VoicePlaybackItem
  } from '@/components/VoicePlaybackList/VoicePlaybackList.vue'
  import {
    playCircleIcon,
    statusBuildCircleIcon,
    statusCheckCircleIcon,
    statusPendingActionsIcon,
    statusTaskAltIcon,
    tvGenIcon
  } from '@/svgs'
  import { themeColors } from '@/constants/theme'
  import { formatIsoDateTime } from '@/utils/format'

  /**
   * 安全解码路由参数中的中文状态，避免仍带 % 编码时首屏乱码闪现
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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

  /**
   * 按状态同步默认 Tab：待接单看申请内容，其余看维修过程
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
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

  /** 是否 iOS（用于底部额外 safe-area 留白） */
  const isIOSPlatform = ref(false)

  // 工单信息
  const order = ref<OrderDetail>({
    status: '已关闭',
    mainStatus: '',
    isJasic: true,
    customerName: '',
    customerMobile: '',
    base: { orderNo: '', orderTypeName: '', submitTime: '' },
    product: {
      barcode: '',
      model: '',
      serialNo: '',
      brandName: undefined,
      productName: undefined,
      warrantyClass: undefined
    },
    service: {
      sitePhone: '',
      repairMethod: '',
      senderInfo: '',
      senderVoucherImg: '',
      applySourceLabel: '',
      acceptingParty: ''
    },
    acceptor: { sitePhone: '', acceptorName: '', currentAcceptCompanyPhone: '' },
    faultDesc: '',
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
      quoteAmount: '',
      quoteDesc: '',
      repairTime: '',
      returnMethod: '',
      returnReceiverTitle: '',
      returnAddress: '',
      returnExpressNo: '',
      returnExpressVoucherImg: ''
    },
    faultPoint: { current: { date: '', desc: '' }, records: [] }
  })

  /**
   * 详情顶栏状态标题：与列表「待接单」等 Tab 中文桶一致，不再单独展示「待派单」
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const orderStatusBannerText = computed(() => orderStatus.value)

  /**
   * 页面加载
   * @param options - 选项
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  onLoad((options: any) => {
    try {
      const platform = String(uni.getSystemInfoSync().platform || '').toLowerCase()
      isIOSPlatform.value = platform === 'ios'
    } catch {
      isIOSPlatform.value = false
    }

    orderId.value = String(options?.id || options?.orderId || '')

    const rawStatus = options?.status
    if (rawStatus != null && String(rawStatus).trim() !== '') {
      orderStatus.value = safeDecodeQueryParam(String(rawStatus))
    }

    syncTabByStatus()
  })

  /**
   * 每次进入页面拉取详情（含从评价页返回后展示最新评价）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  onShow(() => {
    loadDetail()
  })

  /**
   * 加载工单详情
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const loadDetail = async () => {
    if (!orderId.value) return
    try {
      const res = await getCustomerWorkOrder({ id: orderId.value })
      const prevStatus = orderStatus.value
      order.value = res.data
      orderStatus.value = res.data.status || orderStatus.value
      // 仅状态变化时同步默认 Tab；预览图片/视频关闭会触发 onShow，避免冲掉用户当前选中的 Tab
      if (prevStatus !== orderStatus.value) {
        syncTabByStatus()
      }
    } catch {
      /* 失败提示由 http 层使用接口 msg */
    }
  }

  /**
   * 非空字符串（trim 后）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasStr = (v: unknown) => v != null && String(v).trim().length > 0

  /**
   * 故障判定：有故障红标、无故障绿标（其余中性灰）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const faultJudgeTagClass = computed(() => {
    const t = String(order.value.repair.faultJudge ?? '').trim()
    if (!t) return 'is-neutral'
    if (t.includes('无故障')) return 'is-in'
    if (t.includes('有故障')) return 'is-out'
    return 'is-neutral'
  })

  /**
   * 商品信息卡片是否有任一可展示字段（不含品牌）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasProductInfoCard = computed(() => {
    const p = order.value.product
    return hasStr(p.model) || hasStr(p.barcode) || hasStr(p.serialNo) || hasStr(p.lastOutDate)
  })

  /**
   * 将详情接口的语音时长文案解析为毫秒（与 VoiceInputField 元数据一致）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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

  /**
   * 工单详情语音条（与报修页 VoiceInputField 列表项对应，用于只读播放）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
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

  /**
   * 是否展示「语音说明」分组（有时长文案或可播放地址）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const showFaultVoiceSection = computed(
    () => faultVoicePlaybackItems.value.length > 0 || hasStr(order.value.fault.voiceDuration)
  )

  /**
   * 故障描述：与详情接口 `faultDesc` 一致，不做分隔符转换
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const faultDescTrimmed = computed(() => String(order.value.faultDesc ?? '').trim())

  /**
   * 故障描述是否全等于「其它故障 / 其他故障」（不展示主描述，仅展示备注）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isFaultOtherExact = computed(() => {
    const d = faultDescTrimmed.value
    return d === '其它故障' || d === '其他故障'
  })

  /**
   * 故障描述中是否包含「其它故障 / 其他故障」（含全等时需展示备注说明）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const faultDescContainsOtherFault = computed(() => {
    const d = faultDescTrimmed.value
    return d.includes('其它故障') || d.includes('其他故障')
  })

  const showFaultDescGroup = computed(
    () => !isFaultOtherExact.value && hasStr(order.value.faultDesc)
  )

  const showFaultRemarkGroup = computed(
    () => faultDescContainsOtherFault.value && hasStr(order.value.fault.remark)
  )

  /**
   * 工单基础信息是否有任一字段
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasBaseInfo = computed(() => {
    const b = order.value.base
    return (
      hasStr(b.orderNo) ||
      hasStr(b.orderTypeName) ||
      hasStr(b.submitTime) ||
      hasStr(order.value.customerName)
    )
  })

  /**
   * 故障图片预览地址：优先接口 `faultImageFiles`，否则兼容旧字段 `fault.images`
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const faultImagePreviewUrls = computed(() => {
    const files = order.value.faultImageFiles
    if (files?.length) {
      return files
        .slice()
        .sort((a, b) => (Number(a.sortNum) || 0) - (Number(b.sortNum) || 0))
        .map((f) => String(f.previewUrl ?? '').trim())
        .filter((x) => hasStr(x))
    }
    return (order.value.fault.images ?? []).filter((x) => hasStr(x))
  })

  /**
   * 故障图片/视频是否有内容
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasFaultMedia = computed(() => {
    const f = order.value.fault
    return faultImagePreviewUrls.value.length > 0 || hasStr(f.videoUrl) || hasStr(f.videoThumb)
  })

  /**
   * 是否有故障视频
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasFaultVideo = computed(() => hasStr(order.value.fault.videoUrl))

  /**
   * 判断一个 URL 是否更像“视频链接”而不是图片封面
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isLikelyVideoUrl = (url: string) => /\.(mp4|mov|webm|m3u8|avi|mkv)(\?|#|$)/i.test(url)

  /**
   * 视频封面：优先后端封面，否则使用本地占位图
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const videoCoverSrc = computed(() => {
    const thumbRaw = String(order.value.fault.videoThumb ?? '').trim()
    if (thumbRaw && !isLikelyVideoUrl(thumbRaw)) return thumbRaw
    return tvGenIcon
  })

  /**
   * 故障面板（描述/语音/媒体）是否有可展示内容
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasFaultPanelContent = computed(() => {
    const f = order.value.fault
    const textOk =
      (!isFaultOtherExact.value && hasStr(order.value.faultDesc)) ||
      (faultDescContainsOtherFault.value && hasStr(f.remark))
    const voiceOk =
      hasStr(f.voiceDuration) ||
      hasStr(f.voiceUrl) ||
      (f.voiceList?.some((x) => hasStr(x.url)) ?? false)
    return textOk || voiceOk || hasFaultMedia.value
  })

  /**
   * 维修过程 Tab 内「维修信息」是否有可展示字段
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
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

  /**
   * 维修过程 Tab 白底内是否有任一块 section（待接单·故障信息 / 非待接单·维修信息）
   * 与模板上两个 `section-box` 的 v-if 条件一致，用于与下方「外部卡片」衔接时抵消 content-wrap 的 gap
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasRepairProcessTabInner = computed(() => {
    const s = orderStatus.value
    if (s === '待接单') return hasFaultPanelContent.value
    return hasRepairTabInfo.value
  })

  /**
   * 服务信息卡片是否有可展示字段
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasServiceInfo = computed(() => {
    const s = order.value.service
    if (hasStr(order.value.customerMobile)) return true
    if (hasStr(s.applySourceLabel)) return true
    if (hasStr(s.acceptingParty)) return true
    if (hasStr(s.sitePhone)) return true
    if (hasStr(order.value.acceptor.acceptorName)) return true
    if (hasStr(serviceModeLabel.value)) return true
    if (!isInStoreRepair.value) {
      if (hasStr(s.senderInfo)) return true
      if (hasStr(s.senderVoucherImg)) return true
    }
    return false
  })

  /**
   * 维修方式展示文案：优先新字段 serviceModeLabel，兼容旧字段 repairMethod
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const serviceModeLabel = computed(() => {
    const service = order.value.service as { serviceModeLabel?: string; repairMethod?: string }
    return String(service.serviceModeLabel ?? service.repairMethod ?? '').trim()
  })

  /**
   * 是否到店类维修（与寄件信息互斥；兼容历史「送店维修」、STORE 等）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const isInStoreRepair = computed(() => {
    const t = serviceModeLabel.value
    if (!t) return false
    const u = t.toUpperCase().replace(/-/g, '_')
    if (u === 'STORE' || u === 'SHOP') return true
    return /到店|送店/.test(t)
  })

  /**
   * 维修过程「受理方信息」卡片：网点电话展示
   * 优先详情 `currentAcceptCompanyPhone`，兼容历史 `acceptor.sitePhone`。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const acceptorOutletPhoneDisplay = computed(() => {
    const a = order.value.acceptor
    return String(a.currentAcceptCompanyPhone ?? a.sitePhone ?? '').trim()
  })

  /**
   * 受理方信息是否有可展示字段
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasAcceptorInfo = computed(() => {
    const a = order.value.acceptor
    return hasStr(acceptorOutletPhoneDisplay.value) || hasStr(a.acceptorName)
  })

  /**
   * 客户评价是否有可展示内容
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasEvaluateContent = computed(() => {
    const e = order.value.evaluate
    if (!e) return false
    if ((e.timeliness ?? 0) > 0) return true
    if ((e.quality ?? 0) > 0) return true
    if ((e.satisfaction ?? 0) > 0) return true
    return hasStr(e.comment)
  })

  /**
   * 状态描述
   * @returns string
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const statusDesc = computed(() => {
    const ms = String(order.value.mainStatus ?? '')
      .trim()
      .toUpperCase()
      .replace(/-/g, '_')
    if (ms === WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN) {
      return getStatusDesc(WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN)
    }
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
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const steps = computed(() => ['待接单', '维修中', '已完成', '已关闭'])

  /**
   * 故障点记录标签
   * @returns string
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const faultPointRecordLabel = computed(() => {
    const s = orderStatus.value
    return s === '待接单' || s === '维修中' ? '上次维修' : '当前维修'
  })

  /**
   * 是否有故障点信息
   * @returns boolean
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const hasFaultPointInfo = computed(() => {
    const c = order.value.faultPoint?.current
    if (!c) return false
    const date = (c.date ?? '').trim()
    const desc = (c.desc ?? '').trim()
    return !!(date || desc)
  })

  /**
   * 是否显示评价 Tab：已有评价内容、仍可评价、或已关闭工单需查看/占位
   * 注意：接口在用户评价后常返回 canEvaluate=false，不能用 ?? 依赖该字段，否则 Tab 会消失
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const showEvaluateTab = computed(() => {
    if (hasEvaluateContent.value) return true
    if (order.value.canEvaluate === true) return true
    return orderStatus.value === '已关闭'
  })

  /**
   * 监听评价Tab显示状态
   * @param show - 是否显示
   * @returns void
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  watch(showEvaluateTab, (show) => {
    if (!show && currentTab.value === 2) {
      currentTab.value = 1
    }
  })

  /**
   * 预览故障图片
   * @param current - 当前图片地址
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const previewFaultImages = (current: string) => {
    const urls = faultImagePreviewUrls.value
    if (!urls.length || !hasStr(current)) return
    uni.previewImage({
      urls,
      current
    })
  }

  /**
   * 预览单张图片
   * @param url - 图片地址
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
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
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  const previewFaultVideo = () => {
    const videoUrl = String(order.value.fault.videoUrl ?? '').trim()
    if (!videoUrl) {
      void showApiToast('暂无可播放视频')
      return
    }
    if (typeof uni.previewMedia === 'function') {
      uni.previewMedia({
        sources: [{ url: videoUrl, type: 'video' }],
        fail: () => {
          void showApiToast('无法预览视频')
        }
      })
      return
    }
    void showApiToast('当前端不支持视频预览')
  }
</script>

<style lang="scss" scoped>
  /* 不继承全局 .page-index 的 min-height:100vh，避免与自定义导航栏叠算后短内容也出现滚动条；内容超出时由页面自然滚动 */
  .page-container.page-index.order-detail-page {
    gap: 0;
    min-height: auto;
  }

  .top-section {
    position: relative;
    z-index: 0;
    background-color: $primary;
    color: $primary-contrast;
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
            background-color: $primary-contrast;
            box-shadow: 0 0 0 $space-xs rgba(255, 255, 255, 0.2);
          }
        }

        .step-text {
          font-size: $font-sm;
          font-weight: 500;
          color: rgba(255, 255, 255, 0.7);

          &.active {
            font-weight: bold;
            color: $primary-contrast;
          }
        }
      }
    }
  }

  .main-content.page-padding {
    margin-top: -142rpx;
    position: relative;
    /* 低于 .top-section，上叠时透明区可透出状态/步骤，白底从 tab-container 起，避免 Tab 被挡 */
    z-index: 1;
    background: transparent;
    box-sizing: border-box;
    /* 默认底部留白；iOS 再叠加 Home 指示条安全区（见 .order-detail-page--ios） */
    padding-bottom: 40rpx;
  }

  .order-detail-page--ios .main-content.page-padding {
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
    padding-bottom: calc(20rpx + constant(safe-area-inset-bottom));
  }

  .content-wrap {
    @include flex-column;
    gap: $space-lg;
    /* 与 contractor 详情一致：为负边距上叠留透明带 */
    padding-top: 80rpx;
  }

  .tab-repair-extras {
    @include flex-column;
    gap: $space-lg;
    width: 100%;
    box-sizing: border-box;
  }

  .tab-repair-extras--flush {
    margin-top: -$space-lg;
  }

  /* 参考稿：白底顶缘大圆角、主色下划线、未选深灰 / 选主色 */
  .tab-container {
    background-color: $bg-card;
    border-radius: $radius-lg $radius-lg 0 0;
    overflow: hidden;
    box-shadow: 0 2rpx 12rpx rgba(15, 23, 42, 0.04);
  }

  .tab-bar {
    display: flex;
    justify-content: space-around;
    align-items: stretch;
    padding: 0;
    border-bottom: 1rpx solid $bg-light;

    .tab-item {
      position: relative;
      flex: 1;
      padding: 32rpx $space-sm 20rpx;
      text-align: center;
      @include flex-column;
      align-items: center;
      transition: color 0.2s;

      .tab-text {
        font-size: $font-md;
        font-weight: 500;
        line-height: 1.2;
        color: $text-slate-800;
      }

      &.active {
        .tab-text {
          color: $primary;
          font-weight: 600;
        }

        &::after {
          content: '';
          position: absolute;
          left: 50%;
          bottom: 0;
          width: 72rpx;
          height: 6rpx;
          margin-left: -36rpx;
          background-color: $primary;
          border-radius: 3rpx 3rpx 0 0;
        }
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

  // info-list 扩展：快递凭证等
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

    .info-item-col {
      .return-receiver-title {
        display: block;
        font-weight: 500;
        font-size: 24rpx;
        color: $text-dark;
        margin-bottom: 8rpx;
      }

      .return-express-no-text {
        display: block;
        font-size: $font-lg;
        font-weight: 500;
        color: $text-dark;
        margin-bottom: $space-sm;
        letter-spacing: 1rpx;
      }

      .return-express-voucher-img {
        display: block;
        width: 200rpx;
        border-radius: $radius-md;
        border: 2rpx solid $border-slate;
      }
    }

    /* 质保判定角标（保外/保内：同结构半透明底 + 饱和色字） */
    .warranty-judge-tag {
      flex-shrink: 0;
      padding: 6rpx 20rpx;
      border-radius: 8rpx;
      line-height: 1.2;

      .warranty-judge-tag-text {
        font-size: $font-sm;
        font-weight: 500;
      }

      &.is-out {
        background-color: rgba($warranty-out, 0.14);

        .warranty-judge-tag-text {
          color: $warranty-out;
        }
      }

      &.is-in {
        background-color: rgba($success-solid, 0.14);

        .warranty-judge-tag-text {
          color: $success-solid;
        }
      }

      &.is-neutral {
        background-color: $bg-hover;

        .warranty-judge-tag-text {
          color: $text-label;
        }
      }
    }
  }

  .card-box {
    @include white-card;
    box-shadow: none;
    border: none;
  }

  .fault-details {
    @include flex-column;
    gap: $space-lg;
    padding: 0 24rpx;

    /* 与全局 `.info-list .info-item-col` 中「报价说明」块一致（本区不在 `.info-list` 内需本地补全） */
    > .info-item-col {
      display: flex;
      flex-direction: column;
      font-size: $font-sm;

      .info-label {
        @include info-label;
        margin-bottom: $space-sm;
        display: block;
      }

      .desc-box {
        @include desc-box;

        .desc-text {
          font-size: $font-sm;
          color: $text-desc;
          line-height: 1.6;
        }
      }
    }

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

      // 与 .info-list .info-item .shipping-img（寄件快递单号）视觉一致
      .image-grid {
        display: flex;
        flex-wrap: wrap;
        gap: $space-sm;

        .grid-img {
          width: 128rpx;
          height: 128rpx;
          border-radius: 12rpx;
          border: 2rpx solid $border-light;
        }

        .video-thumbnail {
          position: relative;
          width: 128rpx;
          height: 128rpx;
          border-radius: 12rpx;
          border: 2rpx solid $border-light;
          overflow: hidden;
          box-sizing: border-box;

          .grid-img {
            width: 100%;
            height: 100%;
            border: none;
            border-radius: 0;
          }

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
      .history-title {
        @include info-label;
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
          color: $border-slate;

          &.active {
            color: $warning-amber;
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
