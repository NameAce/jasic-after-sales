-- 删除工单维修登记表的顶层维修摘要/维修说明/其他说明字段

SET @drop_work_order_repair_summary = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_repair'
    AND COLUMN_NAME = 'repair_summary'
);
SET @sql_work_order_repair_summary = IF(
  @drop_work_order_repair_summary = 1,
  'ALTER TABLE `work_order_repair` DROP COLUMN `repair_summary`',
  'SELECT 1'
);
PREPARE stmt_work_order_repair_summary FROM @sql_work_order_repair_summary;
EXECUTE stmt_work_order_repair_summary;
DEALLOCATE PREPARE stmt_work_order_repair_summary;

SET @drop_work_order_repair_desc = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_repair'
    AND COLUMN_NAME = 'repair_desc'
);
SET @sql_work_order_repair_desc = IF(
  @drop_work_order_repair_desc = 1,
  'ALTER TABLE `work_order_repair` DROP COLUMN `repair_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_repair_desc FROM @sql_work_order_repair_desc;
EXECUTE stmt_work_order_repair_desc;
DEALLOCATE PREPARE stmt_work_order_repair_desc;

SET @drop_work_order_repair_other_desc = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_repair'
    AND COLUMN_NAME = 'other_desc'
);
SET @sql_work_order_repair_other_desc = IF(
  @drop_work_order_repair_other_desc = 1,
  'ALTER TABLE `work_order_repair` DROP COLUMN `other_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_repair_other_desc FROM @sql_work_order_repair_other_desc;
EXECUTE stmt_work_order_repair_other_desc;
DEALLOCATE PREPARE stmt_work_order_repair_other_desc;
