/**
 * 文件服务：上传、下载等二进制与附件相关接口。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { request } from '../request';

type IdLike = string | number;
type FileBizType =
  | 'WORK_ORDER_FAULT_IMAGE'
  | 'WORK_ORDER_FAULT_VIDEO'
  | 'WORK_ORDER_FAULT_VOICE'
  | 'WORK_ORDER_SENDER_VOUCHER'
  | 'WORK_ORDER_RETURN_VOUCHER'
  | 'WORK_ORDER_REPAIR_OLD_IMAGE'
  | 'WORK_ORDER_REPAIR_NEW_IMAGE'
  | 'WORK_ORDER_REPAIR_MACHINE_IMAGE'
  | 'WORK_ORDER_REPAIR_BARCODE_IMAGE'
  | 'WORK_ORDER_REPAIR_OTHER_IMAGE';

interface FileBizParams {
  bizType: FileBizType;
  bizId: IdLike;
}

interface BindBizFilesParams extends FileBizParams {
  fileIds: IdLike[];
  remark?: string;
}

interface UnbindBizFileParams extends FileBizParams {
  fileId: IdLike;
}

/**
 * 作用：上传系统文件（multipart）。
 * @param file 文件二进制
 * @returns {Promise}
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

/**
 * 作用：将已上传文件绑定到业务实体（工单故障图、凭证等）。
 * @param data - 业务类型、业务 ID、fileIds
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function bindBizFiles(data: BindBizFilesParams) {
  return request({ url: '/system/file/biz/bind', method: 'post', data });
}

/**
 * 作用：解除业务与文件的绑定关系。
 * @param data - 业务类型、业务 ID、fileId
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function unbindBizFile(data: UnbindBizFileParams) {
  return request({ url: '/system/file/biz/unbind', method: 'post', data });
}
