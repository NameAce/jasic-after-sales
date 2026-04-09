-- =============================================
-- 佳士售后系统 - 工单品牌类型重构增量脚本
-- 适用场景：已执行过历史工单建表/升级脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order` ADD COLUMN `brand_type` varchar(16) DEFAULT NULL COMMENT ''品牌类型'' AFTER `machine_no`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'brand_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE `work_order` DROP COLUMN `report_biz_type`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'report_biz_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
