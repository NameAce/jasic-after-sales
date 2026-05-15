/**
 * 通用是否类常量：是/否等 Record 与 options，供表单与列表展示复用。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { transformRecordToOption } from '@/utils/common';

export const yesOrNoRecord: Record<CommonType.YesOrNo, App.I18n.I18nKey> = {
  Y: 'common.yesOrNo.yes',
  N: 'common.yesOrNo.no'
};

export const yesOrNoOptions = transformRecordToOption(yesOrNoRecord);
