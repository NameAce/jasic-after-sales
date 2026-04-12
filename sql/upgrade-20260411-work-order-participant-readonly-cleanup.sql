-- =============================================
-- 佳士售后系统 - 工单参与方只读字段清理脚本
-- 适用场景：已执行过工单核心建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @participant_readonly_idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_participant'
    AND INDEX_NAME = 'idx_company_readonly'
);
SET @drop_participant_readonly_idx_sql = IF(
  @participant_readonly_idx_exists > 0,
  'ALTER TABLE `work_order_participant` DROP INDEX `idx_company_readonly`',
  'SELECT 1'
);
PREPARE stmt FROM @drop_participant_readonly_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @participant_readonly_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_participant'
    AND COLUMN_NAME = 'is_readonly'
);
SET @drop_participant_readonly_column_sql = IF(
  @participant_readonly_column_exists > 0,
  'ALTER TABLE `work_order_participant` DROP COLUMN `is_readonly`',
  'SELECT 1'
);
PREPARE stmt FROM @drop_participant_readonly_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
