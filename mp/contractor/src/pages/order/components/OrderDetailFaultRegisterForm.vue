<template>
  <view>
    <!-- 维修说明标题 -->
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">{{ isRecheck ? '复检登记' : '故障点登记' }}</text>
    </view>
    <!-- 维修说明内容 -->
    <view class="fault-register-v2">
      <!-- 维修确认故障（仅"维修登记"且"有配置"时展示；对应后端 WorkOrderRepairDTO.faultItems） -->
      <view v-if="showFaultItemsSelect" class="form-item-v2">
        <text class="form-label-v2">维修确认故障 <text class="text-red">*</text></text>
        <view class="form-picker-v2" @click="openFaultItemsDropdown">
          <text :class="['picker-text', faultItemsDisplayText ? '' : 'placeholder']">
            {{ faultItemsDisplayText || '请选择' }}
          </text>
          <uni-icons type="down" size="15" color="#cbd5e1"></uni-icons>
        </view>
        <view v-if="showFaultItemsDropdown" class="repair-desc-dropdown">
          <view
            v-for="option in faultItemOptions"
            :key="option"
            class="repair-desc-option"
            @click.stop="toggleDraftFaultItem(option)"
          >
            <checkbox
              class="repair-desc-checkbox"
              :checked="draftFaultItems.includes(option)"
              color="#f26604"
              style="transform: scale(0.8); transform-origin: center;"
            />
            <text class="repair-desc-option-text">{{ option }}</text>
          </view>
          <view class="repair-desc-dropdown-actions">
            <view class="dropdown-btn dropdown-btn--cancel" @click.stop="cancelFaultItemsSelect">
              取消
            </view>
            <view class="dropdown-btn dropdown-btn--confirm" @click.stop="confirmFaultItemsSelect">
              确定
            </view>
          </view>
        </view>
      </view>

      <!-- 其它故障说明（仅维修登记 + faultItems 含「其它故障」时） -->
      <view v-if="showFaultRemarkInput" class="form-item-v2">
        <text class="form-label-v2">其它故障说明 <text class="text-red">*</text></text>
        <textarea
          v-model="faultRemark"
          class="form-textarea-v2"
          placeholder="请输入其它故障说明"
          auto-height
        />
      </view>

      <!-- 复检登记：只读回显首次维修确认故障（由详情页传入；无时不展示，保持与 jasic-ui 一致） -->
      <view v-if="isRecheck && reviewFaultDescDisplay" class="form-item-v2">
        <text class="form-label-v2">维修确认故障</text>
        <view class="form-picker-v2 is-readonly">
          <text class="picker-text">{{ reviewFaultDescDisplay }}</text>
        </view>
      </view>
      <view v-if="isRecheck && reviewFaultRemarkDisplay" class="form-item-v2">
        <text class="form-label-v2">其它故障说明</text>
        <view class="form-textarea-v2 is-readonly">{{ reviewFaultRemarkDisplay }}</view>
      </view>

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
              <view class="upload-grid-box">
                <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                <text class="upload-text">故障处旧图片</text>
              </view>
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
              <view class="upload-grid-box">
                <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                <text class="upload-text">故障处新图片</text>
              </view>
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
              <view class="upload-grid-box">
                <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                <text class="upload-text">机器正面图片</text>
              </view>
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
              <view class="upload-grid-box">
                <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                <text class="upload-text">机器条码图片</text>
              </view>
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
              <view class="upload-grid-box">
                <image class="upload-icon" :src="addAPhotoIcon" mode="aspectFit" />
                <text class="upload-text">其它图片</text>
              </view>
            </MediaUploadField>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import MediaUploadField from '@/components/MediaUploadField/MediaUploadField.vue'
  import { addAPhotoIcon } from '@/svgs'
  import type { WorkOrderRepairFaultOptionVO } from '@/api/workOrder'

  const OTHER_REPAIR_DESC = '其它维修说明'
  /** 与后端 WorkOrderServiceImpl.OTHER_FAULT_LABEL 对齐 */
  const OTHER_FAULT_LABEL = '其它故障'

  type ReplacePartRowModel = { id: number; part: string; quantity: string }

  const maxReplacePartRowId = (rows: ReplacePartRowModel[]) =>
    rows.reduce((m, r) => Math.max(m, r.id), 0)

  /**
   * 故障点登记 Form（复检与维修共用，isRecheck 仅影响区块标题）
   * - 维修登记：按 `faultOptions` 展示「维修确认故障」多选，并过滤"维修说明"下拉
   * - 复检登记：按 jasic-ui 规则沿用首次维修的故障描述，当前页不再采集 faultItems/faultRemark
   * - 无配置时：隐藏「维修确认故障」，"维修说明"退化为只有「其它维修说明」
   */
  const props = withDefaults(
    defineProps<{
      isRecheck?: boolean
      faultOptions?: WorkOrderRepairFaultOptionVO[]
      /** 复检登记时用于只读回显首次维修确认故障 */
      firstRepairFaultDesc?: string
      /** 复检登记时用于只读回显首次故障备注 */
      firstRepairFaultRemark?: string
      /** 复检：确认故障项，用于按故障过滤「维修说明」候选（与维修登记 faultItems 语义对齐） */
      recheckConfirmFaultItems?: string[]
    }>(),
    {
      isRecheck: false,
      faultOptions: () => [],
      firstRepairFaultDesc: '',
      firstRepairFaultRemark: '',
      recheckConfirmFaultItems: () => []
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

  /** 故障与维修配置是否存在（无配置走 repairDesc 手工兜底） */
  const hasRepairFaultConfig = computed(() => normalizedFaultOptions.value.length > 0)

  /** 维修登记 tab：是否展示「维修确认故障」多选（配置非空 + 非复检） */
  const showFaultItemsSelect = computed(
    () => !props.isRecheck && hasRepairFaultConfig.value
  )

  /** 维修确认故障候选（含末尾追加的「其它故障」） */
  const faultItemOptions = computed(() => {
    const seen = new Set<string>()
    const out: string[] = []
    for (const f of normalizedFaultOptions.value) {
      if (!seen.has(f.faultDesc)) {
        seen.add(f.faultDesc)
        out.push(f.faultDesc)
      }
    }
    if (!seen.has(OTHER_FAULT_LABEL)) out.push(OTHER_FAULT_LABEL)
    return out
  })

  /** 维修登记：选中 faultItems 对应的 repairOptions 并集 + 末尾「其它维修说明」 */
  const faultFilteredRepairOptions = computed(() => {
    const selected = new Set(faultItems.value || [])
    const seen = new Set<string>()
    const out: string[] = []
    for (const f of normalizedFaultOptions.value) {
      if (!selected.has(f.faultDesc)) continue
      for (const r of f.repairOptions) {
        if (!seen.has(r)) {
          seen.add(r)
          out.push(r)
        }
      }
    }
    if (!seen.has(OTHER_REPAIR_DESC)) out.push(OTHER_REPAIR_DESC)
    return out
  })

  /** 复检：按只读「维修确认故障」过滤维修说明候选 */
  const recheckFaultFilteredRepairOptions = computed(() => {
    const items = (props.recheckConfirmFaultItems || []).map((x) => String(x || '').trim()).filter(Boolean)
    const selected = new Set(items)
    const seen = new Set<string>()
    const out: string[] = []
    for (const f of normalizedFaultOptions.value) {
      if (!selected.has(f.faultDesc)) continue
      for (const r of f.repairOptions) {
        if (!seen.has(r)) {
          seen.add(r)
          out.push(r)
        }
      }
    }
    if (!seen.has(OTHER_REPAIR_DESC)) out.push(OTHER_REPAIR_DESC)
    return out
  })

  /** 兜底：复检 / 无配置场景继续使用"所有 repairOptions 并集 + 其它维修说明" */
  const allRepairOptions = computed(() => {
    const seen = new Set<string>()
    const out: string[] = []
    for (const x of normalizedFaultOptions.value) {
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

  /** 当前维修说明下拉：维修登记按 faultItems；复检按确认故障项；否则全量 */
  const repairDescOptions = computed(() => {
    if (showFaultItemsSelect.value) return faultFilteredRepairOptions.value
    if (
      props.isRecheck &&
      hasRepairFaultConfig.value &&
      (props.recheckConfirmFaultItems || []).some((x) => String(x || '').trim())
    ) {
      return recheckFaultFilteredRepairOptions.value
    }
    return allRepairOptions.value
  })

  /** 维修确认故障（多选） */
  const faultItems = defineModel<string[]>('faultItems', { default: () => [] })
  /** 其它故障说明（faultItems 含「其它故障」时必填） */
  const faultRemark = defineModel<string>('faultRemark', { default: '' })
  /** 维修说明（多选） */
  const repairDesc = defineModel<string[]>('repairDesc', { default: () => [] })
  /** 其它维修说明 */
  const otherRepairDesc = defineModel<string>('otherRepairDesc', { default: '' })
  /** 更换配件（多行；已删除行不会出现在提交数据中）
   * defineModel 的 default 不能引用本 setup 内声明的变量，首行 id 用字面量 */
  const replaceParts = defineModel<ReplacePartRowModel[]>('replaceParts', {
    default: () => [{ id: 1, part: '', quantity: '' }]
  })

  /** faultItems 含「其它故障」时展示 faultRemark 输入框 */
  const showFaultRemarkInput = computed(
    () => showFaultItemsSelect.value && (faultItems.value || []).includes(OTHER_FAULT_LABEL)
  )

  /** 复检登记：只读展示首次维修确认故障（优先用父组件传入，否则回退空串） */
  const reviewFaultDescDisplay = computed(() => String(props.firstRepairFaultDesc || '').trim())
  const reviewFaultRemarkDisplay = computed(() => String(props.firstRepairFaultRemark || '').trim())

  const faultItemsDisplayText = computed(() => (faultItems.value || []).join('、'))

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

  const showFaultItemsDropdown = ref(false)
  const draftFaultItems = ref<string[]>([])

  const openFaultItemsDropdown = () => {
    draftFaultItems.value = [...(faultItems.value || [])]
    showFaultItemsDropdown.value = true
  }

  const toggleDraftFaultItem = (option: string) => {
    if (draftFaultItems.value.includes(option)) {
      draftFaultItems.value = draftFaultItems.value.filter((x) => x !== option)
      return
    }
    draftFaultItems.value = [...draftFaultItems.value, option]
  }

  const cancelFaultItemsSelect = () => {
    showFaultItemsDropdown.value = false
    draftFaultItems.value = []
  }

  const confirmFaultItemsSelect = () => {
    const next = [...draftFaultItems.value]
    faultItems.value = next
    if (!next.includes(OTHER_FAULT_LABEL)) {
      faultRemark.value = ''
    }
    // faultItems 变化后剪掉不再命中的维修说明选项（OTHER_REPAIR_DESC 始终保留）
    if (showFaultItemsSelect.value) {
      const allowed = new Set(faultFilteredRepairOptions.value)
      const nextRepair = (repairDesc.value || []).filter((r) => allowed.has(r))
      if (nextRepair.length !== (repairDesc.value || []).length) {
        repairDesc.value = nextRepair
        if (!nextRepair.includes(OTHER_REPAIR_DESC)) {
          otherRepairDesc.value = ''
        }
      }
    }
    showFaultItemsDropdown.value = false
    draftFaultItems.value = []
  }

  const showRepairDescDropdown = ref(false)
  const draftRepairDesc = ref<string[]>([])
  const selectedRepairDescText = computed(() => (repairDesc.value || []).join('、'))

  watch(
    () => [
      props.isRecheck,
      hasRepairFaultConfig.value,
      repairDescOptions.value.join('\u0001'),
      (repairDesc.value || []).join('\u0001'),
    ],
    () => {
      if (!props.isRecheck) return
      if (!hasRepairFaultConfig.value) return
      const allowed = new Set(repairDescOptions.value)
      const cur = repairDesc.value || []
      const next = cur.filter((r) => allowed.has(r))
      if (next.length !== cur.length) {
        repairDesc.value = next
        if (!next.includes(OTHER_REPAIR_DESC)) {
          otherRepairDesc.value = ''
        }
      }
    },
  )

  const openRepairDescDropdown = () => {
    draftRepairDesc.value = [...(repairDesc.value || [])]
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

        &.is-readonly {
          background: $bg-light;
          pointer-events: none;
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

        &.is-readonly {
          background: $bg-light;
          min-height: unset;
          white-space: pre-wrap;
          word-break: break-all;
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
       * 与「机器条码图片」同一行且排在条码右侧（占第 2～3 列），避免独占一行看起来像跑到条码「上方」。
       */
      .upload-grid-item--other-files {
        grid-column: 2 / -1;

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
