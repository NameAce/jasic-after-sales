-- =============================================
-- 消息通知一期 Phase 1：建表与枚举基线
-- 说明：
-- 1. 本脚本只负责建立一期通知基础表结构，不包含后续业务接入。
-- 2. 如果环境中已存在旧通知表，请先执行 upgrade-20260418-remove-notify.sql。
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_notify_event` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_key` varchar(128) NOT NULL COMMENT '幂等键',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` bigint unsigned NOT NULL COMMENT '业务ID',
  `biz_no` varchar(64) NOT NULL COMMENT '业务编号',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '操作人ID',
  `receiver_id` bigint unsigned NOT NULL COMMENT '接收人ID',
  `payload_json` text NOT NULL COMMENT '事件载荷',
  `status` varchar(32) NOT NULL COMMENT '事件状态',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `error_message` varchar(500) DEFAULT NULL COMMENT '失败信息',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_key` (`event_key`),
  KEY `idx_status_next_retry` (`status`, `next_retry_time`),
  KEY `idx_biz_type_id` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知事件表';

CREATE TABLE IF NOT EXISTS `sys_notify_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` bigint unsigned NOT NULL COMMENT '来源事件ID',
  `message_type` varchar(32) NOT NULL COMMENT '消息类型',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` bigint unsigned NOT NULL COMMENT '业务ID',
  `biz_no` varchar(64) NOT NULL COMMENT '业务编号',
  `receiver_id` bigint unsigned NOT NULL COMMENT '接收人ID',
  `receiver_company_id` bigint unsigned NOT NULL COMMENT '接收公司ID',
  `receiver_name` varchar(64) NOT NULL COMMENT '接收人名称快照',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `summary` varchar(255) NOT NULL COMMENT '摘要',
  `route_type` varchar(32) NOT NULL COMMENT '跳转类型',
  `route_value` varchar(128) NOT NULL COMMENT '跳转值',
  `todo_status` varchar(32) NOT NULL COMMENT '待办状态',
  `invalid_reason` varchar(64) DEFAULT NULL COMMENT '失效原因',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `done_time` datetime DEFAULT NULL COMMENT '已处理时间',
  `invalid_time` datetime DEFAULT NULL COMMENT '失效时间',
  `ext_json` text DEFAULT NULL COMMENT '扩展字段',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_status_time` (`receiver_id`, `todo_status`, `create_time`),
  KEY `idx_receiver_company_status_time` (`receiver_company_id`, `receiver_id`, `todo_status`, `create_time`),
  KEY `idx_biz_receiver` (`biz_type`, `biz_id`, `receiver_company_id`, `receiver_id`),
  KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知消息表';

CREATE TABLE IF NOT EXISTS `sys_notify_message_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id` bigint unsigned NOT NULL COMMENT '消息ID',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `action_user_id` bigint unsigned DEFAULT NULL COMMENT '动作执行人',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `snapshot_json` text DEFAULT NULL COMMENT '快照',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_time` (`message_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知消息日志表';
