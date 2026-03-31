-- =============================================
-- 佳士售后服务系统 - 菜单发布权限增量脚本
-- 适用场景：为平台菜单管理新增“菜单发布”按钮及授权
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @platform_menu_manage_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `menu_type` = 'C'
    AND `path` = 'menu'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '菜单发布', @platform_menu_manage_id, 'F', NULL, NULL, 'system:menu:publish', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_menu_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @platform_menu_manage_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:menu:publish'
  );

SET @menu_publish_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `parent_id` = @platform_menu_manage_id
    AND `menu_type` = 'F'
    AND `perms` = 'system:menu:publish'
  LIMIT 1
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`id`, @menu_publish_id, NOW(), NOW()
FROM `sys_role` r
WHERE @menu_publish_id IS NOT NULL
  AND r.`role_key` = 'platform_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`id`
      AND rm.`menu_id` = @menu_publish_id
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', @menu_publish_id, NOW(), NOW()
FROM DUAL
WHERE @menu_publish_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu`
    WHERE `type_code` = 'PLATFORM'
      AND `menu_id` = @menu_publish_id
  );
