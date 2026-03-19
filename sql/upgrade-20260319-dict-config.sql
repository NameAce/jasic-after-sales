-- =============================================
-- 佳士售后系统 - 字典管理/参数设置增量升级脚本
-- 适用场景：已有业务数据的库，禁止重跑 schema.sql / init-data.sql
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 新增表：sys_dict_type
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_name`   varchar(100)     NOT NULL                COMMENT '字典名称',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '字典类型',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- -------------------------------------------
-- 2. 新增表：sys_dict_data
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '字典类型',
  `dict_label`  varchar(100)     NOT NULL                COMMENT '字典标签',
  `dict_value`  varchar(100)     NOT NULL                COMMENT '字典键值',
  `dict_sort`   int              DEFAULT 0               COMMENT '排序',
  `css_class`   varchar(100)     DEFAULT NULL            COMMENT '自定义样式',
  `list_class`  varchar(100)     DEFAULT NULL            COMMENT '标签样式',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '是否默认（1=是，0=否）',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_value` (`dict_type`, `dict_value`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- -------------------------------------------
-- 3. 新增表：sys_config
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name`  varchar(100)     NOT NULL                COMMENT '参数名称',
  `config_key`   varchar(100)     NOT NULL                COMMENT '参数键名',
  `config_value` text             NOT NULL                COMMENT '参数键值',
  `config_type`  tinyint unsigned DEFAULT 0               COMMENT '是否内置（1=是，0=否）',
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`  datetime         NOT NULL                COMMENT '创建时间',
  `update_time`  datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数设置表';

-- -------------------------------------------
-- 4. 平台系统管理根菜单
-- -------------------------------------------
INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '系统管理', 0, 'M', 'system', NULL, NULL, 'el-icon-setting', 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = 0
    AND `menu_type` = 'M'
    AND `path` = 'system'
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

-- -------------------------------------------
-- 5. 平台页面菜单
-- -------------------------------------------
INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典管理', @platform_system_root_id, 'C', 'dictType', 'system/dictType/index', NULL, 'el-icon-collection-tag', 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'dictType'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典数据', @platform_system_root_id, 'C', 'dictData/:dictId', 'system/dictData/index', NULL, 'el-icon-document', 99, 0, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'dictData/:dictId'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '参数设置', @platform_system_root_id, 'C', 'config', 'system/config/index', NULL, 'el-icon-setting', 6, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @platform_system_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `subject_type` = 'PLATFORM'
      AND `parent_id` = @platform_system_root_id
      AND `menu_type` = 'C'
      AND `path` = 'config'
  );

SET @dict_type_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'dictType'
  LIMIT 1
);

SET @dict_data_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'dictData/:dictId'
  LIMIT 1
);

SET @config_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `parent_id` = @platform_system_root_id
    AND `menu_type` = 'C'
    AND `path` = 'config'
  LIMIT 1
);

-- -------------------------------------------
-- 6. 平台按钮权限
-- -------------------------------------------
INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典类型查询', @dict_type_menu_id, 'F', NULL, NULL, 'system:dictType:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_type_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_type_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictType:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典类型新增', @dict_type_menu_id, 'F', NULL, NULL, 'system:dictType:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_type_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_type_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictType:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典类型修改', @dict_type_menu_id, 'F', NULL, NULL, 'system:dictType:update', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_type_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_type_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictType:update');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典类型删除', @dict_type_menu_id, 'F', NULL, NULL, 'system:dictType:remove', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_type_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_type_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictType:remove');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典缓存刷新', @dict_type_menu_id, 'F', NULL, NULL, 'system:dictType:refresh', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_type_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_type_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictType:refresh');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典数据查询', @dict_data_menu_id, 'F', NULL, NULL, 'system:dictData:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_data_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_data_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictData:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典数据新增', @dict_data_menu_id, 'F', NULL, NULL, 'system:dictData:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_data_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_data_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictData:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典数据修改', @dict_data_menu_id, 'F', NULL, NULL, 'system:dictData:update', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_data_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_data_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictData:update');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '字典数据删除', @dict_data_menu_id, 'F', NULL, NULL, 'system:dictData:remove', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @dict_data_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @dict_data_menu_id AND `menu_type` = 'F' AND `perms` = 'system:dictData:remove');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '参数查询', @config_menu_id, 'F', NULL, NULL, 'system:config:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:config:list');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '参数新增', @config_menu_id, 'F', NULL, NULL, 'system:config:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:config:add');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '参数修改', @config_menu_id, 'F', NULL, NULL, 'system:config:update', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:config:update');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '参数删除', @config_menu_id, 'F', NULL, NULL, 'system:config:remove', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:config:remove');

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '参数缓存刷新', @config_menu_id, 'F', NULL, NULL, 'system:config:refresh', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @config_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @config_menu_id AND `menu_type` = 'F' AND `perms` = 'system:config:refresh');

