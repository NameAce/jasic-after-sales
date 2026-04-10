<template>
  <view>
    <!-- 维修说明标题 -->
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">{{ isRecheck ? '复检登记' : '故障点登记' }}</text>
    </view>
    <!-- 维修说明内容 -->
    <view class="fault-register-v2">
      <!-- 故障描述 -->
      <view v-if="faultDescOptions.length" class="form-item-v2">
        <text class="form-label-v2">故障描述</text>
        <picker mode="selector" :range="faultDescOptions" @change="onFaultDescPicker">
          <view class="form-picker-v2">
            <text :class="['picker-text', faultDesc ? '' : 'placeholder']">
              {{ faultDesc || '请选择' }}
            </text>
            <uni-icons type="down" size="24" color="#cbd5e1"></uni-icons>
          </view>
        </picker>
      </view>
      <!-- 维修说明 -->
      <view class="form-item-v2">
        <text class="form-label-v2">维修说明</text>
        <picker mode="selector" :range="repairDescOptions" @change="onRepairDescPicker">
          <view class="form-picker-v2">
            <text :class="['picker-text', repairDesc ? '' : 'placeholder']">
              {{ repairDesc || '请选择' }}
            </text>
            <uni-icons type="down" size="24" color="#cbd5e1"></uni-icons>
          </view>
        </picker>
      </view>

      <!-- 其它维修说明 -->
      <view v-if="repairDesc === '其它维修说明'" class="form-item-v2">
        <text class="form-label-v2">其它维修说明 <text class="text-red">*</text></text>
        <textarea
          v-model="otherRepairDesc"
          class="form-textarea-v2"
          placeholder="请输入其它维修说明"
        />
      </view>

      <!-- 更换配件 -->
      <view class="form-item-v2">
        <text class="form-label-v2">更换配件 <text class="text-red">*</text></text>
        <view class="form-row-v2">
          <input v-model="replacePart" class="form-input-v2 flex-1" placeholder="配件名称" />
          <input
            v-model="replaceQuantity"
            class="form-input-v2 w-20"
            type="number"
            placeholder="数量"
          />
        </view>
      </view>

      <!-- 故障点图片 -->
      <view class="form-item-v2 mt-2">
        <text class="form-label-v2 mb-0">故障点图片</text>
        <view class="upload-section-grid">
          <view class="upload-grid-item">
            <MediaUploadField
              v-model="faultOldImages"
              label=""
              tip=""
              :show-label-row="false"
              file-mediatype="image"
              :limit="1"
              :del-icon="true"
            >
              <template #add>
                <view class="upload-grid-box">
                  <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                  <text class="upload-text">故障处旧图片</text>
                </view>
              </template>
            </MediaUploadField>
          </view>
          <view class="upload-grid-item">
            <MediaUploadField
              v-model="faultPointImages"
              :show-label-row="false"
              file-mediatype="image"
              :limit="1"
              :del-icon="true"
            >
              <template #add>
                <view class="upload-grid-box">
                  <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                  <text class="upload-text">故障处新图片</text>
                </view>
              </template>
            </MediaUploadField>
          </view>
          <view class="upload-grid-item">
            <MediaUploadField
              v-model="machineFrontImages"
              :show-label-row="false"
              file-mediatype="image"
              :limit="1"
              :del-icon="true"
            >
              <template #add>
                <view class="upload-grid-box">
                  <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                  <text class="upload-text">机器正面照片</text>
                </view>
              </template>
            </MediaUploadField>
          </view>
          <view class="upload-grid-item">
            <MediaUploadField
              v-model="machineBarcodeImages"
              :show-label-row="false"
              file-mediatype="image"
              :limit="1"
              :del-icon="true"
            >
              <template #add>
                <view class="upload-grid-box">
                  <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                  <text class="upload-text">机器条码照片</text>
                </view>
              </template>
            </MediaUploadField>
          </view>
          <view class="upload-grid-item">
            <MediaUploadField
              v-model="otherImages"
              :show-label-row="false"
              file-mediatype="image"
              :limit="5"
              :del-icon="true"
            >
              <template #add>
                <view class="upload-grid-box">
                  <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                  <text class="upload-text">其它图片</text>
                </view>
              </template>
            </MediaUploadField>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import { addAPhotoIcon } from '@/svgs'
  import type { WorkOrderRepairFaultOptionVO } from '@/api/order'

  const OTHER_REPAIR_DESC = '其它维修说明'

  /** 故障点登记 Form */
  const props = defineProps<{
    isRecheck: boolean
    faultOptions?: WorkOrderRepairFaultOptionVO[]
  }>()

  const normalizedFaultOptions = computed(() => {
    const list = props.faultOptions ?? []
    if (!Array.isArray(list)) return []
    return list
      .filter((x) => x && typeof x.faultDesc === 'string' && x.faultDesc.trim())
      .map((x) => ({
        faultDesc: x.faultDesc.trim(),
        repairOptions: Array.isArray(x.repairOptions)
          ? x.repairOptions.map((s) => String(s).trim()).filter(Boolean)
          : []
      }))
  })

  const faultDescOptions = computed(() => normalizedFaultOptions.value.map((x) => x.faultDesc))

  const repairDescOptions = computed(() => {
    const list = normalizedFaultOptions.value
    const selectedFault = faultDesc.value?.trim()
    const base =
      selectedFault && list.length
        ? list.find((x) => x.faultDesc === selectedFault)?.repairOptions ?? []
        : []
    // 始终允许兜底输入
    const out = base.slice()
    if (!out.includes(OTHER_REPAIR_DESC)) out.push(OTHER_REPAIR_DESC)
    return out
  })

  /** 故障描述 */
  const faultDesc = defineModel<string>('faultDesc', { default: '' })
  /** 维修说明 */
  const repairDesc = defineModel<string>('repairDesc', { default: '' })
  /** 其它维修说明 */
  const otherRepairDesc = defineModel<string>('otherRepairDesc', { default: '' })
  /** 更换配件 */
  const replacePart = defineModel<string>('replacePart', { default: '' })
  /** 更换配件数量 */
  const replaceQuantity = defineModel<string>('replaceQuantity', { default: '' })
  /** 故障点旧图片 */
  const faultOldImages = defineModel<unknown[]>('faultOldImages', { default: () => [] })
  /** 故障点新图片 */
  const faultPointImages = defineModel<unknown[]>('faultPointImages', { default: () => [] })
  /** 机器正面照片 */
  const machineFrontImages = defineModel<unknown[]>('machineFrontImages', { default: () => [] })
  /** 机器条码照片 */
  const machineBarcodeImages = defineModel<unknown[]>('machineBarcodeImages', {
    default: () => []
  })
  /** 其它图片 */
  const otherImages = defineModel<unknown[]>('otherImages', { default: () => [] })

  /**
   * 故障描述选择
   * @param e 事件
   */
  const onFaultDescPicker = (e: { detail: { value: string | number } }) => {
    const idx = Number(e.detail.value)
    faultDesc.value = faultDescOptions.value[idx] ?? ''
    // 切换故障后重置维修说明，避免不匹配
    repairDesc.value = ''
    otherRepairDesc.value = ''
  }

  /**
   * 维修说明选择
   * @param e 事件
   */
  const onRepairDescPicker = (e: { detail: { value: string | number } }) => {
    const idx = Number(e.detail.value)
    repairDesc.value = repairDescOptions.value[idx] ?? ''
  }
