<template>
  <custom-nav-bar :title="isEdit ? '编辑地址' : '新增地址'" surface="sticky" />

  <view class="page-index">
    <!-- 内容区域 -->
    <scroll-view
      class="main-content page-padding"
      scroll-y
      :scroll-into-view="scrollIntoView"
      scroll-with-animation
    >
      <view class="form-card">
        <view class="form-item">
          <FormItemAnchor name="name" />
          <text class="label">收件人</text>
          <input
            v-model="form.name"
            class="input"
            placeholder="请输入姓名"
            :focus="focusField === 'name'"
            @blur="clearFocusField"
          />
        </view>
        <view class="form-item">
          <FormItemAnchor name="phone" />
          <text class="label">手机号</text>
          <input
            v-model="form.phone"
            class="input"
            type="number"
            :maxlength="11"
            placeholder="请输入手机号"
            :focus="focusField === 'phone'"
            @blur="clearFocusField"
          />
        </view>
        <view class="form-item">
          <FormItemAnchor name="region" />
          <text class="label">所在地区</text>
          <picker mode="region" :value="regionValue" @change="onRegionChange">
            <view class="picker-display">
              <text :class="regionText ? 'val' : 'placeholder'">{{
                regionText || '请选择省市区'
              }}</text>
              <uni-icons type="down" size="12" :color="themeColors.textMuted"></uni-icons>
            </view>
          </picker>
        </view>
        <view class="form-item block">
          <FormItemAnchor name="detail" />
          <text class="label">详细地址</text>
          <textarea
            v-model="form.detail"
            class="textarea"
            auto-height
            placeholder="街道、楼牌号等"
            :focus="focusField === 'detail'"
            @blur="clearFocusField"
          />
        </view>
      </view>

      <view class="save-btn" @click="save">保存</view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  import { computed, nextTick, reactive, ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'
  import { triggerScrollIntoView } from '@/utils/formFieldScrollFocus'
  import { showApiToast } from '@/utils/uiFeedback'
  import { addCompanyAddress, updateCompanyAddress } from '@/api/companyAddress'
  import { loadAddresses, saveAddresses, type SavedAddress } from '@/utils/addressStorage'
  import { resolveSavedAddressRegion } from '@/utils/parseAddressRegion'
  import { themeColors } from '@/theme/colors'

  const scrollIntoView = ref('')
  const focusField = ref('')

  const clearFocusField = () => {
    focusField.value = ''
  }

  /**
   * 校验失败时滚到对应项并尽量聚焦可输入控件
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const goToFirstInvalidField = (fieldKey: string, requestFocus: boolean) => {
    triggerScrollIntoView(scrollIntoView, fieldKey)
    if (!requestFocus) return
    focusField.value = ''
    nextTick(() => {
      focusField.value = fieldKey
    })
  }

  // 编辑ID
  const editId = ref<string | null>(null)
  const isEdit = computed(() => Boolean(editId.value))

  // 表单数据
  const form = reactive({
    name: '',
    phone: '',
    province: '',
    city: '',
    county: '',
    detail: '',
    /**
 * 编辑时沿用列表/缓存中的默认标记，提交 PUT 时回传
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    isDefault: 0
  })

  /**
 * picker mode=region 绑定的 [省,市,区]
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const regionValue = ref<string[]>(['', '', ''])

  /**
   * 地区文本
   * @returns 地区文本
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const regionText = computed(() => {
    const { province, city, county } = form
    if (!province && !city && !county) return ''
    return `${province}${city}${county}`
  })

  /**
   * 编辑回显：写入表单与 region picker（与 aftersale 一致；公司地址仅整段时需先解析省市区）
   * @param found 本地缓存地址
   */
  const fillFormFromSaved = (found: SavedAddress) => {
    const region = resolveSavedAddressRegion(found)
    form.name = found.name
    form.phone = found.phone
    form.province = region.province
    form.city = region.city
    form.county = region.county
    form.detail = region.detail
    form.isDefault = found.isDefault ?? 0
    regionValue.value = [region.province, region.city, region.county]
  }

  /**
   * 页面加载
   * @param options - 选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  onLoad((options?: Record<string, string>) => {
    const id = String(options?.id ?? '').trim()
    if (!id) return
    editId.value = id
    const found = loadAddresses().find((a) => String(a.id) === id)
    if (found) {
      fillFormFromSaved(found)
    }
  })

  /**
   * 地区选择变化
   * @param e - 事件
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const onRegionChange = (e: { detail: { value: string[] } }) => {
    const v = e.detail.value
    regionValue.value = v
    form.province = v[0] || ''
    form.city = v[1] || ''
    form.county = v[2] || ''
  }

  /**
   * 保存
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const save = async () => {
    const name = form.name.trim()
    const phone = form.phone.trim()
    const detail = form.detail.trim()
    if (!name) {
      void showApiToast('请填写收件人')
      goToFirstInvalidField('name', true)
      return
    }
    if (!/^1\d{10}$/.test(phone)) {
      void showApiToast('请输入正确手机号')
      goToFirstInvalidField('phone', true)
      return
    }
    if (!form.province || !form.city) {
      void showApiToast('请选择所在地区')
      goToFirstInvalidField('region', false)
      return
    }
    if (!detail) {
      void showApiToast('请填写详细地址')
      goToFirstInvalidField('detail', true)
      return
    }

    const list = loadAddresses()

    if (editId.value) {
      const idNum = Number(editId.value)
      if (!Number.isFinite(idNum)) {
        void showApiToast('地址无效，请删除后重新添加', { duration: 2000 })
        return
      }
      try {
        // updateCompanyAddress 是 PUT 写接口，http.ts 自动显示带 mask 的 loading
        const fullAddress = `${form.province}${form.city}${form.county}${detail}`
        await updateCompanyAddress({
          id: idNum,
          address: fullAddress,
          contactName: name,
          contactPhone: phone,
          isDefault: form.isDefault
        })
        const payload: SavedAddress = {
          id: editId.value,
          name,
          phone,
          province: form.province,
          city: form.city,
          county: form.county,
          detail,
          fullAddress,
          isDefault: form.isDefault
        }
        const idx = list.findIndex((a) => a.id === editId.value)
        if (idx >= 0) list[idx] = payload
        else list.unshift(payload)
        saveAddresses(list)
        // 提示完成后再返回上页，保证用户能看完"已保存"
        await showApiToast('已保存')
        uni.navigateBack()
      } catch {
        /* http 已 showApiToast */
      }
      return
    }

    try {
      // addCompanyAddress 是 POST 写接口，http.ts 自动显示带 mask 的 loading
      const fullAddressLine = `${form.province}${form.city}${form.county || ''}${detail}`
      const res = await addCompanyAddress({
        address: fullAddressLine,
        contactName: name,
        contactPhone: phone,
        isDefault: 0
      })
      const serverId = String(res.data)
      const payload: SavedAddress = {
        id: serverId,
        name,
        phone,
        province: form.province,
        city: form.city,
        county: form.county,
        detail
      }
      list.unshift(payload)
      saveAddresses(list)
      await showApiToast('已保存')
      uni.navigateBack()
    } catch {
      /* http 已 showApiToast */
    }
  }
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .main-content.page-padding {
    @include flex-column-gap;
    flex: 1;
    box-sizing: border-box;
    padding-top: $space-lg;
  }

  .form-card {
    @include white-card;
    padding: $space-xs $space-lg $space-lg;
  }

  .form-item {
    position: relative;
    padding: $space-md 0;
    border-bottom: 2rpx solid $border-lighter;
    display: flex;
    align-items: center;
    gap: $space-md;

    &.block {
      flex-direction: column;
      align-items: stretch;
    }

    &:last-child {
      border-bottom: none;
    }

    .label {
      width: 160rpx;
      flex-shrink: 0;
      font-size: $font-md;
      color: $text-label;
    }

    &.block .label {
      width: auto;
      margin-bottom: 12rpx;
    }

    .input {
      flex: 1;
      font-size: 30rpx;
      color: $text-dark;
    }

    .textarea {
      width: 100%;
      min-height: 160rpx;
      font-size: 30rpx;
      color: $text-dark;
      line-height: 1.5;
    }

    .picker-display {
      flex: 1;
      @include flex-between;
      min-height: $space-xl;

      .val {
        font-size: 30rpx;
        color: $text-dark;
      }

      .placeholder {
        font-size: 30rpx;
        color: $text-muted;
      }
    }
  }

  .save-btn {
    @include btn-primary-round;
    font-size: $font-lg;
    margin-top: $space-lg;
  }
</style>
