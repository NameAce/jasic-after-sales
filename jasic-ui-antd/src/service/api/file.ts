import { request } from '../request';

type IdLike = string | number;
type Query = Record<string, unknown>;

/** 与 jasic-ui `src/api/file.js`：`uploadSystemFile` */
export function uploadSystemFile(file: Blob | File) {
  const formData = new FormData();
  formData.append('file', file);
  return request<Record<string, unknown>>({
    url: '/system/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/** 与 jasic：`getFilePreviewUrl` */
export function getFilePreviewUrl(fileId: IdLike) {
  return request<string | Record<string, unknown>>({
    url: `/system/file/${fileId}/preview-url`,
    method: 'get'
  });
}

/** 与 jasic：`listBizFiles` */
export function listBizFiles(params?: Query) {
  return request<unknown>({ url: '/system/file/biz/list', method: 'get', params });
}

/** 与 jasic：`bindBizFiles` */
export function bindBizFiles(data: Record<string, unknown>) {
  return request({ url: '/system/file/biz/bind', method: 'post', data });
}

/** 与 jasic：`unbindBizFile` */
export function unbindBizFile(data: Record<string, unknown>) {
  return request({ url: '/system/file/biz/unbind', method: 'post', data });
}
