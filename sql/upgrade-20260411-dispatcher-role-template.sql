-- =============================================
-- 佳士售后系统 - 一级/二级派单员模板与接单能力补齐脚本
-- 适用场景：已执行过基础组织权限与工单菜单脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 一级/二级新增派单员角色模板
-- -------------------------------------------
INSERT INTO `sys_role_template`
(`type_code`, `role_name`, `role_key`, `data_scope`, `is_admin`, `order_num`, `remark`, `create_time`, `update_time`)
SELECT 'FIRST', '派单员', 'dispatcher', 'COMPANY', 0, 2, '一级网点工单派单角色', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_role_template`
  WHERE `type_code` = 'FIRST'
    AND `role_key` = 'dispatcher'
);

INSERT INTO `sys_role_template`
(`type_code`, `role_name`, `role_key`, `data_scope`, `is_admin`, `order_num`, `remark`, `create_time`, `update_time`)
SELECT 'SECOND', '派单员', 'dispatcher', 'COMPANY', 0, 2, '二级网点工单派单角色', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_role_template`
  WHERE `type_code` = 'SECOND'
    AND `role_key` = 'dispatcher'
);

-- -------------------------------------------
-- 2. 派单员模板菜单：查询 / 派单 / 转单
-- -------------------------------------------
INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE rt.`type_code` IN ('FIRST', 'SECOND')
  AND rt.`role_key` = 'dispatcher'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`menu_type` = 'C' AND m.`path` = 'workOrder')
    OR m.`perms` IN ('workorder:list', 'workorder:assign', 'workorder:transfer')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` existed
    WHERE existed.`template_id` = rt.`id`
      AND existed.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 3. 一级/二级管理员补齐接单能力
-- 说明：小公司只有老板/管理员时，允许管理员被派单后执行接单
-- -------------------------------------------
INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m
  ON m.`subject_type` = tc.`subject_type`
 AND m.`perms` = 'workorder:accept'
WHERE rt.`type_code` IN ('FIRST', 'SECOND')
  AND rt.`role_key` IN ('js-admin', 'admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` existed
    WHERE existed.`template_id` = rt.`id`
      AND existed.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 4. 给已有一级/二级公司补建派单员系统角色
-- -------------------------------------------
INSERT INTO `sys_role`
(`company_id`, `role_name`, `role_key`, `data_scope`, `role_type`, `is_system`, `status`, `order_num`, `remark`, `create_time`, `update_time`)
SELECT c.`id`,
       rt.`role_name`,
       rt.`role_key`,
       rt.`data_scope`,
       2,
       1,
       1,
       COALESCE(rt.`order_num`, 0),
       rt.`remark`,
       NOW(),
       NOW()
FROM `sys_company` c
JOIN `sys_role_template` rt
  ON rt.`type_code` = c.`type_code`
 AND rt.`role_key` = 'dispatcher'
WHERE c.`type_code` IN ('FIRST', 'SECOND')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role` existed
    WHERE existed.`company_id` = c.`id`
      AND existed.`role_key` = rt.`role_key`
  );

-- -------------------------------------------
-- 5. 已有系统角色同步模板菜单
-- -------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`id`, rtm.`menu_id`, NOW(), NOW()
FROM `sys_role` r
JOIN `sys_company` c ON c.`id` = r.`company_id`
JOIN `sys_role_template` rt
  ON rt.`type_code` = c.`type_code`
 AND rt.`role_key` = r.`role_key`
JOIN `sys_role_template_menu` rtm ON rtm.`template_id` = rt.`id`
WHERE c.`type_code` IN ('FIRST', 'SECOND')
  AND r.`is_system` = 1
  AND r.`role_key` = 'dispatcher'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed
    WHERE existed.`role_id` = r.`id`
      AND existed.`menu_id` = rtm.`menu_id`
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`id`, accept_menu.`id`, NOW(), NOW()
FROM `sys_role` r
JOIN `sys_company` c ON c.`id` = r.`company_id`
JOIN `sys_company_type` tc ON tc.`type_code` = c.`type_code`
JOIN `sys_menu` accept_menu
  ON accept_menu.`subject_type` = tc.`subject_type`
 AND accept_menu.`perms` = 'workorder:accept'
WHERE c.`type_code` IN ('FIRST', 'SECOND')
  AND r.`is_system` = 1
  AND r.`role_key` IN ('js-admin', 'admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed
    WHERE existed.`role_id` = r.`id`
      AND existed.`menu_id` = accept_menu.`id`
  );