-- -------------------------------------------
-- 7. 平台管理员角色授权
-- -------------------------------------------
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
    (m.`parent_id` = @platform_system_root_id AND m.`path` IN ('dictType', 'dictData/:dictId', 'config'))
    OR m.`perms` IN (
      'system:dictType:list', 'system:dictType:add', 'system:dictType:update', 'system:dictType:remove', 'system:dictType:refresh',
      'system:dictData:list', 'system:dictData:add', 'system:dictData:update', 'system:dictData:remove',
      'system:config:list', 'system:config:add', 'system:config:update', 'system:config:remove', 'system:config:refresh'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    WHERE rm.`role_id` = @platform_role_id
      AND rm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 8. PLATFORM 菜单上限
-- -------------------------------------------
INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE m.`subject_type` = 'PLATFORM'
  AND (
    (m.`parent_id` = @platform_system_root_id AND m.`path` IN ('dictType', 'dictData/:dictId', 'config'))
    OR m.`perms` IN (
      'system:dictType:list', 'system:dictType:add', 'system:dictType:update', 'system:dictType:remove', 'system:dictType:refresh',
      'system:dictData:list', 'system:dictData:add', 'system:dictData:update', 'system:dictData:remove',
      'system:config:list', 'system:config:add', 'system:config:update', 'system:config:remove', 'system:config:refresh'
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );

-- -------------------------------------------
-- 9. 初始化字典类型
-- -------------------------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `remark`, `create_time`, `update_time`)
SELECT '是否字典', 'sys_yes_no', 1, '是/否通用字典', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'sys_yes_no');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `remark`, `create_time`, `update_time`)
SELECT '状态字典', 'sys_normal_disable', 1, '启用/停用通用字典', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'sys_normal_disable');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `remark`, `create_time`, `update_time`)
SELECT '显示字典', 'sys_show_hide', 1, '显示/隐藏通用字典', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'sys_show_hide');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `remark`, `create_time`, `update_time`)
SELECT '操作类型', 'sys_oper_type', 1, '操作日志类型字典', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'sys_oper_type');

-- -------------------------------------------
-- 10. 初始化字典数据
-- -------------------------------------------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_yes_no', '是', '1', 1, NULL, 'primary', 1, 1, '系统默认是', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_yes_no' AND `dict_value` = '1');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_yes_no', '否', '0', 2, NULL, 'info', 0, 1, '系统默认否', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_yes_no' AND `dict_value` = '0');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_normal_disable', '启用', '1', 1, NULL, 'success', 1, 1, '启用状态', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_normal_disable' AND `dict_value` = '1');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_normal_disable', '停用', '0', 2, NULL, 'danger', 0, 1, '停用状态', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_normal_disable' AND `dict_value` = '0');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_show_hide', '显示', '1', 1, NULL, 'primary', 1, 1, '显示状态', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_show_hide' AND `dict_value` = '1');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_show_hide', '隐藏', '0', 2, NULL, 'info', 0, 1, '隐藏状态', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_show_hide' AND `dict_value` = '0');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '其他', '0', 1, NULL, 'info', 0, 1, '其他操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '0');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '新增', '1', 2, NULL, 'primary', 0, 1, '新增操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '1');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '修改', '2', 3, NULL, 'warning', 0, 1, '修改操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '2');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '删除', '3', 4, NULL, 'danger', 0, 1, '删除操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '3');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '授权', '4', 5, NULL, 'success', 0, 1, '授权操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '4');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '导出', '5', 6, NULL, 'warning', 0, 1, '导出操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '5');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '登录', '6', 7, NULL, 'success', 0, 1, '登录操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '6');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '登出', '7', 8, NULL, 'info', 0, 1, '登出操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '7');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'sys_oper_type', '强制下线', '8', 9, NULL, 'danger', 0, 1, '强制下线操作', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'sys_oper_type' AND `dict_value` = '8');

-- -------------------------------------------
-- 11. 初始化参数
-- -------------------------------------------
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '公司管理员初始密码', 'org.company.adminInitPassword', 'Jasic@123', 1, '创建公司时默认管理员账号的初始密码', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'org.company.adminInitPassword');
