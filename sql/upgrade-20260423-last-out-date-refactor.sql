-- =============================================
-- 佳士售后系统 - 最后出库日期与质保状态收口升级脚本
-- 适用场景：已有业务数据的库，禁止重跑 schema.sql / init-data.sql
-- =============================================

SET NAMES utf8mb4;

SET @rename_last_out_date_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'machine_barcode'
        AND COLUMN_NAME = 'dealer_out_date'
    ),
    'ALTER TABLE `machine_barcode` CHANGE COLUMN `dealer_out_date` `last_out_date` datetime DEFAULT NULL COMMENT ''最后出库日期''',
    'SELECT 1'
  )
);
PREPARE rename_last_out_date_stmt FROM @rename_last_out_date_sql;
EXECUTE rename_last_out_date_stmt;
DEALLOCATE PREPARE rename_last_out_date_stmt;

ALTER TABLE `machine_barcode`
  ADD COLUMN IF NOT EXISTS `last_out_date` datetime DEFAULT NULL COMMENT '最后出库日期' AFTER `scan_date`;

ALTER TABLE `machine_barcode`
  DROP COLUMN IF EXISTS `warranty_status`;

ALTER TABLE `work_order`
  ADD COLUMN IF NOT EXISTS `last_out_date` datetime DEFAULT NULL COMMENT '最后出库日期快照' AFTER `service_mode`;
