-- =============================================
-- 消息通知二期：模板后台配置
-- 说明：
-- 1. 新增通知模板表 sys_notify_template
-- 2. 初始化 WORK_ORDER_ASSIGNED 内置模板
-- 3. 为 PLATFORM admin 增加通知模板菜单与权限
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_notify_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `template_source` varchar(16) NOT NULL COMMENT '模板来源',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `message_type` varchar(32) NOT NULL COMMENT '消息类型',
  `notify_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '通知总开关',
  `override_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '覆盖开关',
  `route_type` varchar(32) DEFAULT NULL COMMENT '跳转类型',
  `title_template` varchar(128) DEFAULT NULL COMMENT '标题模板',
  `summary_template` varchar(255) DEFAULT NULL COMMENT '摘要模板',
  `route_value_template` varchar(128) DEFAULT NULL COMMENT '跳转值模板',
  `variables_json` text DEFAULT NULL COMMENT '变量说明',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code_source` (`template_code`, `template_source`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知模板表';

INSERT INTO `sys_notify_template`
(`template_code`, `template_name`, `template_source`, `biz_type`, `event_type`, `message_type`,
 `notify_enabled`, `override_enabled`, `route_type`, `title_template`, `summary_template`,
 `route_value_template`, `variables_json`, `remark`, `create_time`, `update_time`)
SELECT
  'WORK_ORDER_ASSIGNED',
  '工单派单通知',
  'BUILT_IN',
  'WORK_ORDER',
  'WORK_ORDER_ASSIGNED',
  'TODO',
  1,
  0,
  'WORK_ORDER_DETAIL',
  '你有新的工单待处理',
  '工单${bizNo}已派发给你，请尽快处理',
  '${bizId}',
  '[{"name":"bizId","desc":"业务ID"},{"name":"bizNo","desc":"业务编号"},{"name":"receiverId","desc":"接收人ID"},{"name":"receiverName","desc":"接收人名称"},{"name":"operatorId","desc":"操作人ID"},{"name":"oldAssignedUserId","desc":"旧接收人ID"},{"name":"newAssignedUserId","desc":"新接收人ID"},{"name":"assignType","desc":"派单类型"},{"name":"operationId","desc":"操作唯一标识"}]',
  '消息通知二期内置模板',
  NOW(),
  NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_notify_template`
  WHERE `template_code` = 'WORK_ORDER_ASSIGNED'
    AND `template_source` = 'BUILT_IN'
);

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
SELECT 'PLATFORM', '通知模板', @platform_system_root_id, 'C', 'notifyTemplate', 'system/notifyTemplate/index', NULL,
       'el-icon-message-solid', 8, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'notifyTemplate'
  );

SET @notify_template_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'notifyTemplate'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板查询', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板查看', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:view', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:view');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板新增', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:add', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板修改', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:update', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:update');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板删除', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:remove', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:remove');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板预览', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:preview', NULL, 6, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:preview');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板缓存刷新', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:refresh', NULL, 7, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @notify_template_menu_id AND `menu_type` = 'F' AND `perms` = 'system:notifyTemplate:refresh');

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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyTemplate')
    OR m.`perms` IN (
      'system:notifyTemplate:list',
      'system:notifyTemplate:view',
      'system:notifyTemplate:add',
      'system:notifyTemplate:update',
      'system:notifyTemplate:remove',
      'system:notifyTemplate:preview',
      'system:notifyTemplate:refresh'
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyTemplate')
    OR m.`perms` IN (
      'system:notifyTemplate:list',
      'system:notifyTemplate:view',
      'system:notifyTemplate:add',
      'system:notifyTemplate:update',
      'system:notifyTemplate:remove',
      'system:notifyTemplate:preview',
      'system:notifyTemplate:refresh'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