</script>

<style lang="scss" scoped>
  .od-apply-section-header {
    @include section-title-bar;
  }

  .fault-register-v2 {
    @include flex-col;
    gap: $space-lg;
    padding: 0 $space-md;

    .form-item-v2 {
      @include flex-col;

      .form-label-v2 {
        font-size: $font-md;
        color: $text-slate-500;
        display: block;
        margin-bottom: $space-sm;
      }

      .mb-0 {
        margin-bottom: 0;
      }

      .form-picker-v2 {
        @include flex-between;
        @include form-field-soft;
        padding: 20rpx $space-lg;

        .picker-text {
          font-size: 26rpx;
          color: $text-slate-900;

          &.placeholder {
            color: $text-slate-400;
          }
        }
      }

      .form-textarea-v2 {
        width: 100%;
        @include form-field-soft;
        padding: $space-md;
        font-size: 26rpx;
        color: $text-slate-900;
        min-height: 100rpx;

        &::placeholder {
          color: $text-slate-400;
        }
      }

      .form-row-v2 {
        @include flex-row;
        gap: $space-sm;

        .flex-1 {
          flex: 1;
        }

        .w-20 {
          width: 160rpx;
        }

        .form-input-v2 {
          @include form-field-soft;
          padding: 0 $space-lg;
          font-size: 26rpx;
          color: $text-slate-900;
          height: 80rpx;

          &::placeholder {
            color: $text-slate-400;
          }
        }
      }
    }

    .mt-2 {
      margin-top: $space-sm;
    }

    .upload-section-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: $space-md;
      margin-top: $space-sm;

      .upload-grid-item {
        width: 100%;
      }
    }

    .upload-grid-box {
      width: 100%;
      height: 100%;
      @include flex-column-center;
      gap: $space-xs;
      border: 4rpx dashed $surface-slate-200;
      border-radius: $radius-md;
      background-color: transparent;
      box-sizing: border-box;
      transition: all 0.2s;

      &:active {
        border-color: rgba($primary, 0.5);
      }

      .upload-icon {
        width: 48rpx;
        height: 48rpx;
      }

      .upload-text {
        font-size: 20rpx;
        color: $text-slate-400;
      }
    }
  }
</style>
