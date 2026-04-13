-- =============================================
-- 佳士售后系统 - C 端故障联动与条码主数据增量脚本
-- 执行日期：2026-04-02
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. machine_barcode 补充商品名称 / 机器小号
-- -------------------------------------------
SET @add_machine_barcode_product_name = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'machine_barcode'
    AND COLUMN_NAME = 'product_name'
);
SET @sql_machine_barcode_product_name = IF(
  @add_machine_barcode_product_name = 0,
  'ALTER TABLE `machine_barcode` ADD COLUMN `product_name` varchar(128) DEFAULT NULL COMMENT ''商品名称'' AFTER `product_code`',
  'SELECT 1'
);
PREPARE stmt_machine_barcode_product_name FROM @sql_machine_barcode_product_name;
EXECUTE stmt_machine_barcode_product_name;
DEALLOCATE PREPARE stmt_machine_barcode_product_name;

SET @add_machine_barcode_machine_no = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'machine_barcode'
    AND COLUMN_NAME = 'machine_no'
);
SET @sql_machine_barcode_machine_no = IF(
  @add_machine_barcode_machine_no = 0,
  'ALTER TABLE `machine_barcode` ADD COLUMN `machine_no` varchar(100) DEFAULT NULL COMMENT ''机器小号'' AFTER `product_model`',
  'SELECT 1'
);
PREPARE stmt_machine_barcode_machine_no FROM @sql_machine_barcode_machine_no;
EXECUTE stmt_machine_barcode_machine_no;
DEALLOCATE PREPARE stmt_machine_barcode_machine_no;

-- -------------------------------------------
-- 2. work_order 补充客户故障备注和产品快照
-- -------------------------------------------
SET @add_work_order_product_name = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'product_name'
);
SET @sql_work_order_product_name = IF(
  @add_work_order_product_name = 0,
  'ALTER TABLE `work_order` ADD COLUMN `product_name` varchar(128) DEFAULT NULL COMMENT ''商品名称'' AFTER `product_code`',
  'SELECT 1'
);
PREPARE stmt_work_order_product_name FROM @sql_work_order_product_name;
EXECUTE stmt_work_order_product_name;
DEALLOCATE PREPARE stmt_work_order_product_name;

SET @add_work_order_machine_no = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'machine_no'
);
SET @sql_work_order_machine_no = IF(
  @add_work_order_machine_no = 0,
  'ALTER TABLE `work_order` ADD COLUMN `machine_no` varchar(100) DEFAULT NULL COMMENT ''机器小号'' AFTER `product_model`',
  'SELECT 1'
);
PREPARE stmt_work_order_machine_no FROM @sql_work_order_machine_no;
EXECUTE stmt_work_order_machine_no;
DEALLOCATE PREPARE stmt_work_order_machine_no;

SET @add_work_order_fault_remark = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'fault_remark'
);
SET @sql_work_order_fault_remark = IF(
  @add_work_order_fault_remark = 0,
  'ALTER TABLE `work_order` ADD COLUMN `fault_remark` varchar(500) DEFAULT NULL COMMENT ''客户故障备注'' AFTER `fault_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_remark FROM @sql_work_order_fault_remark;
EXECUTE stmt_work_order_fault_remark;
DEALLOCATE PREPARE stmt_work_order_fault_remark;
