-- =============================================
-- 佳士售后服务系统 - 平台反馈单受理回复增量脚本
-- 适用场景：
-- 1. 已执行平台反馈单初始脚本
-- 2. 需要补充“受理回复”和“修改受理”相关字段与权限点
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 补充受理回复字段
-- -------------------------------------------
SET @feedback_accept_reply_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_feedback'
    AND COLUMN_NAME = 'accept_reply'
);

SET @feedback_accept_reply_sql = IF(
  @feedback_accept_reply_exists = 0,
  'ALTER TABLE `sys_feedback` ADD COLUMN `accept_reply` varchar(200) DEFAULT NULL COMMENT ''受理回复'' AFTER `accept_time`',
  'SELECT 1'
);

PREPARE stmt_feedback_accept_reply FROM @feedback_accept_reply_sql;
EXECUTE stmt_feedback_accept_reply;
DEALLOCATE PREPARE stmt_feedback_accept_reply;

-- -------------------------------------------
-- 2. HQ 新增“修改受理”权限点
-- -------------------------------------------
SET @hq_root_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'afterSales'
  LIMIT 1
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
SELECT 'HQ', '修改受理', @hq_feedback_menu_id, 'F', NULL, NULL, 'feedback:updateAccept', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_feedback_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hq_feedback_menu_id
      AND `menu_type` = 'F'
      AND `perms` = 'feedback:updateAccept'
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT tc.`type_code`, m.`id`, NOW(), NOW()
FROM `sys_company_type` tc
JOIN `sys_menu` m ON m.`subject_type` = 'HQ'
WHERE tc.`subject_type` = 'HQ'
  AND m.`perms` = 'feedback:updateAccept'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = tc.`type_code`
      AND tcm.`menu_id` = m.`id`
  );

INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT rt.`id`, m.`id`, NOW(), NOW()
FROM `sys_role_template` rt
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE rt.`role_key` IN ('js-admin', 'admin')
  AND tc.`subject_type` = 'HQ'
  AND m.`perms` = 'feedback:updateAccept'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` rtm
    WHERE rtm.`template_id` = rt.`id`
      AND rtm.`menu_id` = m.`id`
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`id`, rtm.`menu_id`, NOW(), NOW()
FROM `sys_role` r
JOIN `sys_company` c ON c.`id` = r.`company_id`
JOIN `sys_role_template` rt
  ON rt.`type_code` = c.`type_code`
 AND rt.`role_key` = r.`role_key`
JOIN `sys_role_template_menu` rtm ON rtm.`template_id` = rt.`id`
JOIN `sys_menu` m ON m.`id` = rtm.`menu_id`
WHERE r.`is_system` = 1
  AND m.`perms` = 'feedback:updateAccept'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`id`
      AND rm.`menu_id` = rtm.`menu_id`
  );
