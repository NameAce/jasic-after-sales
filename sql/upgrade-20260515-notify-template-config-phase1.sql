-- =============================================
-- 消息通知模板配置重构：阶段一（文档、SQL 和基础枚举）
-- 说明：
-- 1. 旧 sys_notify_template / sys_notify_template_channel 直接删表重建，旧数据不保留
-- 2. 菜单统一调整为“通知模板配置”，移除 refresh-cache 权限点
-- 3. 初始化两条首批模板和一条小程序渠道配置
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 删除旧模板表并按新模型重建
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_notify_template_channel`;
DROP TABLE IF EXISTS `sys_notify_template`;

CREATE TABLE `sys_notify_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '通知场景编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `title_template` varchar(128) DEFAULT NULL COMMENT '标题模板',
  `content_template` varchar(512) DEFAULT NULL COMMENT '内容模板',
  `route_type` varchar(64) DEFAULT NULL COMMENT '站内跳转类型',
  `route_value_template` varchar(128) DEFAULT NULL COMMENT '跳转值模板',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_code` (`scene_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知模板配置表';

CREATE TABLE `sys_notify_template_channel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '通知场景编码',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `channel_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '渠道状态：1启用，0停用',
  `config_json` text DEFAULT NULL COMMENT '渠道参数 JSON',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_channel` (`scene_code`, `channel_type`),
  KEY `idx_channel_enabled` (`channel_type`, `channel_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知模板渠道配置表';

-- -------------------------------------------
-- 2. 平台菜单与权限调整
-- -------------------------------------------
SET @platform_system_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'system'
  LIMIT 1
);

UPDATE `sys_menu`
SET `menu_name` = '通知模板配置',
    `component` = 'system/notifyTemplate/index',
    `icon` = 'el-icon-message-solid',
    `order_num` = 10,
    `is_visible` = 1,
    `status` = 1,
    `update_time` = NOW()
WHERE @platform_system_root_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `parent_id` = @platform_system_root_id
  AND `menu_type` = 'C'
  AND `path` = 'notifyTemplate';

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板配置', @platform_system_root_id, 'C', 'notifyTemplate',
       'system/notifyTemplate/index', NULL, 'el-icon-message-solid', 10, 1, 1, NOW(), NOW()
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

DELETE rm
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE m.`perms` = 'system:notifyTemplate:refresh';

DELETE tcm
FROM `sys_type_code_menu` tcm
INNER JOIN `sys_menu` m ON m.`id` = tcm.`menu_id`
WHERE m.`perms` = 'system:notifyTemplate:refresh';

DELETE FROM `sys_menu`
WHERE `perms` = 'system:notifyTemplate:refresh';

UPDATE `sys_menu`
SET `menu_name` = '通知模板查询',
    `parent_id` = @notify_template_menu_id,
    `order_num` = 1,
    `update_time` = NOW()
WHERE @notify_template_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` = 'system:notifyTemplate:list';

UPDATE `sys_menu`
SET `menu_name` = '通知模板查看',
    `parent_id` = @notify_template_menu_id,
    `order_num` = 2,
    `update_time` = NOW()
WHERE @notify_template_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` = 'system:notifyTemplate:view';

UPDATE `sys_menu`
SET `menu_name` = '通知模板新增',
    `parent_id` = @notify_template_menu_id,
    `order_num` = 3,
    `update_time` = NOW()
WHERE @notify_template_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` = 'system:notifyTemplate:add';

UPDATE `sys_menu`
SET `menu_name` = '通知模板修改',
    `parent_id` = @notify_template_menu_id,
    `order_num` = 4,
    `update_time` = NOW()
WHERE @notify_template_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` = 'system:notifyTemplate:update';

UPDATE `sys_menu`
SET `menu_name` = '通知模板停用',
    `parent_id` = @notify_template_menu_id,
    `order_num` = 5,
    `update_time` = NOW()
WHERE @notify_template_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` = 'system:notifyTemplate:remove';

UPDATE `sys_menu`
SET `menu_name` = '通知模板预览',
    `parent_id` = @notify_template_menu_id,
    `order_num` = 6,
    `update_time` = NOW()
WHERE @notify_template_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` = 'system:notifyTemplate:preview';

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板查询', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:list',
       NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_template_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTemplate:list'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板查看', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:view',
       NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_template_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTemplate:view'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板新增', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:add',
       NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_template_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTemplate:add'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板修改', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:update',
       NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_template_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTemplate:update'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板停用', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:remove',
       NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_template_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTemplate:remove'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知模板预览', @notify_template_menu_id, 'F', NULL, NULL, 'system:notifyTemplate:preview',
       NULL, 6, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_template_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyTemplate:preview'
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyTemplate')
    OR m.`perms` IN (
      'system:notifyTemplate:list',
      'system:notifyTemplate:view',
      'system:notifyTemplate:add',
      'system:notifyTemplate:update',
      'system:notifyTemplate:remove',
      'system:notifyTemplate:preview'
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
      'system:notifyTemplate:preview'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 3. 首批模板初始化
-- -------------------------------------------
INSERT INTO `sys_notify_template`
(`scene_code`, `template_name`, `title_template`, `content_template`, `route_type`,
 `route_value_template`, `status`, `remark`, `create_time`, `update_time`)
VALUES
(
  'WORK_ORDER_ASSIGNED_TODO',
  '工单派单待办',
  '您有新的维修工单',
  '工单${orderNo}已派给您，请及时处理',
  'WORK_ORDER_DETAIL',
  '${workOrderId}',
  1,
  '阶段一初始化模板：工单派单站内待办',
  NOW(),
  NOW()
),
(
  'WORK_ORDER_EVALUATION_INVITE_MP_C',
  '客户评价邀请订阅消息',
  '客户满意度评价通知',
  '您的维修工单${orderNo}已关闭，邀请您对本次服务进行评价',
  'WORK_ORDER_EVALUATE',
  '${workOrderId}',
  1,
  '阶段一初始化模板：客户评价邀请小程序订阅消息',
  NOW(),
  NOW()
);

INSERT INTO `sys_notify_template_channel`
(`scene_code`, `channel_type`, `channel_enabled`, `config_json`, `remark`, `create_time`, `update_time`)
VALUES
(
  'WORK_ORDER_EVALUATION_INVITE_MP_C',
  'MP_SUBSCRIBE',
  1,
  '{"templateId":"","pagePathTemplate":"pages/order/evaluate?workOrderId=${workOrderId}","fieldMapping":[{"field":"thing1","value":"${orderNo}"},{"field":"phone_number2","value":"${customerMobile}"},{"field":"thing3","value":"${companyName}"}]}',
  '阶段一初始化渠道配置：客户评价邀请 C端小程序订阅消息',
  NOW(),
  NOW()
);

