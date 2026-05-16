-- =============================================
-- 消息通知最终优化 Phase 1：数据库可靠性补齐
-- 说明：
-- 1. 本脚本仅补齐最终优化文档 Phase 1 要求的可靠性字段、索引和历史数据回填。
-- 2. 保留旧通知配置仅用于兼容历史环境，不再作为当前正式通知链路。
-- 3. 所有 DDL/DML 均带幂等保护，可重复执行。
-- =============================================

SET NAMES utf8mb4;

SET @has_notify_event_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_event'
);

SET @has_notify_dispatch_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
);

SET @has_notify_message_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
);

SET @has_sys_config_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_config'
);

SET @has_event_processing_time = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_event'
    AND COLUMN_NAME = 'processing_time'
);

SET @ddl = IF(
  @has_notify_event_table = 0,
  'SELECT ''sys_notify_event table does not exist; skip processing_time migration'' AS upgrade_note',
  IF(@has_event_processing_time = 0,
  'ALTER TABLE `sys_notify_event` ADD COLUMN `processing_time` datetime DEFAULT NULL COMMENT ''开始处理时间'' AFTER `status`',
  'SELECT ''sys_notify_event.processing_time already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_event_processing_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_event'
    AND INDEX_NAME = 'idx_processing_time'
);

SET @ddl = IF(
  @has_notify_event_table = 0,
  'SELECT ''sys_notify_event table does not exist; skip idx_processing_time migration'' AS upgrade_note',
  IF(@has_event_processing_index = 0,
  'ALTER TABLE `sys_notify_event` ADD KEY `idx_processing_time` (`status`, `processing_time`)',
  'SELECT ''sys_notify_event.idx_processing_time already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dispatch_processing_time = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
    AND COLUMN_NAME = 'processing_time'
);

SET @ddl = IF(
  @has_notify_dispatch_table = 0,
  'SELECT ''sys_notify_dispatch table does not exist; skip processing_time migration'' AS upgrade_note',
  IF(@has_dispatch_processing_time = 0,
  'ALTER TABLE `sys_notify_dispatch` ADD COLUMN `processing_time` datetime DEFAULT NULL COMMENT ''开始处理时间'' AFTER `dispatch_status`',
  'SELECT ''sys_notify_dispatch.processing_time already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dispatch_processing_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
    AND INDEX_NAME = 'idx_dispatch_processing_time'
);

SET @ddl = IF(
  @has_notify_dispatch_table = 0,
  'SELECT ''sys_notify_dispatch table does not exist; skip idx_dispatch_processing_time migration'' AS upgrade_note',
  IF(@has_dispatch_processing_index = 0,
  'ALTER TABLE `sys_notify_dispatch` ADD KEY `idx_dispatch_processing_time` (`dispatch_status`, `processing_time`)',
  'SELECT ''sys_notify_dispatch.idx_dispatch_processing_time already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_message_template_code = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND COLUMN_NAME = 'template_code'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip template_code migration'' AS upgrade_note',
  IF(@has_message_template_code = 0,
  'ALTER TABLE `sys_notify_message` ADD COLUMN `template_code` varchar(64) DEFAULT NULL COMMENT ''通知场景编码'' AFTER `event_type`',
  'SELECT ''sys_notify_message.template_code already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip template_code backfill'' AS upgrade_note',
  'UPDATE `sys_notify_message`
   SET `template_code` = ''WORK_ORDER_ASSIGNED''
   WHERE `event_type` = ''WORK_ORDER_ASSIGNED''
     AND (`template_code` IS NULL OR TRIM(`template_code`) = '''')'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_message_template_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND INDEX_NAME = 'idx_notify_message_template'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip idx_notify_message_template migration'' AS upgrade_note',
  IF(@has_message_template_index = 0,
  'ALTER TABLE `sys_notify_message` ADD KEY `idx_notify_message_template` (`template_code`)',
  'SELECT ''sys_notify_message.idx_notify_message_template already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  @has_sys_config_table = 0,
  'SELECT ''sys_config table does not exist; skip deprecated notify config marking'' AS upgrade_note',
  'UPDATE `sys_config`
   SET `remark` = CASE
     WHEN `remark` IS NULL OR TRIM(`remark`) = ''''
       THEN ''历史废弃通知配置，仅兼容旧环境；当前正式模板ID请改为 sys_notify_template_channel 维护，后续可删除''
     WHEN `remark` LIKE ''%历史废弃通知配置%''
       THEN `remark`
     ELSE CONCAT(`remark`, ''；历史废弃通知配置，仅兼容旧环境；当前正式模板ID请改为 sys_notify_template_channel 维护，后续可删除'')
   END
   WHERE `config_key` IN (
     ''wechat.notify.customer.repairFinished.templateId'',
     ''wechat.notify.customer.repairFinished.pagePath'',
     ''wechat.notify.customer.evaluationInvite.templateId'',
     ''wechat.notify.customer.evaluationInvite.pagePath'',
     ''wechat.notify.company.customerEvaluated.templateId'',
     ''wechat.notify.company.customerEvaluated.pagePath''
   )'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
