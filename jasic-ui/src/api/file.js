import request from '@/utils/request'

export function uploadSystemFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/system/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getFilePreviewUrl(fileId) {
  return request({
    url: `/system/file/${fileId}/preview-url`,
    method: 'get'
  })
}

export function listBizFiles(params) {
  return request({
    url: '/system/file/biz/list',
    method: 'get',
    params
  })
}

export function bindBizFiles(data) {
  return request({
    url: '/system/file/biz/bind',
    method: 'post',
    data
  })
}

export function unbindBizFile(data) {
  return request({
    url: '/system/file/biz/unbind',
    method: 'post',
    data
  })
}
