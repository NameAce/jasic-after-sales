-- =============================================
-- 佳士售后系统 - 条码档案同步重构增量脚本
-- 适用场景：已存在条码档案管理与工单数据
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `crm_company_mapping` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cust_id`       varchar(64)      DEFAULT NULL            COMMENT 'CRM公司ID',
  `sales_org`     varchar(64)      DEFAULT NULL            COMMENT '销售组织',
  `hq_company_id` bigint unsigned  DEFAULT NULL            COMMENT '归属总部ID',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_company_mapping_cust` (`cust_id`),
  UNIQUE KEY `uk_crm_company_mapping_sales_org` (`sales_org`),
  KEY `idx_crm_company_mapping_hq` (`hq_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM公司映射表';

INSERT INTO `crm_company_mapping`
(`cust_id`, `sales_org`, `hq_company_id`, `status`, `remark`, `create_time`, `update_time`)
VALUES
('105112', '1001', 2, 1, '条码同步初始化映射', NOW(), NOW()),
('105113', '2001', NULL, 1, '条码同步初始化映射', NOW(), NOW()),
('107618', '8001', NULL, 1, '条码同步初始化映射', NOW(), NOW()),
('107622', '3001', NULL, 1, '条码同步初始化映射', NOW(), NOW())
ON DUPLICATE KEY UPDATE
`sales_org` = VALUES(`sales_org`),
`hq_company_id` = VALUES(`hq_company_id`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`update_time` = VALUES(`update_time`);

ALTER TABLE `machine_barcode`
  MODIFY COLUMN `barcode` varchar(100) NOT NULL COMMENT '机器条码',
  MODIFY COLUMN `product_model` varchar(100) DEFAULT NULL COMMENT '产品型号',
  MODIFY COLUMN `machine_no` varchar(100) DEFAULT NULL COMMENT '机器小号',
  MODIFY COLUMN `scan_date` datetime DEFAULT NULL COMMENT '条码扫描时间';

SET @add_machine_barcode_deliver_number_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `machine_barcode` ADD COLUMN `deliver_number` varchar(50) DEFAULT NULL COMMENT ''发货单号'' AFTER `barcode`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'machine_barcode'
      AND column_name = 'deliver_number'
);
PREPARE stmt FROM @add_machine_barcode_deliver_number_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_machine_barcode_product_trumpet_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT 1',
        'ALTER TABLE `machine_barcode` DROP COLUMN `product_trumpet`'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'machine_barcode'
      AND column_name = 'product_trumpet'
);
PREPARE stmt FROM @drop_machine_barcode_product_trumpet_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `work_order`
  MODIFY COLUMN `barcode` varchar(100) DEFAULT NULL COMMENT '机器条码',
  MODIFY COLUMN `product_model` varchar(100) DEFAULT NULL COMMENT '机器型号',
  MODIFY COLUMN `machine_no` varchar(100) DEFAULT NULL COMMENT '机器小号';

DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE m.`perms` IN (
  'system:machineBarcode:add',
  'system:machineBarcode:update',
  'system:machineBarcode:remove',
  'system:machineBarcode:import'
);

DELETE tcm
FROM `sys_type_code_menu` tcm
JOIN `sys_menu` m ON m.`id` = tcm.`menu_id`
WHERE m.`perms` IN (
  'system:machineBarcode:add',
  'system:machineBarcode:update',
  'system:machineBarcode:remove',
  'system:machineBarcode:import'
);

DELETE FROM `sys_menu`
WHERE `perms` IN (
  'system:machineBarcode:add',
  'system:machineBarcode:update',
  'system:machineBarcode:remove',
  'system:machineBarcode:import'
);

SET @machine_barcode_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `menu_type` = 'C'
    AND `path` = 'machineBarcode'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码同步', @machine_barcode_menu_id, 'F', NULL, NULL, 'system:machineBarcode:sync', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @machine_barcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @machine_barcode_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:machineBarcode:sync'
  );

SET @platform_role_id = (
  SELECT `id`
  FROM `sys_role`
  WHERE `company_id` = 1
    AND `role_key` = 'platform_admin'
  LIMIT 1
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT @platform_role_id, m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE @platform_role_id IS NOT NULL
  AND m.`perms` = 'system:machineBarcode:sync'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = @platform_role_id
      AND rm.`menu_id` = m.`id`
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE m.`perms` = 'system:machineBarcode:sync'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
