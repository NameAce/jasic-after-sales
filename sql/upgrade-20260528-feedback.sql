-- =============================================
-- 佳士售后服务系统 - 平台反馈单增量脚本
-- 适用场景：
-- 1. 已存在基础组织、角色、菜单、权限和工单数据模型
-- 2. 需要补充平台反馈单表结构与 HQ / SERVICE 权限菜单
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 新增平台反馈单主表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_feedback` (
  `id`                    bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submitter_type`        varchar(64)     NOT NULL COMMENT '提交主体类型（CUSTOMER/SERVICE_COMPANY_USER）',
  `submitter_id`          bigint unsigned NOT NULL COMMENT '提交人ID',
  `submitter_name`        varchar(128)    DEFAULT NULL COMMENT '提交人姓名快照',
  `submit_company_id`     bigint unsigned DEFAULT NULL COMMENT '提交网点ID',
  `submit_source_type`    varchar(64)     NOT NULL COMMENT '提交来源类型（CUSTOMER_WORK_ORDER/CUSTOMER_DIRECT/SERVICE_COMPANY）',
  `submit_source_name`    varchar(128)    NOT NULL COMMENT '提交来源名称快照',
  `contact_phone`         varchar(32)     DEFAULT NULL COMMENT '联系电话快照',
  `related_work_order_id` bigint unsigned DEFAULT NULL COMMENT '关联工单ID',
  `hq_company_id`         bigint unsigned NOT NULL COMMENT '归属总部ID',
  `content`               varchar(500)    NOT NULL COMMENT '反馈内容',
  `status`                varchar(32)     NOT NULL COMMENT '反馈状态（UNACCEPTED/ACCEPTED）',
  `accept_user_id`        bigint unsigned DEFAULT NULL COMMENT '受理人系统用户ID',
  `accept_user_name`      varchar(128)    DEFAULT NULL COMMENT '受理人姓名快照',
  `accept_time`           datetime        DEFAULT NULL COMMENT '受理时间',
  `create_time`           datetime        NOT NULL COMMENT '创建时间',
  `update_time`           datetime        NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台反馈单';

-- -------------------------------------------
-- 2. SERVICE 主体菜单与权限
-- 说明：
-- 1. 当前轮只做后端，因此目录和页面菜单先按隐藏方式落库
-- 2. 功能权限点仍然提前补齐，便于后端接口联调和角色授权
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
SELECT 'SERVICE', '投诉与建议', @service_root_id, 'C', 'feedback', 'feedback/index', NULL, 'el-icon-chat-line-square', 20, 0, 1, NOW(), NOW()
FROM DUAL
WHERE @service_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'SERVICE'
      AND `parent_id` = @service_root_id
      AND `menu_type` = 'C'
      AND `path` = 'feedback'
  );

SET @service_feedback_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'SERVICE'
    AND `parent_id` = @service_root_id
    AND `menu_type` = 'C'
    AND `path` = 'feedback'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '反馈提交', @service_feedback_menu_id, 'F', NULL, NULL, 'feedback:add', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_feedback_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @service_feedback_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'feedback:add'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '反馈查询', @service_feedback_menu_id, 'F', NULL, NULL, 'feedback:list', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_feedback_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @service_feedback_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'feedback:list'
  );

-- -------------------------------------------
-- 3. HQ 主体菜单与权限
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
SELECT 'HQ', '投诉与建议', @hq_root_id, 'C', 'feedback', 'feedback/index', NULL, 'el-icon-chat-line-square', 20, 0, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'HQ'
      AND `parent_id` = @hq_root_id
      AND `menu_type` = 'C'
      AND `path` = 'feedback'
  );

SET @hq_feedback_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = @hq_root_id
    AND `menu_type` = 'C'
    AND `path` = 'feedback'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '反馈查询', @hq_feedback_menu_id, 'F', NULL, NULL, 'feedback:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_feedback_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hq_feedback_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'feedback:list'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '反馈受理', @hq_feedback_menu_id, 'F', NULL, NULL, 'feedback:accept', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_feedback_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hq_feedback_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'feedback:accept'
  );

-- -------------------------------------------
-- 4. 公司类型菜单授权
-- -------------------------------------------
INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT tc.`type_code`, m.`id`, NOW(), NOW()
FROM `sys_company_type` tc
JOIN `sys_menu` m ON m.`subject_type` = 'SERVICE'
WHERE tc.`subject_type` = 'SERVICE'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`parent_id` = @service_root_id AND m.`path` = 'feedback')
    OR m.`perms` IN ('feedback:add', 'feedback:list')
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
    OR (m.`parent_id` = @hq_root_id AND m.`path` = 'feedback')
    OR m.`perms` IN ('feedback:list', 'feedback:accept')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = tc.`type_code`
      AND tcm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 5. 角色模板授权
-- 说明：
-- 1. SERVICE 侧给系统管理员和维修员开放提交/查询自己的反馈
-- 2. HQ 侧给系统管理员开放查询与受理能力
-- -------------------------------------------
INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE rt.`role_key` IN ('js-admin', 'admin')
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`menu_type` = 'C' AND m.`path` = 'feedback')
    OR (
      tc.`subject_type` = 'SERVICE'
      AND m.`perms` IN ('feedback:add', 'feedback:list')
    )
    OR (
      tc.`subject_type` = 'HQ'
      AND m.`perms` IN ('feedback:list', 'feedback:accept')
    )
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
  AND tc.`subject_type` = 'SERVICE'
  AND (
    (m.`parent_id` = 0 AND m.`path` = 'afterSales')
    OR (m.`menu_type` = 'C' AND m.`path` = 'feedback')
    OR m.`perms` IN ('feedback:add', 'feedback:list')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` rtm
    WHERE rtm.`template_id` = rt.`id`
      AND rtm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 6. 已有系统角色授权回填
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
