-- =============================================
-- 佳士售后系统 - 公司主数据与 CRM 快照收口升级脚本
-- 适用场景：已执行过基础建表脚本的库
-- 可重复执行：是
-- 兼容版本：MySQL 5.7 / 8.x
-- =============================================

SET NAMES utf8mb4;

SET @sys_company_short_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `company_short_name` varchar(128) DEFAULT NULL COMMENT ''公司简称'' AFTER `company_name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'company_short_name'
);
PREPARE stmt FROM @sys_company_short_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_company_province_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `province_name` varchar(64) DEFAULT NULL COMMENT ''省份'' AFTER `address`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'province_name'
);
PREPARE stmt FROM @sys_company_province_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_company_city_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `city_name` varchar(64) DEFAULT NULL COMMENT ''城市'' AFTER `province_name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'city_name'
);
PREPARE stmt FROM @sys_company_city_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_company_district_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `district_name` varchar(64) DEFAULT NULL COMMENT ''区县'' AFTER `city_name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'district_name'
);
PREPARE stmt FROM @sys_company_district_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_company_service_phone_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `service_phone` varchar(32) DEFAULT NULL COMMENT ''客服电话'' AFTER `latitude`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'service_phone'
);
PREPARE stmt FROM @sys_company_service_phone_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_company_source_type_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `source_type` varchar(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''来源类型'' AFTER `service_phone`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'source_type'
);
PREPARE stmt FROM @sys_company_source_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_company_sales_org_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD COLUMN `sales_org` varchar(64) DEFAULT NULL COMMENT ''销售组织'' AFTER `source_type`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND column_name = 'sales_org'
);
PREPARE stmt FROM @sys_company_sales_org_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_sys_company_sales_org_uk = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_company` ADD UNIQUE KEY `uk_company_sales_org` (`sales_org`)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_company'
    AND index_name = 'uk_company_sales_org'
);
PREPARE stmt FROM @add_sys_company_sales_org_uk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_company`
SET `source_type` = 'CRM'
WHERE `source_type` IS NULL OR TRIM(`source_type`) = '';

SET @crm_snapshot_sap_company_code_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD COLUMN `sap_company_code` varchar(64) DEFAULT NULL COMMENT ''SAP公司编码'' AFTER `company_address`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND column_name = 'sap_company_code'
);
PREPARE stmt FROM @crm_snapshot_sap_company_code_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @crm_snapshot_cust_rage_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD COLUMN `cust_rage` int DEFAULT NULL COMMENT ''客户范围'' AFTER `sap_company_code`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND column_name = 'cust_rage'
);
PREPARE stmt FROM @crm_snapshot_cust_rage_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @crm_snapshot_company_short_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD COLUMN `company_short_name` varchar(128) DEFAULT NULL COMMENT ''公司简称'' AFTER `cust_rage`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND column_name = 'company_short_name'
);
PREPARE stmt FROM @crm_snapshot_company_short_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @crm_snapshot_province_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD COLUMN `province_name` varchar(64) DEFAULT NULL COMMENT ''省份'' AFTER `company_short_name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND column_name = 'province_name'
);
PREPARE stmt FROM @crm_snapshot_province_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @crm_snapshot_city_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD COLUMN `city_name` varchar(64) DEFAULT NULL COMMENT ''城市'' AFTER `province_name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND column_name = 'city_name'
);
PREPARE stmt FROM @crm_snapshot_city_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @crm_snapshot_district_name_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD COLUMN `district_name` varchar(64) DEFAULT NULL COMMENT ''区县'' AFTER `city_name`',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND column_name = 'district_name'
);
PREPARE stmt FROM @crm_snapshot_district_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_crm_snapshot_sap_code_idx = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `crm_biz_company_snapshot` ADD KEY `idx_crm_biz_company_snapshot_sap_code` (`sap_company_code`)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_biz_company_snapshot'
    AND index_name = 'idx_crm_biz_company_snapshot_sap_code'
);
PREPARE stmt FROM @add_crm_snapshot_sap_code_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_company` c
JOIN `crm_biz_company_snapshot` s ON s.`sap_company_code` = c.`company_code`
SET c.`company_short_name` = COALESCE(NULLIF(TRIM(s.`company_short_name`), ''), c.`company_short_name`),
    c.`province_name` = COALESCE(NULLIF(TRIM(s.`province_name`), ''), c.`province_name`),
    c.`city_name` = COALESCE(NULLIF(TRIM(s.`city_name`), ''), c.`city_name`),
    c.`district_name` = COALESCE(NULLIF(TRIM(s.`district_name`), ''), c.`district_name`),
    c.`update_time` = NOW()
WHERE s.`sap_company_code` IS NOT NULL
  AND TRIM(s.`sap_company_code`) <> '';
