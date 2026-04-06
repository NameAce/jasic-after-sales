-- =============================================
-- 佳士售后系统 - 工单评价三维评分增量脚本
-- 适用场景：已执行过工单核心建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order_evaluation` ADD COLUMN `timeliness_score` tinyint unsigned DEFAULT NULL COMMENT ''服务时效评分'' AFTER `company_id`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_evaluation'
      AND column_name = 'timeliness_score'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order_evaluation` ADD COLUMN `quality_score` tinyint unsigned DEFAULT NULL COMMENT ''维修质量评分'' AFTER `timeliness_score`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_evaluation'
      AND column_name = 'quality_score'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order_evaluation` ADD COLUMN `satisfaction_score` tinyint unsigned DEFAULT NULL COMMENT ''服务满意度评分'' AFTER `quality_score`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_evaluation'
      AND column_name = 'satisfaction_score'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
