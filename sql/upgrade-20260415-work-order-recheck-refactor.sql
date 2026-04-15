-- 工单维修/复检重构：补齐维修登记阶段字段，故障快照改为文本字段，兼容历史数据

SET @add_work_order_repair_register_stage = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_repair'
    AND COLUMN_NAME = 'register_stage'
);
SET @sql_work_order_repair_register_stage = IF(
  @add_work_order_repair_register_stage = 0,
  'ALTER TABLE `work_order_repair` ADD COLUMN `register_stage` varchar(32) NOT NULL DEFAULT ''REPAIR'' COMMENT ''登记阶段（REPAIR=维修登记，RECHECK=复检登记）'' AFTER `repair_user_id`',
  'SELECT 1'
);
PREPARE stmt_work_order_repair_register_stage FROM @sql_work_order_repair_register_stage;
EXECUTE stmt_work_order_repair_register_stage;
DEALLOCATE PREPARE stmt_work_order_repair_register_stage;

UPDATE `work_order_repair`
SET `register_stage` = 'REPAIR'
WHERE `register_stage` IS NULL OR `register_stage` = '';

SET @add_work_order_fault_other_desc = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'other_desc'
);
SET @sql_work_order_fault_other_desc = IF(
  @add_work_order_fault_other_desc = 0,
  'ALTER TABLE `work_order_fault` ADD COLUMN `other_desc` varchar(500) DEFAULT NULL COMMENT ''其他维修说明'' AFTER `repair_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_other_desc FROM @sql_work_order_fault_other_desc;
EXECUTE stmt_work_order_fault_other_desc;
DEALLOCATE PREPARE stmt_work_order_fault_other_desc;

SET @add_work_order_fault_part_name = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_name'
);
SET @sql_work_order_fault_part_name = IF(
  @add_work_order_fault_part_name = 0,
  'ALTER TABLE `work_order_fault` ADD COLUMN `part_name` varchar(500) DEFAULT NULL COMMENT ''配件名称'' AFTER `other_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_part_name FROM @sql_work_order_fault_part_name;
EXECUTE stmt_work_order_fault_part_name;
DEALLOCATE PREPARE stmt_work_order_fault_part_name;

SET @add_work_order_fault_part_qty = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_qty'
);
SET @sql_work_order_fault_part_qty = IF(
  @add_work_order_fault_part_qty = 0,
  'ALTER TABLE `work_order_fault` ADD COLUMN `part_qty` int unsigned DEFAULT NULL COMMENT ''配件数量'' AFTER `part_name`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_part_qty FROM @sql_work_order_fault_part_qty;
EXECUTE stmt_work_order_fault_part_qty;
DEALLOCATE PREPARE stmt_work_order_fault_part_qty;

SET @has_work_order_fault_part_desc = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_desc'
);
SET @has_work_order_fault_part_name = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_name'
);
SET @sql_work_order_fault_backfill_part_name = IF(
  @has_work_order_fault_part_desc = 1 AND @has_work_order_fault_part_name = 1,
  'UPDATE `work_order_fault` SET `part_name` = `part_desc` WHERE (`part_name` IS NULL OR `part_name` = '''') AND `part_desc` IS NOT NULL AND `part_desc` <> ''''',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_backfill_part_name FROM @sql_work_order_fault_backfill_part_name;
EXECUTE stmt_work_order_fault_backfill_part_name;
DEALLOCATE PREPARE stmt_work_order_fault_backfill_part_name;

SET @drop_work_order_fault_part_desc = IF(
  @has_work_order_fault_part_desc = 1,
  'ALTER TABLE `work_order_fault` DROP COLUMN `part_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_part_desc FROM @drop_work_order_fault_part_desc;
EXECUTE stmt_work_order_fault_part_desc;
DEALLOCATE PREPARE stmt_work_order_fault_part_desc;

SET @has_work_order_fault_image_urls = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'image_urls'
);
SET @drop_work_order_fault_image_urls = IF(
  @has_work_order_fault_image_urls = 1,
  'ALTER TABLE `work_order_fault` DROP COLUMN `image_urls`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_image_urls FROM @drop_work_order_fault_image_urls;
EXECUTE stmt_work_order_fault_image_urls;
DEALLOCATE PREPARE stmt_work_order_fault_image_urls;
