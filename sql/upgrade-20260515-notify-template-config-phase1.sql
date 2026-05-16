-- =============================================
-- 消息通知场景多目标重构：阶段一
-- 说明：
-- 1. 严格按“统一通知场景 + 多通知目标配置”模型落库
-- 2. 后台配置入口统一切换为“通知场景配置”
-- 3. 本脚本不处理历史数据迁移兼容，旧模板配置表直接下线
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 旧模板配置表下线并切换到新模型
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_notify_template_channel`;
DROP TABLE IF EXISTS `sys_notify_template`;
DROP TABLE IF EXISTS `notify_scene_target`;
DROP TABLE IF EXISTS `notify_scene`;

CREATE TABLE `notify_scene` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code`  varchar(64)     NOT NULL COMMENT '通知场景编码',
  `scene_name`  varchar(128)    NOT NULL COMMENT '通知场景名称',
  `biz_type`    varchar(64)     NOT NULL COMMENT '业务类型',
  `event_code`  varchar(64)     NOT NULL COMMENT '事件编码',
  `status`      tinyint(1)      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `remark`      varchar(255)    DEFAULT NULL COMMENT '备注',
  `create_time` datetime        NOT NULL COMMENT '创建时间',
  `update_time` datetime        NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_code` (`scene_code`),
  KEY `idx_biz_type_status` (`biz_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知场景表';

CREATE TABLE `notify_scene_target` (
  `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code`           varchar(64)     NOT NULL COMMENT '通知场景编码',
  `target_type`          varchar(32)     NOT NULL COMMENT '通知目标类型',
  `enabled`              tinyint(1)      NOT NULL DEFAULT 0 COMMENT '目标开关：1启用，0停用',
  `title_template`       varchar(128)    DEFAULT NULL COMMENT '标题模板',
  `content_template`     varchar(512)    DEFAULT NULL COMMENT '内容模板',
  `route_type`           varchar(64)     DEFAULT NULL COMMENT '跳转类型',
  `route_value_template` varchar(256)    DEFAULT NULL COMMENT '跳转值模板',
  `config_json`          text            DEFAULT NULL COMMENT '目标专属参数 JSON',
  `remark`               varchar(255)    DEFAULT NULL COMMENT '备注',
  `create_time`          datetime        NOT NULL COMMENT '创建时间',
  `update_time`          datetime        NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_target` (`scene_code`, `target_type`),
  KEY `idx_target_type_enabled` (`target_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知场景目标配置表';

-- -------------------------------------------
-- 2. 平台菜单与权限切换为通知场景配置
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
SET `menu_name` = '通知场景配置',
    `path` = 'notifyScene',
    `component` = 'system/notifyScene/index',
    `icon` = 'el-icon-message-solid',
    `order_num` = 10,
    `is_visible` = 1,
    `status` = 1,
    `update_time` = NOW()
WHERE @platform_system_root_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `parent_id` = @platform_system_root_id
  AND `menu_type` = 'C'
  AND `path` IN ('notifyTemplate', 'notifyScene');

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '通知场景配置', @platform_system_root_id, 'C', 'notifyScene',
       'system/notifyScene/index', NULL, 'el-icon-message-solid', 10, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'notifyScene'
  );

SET @notify_scene_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'notifyScene'
  LIMIT 1
);

DELETE rm
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE m.`perms` IN (
  'system:notifyTemplate:add',
  'system:notifyTemplate:remove',
  'system:notifyTemplate:refresh'
);

DELETE tcm
FROM `sys_type_code_menu` tcm
INNER JOIN `sys_menu` m ON m.`id` = tcm.`menu_id`
WHERE m.`perms` IN (
  'system:notifyTemplate:add',
  'system:notifyTemplate:remove',
  'system:notifyTemplate:refresh'
);

DELETE FROM `sys_menu`
WHERE `perms` IN (
  'system:notifyTemplate:add',
  'system:notifyTemplate:remove',
  'system:notifyTemplate:refresh'
);

UPDATE `sys_menu`
SET `menu_name` = '配置查询',
    `parent_id` = @notify_scene_menu_id,
    `perms` = 'system:notifyScene:list',
    `order_num` = 1,
    `update_time` = NOW()
WHERE @notify_scene_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` IN ('system:notifyTemplate:list', 'system:notifyScene:list');

UPDATE `sys_menu`
SET `menu_name` = '配置查看',
    `parent_id` = @notify_scene_menu_id,
    `perms` = 'system:notifyScene:view',
    `order_num` = 2,
    `update_time` = NOW()
WHERE @notify_scene_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` IN ('system:notifyTemplate:view', 'system:notifyScene:view');

UPDATE `sys_menu`
SET `menu_name` = '配置保存',
    `parent_id` = @notify_scene_menu_id,
    `perms` = 'system:notifyScene:update',
    `order_num` = 3,
    `update_time` = NOW()
