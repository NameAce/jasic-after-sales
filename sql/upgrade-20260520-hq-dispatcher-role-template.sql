-- =============================================
-- 佳士售后系统 - 总部派单员角色模板补齐脚本
-- 适用场景：
-- 1. 总部账号需要处理转单回总部后的待派单工单
-- 2. 现有脚本只补齐了一、二级网点 dispatcher，未补齐总部 dispatcher
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 给全部总部公司类型补齐派单员模板
-- 口径：总部派单员数据范围为 ALL，可查看并处理当前总部下的待派单工单
-- -------------------------------------------
INSERT INTO `sys_role_template`
(`type_code`, `role_name`, `role_key`, `data_scope`, `is_admin`, `order_num`, `remark`, `create_time`, `update_time`)
SELECT tc.`type_code`, '派单员', 'dispatcher', 'ALL', 0, 2, '总部工单派单角色', NOW(), NOW()
FROM `sys_company_type` tc
WHERE tc.`subject_type` = 'HQ'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template` existed
    WHERE existed.`type_code` = tc.`type_code`
      AND existed.`role_key` = 'dispatcher'
  );

-- -------------------------------------------
-- 2. 给总部派单员模板补齐菜单
-- 口径与网点派单员一致：查询 / 派单 / 转单
-- -------------------------------------------
INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE tc.`subject_type` = 'HQ'
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
-- 3. 给已有总部公司补建 dispatcher 系统角色
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
JOIN `sys_company_type` tc ON tc.`type_code` = c.`type_code`
JOIN `sys_role_template` rt
  ON rt.`type_code` = c.`type_code`
 AND rt.`role_key` = 'dispatcher'
WHERE tc.`subject_type` = 'HQ'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role` existed
    WHERE existed.`company_id` = c.`id`
      AND existed.`role_key` = rt.`role_key`
  );

-- -------------------------------------------
-- 4. 同步总部 dispatcher 模板菜单到已有系统角色
-- -------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`id`, rtm.`menu_id`, NOW(), NOW()
FROM `sys_role` r
JOIN `sys_company` c ON c.`id` = r.`company_id`
JOIN `sys_company_type` tc ON tc.`type_code` = c.`type_code`
JOIN `sys_role_template` rt
  ON rt.`type_code` = c.`type_code`
 AND rt.`role_key` = r.`role_key`
JOIN `sys_role_template_menu` rtm ON rtm.`template_id` = rt.`id`
WHERE tc.`subject_type` = 'HQ'
  AND r.`is_system` = 1
  AND r.`role_key` = 'dispatcher'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed
    WHERE existed.`role_id` = r.`id`
      AND existed.`menu_id` = rtm.`menu_id`
  );
