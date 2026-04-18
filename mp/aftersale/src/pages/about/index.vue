<template>
  <custom-nav-bar title="关于我们" surface="sticky" />
  <view class="page-index page-about">
    <scroll-view class="page-padding" scroll-y>
      <view class="about-content">
        <view class="brand-hero">
          <text class="brand-title">深圳市佳士科技股份有限公司</text>
          <text class="brand-subtitle">JASIC · 焊接与切割整体解决方案服务商</text>
          <view class="tag-row">
            <text class="tag-item">国家高新技术企业</text>
            <text class="tag-item">焊割设备制造</text>
            <text class="tag-item">智能制造方案</text>
          </view>
        </view>

        <view class="card-block">
          <view class="section-header">
            <view class="section-mark"></view>
            <text class="section-title">公司简介</text>
          </view>
          <text class="about-text">
            佳士科技成立于2005年，总部位于深圳，是一家集研发、生产、销售与服务于一体的焊接与切割技术企业。
            公司长期深耕逆变焊割领域，以稳定可靠的产品性能和持续迭代的技术方案服务工业用户。
          </text>
          <text class="about-text">
            作为国内较早布局逆变焊机产业化的企业之一，佳士围绕“设备 + 工艺 + 服务”的一体化能力建设，
            不断完善从中小型作业场景到工业级应用场景的产品矩阵。
          </text>
        </view>

        <view class="card-block">
          <view class="section-header">
            <view class="section-mark"></view>
            <text class="section-title">主营业务</text>
          </view>
          <view class="biz-list">
            <view class="biz-item">
              <text class="biz-name">逆变焊接设备</text>
              <text class="biz-desc">覆盖手工焊、氩弧焊、气保焊等主流工艺设备</text>
            </view>
            <view class="biz-item">
              <text class="biz-name">切割与配套产品</text>
              <text class="biz-desc">提供切割、电源及焊接周边配套的整体应用方案</text>
            </view>
            <view class="biz-item">
              <text class="biz-name">自动化与智能制造</text>
              <text class="biz-desc">面向行业客户提供焊接自动化、产线升级与工艺支持</text>
            </view>
          </view>
        </view>

        <view class="card-block">
          <view class="section-header">
            <view class="section-mark"></view>
            <text class="section-title">发展里程碑</text>
          </view>
          <view class="timeline">
            <view class="timeline-item">
              <text class="timeline-year">2005</text>
              <text class="timeline-text">公司成立，启动逆变焊割设备研发与产业化布局。</text>
            </view>
            <view class="timeline-item">
              <text class="timeline-year">2010-2011</text>
              <text class="timeline-text">完成资本市场阶段性突破，品牌影响力持续扩大。</text>
            </view>
            <view class="timeline-item">
              <text class="timeline-year">近年</text>
              <text class="timeline-text">持续推进自动化与智能制造方向，强化全球化服务能力。</text>
            </view>
          </view>
        </view>

        <view class="card-block">
          <view class="section-header">
            <view class="section-mark"></view>
            <text class="section-title">企业价值观</text>
          </view>
          <text class="about-text">
            以客户价值为导向，坚持技术创新与品质优先，持续提升产品可靠性、使用体验和服务响应效率，
            与合作伙伴共同构建长期共赢生态。
          </text>
        </view>

        <view class="about-footer">
          <text class="footer-line">本页面信息用于产品展示，具体内容以企业官方发布为准。</text>
          <text class="footer-line">Copyright © JASIC</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
</script>

<style lang="scss" scoped>
  .page-about {
    .about-content {
      @include flex-column-gap;
      padding-top: $space-lg;
      padding-bottom: calc(#{$space-xl} + env(safe-area-inset-bottom));
      box-sizing: border-box;
    }

    .brand-hero {
      @include white-card($radius-lg, $space-lg);
      background: linear-gradient(135deg, $primary-tint-cream 0%, $bg-card 65%);
      border: 2rpx solid $primary-border-warm;
      @include flex-column;
      gap: $space-sm;
    }

    .brand-title {
      color: $text-dark;
      font-size: $font-xl;
      font-weight: 700;
      line-height: 1.5;
      display: block;
    }

    .brand-subtitle {
      color: $text-desc;
      font-size: $font-sm;
      line-height: 1.6;
      display: block;
    }

    .tag-row {
      display: flex;
      flex-wrap: wrap;
      gap: $space-sm;
      margin-top: $space-xs;
    }

    .tag-item {
      padding: 6rpx 16rpx;
      border-radius: $radius-round;
      font-size: $font-xs;
      color: $primary;
      background-color: rgba($primary, 0.1);
    }

    .card-block {
      @include white-card($radius-lg, $space-lg);
      @include flex-column;
      gap: $space-md;
    }

    .biz-list {
      @include flex-column;
      gap: $space-md;
    }

    .biz-item {
      @include desc-box;
    }

    .biz-name {
      display: block;
      font-size: $font-md;
      font-weight: 600;
      color: $text-dark;
      margin-bottom: 8rpx;
    }

    .biz-desc {
      display: block;
      font-size: $font-sm;
      line-height: 1.7;
      color: $text-desc;
    }

    .timeline {
      @include flex-column;
      gap: $space-md;
    }

    .timeline-item {
      position: relative;
      padding-left: 22rpx;
      border-left: 2rpx solid $primary-border-warm-light;
    }

    .timeline-year {
      display: block;
      font-size: $font-md;
      font-weight: 700;
      color: $primary;
      margin-bottom: 8rpx;
    }

    .timeline-text {
      display: block;
      color: $text-body;
      font-size: $font-sm;
      line-height: 1.7;
    }

    .about-text {
      white-space: pre-wrap;
      color: $text-body;
      font-size: $font-md;
      line-height: 1.8;
      display: block;
    }

    .about-footer {
      @include flex-column;
      gap: 10rpx;
      padding: 0 $space-sm;
      margin-top: $space-xs;
    }

    .footer-line {
      text-align: center;
      font-size: $font-xs;
      color: $text-muted;
      line-height: 1.6;
      display: block;
    }
  }
</style>
