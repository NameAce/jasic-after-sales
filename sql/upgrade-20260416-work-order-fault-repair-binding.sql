-- =============================================
-- 工单绑定故障与维修配置增量脚本
-- 执行日期：2026-04-16
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 工单主表补充绑定配置ID
-- -------------------------------------------
SET @has_work_order_fault_repair_config_id = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'fault_repair_config_id'
);
SET @sql_add_work_order_fault_repair_config_id = IF(
  @has_work_order_fault_repair_config_id = 0,
  'ALTER TABLE `work_order` ADD COLUMN `fault_repair_config_id` bigint unsigned DEFAULT NULL COMMENT ''绑定的故障与维修配置ID'' AFTER `hq_company_id`',
  'SELECT 1'
);
PREPARE stmt_add_work_order_fault_repair_config_id FROM @sql_add_work_order_fault_repair_config_id;
EXECUTE stmt_add_work_order_fault_repair_config_id;
DEALLOCATE PREPARE stmt_add_work_order_fault_repair_config_id;

SET @has_idx_work_order_fault_repair_config = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND INDEX_NAME = 'idx_fault_repair_config'
);
SET @sql_add_idx_work_order_fault_repair_config = IF(
  @has_idx_work_order_fault_repair_config = 0,
  'ALTER TABLE `work_order` ADD KEY `idx_fault_repair_config` (`fault_repair_config_id`)',
  'SELECT 1'
);
PREPARE stmt_add_idx_work_order_fault_repair_config FROM @sql_add_idx_work_order_fault_repair_config;
EXECUTE stmt_add_idx_work_order_fault_repair_config;
DEALLOCATE PREPARE stmt_add_idx_work_order_fault_repair_config;

-- -------------------------------------------
-- 2. 故障与维修配置改为轻量版本化，去掉物理唯一键
-- -------------------------------------------
SET @has_uk_fault_repair_config_product = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'fault_repair_config'
    AND INDEX_NAME = 'uk_fault_repair_config_product'
);
SET @sql_drop_uk_fault_repair_config_product = IF(
  @has_uk_fault_repair_config_product = 1,
  'ALTER TABLE `fault_repair_config` DROP INDEX `uk_fault_repair_config_product`',
  'SELECT 1'
);
PREPARE stmt_drop_uk_fault_repair_config_product FROM @sql_drop_uk_fault_repair_config_product;
EXECUTE stmt_drop_uk_fault_repair_config_product;
DEALLOCATE PREPARE stmt_drop_uk_fault_repair_config_product;

SET @has_idx_fault_repair_config_product = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'fault_repair_config'
    AND INDEX_NAME = 'idx_fault_repair_config_product'
);
SET @sql_add_idx_fault_repair_config_product = IF(
  @has_idx_fault_repair_config_product = 0,
  'ALTER TABLE `fault_repair_config` ADD KEY `idx_fault_repair_config_product` (`company_id`, `product_code`, `product_model`)',
  'SELECT 1'
);
PREPARE stmt_add_idx_fault_repair_config_product FROM @sql_add_idx_fault_repair_config_product;
EXECUTE stmt_add_idx_fault_repair_config_product;
DEALLOCATE PREPARE stmt_add_idx_fault_repair_config_product;

-- -------------------------------------------
-- 3. 老工单回填 fault_repair_config_id
-- -------------------------------------------
UPDATE `work_order` w
SET w.`fault_repair_config_id` = (
  SELECT c.`id`
  FROM `fault_repair_config` c
  WHERE c.`status` = 1
    AND c.`company_id` = w.`hq_company_id`
    AND (
      (c.`product_code` = w.`product_code`)
      OR (c.`product_code` IS NULL AND w.`product_code` IS NULL)
    )
    AND (
      (c.`product_model` = w.`product_model`)
      OR (c.`product_model` IS NULL AND w.`product_model` IS NULL)
    )
  ORDER BY c.`update_time` DESC, c.`id` DESC
  LIMIT 1
)
WHERE w.`fault_repair_config_id` IS NULL
  AND w.`brand_type` = 'JASIC'
  AND w.`product_model` IS NOT NULL;

-- -------------------------------------------
-- 4. 清理已废弃的故障与维修配置删除权限
-- -------------------------------------------
DELETE rm
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE m.`perms` = 'system:faultRepairConfig:remove';

DELETE tcm
FROM `sys_type_code_menu` tcm
INNER JOIN `sys_menu` m ON m.`id` = tcm.`menu_id`
WHERE m.`perms` = 'system:faultRepairConfig:remove';

DELETE FROM `sys_menu`
WHERE `perms` = 'system:faultRepairConfig:remove';
