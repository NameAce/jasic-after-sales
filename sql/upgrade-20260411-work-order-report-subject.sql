-- =============================================
-- 佳士售后系统 - 工单报修主体字段补充增量脚本
-- 适用场景：已执行过基础建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

ALTER TABLE `work_order`
  MODIFY COLUMN `customer_id` bigint unsigned DEFAULT NULL COMMENT '客户ID';

SET @report_subject_type_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'report_subject_type'
);
SET @report_subject_type_sql = IF(
  @report_subject_type_column_exists = 0,
  'ALTER TABLE `work_order` ADD COLUMN `report_subject_type` varchar(16) DEFAULT NULL COMMENT ''报修主体类型（CUSTOMER/COMPANY）'' AFTER `customer_mobile`',
  'SELECT 1'
);
PREPARE stmt FROM @report_subject_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @report_company_id_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'report_company_id'
);
SET @report_company_id_sql = IF(
  @report_company_id_column_exists = 0,
  'ALTER TABLE `work_order` ADD COLUMN `report_company_id` bigint unsigned DEFAULT NULL COMMENT ''报修主体公司ID'' AFTER `report_subject_type`',
  'SELECT 1'
);
PREPARE stmt FROM @report_company_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @report_company_idx_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND INDEX_NAME = 'idx_report_company'
);
SET @report_company_idx_sql = IF(
  @report_company_idx_exists = 0,
  'ALTER TABLE `work_order` ADD KEY `idx_report_company` (`report_company_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @report_company_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `work_order`
SET `report_subject_type` = 'COMPANY',
    `report_company_id` = `create_company_id`
WHERE `create_entry_type` IN ('UPSTREAM_FIRST', 'UPSTREAM_HQ')
  AND (
    `report_subject_type` IS NULL
    OR `report_subject_type` <> 'COMPANY'
    OR `report_company_id` IS NULL
  );

UPDATE `work_order`
SET `report_subject_type` = 'CUSTOMER',
    `report_company_id` = NULL
WHERE (`create_entry_type` IS NULL OR `create_entry_type` NOT IN ('UPSTREAM_FIRST', 'UPSTREAM_HQ'))
  AND (
    `report_subject_type` IS NULL
    OR `report_subject_type` <> 'CUSTOMER'
    OR `report_company_id` IS NOT NULL
  );

ALTER TABLE `work_order`
  MODIFY COLUMN `report_subject_type` varchar(16) NOT NULL COMMENT '报修主体类型（CUSTOMER/COMPANY）';
