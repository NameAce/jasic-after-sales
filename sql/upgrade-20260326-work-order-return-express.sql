-- =============================================
-- 佳士售后系统 - 工单回寄快递单号增量脚本
-- 适用场景：已执行过工单核心建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order` ADD COLUMN `return_express_no` varchar(64) DEFAULT NULL COMMENT ''回寄快递单号'' AFTER `return_method`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'return_express_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
