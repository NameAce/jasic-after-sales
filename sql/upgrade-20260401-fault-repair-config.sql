-- =============================================
-- 佳士售后系统 - 故障与维修配置增量脚本
-- 执行日期：2026-04-01
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 工单故障点补充其他维修说明字段
-- -------------------------------------------
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

-- -------------------------------------------
-- 2. 新增表：故障与维修配置
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `fault_repair_config` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '归属总部ID',
  `product_code`  varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_model` varchar(64)      DEFAULT NULL            COMMENT '产品型号',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fault_repair_config_product` (`company_id`, `product_code`, `product_model`),
  KEY `idx_fault_repair_config_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置表';

CREATE TABLE IF NOT EXISTS `fault_repair_config_fault` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id`   bigint unsigned NOT NULL                COMMENT '配置ID',
  `fault_desc`  varchar(500)    NOT NULL                COMMENT '故障描述',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '排序号',
  `create_time` datetime        NOT NULL                COMMENT '创建时间',
  `update_time` datetime        NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_fault` (`config_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置故障项表';

CREATE TABLE IF NOT EXISTS `fault_repair_config_option` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fault_id`    bigint unsigned NOT NULL                COMMENT '故障项ID',
  `repair_desc` varchar(500)    NOT NULL                COMMENT '维修说明',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '排序号',
  `create_time` datetime        NOT NULL                COMMENT '创建时间',
  `update_time` datetime        NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_option_fault` (`fault_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置维修项表';

-- -------------------------------------------
-- 3. 平台菜单：故障与维修配置
-- -------------------------------------------
SET @platform_system_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = 0
    AND `path` = 'system'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '故障与维修配置', @platform_system_root_id, 'C', 'faultRepairConfig', 'system/faultRepairConfig/index', NULL, 'el-icon-setting', 8, 1, 1, NOW(), NOW()
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `path` = 'faultRepairConfig'
  );

SET @fault_repair_config_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `path` = 'faultRepairConfig'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置查询', @fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:list', NULL, 1, 1, 1, NOW(), NOW()
WHERE @fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @fault_repair_config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:faultRepairConfig:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置新增', @fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:add', NULL, 2, 1, 1, NOW(), NOW()
WHERE @fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @fault_repair_config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:faultRepairConfig:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置修改', @fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:update', NULL, 3, 1, 1, NOW(), NOW()
WHERE @fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @fault_repair_config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:faultRepairConfig:update');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置删除', @fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:remove', NULL, 4, 1, 1, NOW(), NOW()
WHERE @fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @fault_repair_config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:faultRepairConfig:remove');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE m.`subject_type` = 'PLATFORM'
  AND (
    (m.`menu_type` = 'C' AND m.`path` = 'faultRepairConfig')
    OR m.`perms` IN (
      'system:faultRepairConfig:list',
      'system:faultRepairConfig:add',
      'system:faultRepairConfig:update',
      'system:faultRepairConfig:remove'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = 1
      AND rm.`menu_id` = m.`id`
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE m.`subject_type` = 'PLATFORM'
  AND (
    (m.`menu_type` = 'C' AND m.`path` = 'faultRepairConfig')
    OR m.`perms` IN (
      'system:faultRepairConfig:list',
      'system:faultRepairConfig:add',
      'system:faultRepairConfig:update',
      'system:faultRepairConfig:remove'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
