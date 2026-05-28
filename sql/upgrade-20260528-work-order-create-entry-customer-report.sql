-- =============================================
-- 佳士售后服务系统 - 工单建单入口 CUSTOMER_REPORT 补齐脚本
-- 适用场景：
-- 1. 统一 create_entry_type 为全量工单建单语义字段
-- 2. 历史空值全部回填为 CUSTOMER_REPORT
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 保证工单主表存在建单入口字段
-- -------------------------------------------
SET @work_order_create_entry_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'create_entry_type'
);

SET @work_order_create_entry_add_sql = IF(
  @work_order_create_entry_column_exists = 0,
  'ALTER TABLE `work_order` ADD COLUMN `create_entry_type` varchar(32) DEFAULT NULL COMMENT ''建单入口类型'' AFTER `create_company_id`',
  'SELECT 1'
);
PREPARE stmt_add_work_order_create_entry_type FROM @work_order_create_entry_add_sql;
EXECUTE stmt_add_work_order_create_entry_type;
DEALLOCATE PREPARE stmt_add_work_order_create_entry_type;

-- -------------------------------------------
-- 2. 历史空值全部按客户报修回填
-- -------------------------------------------
UPDATE `work_order`
SET `create_entry_type` = 'CUSTOMER_REPORT'
WHERE `create_entry_type` IS NULL;
