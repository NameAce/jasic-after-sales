-- 工单维修/复检重构：补齐维修登记阶段字段，故障快照改为文本字段，兼容历史数据

SET @add_work_order_repair_register_stage = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_repair'
    AND COLUMN_NAME = 'register_stage'
);
SET @sql_work_order_repair_register_stage = IF(
  @add_work_order_repair_register_stage = 0,
  'ALTER TABLE `work_order_repair` ADD COLUMN `register_stage` varchar(32) NOT NULL DEFAULT ''REPAIR'' COMMENT ''登记阶段（REPAIR=维修登记，RECHECK=复检登记）'' AFTER `repair_user_id`',
  'SELECT 1'
);
PREPARE stmt_work_order_repair_register_stage FROM @sql_work_order_repair_register_stage;
EXECUTE stmt_work_order_repair_register_stage;
DEALLOCATE PREPARE stmt_work_order_repair_register_stage;

UPDATE `work_order_repair`
SET `register_stage` = 'REPAIR'
WHERE `register_stage` IS NULL OR `register_stage` = '';

SET @add_work_order_fault_other_desc = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'other_desc'
);
SET @sql_work_order_fault_other_desc = IF(
  @add_work_order_fault_other_desc = 0,
  'ALTER TABLE `work_order_fault` ADD COLUMN `other_desc` varchar(500) DEFAULT NULL COMMENT ''其他维修说明'' AFTER `repair_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_other_desc FROM @sql_work_order_fault_other_desc;
EXECUTE stmt_work_order_fault_other_desc;
DEALLOCATE PREPARE stmt_work_order_fault_other_desc;

CREATE TABLE IF NOT EXISTS `work_order_fault_part` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `fault_id`      bigint unsigned  NOT NULL                COMMENT '故障点ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '登记公司ID',
  `part_name`     varchar(500)     NOT NULL                COMMENT '配件名称',
  `part_qty`      int unsigned     NOT NULL                COMMENT '配件数量',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '排序号',
  `created_by`    bigint unsigned  NOT NULL                COMMENT '登记人ID',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_part` (`fault_id`, `sort_num`),
  KEY `idx_work_order_fault_part_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_part_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单故障点配件明细表';

SET @has_work_order_fault_part_desc = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_desc'
);

SET @drop_work_order_fault_part_desc = IF(
  @has_work_order_fault_part_desc = 1,
  'ALTER TABLE `work_order_fault` DROP COLUMN `part_desc`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_part_desc FROM @drop_work_order_fault_part_desc;
EXECUTE stmt_work_order_fault_part_desc;
DEALLOCATE PREPARE stmt_work_order_fault_part_desc;

SET @has_work_order_fault_part_name = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_name'
);
SET @drop_work_order_fault_part_name = IF(
  @has_work_order_fault_part_name = 1,
  'ALTER TABLE `work_order_fault` DROP COLUMN `part_name`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_part_name FROM @drop_work_order_fault_part_name;
EXECUTE stmt_work_order_fault_part_name;
DEALLOCATE PREPARE stmt_work_order_fault_part_name;

SET @has_work_order_fault_part_qty = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'part_qty'
);
SET @drop_work_order_fault_part_qty = IF(
  @has_work_order_fault_part_qty = 1,
  'ALTER TABLE `work_order_fault` DROP COLUMN `part_qty`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_part_qty FROM @drop_work_order_fault_part_qty;
EXECUTE stmt_work_order_fault_part_qty;
DEALLOCATE PREPARE stmt_work_order_fault_part_qty;

SET @has_work_order_fault_image_urls = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order_fault'
    AND COLUMN_NAME = 'image_urls'
);
SET @drop_work_order_fault_image_urls = IF(
  @has_work_order_fault_image_urls = 1,
  'ALTER TABLE `work_order_fault` DROP COLUMN `image_urls`',
  'SELECT 1'
);
PREPARE stmt_work_order_fault_image_urls FROM @drop_work_order_fault_image_urls;
EXECUTE stmt_work_order_fault_image_urls;
DEALLOCATE PREPARE stmt_work_order_fault_image_urls;
