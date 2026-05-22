<template>
  <CommonModal v-model="visible" :title="title" safe-area animation="slide-up" @close="onClose">
    <!-- 搜索 -->
    <view class="atm-search">
      <view class="search-box">
        <uni-icons type="search" size="24" color="#cbd5e1" class="icon"></uni-icons>
        <input
          v-model="searchQuery"
          class="input"
          :placeholder="searchPlaceholder"
          placeholder-class="placeholder"
        />
      </view>
    </view>
    <!-- 维修员列表 -->
    <scroll-view scroll-y class="atm-list">
      <view
        v-for="tech in filteredTechnicianList"
        :key="tech.id"
        :class="['tech-card', { selected: selectedId === tech.id, busy: !!tech.isBusy }]"
        @tap="selectTechnician(tech.id)"
      >
        <view class="avatar-wrap">
          <image class="avatar" :src="avatarDisplayUrl(tech.avatar)" mode="aspectFill" />
          <view v-if="selectedId === tech.id" class="check-badge">
            <uni-icons type="checkmarkempty" size="12" color="#fff"></uni-icons>
          </view>
        </view>

        <view class="info">
          <view class="name-row">
            <text class="name">{{ tech.name }}</text>
            <text v-if="tech.isRecommend" class="tag">推荐</text>
          </view>
          <text v-if="tech.desc" class="desc">{{ tech.desc }}</text>
        </view>

        <view class="distance">
          <text v-if="tech.distance" class="dist-val">{{ tech.distance }}</text>
          <text v-if="tech.time" class="dist-time">{{ tech.time }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部按钮 -->
    <template #footer>
      <view class="atm-actions">
        <view class="btns btn-cancel" @tap="onClose">
          <text class="text">取消</text>
        </view>
        <view class="btns btn-confirm" @tap="onConfirm">
          <text class="text">确认指派</text>
        </view>
      </view>
    </template>
  </CommonModal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import CommonModal from '@/components/CommonModal/CommonModal.vue'
  import { ASSET_IMAGES } from '@/constants/assets'
  import { WECHAT_TMPL_WORKORDER_ASSIGN_NOTICE } from '@/constants/subscribeMessage'
  import { requestWorkOrderSubscribeWithTemplateIds } from '@/utils/requestWorkOrderSubscribe'

  /**
 * 派单弹窗：未设置头像时使用默认维修员形象
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const DEFAULT_ASSIGN_AVATAR = ASSET_IMAGES.worker

  const avatarDisplayUrl = (avatar?: string) => {
    const s = (avatar ?? '').trim()
    return s || DEFAULT_ASSIGN_AVATAR
  }

  /**
 * 维修员类型
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  export type Technician = {
    id: number | string
    name: string
    phone?: string
    avatar: string
    isRecommend?: boolean
    desc?: string
    distance?: string
    time?: string
    isBusy?: boolean
  }

  /**
   * 组件属性
   * @param modelValue 是否显示弹窗
   * @param technicianList 维修员列表
   * @param title 标题
   * @param searchPlaceholder 搜索占位符
   * @param selectedTechId 选择的维修员ID
   * @param resetOnOpen 是否重置搜索
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const props = withDefaults(
    defineProps<{
      modelValue: boolean
      technicianList: Technician[]
      /**
 * 当前要派单的工单 ID（与列表/工作台 `order.id` 一致），确认时原样带回，避免异步加载人员期间切换工单导致入参错乱
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      assignWorkOrderId?: string | number | null
      title?: string
      searchPlaceholder?: string
      selectedTechId?: number | string | null
      resetOnOpen?: boolean
    }>(),
    {
      assignWorkOrderId: null,
      title: '指派维修员',
      searchPlaceholder: '搜索姓名/手机号',
      selectedTechId: null,
      resetOnOpen: true
    }
  )

  /**
   * 组件事件
   * @param e 事件
   * @param v 值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'update:selectedTechId', v: number | string | null): void
    (e: 'close'): void
    (e: 'confirm', payload: { workOrderId: string | number; selectedTechId: number | string; technician: Technician }): void
  }>()

  /**
   * 是否显示弹窗
   * @returns 是否显示弹窗
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const visible = computed({
    get: () => props.modelValue,
    set: (v: boolean) => emit('update:modelValue', v)
  })

  /**
   * 搜索关键词
   * @returns 搜索关键词
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const searchQuery = ref('')
  /**
   * 选择的维修员ID
   * @returns 选择的维修员ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const selectedId = computed({
    get: () => props.selectedTechId,
    set: (v: number | string | null) => emit('update:selectedTechId', v)
  })

  /**
   * 监听弹窗是否显示
   * @param v 是否显示弹窗
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  watch(
    () => props.modelValue,
    (v) => {
      if (!v) return
      if (props.resetOnOpen) searchQuery.value = ''
    }
  )

  /**
   * 过滤维修员列表
   * @returns 过滤后的维修员列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const filteredTechnicianList = computed(() => {
    const q = searchQuery.value?.trim()
    if (!q) return props.technicianList
    return props.technicianList.filter((tech) => {
      const nameHit = tech.name?.includes(q)
      const phoneHit = tech.phone ? tech.phone.includes(q) : false
      return nameHit || phoneHit
    })
  })

  /**
   * 选择维修员
   * @param id 维修员ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const selectTechnician = (id: number | string) => {
    selectedId.value = id
  }

  /**
   * 关闭弹窗
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const onClose = () => {
    visible.value = false
    emit('close')
  }

  /**
   * 确认指派
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const onConfirm = async () => {
    if (selectedId.value === null || selectedId.value === undefined || selectedId.value === '') {
      uni.showToast({ title: '请选择维修员', icon: 'none' })
      return
    }
    const tech = props.technicianList.find((t) => t.id === selectedId.value)
    if (!tech) {
      uni.showToast({ title: '所选维修员不存在', icon: 'none' })
      return
    }
    const wid = props.assignWorkOrderId
    if (wid === null || wid === undefined || wid === '') {
      uni.showToast({ title: '工单信息缺失，请关闭后重试', icon: 'none' })
      return
    }
    await requestWorkOrderSubscribeWithTemplateIds([WECHAT_TMPL_WORKORDER_ASSIGN_NOTICE])
    emit('confirm', { workOrderId: wid, selectedTechId: selectedId.value, technician: tech })
  }
</script>

<style lang="scss" scoped>
  .atm-search {
    padding: 24rpx 32rpx;

    .search-box {
      position: relative;
      display: flex;
      align-items: center;

      .icon {
        position: absolute;
        left: 24rpx;
        pointer-events: none;
        color: $text-slate-400;
        font-size: 40rpx;
      }

      .input {
        width: 100%;
        height: 80rpx;
        padding-left: 80rpx;
        border-radius: 24rpx;
        background-color: $bg-hover;
        font-size: 28rpx;
        color: $text-slate-900;
        box-sizing: border-box;
      }

      .placeholder {
        color: $text-slate-400;
      }
    }
  }

  .atm-list {
    padding: 0 32rpx;
    box-sizing: border-box;
    height: 50vh;
  }

  .tech-card {
    display: flex;
    align-items: center;
    gap: 32rpx;
    background-color: $bg-card;
    padding: 20rpx 32rpx;
    border-radius: 24rpx;
    border: 2rpx solid $bg-hover;
    box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.05);
    margin-bottom: 24rpx;

    &.selected {
      border: 4rpx solid $primary;
      background-color: rgba(242, 102, 4, 0.05);

      .avatar {
        border: 4rpx solid $bg-card;
      }
    }

    &.busy {
      opacity: 0.8;
    }

    .avatar-wrap {
      position: relative;
      width: 100rpx;
      height: 100rpx;
    }

    .check-badge {
      position: absolute;
      right: -4rpx;
      bottom: -4rpx;
      z-index: 1;
      box-sizing: border-box;
      width: 40rpx;
      height: 40rpx;
      border-radius: 50%;
      border: 3rpx solid $bg-card;
      background-color: $primary;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
      border: 4rpx solid transparent;
    }

    .info {
      flex: 1;
      min-width: 0;
    }

    .name-row {
      display: flex;
      align-items: baseline;
      gap: 16rpx;
    }

    .name {
      color: $text-slate-900;
      font-size: 28rpx;
      font-weight: bold;
    }

    .tag {
      font-size: 24rpx;
      color: $primary;
      font-weight: 500;
      background-color: rgba(242, 102, 4, 0.1);
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
      flex-shrink: 0;
    }

    .desc {
      color: $text-slate-500;
      font-size: 24rpx;
      margin-top: 8rpx;
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .distance {
      text-align: right;
      flex-shrink: 0;
    }

    .dist-val {
      color: $text-slate-900;
      font-size: 28rpx;
      font-weight: 500;
      display: block;
    }

    .dist-time {
      color: $text-slate-400;
      font-size: 24rpx;
      margin-top: 8rpx;
      display: block;
    }
  }

  .atm-actions {
    @include modal-footer-bar;
    background-color: $bg-card;
    padding: $space-sm $space-lg;
  }
</style>
