-- =============================================
-- 佳士售后系统 - 公司销售组织字段增量脚本
-- 适用场景：老环境补齐 sys_company.sales_org 字段与唯一索引
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

ALTER TABLE `sys_company`
  ADD COLUMN IF NOT EXISTS `sales_org` varchar(64) DEFAULT NULL COMMENT '销售组织' AFTER `source_type`;

SET @add_sys_company_sales_org_uk_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `sys_company` ADD UNIQUE KEY `uk_company_sales_org` (`sales_org`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_company'
      AND INDEX_NAME = 'uk_company_sales_org'
);
PREPARE stmt FROM @add_sys_company_sales_org_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
