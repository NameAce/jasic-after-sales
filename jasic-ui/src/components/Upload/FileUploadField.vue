<template>
  <div class="file-upload-field">
    <el-upload
      action=""
      multiple
      :disabled="disabled || uploading"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
    >
      <el-button size="small" type="primary" plain :loading="uploading" :disabled="disabled || uploading">
        {{ buttonText }}
      </el-button>
    </el-upload>
    <div v-if="tip" class="file-upload-field__tip">{{ tip }}</div>
    <div v-if="normalizedValue.length" class="file-upload-field__list">
      <div v-for="item in normalizedValue" :key="item.fileId" class="file-upload-field__item">
        <span class="file-upload-field__name" :title="item.originalName || item.fileName">
          {{ item.originalName || item.fileName || `文件-${item.fileId}` }}
        </span>
        <span class="file-upload-field__meta">{{ formatFileSize(item.fileSize) }}</span>
        <el-button type="text" size="mini" @click.stop="handlePreview(item)">预览</el-button>
        <el-button v-if="!disabled" type="text" size="mini" @click.stop="handleRemove(item.fileId)">移除</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getFilePreviewUrl, uploadSystemFile } from '@/api/file'

export default {
  name: 'FileUploadField',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    accept: {
      type: String,
      default: ''
    },
    sizeLimitMb: {
      type: Number,
      default: 10
    },
    buttonText: {
      type: String,
      default: '上传文件'
    },
    tip: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      uploading: false
    }
  },
  computed: {
    normalizedValue() {
      return Array.isArray(this.value) ? this.value : []
    },
    acceptedExtensions() {
      return (this.accept || '')
        .split(',')
        .map(item => item.trim().toLowerCase())
        .filter(item => item)
    }
  },
  methods: {
    beforeUpload(file) {
      if (!file) {
        return false
      }
      const fileName = file.name || ''
      const extension = fileName.includes('.') ? `.${fileName.split('.').pop().toLowerCase()}` : ''
      if (this.acceptedExtensions.length && !this.acceptedExtensions.includes(extension)) {
        this.$message.error(`仅支持 ${this.acceptedExtensions.join(' / ')} 文件`)
        return false
      }
      const maxSize = this.sizeLimitMb * 1024 * 1024
      if (this.sizeLimitMb > 0 && file.size > maxSize) {
        this.$message.error(`文件大小不能超过 ${this.sizeLimitMb}MB`)
        return false
      }
      return true
    },
    handleUpload(option) {
      const rawFile = option && option.file
      if (!rawFile) {
        return
      }
      this.uploading = true
      uploadSystemFile(rawFile).then(res => {
        if (!res || !res.data) {
          return
        }
        const nextList = this.normalizedValue.concat({
          fileId: res.data.fileId,
          originalName: res.data.originalName,
          fileSize: res.data.fileSize,
          fileExt: res.data.fileExt,
          previewUrl: res.data.previewUrl
        })
        this.$emit('input', nextList)
      }).finally(() => {
        this.uploading = false
      })
    },
    handlePreview(item) {
      if (!item || !item.fileId) {
        return
      }
      if (item.previewUrl) {
        window.open(item.previewUrl, '_blank')
        return
      }
      getFilePreviewUrl(item.fileId).then(res => {
        const previewUrl = res && res.data ? res.data.previewUrl : ''
        if (!previewUrl) {
          return
        }
        const nextList = this.normalizedValue.map(fileItem => {
          if (String(fileItem.fileId) !== String(item.fileId)) {
            return fileItem
          }
          return {
            ...fileItem,
            previewUrl
          }
        })
        this.$emit('input', nextList)
        window.open(previewUrl, '_blank')
      })
    },
    handleRemove(fileId) {
      this.$emit(
        'input',
        this.normalizedValue.filter(item => String(item.fileId) !== String(fileId))
      )
    },
    formatFileSize(size) {
      if (!size && size !== 0) {
        return ''
      }
      if (size < 1024) {
        return `${size} B`
      }
      if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)} KB`
      }
      return `${(size / (1024 * 1024)).toFixed(1)} MB`
    }
  }
}
</script>

<style lang="scss" scoped>
.file-upload-field__tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.file-upload-field__list {
  margin-top: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.file-upload-field__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
}

.file-upload-field__item + .file-upload-field__item {
  border-top: 1px solid #f5f7fa;
}

.file-upload-field__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-upload-field__meta {
  color: #909399;
  font-size: 12px;
}
</style>
