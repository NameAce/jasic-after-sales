-- =============================================
-- 佳士售后服务系统 - 工单菜单/权限增量脚本
-- 适用场景：已存在基础组织权限数据，新增 HQ / SERVICE 工单菜单与按钮
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. SERVICE 主体菜单
-- -------------------------------------------
INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '售后管理', 0, 'M', 'afterSales', NULL, NULL, 'el-icon-s-order', 10, 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `subject_type` = 'SERVICE'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'afterSales'
);

SET @service_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'SERVICE'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'afterSales'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '维修工单', @service_root_id, 'C', 'workOrder', 'workOrder/index', NULL, 'el-icon-tickets', 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'SERVICE'
      AND `parent_id` = @service_root_id
      AND `menu_type` = 'C'
      AND `path` = 'workOrder'
  );

SET @service_work_order_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'SERVICE'
    AND `parent_id` = @service_root_id
    AND `menu_type` = 'C'
    AND `path` = 'workOrder'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '工单查询', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '工单新增', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '工单派单', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:assign', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:assign');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '维修员接单', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:accept', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:accept');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '工单转单', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:transfer', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:transfer');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '维修报价', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:quote', NULL, 6, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:quote');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '维修登记', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:repair', NULL, 7, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:repair');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '复检登记', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:review', NULL, 8, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:review');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '工单关闭', @service_work_order_menu_id, 'F', NULL, NULL, 'workorder:close', NULL, 9, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:close');

-- -------------------------------------------
-- 2. HQ 主体菜单
-- -------------------------------------------
INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '售后管理', 0, 'M', 'afterSales', NULL, NULL, 'el-icon-s-order', 10, 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'afterSales'
);

SET @hq_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'afterSales'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '维修工单', @hq_root_id, 'C', 'workOrder', 'workOrder/index', NULL, 'el-icon-tickets', 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'HQ'
      AND `parent_id` = @hq_root_id
      AND `menu_type` = 'C'
      AND `path` = 'workOrder'
  );

SET @hq_work_order_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = @hq_root_id
    AND `menu_type` = 'C'
    AND `path` = 'workOrder'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '工单查询', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '工单新增', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '工单派单', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:assign', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:assign');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '维修员接单', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:accept', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:accept');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '工单转单', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:transfer', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:transfer');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '维修报价', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:quote', NULL, 6, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:quote');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '维修登记', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:repair', NULL, 7, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:repair');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '复检登记', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:review', NULL, 8, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:review');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '工单关闭', @hq_work_order_menu_id, 'F', NULL, NULL, 'workorder:close', NULL, 9, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'workorder:close');

-- -------------------------------------------
-- 3. 公司类型菜单上限
-- -------------------------------------------
INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT tc.`type_code`, m.`id`, NOW(), NOW()
FROM `sys_company_type` tc
JOIN `sys_menu` m ON m.`subject_type` = 'SERVICE'
WHERE tc.`subject_type` = 'SERVICE'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`parent_id` = @service_root_id AND m.`path` = 'workOrder')
    OR m.`perms` IN ('workorder:list', 'workorder:add', 'workorder:assign', 'workorder:accept', 'workorder:transfer', 'workorder:quote', 'workorder:repair', 'workorder:review', 'workorder:close')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = tc.`type_code`
      AND tcm.`menu_id` = m.`id`
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT tc.`type_code`, m.`id`, NOW(), NOW()
FROM `sys_company_type` tc
JOIN `sys_menu` m ON m.`subject_type` = 'HQ'
WHERE tc.`subject_type` = 'HQ'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`parent_id` = @hq_root_id AND m.`path` = 'workOrder')
    OR m.`perms` IN ('workorder:list', 'workorder:add', 'workorder:assign', 'workorder:accept', 'workorder:transfer', 'workorder:quote', 'workorder:repair', 'workorder:review', 'workorder:close')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = tc.`type_code`
      AND tcm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 4. 角色模板授权回填
-- 管理员模板：建单/派单/转单/复检/关闭
-- 维修员模板：列表/接单/报价/维修登记
-- -------------------------------------------
INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE rt.`role_key` IN ('js-admin', 'admin')
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`menu_type` = 'C' AND m.`path` = 'workOrder')
    OR m.`perms` IN ('workorder:list', 'workorder:add', 'workorder:assign', 'workorder:transfer', 'workorder:review', 'workorder:close')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` rtm
    WHERE rtm.`template_id` = rt.`id`
      AND rtm.`menu_id` = m.`id`
  );

INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE rt.`role_key` = 'repairer'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`menu_type` = 'C' AND m.`path` = 'workOrder')
    OR m.`perms` IN ('workorder:list', 'workorder:accept', 'workorder:quote', 'workorder:repair')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` rtm
    WHERE rtm.`template_id` = rt.`id`
      AND rtm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 5. 已有系统角色授权回填
-- 通过角色模板把工单菜单同步到现有 system role
-- -------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`id`, rtm.`menu_id`, NOW(), NOW()
FROM `sys_role` r
JOIN `sys_company` c ON c.`id` = r.`company_id`
JOIN `sys_role_template` rt
  ON rt.`type_code` = c.`type_code`
 AND rt.`role_key` = r.`role_key`
JOIN `sys_role_template_menu` rtm ON rtm.`template_id` = rt.`id`
WHERE r.`is_system` = 1
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`id`
      AND rm.`menu_id` = rtm.`menu_id`
  );
