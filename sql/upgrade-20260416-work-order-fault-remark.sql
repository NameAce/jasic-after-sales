-- 工单维修故障补足“其他故障说明”字段
SET @add_work_order_fault_fault_remark = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'fault_remark'
);

SET @sql_work_order_fault_fault_remark = IF(
  @add_work_order_fault_fault_remark = 0,
  'ALTER TABLE `work_order_fault` ADD COLUMN `fault_remark` varchar(500) DEFAULT NULL COMMENT ''其他故障说明'' AFTER `fault_desc`',
  'SELECT 1'
);

PREPARE stmt_work_order_fault_fault_remark FROM @sql_work_order_fault_fault_remark;
EXECUTE stmt_work_order_fault_fault_remark;
DEALLOCATE PREPARE stmt_work_order_fault_fault_remark;
