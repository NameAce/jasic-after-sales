-- =============================================
-- 消息通知场景多目标重构：阶段二执行链路
-- 说明：
-- 1. 本脚本只补齐阶段二运行态所需字段和索引，不处理历史数据回填。
-- 2. 新写入事件、站内消息和分发任务会显式固化 scene_code / target_type。
-- 3. 所有 DDL 都带幂等保护，可重复执行。
-- =============================================

SET NAMES utf8mb4;

SET @has_notify_event_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_event'
);

SET @has_notify_message_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
);

SET @has_notify_dispatch_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
);

-- -------------------------------------------
-- 1. 通知事件表补齐 scene_code
-- -------------------------------------------
SET @has_event_scene_code = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_event'
    AND COLUMN_NAME = 'scene_code'
);

SET @ddl = IF(
  @has_notify_event_table = 0,
  'SELECT ''sys_notify_event table does not exist; skip scene_code migration'' AS upgrade_note',
  IF(@has_event_scene_code = 0,
     'ALTER TABLE `sys_notify_event` ADD COLUMN `scene_code` varchar(64) DEFAULT NULL COMMENT ''通知场景编码'' AFTER `event_type`',
     'SELECT ''sys_notify_event.scene_code already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_event_scene_status_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_event'
    AND INDEX_NAME = 'idx_scene_status_retry'
);

SET @ddl = IF(
  @has_notify_event_table = 0,
  'SELECT ''sys_notify_event table does not exist; skip idx_scene_status_retry migration'' AS upgrade_note',
  IF(@has_event_scene_status_index = 0,
     'ALTER TABLE `sys_notify_event` ADD KEY `idx_scene_status_retry` (`scene_code`, `status`, `next_retry_time`)',
     'SELECT ''sys_notify_event.idx_scene_status_retry already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------
-- 2. 站内消息表补齐 scene_code / target_type
-- -------------------------------------------
SET @has_message_scene_code = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND COLUMN_NAME = 'scene_code'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip scene_code migration'' AS upgrade_note',
  IF(@has_message_scene_code = 0,
     'ALTER TABLE `sys_notify_message` ADD COLUMN `scene_code` varchar(64) DEFAULT NULL COMMENT ''通知场景编码'' AFTER `event_type`',
     'SELECT ''sys_notify_message.scene_code already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_message_target_type = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND COLUMN_NAME = 'target_type'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip target_type migration'' AS upgrade_note',
  IF(@has_message_target_type = 0,
     'ALTER TABLE `sys_notify_message` ADD COLUMN `target_type` varchar(32) DEFAULT NULL COMMENT ''通知目标类型'' AFTER `scene_code`',
     'SELECT ''sys_notify_message.target_type already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_message_event_target_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND INDEX_NAME = 'idx_message_event_target'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip idx_message_event_target migration'' AS upgrade_note',
  IF(@has_message_event_target_index = 0,
     'ALTER TABLE `sys_notify_message` ADD KEY `idx_message_event_target` (`event_id`, `target_type`)',
     'SELECT ''sys_notify_message.idx_message_event_target already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_message_receiver_target_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND INDEX_NAME = 'idx_receiver_target_time'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip idx_receiver_target_time migration'' AS upgrade_note',
  IF(@has_message_receiver_target_index = 0,
     'ALTER TABLE `sys_notify_message` ADD KEY `idx_receiver_target_time` (`receiver_id`, `target_type`, `create_time`)',
     'SELECT ''sys_notify_message.idx_receiver_target_time already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------
-- 3. 分发表补齐 scene_code / target_type
-- -------------------------------------------
SET @has_dispatch_scene_code = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
    AND COLUMN_NAME = 'scene_code'
);

SET @ddl = IF(
  @has_notify_dispatch_table = 0,
  'SELECT ''sys_notify_dispatch table does not exist; skip scene_code migration'' AS upgrade_note',
  IF(@has_dispatch_scene_code = 0,
     'ALTER TABLE `sys_notify_dispatch` ADD COLUMN `scene_code` varchar(64) DEFAULT NULL COMMENT ''通知场景编码'' AFTER `event_id`',
     'SELECT ''sys_notify_dispatch.scene_code already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dispatch_target_type = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
    AND COLUMN_NAME = 'target_type'
);

SET @ddl = IF(
  @has_notify_dispatch_table = 0,
  'SELECT ''sys_notify_dispatch table does not exist; skip target_type migration'' AS upgrade_note',
  IF(@has_dispatch_target_type = 0,
     'ALTER TABLE `sys_notify_dispatch` ADD COLUMN `target_type` varchar(32) DEFAULT NULL COMMENT ''通知目标类型'' AFTER `scene_code`',
     'SELECT ''sys_notify_dispatch.target_type already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dispatch_scene_target_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
    AND INDEX_NAME = 'idx_dispatch_scene_target'
);

SET @ddl = IF(
  @has_notify_dispatch_table = 0,
  'SELECT ''sys_notify_dispatch table does not exist; skip idx_dispatch_scene_target migration'' AS upgrade_note',
  IF(@has_dispatch_scene_target_index = 0,
     'ALTER TABLE `sys_notify_dispatch` ADD KEY `idx_dispatch_scene_target` (`scene_code`, `target_type`)',
     'SELECT ''sys_notify_dispatch.idx_dispatch_scene_target already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dispatch_event_target_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_dispatch'
    AND INDEX_NAME = 'idx_dispatch_event_target'
);

SET @ddl = IF(
  @has_notify_dispatch_table = 0,
  'SELECT ''sys_notify_dispatch table does not exist; skip idx_dispatch_event_target migration'' AS upgrade_note',
  IF(@has_dispatch_event_target_index = 0,
     'ALTER TABLE `sys_notify_dispatch` ADD KEY `idx_dispatch_event_target` (`event_id`, `target_type`)',
     'SELECT ''sys_notify_dispatch.idx_dispatch_event_target already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
