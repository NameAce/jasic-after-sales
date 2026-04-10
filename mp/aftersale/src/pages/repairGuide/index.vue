<template>
  <view class="page-guide page-index page-padding">
    <view class="guide-intro">
      <text class="intro-title">报修流程说明</text>
      <text class="intro-desc">
        按照以下步骤提交报修申请，我们将为您提供高效专业的售后维修服务。
      </text>
    </view>

    <view class="steps-list">
      <view v-for="(step, index) in steps" :key="index" class="step-card">
        <view class="step-header">
          <view class="step-number">{{ index + 1 }}</view>
          <view class="step-info">
            <text class="step-title">{{ step.title }}</text>
            <text class="step-subtitle">{{ step.subtitle }}</text>
          </view>
          <image v-if="step.iconSvg" class="step-svg-icon" :src="step.iconSvg" mode="aspectFit" />
          <text v-else class="step-icon">{{ step.icon }}</text>
        </view>
        <view class="step-body">
          <text class="step-desc">{{ step.desc }}</text>
          <view v-if="step.tips && step.tips.length" class="tips-list">
            <view v-for="(tip, ti) in step.tips" :key="ti" class="tip-item">
              <text class="tip-dot">·</text>
              <text class="tip-text">{{ tip }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="guide-footer">
      <text class="footer-text">如有疑问请联系客服</text>
      <view class="footer-btn" @click="callService">
        <image class="footer-headset-icon" :src="headsetMicIcon" mode="aspectFit" />
        <text>400-888-9999</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import {
    engineeringIcon,
    headsetMicIcon,
    homeRepairServiceIcon,
    submitRepairIcon,
    taskCompleteIcon
  } from '@/svgs'

  const steps = [
    {
      title: '提交报修申请',
      subtitle: '选择品牌 · 填写信息',
      icon: '',
      iconSvg: submitRepairIcon,
      desc: '选择佳士品牌或非佳士品牌入口，填写故障描述、选择维修方式和附近网点。佳士品牌可扫描条码自动关联产品信息。',
      tips: [
        '佳士品牌支持扫码自动查询保修状态',
        '非佳士品牌需手动填写品牌和型号',
        '上传故障照片/视频可加快诊断效率'
      ]
    },
    {
      title: '等待派单处理',
      subtitle: '系统自动分配 · 网点确认',
      icon: '',
      iconSvg: engineeringIcon,
      desc: '提交后系统将自动派单至您选择的维修网点，网点确认后工单状态将变为"维修中"。您可在工单列表中实时查看进度。',
      tips: ['寄修方式请及时上传快递单号', '到店维修请携带机器前往指定网点']
    },
    {
      title: '维修处理',
      subtitle: '故障诊断 · 报价确认',
      icon: '',
      iconSvg: homeRepairServiceIcon,
      desc: '维修技师将对设备进行故障检测与维修。维修过程中会更新故障判定、维修报价等信息，您可随时在工单详情中查看。',
      tips: ['维修报价包含配件费和人工费', '如需额外确认会通过电话联系您']
    },
    {
      title: '完成与评价',
      subtitle: '验收设备 · 提交评价',
      icon: '',
      iconSvg: taskCompleteIcon,
      desc: '维修完成后设备将通过原路返回。收到设备后请及时验收，并对服务进行评价，帮助我们持续改进服务质量。',
      tips: ['寄修方式可在详情页查看回寄快递单号', '评价包含服务时效、维修质量和满意度三个维度']
    }
  ]

  const callService = () => {
    uni.makePhoneCall({
      phoneNumber: '400-888-9999',
      fail: () => {
        uni.showToast({ title: '拨打失败', icon: 'none', duration: 1500 })
      }
    })
  }
</script>

<style lang="scss" scoped>
  .page-guide.page-index.page-padding {
    padding-top: $space-lg;
  }

  .guide-intro {
    .intro-title {
      display: block;
      font-size: $font-xxl;
      font-weight: bold;
      color: $text-dark;
      margin-bottom: 12rpx;
    }

    .intro-desc {
      display: block;
      font-size: 26rpx;
      color: $text-label;
      line-height: 1.6;
    }
  }

  .steps-list {
    display: flex;
    flex-direction: column;
    gap: $space-lg;
  }

  .step-card {
    @include white-card;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
    border: none;

    .step-header {
      @include flex-row;
      gap: 20rpx;
      margin-bottom: $space-md;

      .step-number {
        width: 56rpx;
        height: 56rpx;
        border-radius: $radius-md;
        background: $primary;
        color: #fff;
        font-size: $font-md;
        font-weight: bold;
        @include flex-center;
        flex-shrink: 0;
      }

      .step-info {
        flex: 1;
        display: flex;
        flex-direction: column;

        .step-title {
          font-size: 30rpx;
          font-weight: bold;
          color: $text-dark;
        }

        .step-subtitle {
          font-size: 22rpx;
          color: $text-muted;
          margin-top: 4rpx;
        }
      }

      .step-icon {
        font-size: $font-title;
        color: rgba($primary, 0.3);
      }

      .step-svg-icon {
        width: $font-title;
        height: $font-title;
        display: block;
      }
    }

    .step-body {
      padding-left: 76rpx;

      .step-desc {
        display: block;
        font-size: $font-sm;
        color: $text-desc;
        line-height: 1.7;
        margin-bottom: $space-sm;
      }

      .tips-list {
        display: flex;
        flex-direction: column;
        gap: $space-xs;

        .tip-item {
          display: flex;
          gap: $space-xs;

          .tip-dot {
            color: $primary;
            font-weight: bold;
            flex-shrink: 0;
          }

          .tip-text {
            font-size: 22rpx;
            color: $text-label;
            line-height: 1.5;
          }
        }
      }
    }
  }

  .guide-footer {
    @include flex-column-center;
    gap: $space-sm;

    .footer-text {
      font-size: $font-sm;
      color: $text-muted;
    }

    .footer-btn {
      @include flex-row;
      gap: 12rpx;
      background: rgba($primary, 0.1);
      color: $primary;
      padding: $space-sm 40rpx;
      border-radius: $radius-round;
      font-size: $font-md;
      font-weight: bold;

      .footer-headset-icon {
        width: $font-xl;
        height: $font-xl;
        display: block;
        flex-shrink: 0;
      }
    }
  }
</style>
