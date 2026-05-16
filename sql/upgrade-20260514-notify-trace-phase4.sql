-- =============================================
-- Notification phase 4 bootstrap:
-- 1. Seed hidden notify trace menu to carry backend permissions
-- 2. Seed notify trace function permissions
-- 3. Grant permissions to platform_admin by default
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

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知记录', @platform_system_root_id, 'C', 'notifyTrace', 'system/notifyTrace/index', NULL,
       'el-icon-warning-outline', 9, 0, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'notifyTrace'
  );

SET @notify_trace_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'notifyTrace'
  LIMIT 1
);

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知记录查询', @notify_trace_menu_id, 'F', NULL, NULL, 'system:notifyTrace:list', NULL,
       1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_trace_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @notify_trace_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTrace:list'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知记录详情', @notify_trace_menu_id, 'F', NULL, NULL, 'system:notifyTrace:view', NULL,
       2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_trace_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @notify_trace_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTrace:view'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知记录重试', @notify_trace_menu_id, 'F', NULL, NULL, 'system:notifyTrace:retry', NULL,
       3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_trace_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @notify_trace_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTrace:retry'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知记录死信处理', @notify_trace_menu_id, 'F', NULL, NULL, 'system:notifyTrace:dead', NULL,
       4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_trace_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @notify_trace_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTrace:dead'
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyTrace')
    OR m.`perms` IN (
      'system:notifyTrace:list',
      'system:notifyTrace:view',
      'system:notifyTrace:retry',
      'system:notifyTrace:dead'
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyTrace')
    OR m.`perms` IN (
      'system:notifyTrace:list',
      'system:notifyTrace:view',
      'system:notifyTrace:retry',
      'system:notifyTrace:dead'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
