-- =============================================
-- 佳士售后系统 - C端附近网点历史报修排序索引
-- 适用场景：已执行过工单核心建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @work_order_customer_idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND INDEX_NAME = 'idx_customer_id'
);
SET @work_order_customer_idx_sql = IF(
  @work_order_customer_idx_exists = 0,
  'ALTER TABLE `work_order` ADD KEY `idx_customer_id` (`customer_id`, `id`)',
  'SELECT 1'
);
PREPARE stmt FROM @work_order_customer_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @work_order_flow_history_idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_flow'
    AND INDEX_NAME = 'idx_action_to_company_order_time'
);
SET @work_order_flow_history_idx_sql = IF(
  @work_order_flow_history_idx_exists = 0,
  'ALTER TABLE `work_order_flow` ADD KEY `idx_action_to_company_order_time` (`action_type`, `to_company_id`, `work_order_id`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @work_order_flow_history_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