WHERE @notify_scene_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` IN ('system:notifyTemplate:update', 'system:notifyScene:update');

UPDATE `sys_menu`
SET `menu_name` = '配置预览',
    `parent_id` = @notify_scene_menu_id,
    `perms` = 'system:notifyScene:preview',
    `order_num` = 4,
    `update_time` = NOW()
WHERE @notify_scene_menu_id IS NOT NULL
  AND `subject_type` = 'PLATFORM'
  AND `menu_type` = 'F'
  AND `perms` IN ('system:notifyTemplate:preview', 'system:notifyScene:preview');

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置查询', @notify_scene_menu_id, 'F', NULL, NULL, 'system:notifyScene:list',
       NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_scene_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_scene_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyScene:list'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置查看', @notify_scene_menu_id, 'F', NULL, NULL, 'system:notifyScene:view',
       NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_scene_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_scene_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyScene:view'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置保存', @notify_scene_menu_id, 'F', NULL, NULL, 'system:notifyScene:update',
       NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_scene_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_scene_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyScene:update'
  );

INSERT INTO `sys_menu`
(`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`,
 `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '配置预览', @notify_scene_menu_id, 'F', NULL, NULL, 'system:notifyScene:preview',
       NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @notify_scene_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @notify_scene_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'system:notifyScene:preview'
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyScene')
    OR m.`perms` IN (
      'system:notifyScene:list',
      'system:notifyScene:view',
      'system:notifyScene:update',
      'system:notifyScene:preview'
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` = 'notifyScene')
    OR m.`perms` IN (
      'system:notifyScene:list',
      'system:notifyScene:view',
      'system:notifyScene:update',
      'system:notifyScene:preview'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 3. 首批通知场景与多目标配置初始化
-- -------------------------------------------
INSERT INTO `notify_scene`
(`scene_code`, `scene_name`, `biz_type`, `event_code`, `status`, `remark`, `create_time`, `update_time`)
VALUES
(
  'WORK_ORDER_ASSIGNED',
  '工单派单',
  'WORK_ORDER',
  'WORK_ORDER_ASSIGNED',
  1,
  '阶段一初始化场景：统一通知场景，支持站内消息、站内待办和小程序订阅通知',
  NOW(),
  NOW()
),
(
  'WORK_ORDER_EVALUATION_INVITE',
  '客户评价邀请',
  'WORK_ORDER',
  'WORK_ORDER_EVALUATION_INVITE',
  1,
  '阶段一初始化场景：客户评价邀请，仅开放小程序订阅通知目标',
  NOW(),
  NOW()
);

INSERT INTO `notify_scene_target`
(`scene_code`, `target_type`, `enabled`, `title_template`, `content_template`, `route_type`,
 `route_value_template`, `config_json`, `remark`, `create_time`, `update_time`)
VALUES
(
  'WORK_ORDER_ASSIGNED',
  'IN_APP_MESSAGE',
  0,
  '您有新的维修工单',
  '工单${orderNo}已派给您，请及时查看处理进度',
  'WORK_ORDER_DETAIL',
  '${workOrderId}',
  NULL,
  '阶段一初始化目标：工单派单站内消息',
  NOW(),
  NOW()
),
(
  'WORK_ORDER_ASSIGNED',
  'IN_APP_TODO',
  1,
  '您有新的维修工单',
  '工单${orderNo}已派给您，请及时处理',
  'WORK_ORDER_DETAIL',
  '${workOrderId}',
  NULL,
  '阶段一初始化目标：工单派单站内待办',
  NOW(),
  NOW()
),
(
  'WORK_ORDER_ASSIGNED',
  'MP_SUBSCRIBE',
  0,
  '工单派单通知',
  '工单${orderNo}已派给您，请及时处理',
  NULL,
  NULL,
  '{"templateId":"","pagePathTemplate":"pages/order/detail?workOrderId=${workOrderId}","fieldMapping":[{"field":"thing1","value":"${orderNo}"},{"field":"thing2","value":"${receiverName}"}]}',
  '阶段一初始化目标：工单派单小程序订阅通知',
  NOW(),
  NOW()
),
(
  'WORK_ORDER_EVALUATION_INVITE',
  'MP_SUBSCRIBE',
  1,
  '客户满意度评价通知',
  '您的维修工单${orderNo}已关闭，邀请您对本次服务进行评价',
  'WORK_ORDER_EVALUATE',
  '${workOrderId}',
  '{"templateId":"","pagePathTemplate":"pages/order/evaluate?workOrderId=${workOrderId}","fieldMapping":[{"field":"thing1","value":"${orderNo}"},{"field":"thing2","value":"${companyName}"},{"field":"date3","value":"${closedTime}"}]}',
  '阶段一初始化目标：客户评价邀请小程序订阅通知',
  NOW(),
  NOW()
);
