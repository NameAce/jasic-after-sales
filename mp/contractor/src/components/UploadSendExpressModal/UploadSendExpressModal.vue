<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）组件 UploadSendExpressModal -->
  <view v-if="visible" class="upload-express-mask" @click.self="onCancel">
    <view class="upload-express-card" @click.stop>
      <view class="upload-express-title">上传寄件单号</view>
      <view class="upload-express-field">
        <text class="upload-express-label">寄件快递单号</text>
        <input
          v-model.trim="sendExpressNoInput"
          class="upload-express-input"
          placeholder="请输入寄件快递单号"
          placeholder-class="upload-express-placeholder"
          :maxlength="60"
        />
      </view>
      <view class="upload-express-field">
        <text class="upload-express-label">寄件凭证（可选）</text>
        <view class="upload-express-photo" @click="chooseVoucher">
          <image
            v-if="previewPath"
            class="upload-express-preview"
            :src="previewPath"
            mode="aspectFill"
          />
          <view v-else class="upload-express-photo-placeholder">
            <uni-icons type="camera-filled" size="28" color="#f26604" />
            <text class="upload-express-photo-tip">点击选择凭证照片</text>
          </view>
        </view>
      </view>
      <view class="upload-express-actions">
        <view class="upload-express-btn cancel" @click="onCancel">取消</view>
        <view class="upload-express-btn confirm" @click="onConfirm">确认提交</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  /**
   * 上传寄件单号弹窗（承包商端 UPLOAD_SEND_EXPRESS 动作）
   * - 字段对齐后端 `WorkOrderSendExpressDTO`：sendExpressNo（必填）+ senderVoucherFileIds（可选）
   * - 选择图片后立即走 `/system/file/upload` 上传，拿到 fileId 暂存
   * - 确认时回抛给父组件 submit（PUT `/system/work-order/send-express`）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { ref, watch } from 'vue'
  import { uploadSystemFile } from '@/api/file'
  import { hideRequestLoading, showApiToast, showRequestLoading } from '@/utils/uiFeedback'

  const props = defineProps<{
    visible: boolean
    workOrderId: number | string
  }>()

  const emit = defineEmits<{
    (e: 'update:visible', val: boolean): void
    (e: 'confirm', payload: { workOrderId: number; sendExpressNo: string; senderVoucherFileIds?: number[] }): void
    (e: 'cancel'): void
  }>()

  const sendExpressNoInput = ref('')
  const previewPath = ref('')
  const voucherFileId = ref(0)

  const reset = () => {
    sendExpressNoInput.value = ''
    previewPath.value = ''
    voucherFileId.value = 0
  }

  const onCancel = () => {
    emit('update:visible', false)
    emit('cancel')
  }

  const chooseVoucher = () => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const path = res.tempFilePaths?.[0]
        if (!path) return
        previewPath.value = path
        // 上传走 uploadSystemFile（uni.uploadFile，不经 http.ts），业务侧手动加 loading
        showRequestLoading('上传中...')
        try {
          const uploaded = await uploadSystemFile(path)
          const fid = Number(uploaded.fileId)
          if (!Number.isFinite(fid) || fid <= 0) {
            void showApiToast('上传失败：未获取到凭证文件ID', { duration: 1800 })
            previewPath.value = ''
            voucherFileId.value = 0
            return
          }
          voucherFileId.value = fid
        } catch (e) {
          previewPath.value = ''
          voucherFileId.value = 0
          void showApiToast((e as Error)?.message || '上传失败')
        } finally {
          hideRequestLoading()
        }
      }
    })
  }

  const onConfirm = () => {
    const wid = Number(props.workOrderId)
    if (!Number.isFinite(wid) || wid <= 0) {
      void showApiToast('工单ID无效')
      return
    }
    const no = sendExpressNoInput.value.trim()
    if (!no) {
      void showApiToast('请输入寄件快递单号')
      return
    }
    emit('confirm', {
      workOrderId: wid,
      sendExpressNo: no,
      ...(voucherFileId.value > 0 ? { senderVoucherFileIds: [voucherFileId.value] } : {})
    })
  }

  watch(
    () => props.visible,
    (v) => {
      if (v) reset()
    },
    { immediate: true }
  )
</script>

<style lang="scss" scoped>
  .upload-express-mask {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.48);
    z-index: 999;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .upload-express-card {
    width: 620rpx;
    max-width: 90vw;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $space-lg;
    display: flex;
    flex-direction: column;
    gap: $space-md;
  }

  .upload-express-title {
    font-size: $font-lg;
    font-weight: bold;
    color: $text-main;
  }

  .upload-express-field {
    display: flex;
    flex-direction: column;
    gap: $space-xs;
  }

  .upload-express-label {
    font-size: $font-sm;
    color: $text-slate-500;
  }

  .upload-express-input {
    height: 80rpx;
    padding: 0 $space-lg;
    font-size: 26rpx;
    color: $text-slate-900;
    background: $bg-light;
    border-radius: $radius-md;
  }

  .upload-express-placeholder {
    color: $text-slate-400;
    font-size: 26rpx;
  }

  .upload-express-photo {
    width: 100%;
    height: 240rpx;
    border-radius: $radius-md;
    background: $bg-light;
    border: 2rpx dashed $border-slate;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  .upload-express-preview {
    width: 100%;
    height: 100%;
  }

  .upload-express-photo-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-xs;
  }

  .upload-express-photo-tip {
    font-size: 24rpx;
    color: $text-slate-500;
  }

  .upload-express-actions {
    display: flex;
    justify-content: flex-end;
    gap: $space-sm;
  }

  .upload-express-btn {
    padding: 14rpx 28rpx;
    border-radius: $radius-sm;
    font-size: 26rpx;

    &.cancel {
      color: $text-secondary;
      background: $bg-light;
    }

    &.confirm {
      color: $text-bg;
      background: $primary;
    }
  }
</style>
