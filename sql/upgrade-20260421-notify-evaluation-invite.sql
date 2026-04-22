-- =============================================
-- Notification phase 3 bootstrap:
-- 1. Add notify template channel table
-- 2. Add notify dispatch table
-- 3. Seed built-in evaluation invite template
-- 4. Seed default MP_SUBSCRIBE channel config
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_notify_template_channel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `channel_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '渠道开关',
  `channel_scene` varchar(16) DEFAULT NULL COMMENT '渠道场景',
  `config_json` text DEFAULT NULL COMMENT '渠道配置 JSON',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_channel_scene` (`template_code`, `channel_type`, `channel_scene`),
  KEY `idx_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知模板渠道配置表';

CREATE TABLE IF NOT EXISTS `sys_notify_dispatch` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` bigint unsigned NOT NULL COMMENT '来源事件 ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `receiver_type` varchar(32) NOT NULL COMMENT '接收人类型',
  `receiver_id` bigint unsigned DEFAULT NULL COMMENT '接收人 ID',
  `receiver_address` varchar(128) DEFAULT NULL COMMENT '接收地址快照',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` bigint unsigned NOT NULL COMMENT '业务 ID',
  `biz_no` varchar(64) DEFAULT NULL COMMENT '业务编号',
  `dispatch_status` varchar(32) NOT NULL COMMENT '分发状态',
  `result_code` varchar(64) DEFAULT NULL COMMENT '结果码',
  `result_message` varchar(500) DEFAULT NULL COMMENT '结果说明',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `payload_json` text DEFAULT NULL COMMENT '分发负载快照',
  `channel_response_json` text DEFAULT NULL COMMENT '渠道响应快照',
  `sent_time` datetime DEFAULT NULL COMMENT '发送成功时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_biz_receiver` (`biz_type`, `biz_id`, `template_code`, `channel_type`, `receiver_type`, `receiver_id`),
  KEY `idx_dispatch_status_retry` (`dispatch_status`, `retry_count`, `next_retry_time`),
  KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知分发表';

INSERT INTO `sys_notify_template`
(`template_code`, `template_name`, `template_source`, `biz_type`, `event_type`, `message_type`,
 `notify_enabled`, `override_enabled`, `route_type`, `title_template`, `summary_template`,
 `route_value_template`, `variables_json`, `remark`, `create_time`, `update_time`)
SELECT
  'WORK_ORDER_EVALUATION_INVITE',
  '客户满意度评价通知',
  'BUILT_IN',
  'WORK_ORDER',
  'WORK_ORDER_EVALUATION_INVITE',
  'EXTERNAL_NOTIFY',
  1,
  0,
  'WORK_ORDER_EVALUATE',
  '客户满意度评价通知',
  '您的维修工单${orderNo}已关闭，邀请您对本次服务进行评价',
  '${workOrderId}',
  '[{"name":"workOrderId","desc":"工单ID（仅路由参数）"},{"name":"orderNo","desc":"维修工单号"},{"name":"customerId","desc":"客户ID"},{"name":"customerMobile","desc":"客户联系电话"},{"name":"customerOpenid","desc":"客户openid"},{"name":"companyId","desc":"网点ID"},{"name":"companyName","desc":"网点名称"},{"name":"closedTime","desc":"工单关闭时间"}]',
  '客户满意度评价通知内置模板',
  NOW(),
  NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_notify_template`
  WHERE `template_code` = 'WORK_ORDER_EVALUATION_INVITE'
    AND `template_source` = 'BUILT_IN'
);

INSERT INTO `sys_notify_template_channel`
(`template_code`, `channel_type`, `channel_enabled`, `channel_scene`, `config_json`, `remark`, `create_time`, `update_time`)
SELECT
  'WORK_ORDER_EVALUATION_INVITE',
  'MP_SUBSCRIBE',
  1,
  'C',
  '{"scene":"C","templateId":"","pagePathTemplate":"pages/order/evaluate?workOrderId=${workOrderId}","fieldMapping":[{"field":"thing1","value":"${orderNo}"},{"field":"phone_number2","value":"${customerMobile}"},{"field":"thing3","value":"${companyName}"}]}',
  '客户满意度评价通知默认渠道配置',
  NOW(),
  NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_notify_template_channel`
  WHERE `template_code` = 'WORK_ORDER_EVALUATION_INVITE'
    AND `channel_type` = 'MP_SUBSCRIBE'
    AND `channel_scene` = 'C'
);
