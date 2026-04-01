-- =============================================
-- 佳士售后系统 - 条码档案菜单/权限增量脚本
-- 适用场景：已有平台菜单与角色数据，需要补条码档案管理入口
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @platform_system_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'system'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码档案', @platform_system_root_id, 'C', 'machineBarcode', 'system/machineBarcode/index', NULL, 'el-icon-postcard', 7, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'machineBarcode'
  );

SET @machine_barcode_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'machineBarcode'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码查询', @machine_barcode_menu_id, 'F', NULL, NULL, 'system:machineBarcode:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @machine_barcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @machine_barcode_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:machineBarcode:list'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码新增', @machine_barcode_menu_id, 'F', NULL, NULL, 'system:machineBarcode:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @machine_barcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @machine_barcode_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:machineBarcode:add'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码修改', @machine_barcode_menu_id, 'F', NULL, NULL, 'system:machineBarcode:update', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @machine_barcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @machine_barcode_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:machineBarcode:update'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码删除', @machine_barcode_menu_id, 'F', NULL, NULL, 'system:machineBarcode:remove', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @machine_barcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @machine_barcode_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:machineBarcode:remove'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '条码导入', @machine_barcode_menu_id, 'F', NULL, NULL, 'system:machineBarcode:import', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @machine_barcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @machine_barcode_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:machineBarcode:import'
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
  AND m.`subject_type` = 'PLATFORM'
  AND (
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'machineBarcode')
    OR m.`perms` IN (
      'system:machineBarcode:list',
      'system:machineBarcode:add',
      'system:machineBarcode:update',
      'system:machineBarcode:remove',
      'system:machineBarcode:import'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = @platform_role_id
      AND rm.`menu_id` = m.`id`
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE m.`subject_type` = 'PLATFORM'
  AND (
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'machineBarcode')
    OR m.`perms` IN (
      'system:machineBarcode:list',
      'system:machineBarcode:add',
      'system:machineBarcode:update',
      'system:machineBarcode:remove',
      'system:machineBarcode:import'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
