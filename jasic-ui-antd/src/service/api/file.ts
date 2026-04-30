/**
 * 文件服务：上传、下载等二进制与附件相关接口。
 */
import { request } from '../request';

type IdLike = string | number;
type Query = Record<string, unknown>;

/**
 * 作用：上传系统文件（multipart）。
 * @param file 文件二进制
 * @returns {Promise}
 */
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

/** 作用：获取文件预览 URL。 */
export function getFilePreviewUrl(fileId: IdLike) {
  return request<string | Record<string, unknown>>({
    url: `/system/file/${fileId}/preview-url`,
    method: 'get'
  });
}

/** 作用：分页/条件查询业务已绑定的文件列表。 */
export function listBizFiles(params?: Query) {
  return request<unknown>({ url: '/system/file/biz/list', method: 'get', params });
}

/** 作用：将文件绑定到业务实体。 */
export function bindBizFiles(data: Record<string, unknown>) {
  return request({ url: '/system/file/biz/bind', method: 'post', data });
}

/** 作用：解除业务与文件的绑定关系。 */
export function unbindBizFile(data: Record<string, unknown>) {
  return request({ url: '/system/file/biz/unbind', method: 'post', data });
}
