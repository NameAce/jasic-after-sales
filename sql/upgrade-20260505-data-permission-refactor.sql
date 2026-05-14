-- Data permission refactor upgrade.
--
-- DP-05 adds receiver company isolation to notification/todo messages.
-- Existing rows cannot safely infer receiver_company_id, so the upgrade keeps
-- the new column nullable. Application code fails closed when receiver company
-- context is missing.

SET @has_notify_receiver_company_id = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND COLUMN_NAME = 'receiver_company_id'
);

SET @has_notify_message_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip receiver_company_id migration'' AS upgrade_note',
  IF(@has_notify_receiver_company_id = 0,
  'ALTER TABLE `sys_notify_message` ADD COLUMN `receiver_company_id` bigint unsigned DEFAULT NULL COMMENT ''接收公司ID'' AFTER `receiver_id`',
  'SELECT ''sys_notify_message.receiver_company_id already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_notify_receiver_company_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND INDEX_NAME = 'idx_receiver_company_status_time'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip receiver company index migration'' AS upgrade_note',
  IF(@has_notify_receiver_company_index = 0,
  'ALTER TABLE `sys_notify_message` ADD KEY `idx_receiver_company_status_time` (`receiver_company_id`, `receiver_id`, `todo_status`, `create_time`)',
  'SELECT ''sys_notify_message.idx_receiver_company_status_time already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_notify_biz_receiver_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND INDEX_NAME = 'idx_biz_receiver'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip biz receiver index migration'' AS upgrade_note',
  IF(@has_notify_biz_receiver_index > 0,
  'ALTER TABLE `sys_notify_message` DROP INDEX `idx_biz_receiver`',
  'SELECT ''sys_notify_message.idx_biz_receiver does not exist before rebuild'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_notify_biz_receiver_index = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_notify_message'
    AND INDEX_NAME = 'idx_biz_receiver'
);

SET @ddl = IF(
  @has_notify_message_table = 0,
  'SELECT ''sys_notify_message table does not exist; skip biz receiver index creation'' AS upgrade_note',
  IF(@has_notify_biz_receiver_index = 0,
  'ALTER TABLE `sys_notify_message` ADD KEY `idx_biz_receiver` (`biz_type`, `biz_id`, `receiver_company_id`, `receiver_id`)',
  'SELECT ''sys_notify_message.idx_biz_receiver already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- DP-06 records manual trigger users and scheduled system task identity.
SET @has_sync_task_log_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sync_task_log'
);

SET @has_sync_task_log_trigger_type = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sync_task_log'
    AND COLUMN_NAME = 'trigger_type'
);

SET @ddl = IF(
  @has_sync_task_log_table = 0,
  'SELECT ''sync_task_log table does not exist; skip trigger_type migration'' AS upgrade_note',
  IF(@has_sync_task_log_trigger_type = 0,
  'ALTER TABLE `sync_task_log` ADD COLUMN `trigger_type` varchar(16) NOT NULL DEFAULT ''SCHEDULED'' COMMENT ''触发类型（MANUAL/SCHEDULED）'' AFTER `data_end_time`',
  'SELECT ''sync_task_log.trigger_type already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_sync_task_log_trigger_user_id = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sync_task_log'
    AND COLUMN_NAME = 'trigger_user_id'
);

SET @ddl = IF(
  @has_sync_task_log_table = 0,
  'SELECT ''sync_task_log table does not exist; skip trigger_user_id migration'' AS upgrade_note',
  IF(@has_sync_task_log_trigger_user_id = 0,
  'ALTER TABLE `sync_task_log` ADD COLUMN `trigger_user_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT ''触发人ID，0表示系统任务'' AFTER `trigger_type`',
  'SELECT ''sync_task_log.trigger_user_id already exists'' AS upgrade_note'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- DP-10 keeps region assignment permission explicit for user-region relation writes.
SET @platform_region_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `menu_type` = 'C'
    AND `path` = 'region'
  LIMIT 1
);

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '大区授权', @platform_region_menu_id, 'F', NULL, NULL, 'system:region:assign', NULL,
       5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_region_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `menu_type` = 'F'
      AND `perms` = 'system:region:assign'
  );

SET @platform_role_id = (
  SELECT `id`
  FROM `sys_role`
  WHERE `company_id` = 1
    AND `role_key` = 'platform_admin'
  LIMIT 1
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT @platform_role_id, m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE @platform_role_id IS NOT NULL
  AND m.`subject_type` = 'PLATFORM'
  AND m.`perms` = 'system:region:assign'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = @platform_role_id
      AND rm.`menu_id` = m.`id`
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE m.`subject_type` = 'PLATFORM'
  AND m.`perms` = 'system:region:assign'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
