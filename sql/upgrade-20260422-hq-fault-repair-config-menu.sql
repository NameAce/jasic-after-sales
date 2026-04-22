-- =============================================
-- 佳士售后系统 - 总部故障与维修配置菜单增量脚本
-- 适用场景：已执行过基础建表与既有菜单脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @hq_org_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = 0
    AND `path` = 'org'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '企业管理', 0, 'M', 'org', NULL, NULL, 'el-icon-office-building', 20, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_org_root_id IS NULL;

SET @hq_org_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = 0
    AND `path` = 'org'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '故障与维修配置', @hq_org_root_id, 'C', 'faultRepairConfig', 'system/faultRepairConfig/index', NULL, 'el-icon-setting', 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_org_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'HQ'
      AND `parent_id` = @hq_org_root_id
      AND `path` = 'faultRepairConfig'
  );

SET @hq_fault_repair_config_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = @hq_org_root_id
    AND `path` = 'faultRepairConfig'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '配置查询', @hq_fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hq_fault_repair_config_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:faultRepairConfig:list'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '配置新增', @hq_fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hq_fault_repair_config_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:faultRepairConfig:add'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '配置修改', @hq_fault_repair_config_menu_id, 'F', NULL, NULL, 'system:faultRepairConfig:update', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_fault_repair_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hq_fault_repair_config_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:faultRepairConfig:update'
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT tc.`type_code`, m.`id`, NOW(), NOW()
FROM `sys_company_type` tc
JOIN `sys_menu` m ON m.`subject_type` = 'HQ'
WHERE tc.`subject_type` = 'HQ'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'org')
    OR (m.`parent_id` = @hq_org_root_id AND m.`path` = 'faultRepairConfig')
    OR m.`perms` IN (
      'system:faultRepairConfig:list',
      'system:faultRepairConfig:add',
      'system:faultRepairConfig:update'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = tc.`type_code`
      AND tcm.`menu_id` = m.`id`
  );
