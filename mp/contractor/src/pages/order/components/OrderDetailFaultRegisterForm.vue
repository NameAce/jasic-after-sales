<template>
  <view>
    <!-- 维修说明标题 -->
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">{{ isRecheck ? '复检登记' : '故障点登记' }}</text>
    </view>
    <!-- 维修说明内容 -->
    <view class="fault-register-v2">
      <!-- 维修说明（下拉多选） -->
      <view class="form-item-v2">
        <text class="form-label-v2">维修说明 <text class="text-red">*</text></text>
        <view class="form-picker-v2" @click="openRepairDescDropdown">
          <text :class="['picker-text', selectedRepairDescText ? '' : 'placeholder']">
            {{ selectedRepairDescText || '请选择' }}
          </text>
          <uni-icons type="down" size="15" color="#cbd5e1"></uni-icons>
        </view>
        <view v-if="showRepairDescDropdown" class="repair-desc-dropdown">
          <view
            v-for="option in repairDescOptions"
            :key="option"
            class="repair-desc-option"
            @click.stop="toggleDraftRepairDesc(option)"
          >
            <checkbox
              class="repair-desc-checkbox"
              :checked="draftRepairDesc.includes(option)"
              color="#f26604"
              style="transform: scale(0.8); transform-origin: center;"
            />
            <text class="repair-desc-option-text">{{ option }}</text>
          </view>
          <view class="repair-desc-dropdown-actions">
            <view class="dropdown-btn dropdown-btn--cancel" @click.stop="cancelRepairDescSelect">
              取消
            </view>
            <view class="dropdown-btn dropdown-btn--confirm" @click.stop="confirmRepairDescSelect">
              确定
            </view>
          </view>
        </view>
      </view>

      <!-- 其它维修说明 -->
      <view v-if="repairDesc.includes('其它维修说明')" class="form-item-v2">
        <text class="form-label-v2">其它维修说明 <text class="text-red">*</text></text>
        <textarea
          v-model="otherRepairDesc"
          class="form-textarea-v2"
          placeholder="请输入其它维修说明"
          auto-height
        />
      </view>

      <!-- 更换配件：首行仅加号新增，后续行仅减号删除（删除后不入参） -->
      <view class="form-item-v2">
        <text class="form-label-v2">更换配件 <text class="text-red">*</text></text>
        <view v-for="(row, idx) in replaceParts" :key="row.id" class="replace-part-row">
          <view class="form-row-v2 form-row-v2--with-actions">
            <input
              v-model="row.part"
              class="form-input-v2 flex-1"
              placeholder="请输入配件名称"
              placeholder-class="form-placeholder-v2"
            />
            <input
              v-model="row.quantity"
              class="form-input-v2 w-20"
              type="number"
              placeholder="数量"
              placeholder-class="form-placeholder-v2"
            />
            <view class="replace-part-actions">
              <view v-if="idx > 0" class="replace-part-icon-btn" @click="removeReplacePartRow(idx)">
                <uni-icons type="minus" size="22" color="#64748b" />
              </view>
              <view v-if="idx === 0" class="replace-part-icon-btn" @click="addReplacePartRow">
                <uni-icons type="plus" size="22" color="#64748b" />
              </view>
            </view>
          </view>
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
          <view class="upload-grid-item upload-grid-item--other-files">
            <MediaUploadField
              v-model="otherImages"
              :show-label-row="false"
              file-mediatype="image"
              :limit="1"
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
  import { computed, ref } from 'vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import { addAPhotoIcon } from '@/svgs'
  import type { WorkOrderRepairFaultOptionVO } from '@/api/workOrder'

  const OTHER_REPAIR_DESC = '其它维修说明'

  type ReplacePartRowModel = { id: number; part: string; quantity: string }

  const maxReplacePartRowId = (rows: ReplacePartRowModel[]) =>
    rows.reduce((m, r) => Math.max(m, r.id), 0)

  /** 故障点登记 Form（复检与维修共用，isRecheck 仅影响区块标题） */
  const props = withDefaults(
    defineProps<{
      isRecheck?: boolean
      faultOptions?: WorkOrderRepairFaultOptionVO[]
    }>(),
    {
      isRecheck: false,
      faultOptions: () => []
    }
  )

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

  const repairDescOptions = computed(() => {
    const list = normalizedFaultOptions.value
    const seen = new Set<string>()
    const out: string[] = []
    for (const x of list) {
      for (const r of x.repairOptions) {
        if (!seen.has(r)) {
          seen.add(r)
          out.push(r)
        }
      }
    }
    if (!seen.has(OTHER_REPAIR_DESC)) out.push(OTHER_REPAIR_DESC)
    return out
  })

  /** 维修说明（多选） */
  const repairDesc = defineModel<string[]>('repairDesc', { default: () => [] })
  /** 其它维修说明 */
  const otherRepairDesc = defineModel<string>('otherRepairDesc', { default: '' })
  /** 更换配件（多行；已删除行不会出现在提交数据中）
   * defineModel 的 default 不能引用本 setup 内声明的变量，首行 id 用字面量 */
  const replaceParts = defineModel<ReplacePartRowModel[]>('replaceParts', {
    default: () => [{ id: 1, part: '', quantity: '' }]
  })

  const addReplacePartRow = () => {
    const nextId = maxReplacePartRowId(replaceParts.value) + 1
    replaceParts.value = [...replaceParts.value, { id: nextId, part: '', quantity: '' }]
  }

  const removeReplacePartRow = (index: number) => {
    const prev = replaceParts.value
    const list = prev.filter((_, i) => i !== index)
    if (list.length > 0) {
      replaceParts.value = list
      return
    }
    const nextId = maxReplacePartRowId(prev) + 1
    replaceParts.value = [{ id: nextId, part: '', quantity: '' }]
  }
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

  const showRepairDescDropdown = ref(false)
  const draftRepairDesc = ref<string[]>([])
  const selectedRepairDescText = computed(() => repairDesc.value.join('、'))

  const openRepairDescDropdown = () => {
    draftRepairDesc.value = [...repairDesc.value]
    showRepairDescDropdown.value = true
  }

  const toggleDraftRepairDesc = (option: string) => {
    if (draftRepairDesc.value.includes(option)) {
      draftRepairDesc.value = draftRepairDesc.value.filter((x) => x !== option)
      return
    }
    draftRepairDesc.value = [...draftRepairDesc.value, option]
  }

  const cancelRepairDescSelect = () => {
    showRepairDescDropdown.value = false
    draftRepairDesc.value = []
  }

  const confirmRepairDescSelect = () => {
    repairDesc.value = [...draftRepairDesc.value]
    if (!repairDesc.value.includes(OTHER_REPAIR_DESC)) {
      otherRepairDesc.value = ''
    }
    showRepairDescDropdown.value = false
    draftRepairDesc.value = []
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

      .repair-desc-dropdown {
        margin-top: $space-sm;
        border: 2rpx solid $border-slate;
        border-radius: $radius-md;
        background: $bg-card;
        padding: $space-sm;
      }

      .repair-desc-option {
        @include flex-row;
        align-items: center;
        gap: $space-xs;
        padding: 12rpx 8rpx;
      }

      .repair-desc-option-text {
        font-size: 26rpx;
        color: $text-main;
      }

      .repair-desc-dropdown-actions {
        @include flex-row;
        justify-content: flex-end;
        gap: $space-sm;
        padding-top: $space-sm;
      }

      .dropdown-btn {
        font-size: 24rpx;
        padding: 10rpx 20rpx;
        border-radius: $radius-sm;
      }

      .dropdown-btn--cancel {
        color: $text-secondary;
        background: $bg-light;
      }

      .dropdown-btn--confirm {
        color: $text-bg;
        background: $primary;
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
        align-items: center;
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

        &--with-actions {
          flex-wrap: nowrap;
        }
      }

      .replace-part-row + .replace-part-row {
        margin-top: $space-sm;
      }

      .replace-part-actions {
        @include flex-row;
        align-items: center;
        flex-shrink: 0;
        gap: 4rpx;
      }

      .replace-part-icon-btn {
        width: 72rpx;
        height: 72rpx;
        @include flex-column-center;
        border-radius: $radius-md;
        background: $bg-light;

        &:active {
          opacity: 0.85;
        }
      }
    }

    .mt-2 {
      margin-top: $space-sm;
    }

    .upload-section-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: $space-sm;
      margin-top: $space-sm;

      .upload-grid-item {
        width: 100%;
      }

      /**
       * 其它图片多选：已选缩略图需横向排列；原 1/3 列宽过窄会被迫竖排。
       * 仅拉宽格子，不改动 #add 插槽里的上传入口样式。
       */
      .upload-grid-item--other-files {
        grid-column: 1 / -1;

        :deep(.uni-file-picker) {
          display: block;
          width: 100%;
          flex: none !important;
        }

        :deep(.uni-file-picker__container) {
          flex-direction: row;
          flex-wrap: wrap;
          align-items: flex-start;
        }
      }
    }

    :deep(.form-placeholder-v2) {
      color: #94a3b8;
      font-size: 26rpx;
    }

    .upload-grid-box {
      width: 100%;
      height: 100%;
      @include flex-column-center;
      gap: $space-xs;
      border: 4rpx dashed $border-slate;
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
