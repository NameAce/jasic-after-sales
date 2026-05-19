-- =============================================
-- 小程序订阅通知模板文案增量同步
-- 说明：
-- 1. 本脚本只修正已存在场景配置的名称、文案和备注，不创建新表、不删除数据。
-- 2. 适用于已经执行过通知场景初始化，且库中已存在 `notify_scene / notify_scene_target` 数据的环境。
-- 3. 本脚本会覆盖以下标准场景的当前配置文案：
--    - WORK_ORDER_ACCEPT：建单后待派单通知
--    - WORK_ORDER_ASSIGNED：派单后维修员接单通知
--    - WORK_ORDER_EVALUATION_INVITE：评价通知触发口径备注
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 同步场景名称与场景备注
-- -------------------------------------------
UPDATE `notify_scene`
SET `scene_name` = 'B端待派单通知',
    `remark` = '阶段一初始化场景：工单进入目标承接网点待派单池后，通知当前网点符合口径的可派单用户，仅开放 B 端小程序订阅通知目标',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_ACCEPT';

UPDATE `notify_scene`
SET `scene_name` = 'B端维修员接单通知',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_ASSIGNED';

UPDATE `notify_scene`
SET `remark` = '阶段一初始化场景：工单关闭且允许评价后，向客户发起评价邀请，仅开放 C 端小程序订阅通知目标',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_EVALUATION_INVITE';

-- -------------------------------------------
-- 2. 同步目标级模板标题、正文与备注
-- -------------------------------------------
UPDATE `notify_scene_target`
SET `title_template` = 'B端待派单通知',
    `content_template` = '新工单 ${orderNo} 已进入当前网点待派单池，请及时派单处理',
    `remark` = '阶段一初始化目标：B端建单待派单小程序订阅通知，客户名称需按“客户姓名 -> 客户手机号 -> 客户”兜底',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_ACCEPT'
  AND `target_type` = 'MP_SUBSCRIBE_B';

UPDATE `notify_scene_target`
SET `title_template` = 'B端维修员接单通知',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_ASSIGNED'
  AND `target_type` = 'MP_SUBSCRIBE_B';

UPDATE `notify_scene_target`
SET `remark` = '阶段一初始化目标：C端客户满意度评价通知在工单关闭且允许评价后发送，联系电话按服务电话优先、联系电话兜底规则取值',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_EVALUATION_INVITE'
  AND `target_type` = 'MP_SUBSCRIBE_C';
