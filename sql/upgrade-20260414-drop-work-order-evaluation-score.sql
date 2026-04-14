-- =============================================
-- 佳士售后系统 - 移除工单评价旧 score 字段
-- 适用场景：历史库仍残留 work_order_evaluation.score
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT 1',
        'ALTER TABLE `work_order_evaluation` DROP COLUMN `score`'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_evaluation'
      AND column_name = 'score'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
