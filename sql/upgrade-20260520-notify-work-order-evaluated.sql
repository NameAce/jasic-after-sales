-- =============================================
-- B端客户评价完成提醒通知场景
-- 说明：
-- 1. 新增 WORK_ORDER_EVALUATED 场景与默认小程序订阅目标
-- 2. 默认模板使用“评价提醒”模板，接收人由业务层解析为责任维修员、最后派单人和公司主账号
-- =============================================

SET NAMES utf8mb4;

INSERT INTO `notify_scene`
(`scene_code`, `scene_name`, `biz_type`, `event_code`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'WORK_ORDER_EVALUATED',
       'B端评价提醒',
       'WORK_ORDER',
       'WORK_ORDER_EVALUATED',
       1,
       '客户提交评价成功后，通知当前责任维修员、最后派单人和最终处理公司的主账号，仅开放 B 端小程序订阅通知目标',
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `notify_scene`
    WHERE `scene_code` = 'WORK_ORDER_EVALUATED'
);

UPDATE `notify_scene`
SET `scene_name` = 'B端评价提醒',
    `biz_type` = 'WORK_ORDER',
    `event_code` = 'WORK_ORDER_EVALUATED',
    `status` = 1,
    `remark` = '客户提交评价成功后，通知当前责任维修员、最后派单人和最终处理公司的主账号，仅开放 B 端小程序订阅通知目标',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_EVALUATED';

INSERT INTO `notify_scene_target`
(`scene_code`, `target_type`, `enabled`, `title_template`, `content_template`, `route_type`,
 `route_value_template`, `config_json`, `remark`, `create_time`, `update_time`)
SELECT 'WORK_ORDER_EVALUATED',
       'MP_SUBSCRIBE_B',
       1,
       '评价提醒',
       '维修工单 ${orderNo} 已收到客户满意度评价，请及时查看详情',
       'WORK_ORDER_DETAIL',
       '${workOrderId}',
       '{"templateId":"aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q","channelScene":"B","pagePathTemplate":"pages/order/detail?workOrderId=${workOrderId}","fieldMapping":[{"field":"character_string8","value":"${orderNo}"},{"field":"thing9","value":"${customerName}"},{"field":"phone_number10","value":"${customerMobile}"},{"field":"thing11","value":"${assignedUserName}"}]}',
       'B端客户评价完成提醒小程序订阅通知，接单人字段固定展示客户评价时的最终责任维修员',
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `notify_scene_target`
    WHERE `scene_code` = 'WORK_ORDER_EVALUATED'
      AND `target_type` = 'MP_SUBSCRIBE_B'
);

UPDATE `notify_scene_target`
SET `enabled` = 1,
    `title_template` = '评价提醒',
    `content_template` = '维修工单 ${orderNo} 已收到客户满意度评价，请及时查看详情',
    `route_type` = 'WORK_ORDER_DETAIL',
    `route_value_template` = '${workOrderId}',
    `config_json` = '{"templateId":"aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q","channelScene":"B","pagePathTemplate":"pages/order/detail?workOrderId=${workOrderId}","fieldMapping":[{"field":"character_string8","value":"${orderNo}"},{"field":"thing9","value":"${customerName}"},{"field":"phone_number10","value":"${customerMobile}"},{"field":"thing11","value":"${assignedUserName}"}]}',
    `remark` = 'B端客户评价完成提醒小程序订阅通知，接单人字段固定展示客户评价时的最终责任维修员',
    `update_time` = NOW()
WHERE `scene_code` = 'WORK_ORDER_EVALUATED'
  AND `target_type` = 'MP_SUBSCRIBE_B';
