-- =============================================
-- 佳士售后系统 - 公司地址簿与 B 端地址权限增量脚本
-- 适用场景：已执行过基础建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `company_address` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`     bigint unsigned  NOT NULL                COMMENT '公司ID',
  `contact_name`   varchar(64)      NOT NULL                COMMENT '联系人',
  `contact_phone`  varchar(32)      NOT NULL                COMMENT '联系电话',
  `address`        varchar(255)     NOT NULL                COMMENT '详细地址',
  `is_default`     tinyint unsigned DEFAULT 0               COMMENT '是否默认地址（1=是，0=否）',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_company_address_company` (`company_id`),
  KEY `idx_company_address_default` (`company_id`, `is_default`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司地址簿';

INSERT INTO `company_address`
(`company_id`, `contact_name`, `contact_phone`, `address`, `is_default`, `create_time`, `update_time`)
SELECT c.`id`,
       COALESCE(NULLIF(TRIM(c.`contact_name`), ''), c.`company_name`) AS `contact_name`,
       COALESCE(NULLIF(TRIM(c.`contact_phone`), ''), '')              AS `contact_phone`,
       TRIM(c.`address`)                                              AS `address`,
       1,
       NOW(),
       NOW()
FROM `sys_company` c
WHERE c.`address` IS NOT NULL
  AND TRIM(c.`address`) <> ''
  AND COALESCE(NULLIF(TRIM(c.`contact_phone`), ''), '') <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM `company_address` ca
    WHERE ca.`company_id` = c.`id`
  );

SET @service_work_order_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'SERVICE'
    AND `menu_type` = 'C'
    AND `path` = 'workOrder'
  ORDER BY `id`
  LIMIT 1
);

SET @hq_work_order_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'HQ'
    AND `menu_type` = 'C'
    AND `path` = 'workOrder'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '公司地址簿查看', @service_work_order_menu_id, 'F', NULL, NULL, 'companyAddress:list', NULL, 10, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'companyAddress:list'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'SERVICE', '公司地址簿管理', @service_work_order_menu_id, 'F', NULL, NULL, 'companyAddress:manage', NULL, 11, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @service_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @service_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'companyAddress:manage'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '公司地址簿查看', @hq_work_order_menu_id, 'F', NULL, NULL, 'companyAddress:list', NULL, 10, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'companyAddress:list'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'HQ', '公司地址簿管理', @hq_work_order_menu_id, 'F', NULL, NULL, 'companyAddress:manage', NULL, 11, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @hq_work_order_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @hq_work_order_menu_id AND `menu_type` = 'F' AND `perms` = 'companyAddress:manage'
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT tc.`type_code`, m.`id`, NOW(), NOW()
FROM `sys_company_type` tc
JOIN `sys_menu` m ON m.`subject_type` = tc.`subject_type`
WHERE m.`perms` IN ('companyAddress:list', 'companyAddress:manage')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = tc.`type_code`
      AND tcm.`menu_id` = m.`id`
  );

INSERT INTO `sys_role_template_menu` (`template_id`, `menu_id`, `create_time`, `update_time`)
SELECT DISTINCT rtm.`template_id`, target_menu.`id`, NOW(), NOW()
FROM `sys_role_template_menu` rtm
JOIN `sys_menu` source_menu ON source_menu.`id` = rtm.`menu_id`
JOIN `sys_role_template` rt ON rt.`id` = rtm.`template_id`
JOIN `sys_company_type` tc ON tc.`type_code` = rt.`type_code`
JOIN `sys_menu` target_menu
  ON target_menu.`subject_type` = tc.`subject_type`
 AND target_menu.`perms` IN ('companyAddress:list', 'companyAddress:manage')
WHERE source_menu.`perms` = 'workorder:add'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_template_menu` existed
    WHERE existed.`template_id` = rtm.`template_id`
      AND existed.`menu_id` = target_menu.`id`
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT DISTINCT rm.`role_id`, target_menu.`id`, NOW(), NOW()
FROM `sys_role_menu` rm
JOIN `sys_menu` source_menu ON source_menu.`id` = rm.`menu_id`
JOIN `sys_role` r ON r.`id` = rm.`role_id`
JOIN `sys_company` c ON c.`id` = r.`company_id`
JOIN `sys_company_type` tc ON tc.`type_code` = c.`type_code`
JOIN `sys_menu` target_menu
  ON target_menu.`subject_type` = tc.`subject_type`
 AND target_menu.`perms` IN ('companyAddress:list', 'companyAddress:manage')
WHERE source_menu.`perms` = 'workorder:add'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed
    WHERE existed.`role_id` = rm.`role_id`
      AND existed.`menu_id` = target_menu.`id`
  );
