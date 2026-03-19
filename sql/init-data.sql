-- =============================================
-- 佳士售后系统 - 初始化数据脚本
-- 在执行 schema.sql 后运行
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 初始化公司类型
-- -------------------------------------------
INSERT INTO `sys_company_type` (`id`, `type_code`, `type_name`, `subject_type`, `remark`, `order_num`, `create_time`, `update_time`) VALUES
(1, 'PLATFORM', '平台',       'PLATFORM', '系统平台',         1, NOW(), NOW()),
(2, 'HQ_A',     '总部A',      'HQ',       '总部公司A',        2, NOW(), NOW()),
(3, 'HQ_B',     '总部B',      'HQ',       '总部公司B',        3, NOW(), NOW()),
(4, 'HQ_C',     '总部C',      'HQ',       '总部公司C',        4, NOW(), NOW()),
(5, 'HQ_D',     '总部D',      'HQ',       '总部公司D',        5, NOW(), NOW()),
(6, 'FIRST',    '一级服务网点', 'SERVICE',  '一级服务网点',      6, NOW(), NOW()),
(7, 'SECOND',   '二级服务网点', 'SERVICE',  '二级服务网点',      7, NOW(), NOW());

-- -------------------------------------------
-- 2. 初始化平台公司
-- -------------------------------------------
INSERT INTO `sys_company` (`id`, `company_name`, `company_code`, `type_code`, `contact_name`, `contact_phone`, `address`, `status`, `remark`, `create_time`, `update_time`) VALUES
(1, '佳士售后平台', 'PLATFORM', 'PLATFORM', '系统管理员', '13800000000', '佳士科技总部', 1, '系统平台管理公司', NOW(), NOW());

-- -------------------------------------------
-- 3. 初始化平台管理员账号
-- 默认密码：admin123（BCrypt加密）
-- -------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', '13800000000', 1, 0, NOW(), NOW());

-- -------------------------------------------
-- 4. 平台管理员关联平台公司
-- -------------------------------------------
INSERT INTO `sys_user_company` (`id`, `user_id`, `company_id`, `is_default`, `create_time`, `update_time`) VALUES
(1, 1, 1, 1, NOW(), NOW());

-- -------------------------------------------
-- 5. 初始化平台角色
-- -------------------------------------------
INSERT INTO `sys_role` (`id`, `company_id`, `role_name`, `role_key`, `data_scope`, `role_type`, `is_system`, `status`, `order_num`, `remark`, `create_time`, `update_time`) VALUES
(1, 1, '平台超级管理员', 'platform_admin', 'ALL', 1, 1, 1, 1, '平台最高权限角色', NOW(), NOW());

-- -------------------------------------------
-- 6. 平台管理员关联角色
-- -------------------------------------------
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`, `update_time`) VALUES
(1, 1, 1, NOW(), NOW());

-- -------------------------------------------
-- 7. 初始化平台菜单（subject_type = PLATFORM）
-- -------------------------------------------

-- 一级目录
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(1,  'PLATFORM', '系统管理', 0, 'M', 'system',   NULL, NULL, 'el-icon-setting',   1, 1, 1, NOW(), NOW()),
(2,  'PLATFORM', '组织管理', 0, 'M', 'org',      NULL, NULL, 'el-icon-s-grid',    2, 1, 1, NOW(), NOW()),
(3,  'PLATFORM', '日志管理', 0, 'M', 'log',      NULL, NULL, 'el-icon-document',  3, 1, 1, NOW(), NOW());

-- 二级菜单 - 系统管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(101, 'PLATFORM', '用户管理',     1, 'C', 'user',         'system/user/index',         NULL, 'el-icon-user',     1, 1, 1, NOW(), NOW()),
(102, 'PLATFORM', '角色管理',     1, 'C', 'role',         'system/role/index',         NULL, 'el-icon-s-custom', 2, 1, 1, NOW(), NOW()),
(103, 'PLATFORM', '菜单管理',     1, 'C', 'menu',         'system/menu/index',         NULL, 'el-icon-menu',     3, 1, 1, NOW(), NOW()),
(104, 'PLATFORM', '角色模板管理', 1, 'C', 'roleTemplate', 'system/roleTemplate/index', NULL, 'el-icon-s-tools',  4, 1, 1, NOW(), NOW());

-- 二级菜单 - 组织管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(201, 'PLATFORM', '公司类型管理', 2, 'C', 'companyType', 'org/companyType/index', NULL, 'el-icon-notebook-2',      1, 1, 1, NOW(), NOW()),
(202, 'PLATFORM', '公司管理',     2, 'C', 'company',     'org/company/index',     NULL, 'el-icon-office-building', 2, 1, 1, NOW(), NOW()),
(203, 'PLATFORM', '签约关系管理', 2, 'C', 'contract',    'org/contract/index',    NULL, 'el-icon-connection',      3, 1, 1, NOW(), NOW()),
(204, 'PLATFORM', '大区管理',     2, 'C', 'region',      'org/region/index',      NULL, 'el-icon-map-location',    4, 1, 1, NOW(), NOW());

-- 二级菜单 - 日志管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(301, 'PLATFORM', '操作日志', 3, 'C', 'operLog', 'log/operLog/index', NULL, 'el-icon-tickets', 1, 1, 1, NOW(), NOW());

-- 三级按钮 - 用户管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(1001, 'PLATFORM', '用户查询', 101, 'F', NULL, NULL, 'system:user:list',   NULL, 1, 1, 1, NOW(), NOW()),
(1002, 'PLATFORM', '用户新增', 101, 'F', NULL, NULL, 'system:user:add',    NULL, 2, 1, 1, NOW(), NOW()),
(1003, 'PLATFORM', '用户修改', 101, 'F', NULL, NULL, 'system:user:update', NULL, 3, 1, 1, NOW(), NOW()),
(1004, 'PLATFORM', '用户删除', 101, 'F', NULL, NULL, 'system:user:remove', NULL, 4, 1, 1, NOW(), NOW()),
(1005, 'PLATFORM', '重置密码', 101, 'F', NULL, NULL, 'system:user:resetPwd', NULL, 5, 1, 1, NOW(), NOW()),
(1006, 'PLATFORM', '强制下线', 101, 'F', NULL, NULL, 'system:user:kickout',  NULL, 6, 1, 1, NOW(), NOW());

-- 三级按钮 - 角色管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(1011, 'PLATFORM', '角色查询', 102, 'F', NULL, NULL, 'system:role:list',   NULL, 1, 1, 1, NOW(), NOW()),
(1012, 'PLATFORM', '角色新增', 102, 'F', NULL, NULL, 'system:role:add',    NULL, 2, 1, 1, NOW(), NOW()),
(1013, 'PLATFORM', '角色修改', 102, 'F', NULL, NULL, 'system:role:update', NULL, 3, 1, 1, NOW(), NOW()),
(1014, 'PLATFORM', '角色删除', 102, 'F', NULL, NULL, 'system:role:remove', NULL, 4, 1, 1, NOW(), NOW());

-- 三级按钮 - 菜单管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(1021, 'PLATFORM', '菜单查询', 103, 'F', NULL, NULL, 'system:menu:list',   NULL, 1, 1, 1, NOW(), NOW()),
(1022, 'PLATFORM', '菜单新增', 103, 'F', NULL, NULL, 'system:menu:add',    NULL, 2, 1, 1, NOW(), NOW()),
(1023, 'PLATFORM', '菜单修改', 103, 'F', NULL, NULL, 'system:menu:update', NULL, 3, 1, 1, NOW(), NOW()),
(1024, 'PLATFORM', '菜单删除', 103, 'F', NULL, NULL, 'system:menu:remove', NULL, 4, 1, 1, NOW(), NOW());

-- 三级按钮 - 角色模板管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(1031, 'PLATFORM', '模板查询', 104, 'F', NULL, NULL, 'system:roleTemplate:list',   NULL, 1, 1, 1, NOW(), NOW()),
(1032, 'PLATFORM', '模板新增', 104, 'F', NULL, NULL, 'system:roleTemplate:add',    NULL, 2, 1, 1, NOW(), NOW()),
(1033, 'PLATFORM', '模板修改', 104, 'F', NULL, NULL, 'system:roleTemplate:update', NULL, 3, 1, 1, NOW(), NOW()),
(1034, 'PLATFORM', '模板删除', 104, 'F', NULL, NULL, 'system:roleTemplate:remove', NULL, 4, 1, 1, NOW(), NOW()),
(1035, 'PLATFORM', '模板同步', 104, 'F', NULL, NULL, 'system:roleTemplate:sync',   NULL, 5, 1, 1, NOW(), NOW());

-- 三级按钮 - 公司类型管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(2011, 'PLATFORM', '类型查询', 201, 'F', NULL, NULL, 'org:companyType:list',   NULL, 1, 1, 1, NOW(), NOW()),
(2012, 'PLATFORM', '类型新增', 201, 'F', NULL, NULL, 'org:companyType:add',    NULL, 2, 1, 1, NOW(), NOW()),
(2013, 'PLATFORM', '类型修改', 201, 'F', NULL, NULL, 'org:companyType:update', NULL, 3, 1, 1, NOW(), NOW()),
(2014, 'PLATFORM', '类型删除', 201, 'F', NULL, NULL, 'org:companyType:remove', NULL, 4, 1, 1, NOW(), NOW());

-- 三级按钮 - 公司管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(2021, 'PLATFORM', '公司查询', 202, 'F', NULL, NULL, 'org:company:list',   NULL, 1, 1, 1, NOW(), NOW()),
(2022, 'PLATFORM', '公司新增', 202, 'F', NULL, NULL, 'org:company:add',    NULL, 2, 1, 1, NOW(), NOW()),
(2023, 'PLATFORM', '公司修改', 202, 'F', NULL, NULL, 'org:company:update', NULL, 3, 1, 1, NOW(), NOW()),
(2024, 'PLATFORM', '公司删除', 202, 'F', NULL, NULL, 'org:company:remove', NULL, 4, 1, 1, NOW(), NOW());

-- 三级按钮 - 签约关系管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(2031, 'PLATFORM', '签约查询', 203, 'F', NULL, NULL, 'org:contract:list',   NULL, 1, 1, 1, NOW(), NOW()),
(2032, 'PLATFORM', '签约新增', 203, 'F', NULL, NULL, 'org:contract:add',    NULL, 2, 1, 1, NOW(), NOW()),
(2033, 'PLATFORM', '签约修改', 203, 'F', NULL, NULL, 'org:contract:update', NULL, 3, 1, 1, NOW(), NOW()),
(2034, 'PLATFORM', '签约删除', 203, 'F', NULL, NULL, 'org:contract:remove', NULL, 4, 1, 1, NOW(), NOW());

-- 三级按钮 - 大区管理
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(2041, 'PLATFORM', '大区查询', 204, 'F', NULL, NULL, 'system:region:list',   NULL, 1, 1, 1, NOW(), NOW()),
(2042, 'PLATFORM', '大区新增', 204, 'F', NULL, NULL, 'system:region:add',    NULL, 2, 1, 1, NOW(), NOW()),
(2043, 'PLATFORM', '大区修改', 204, 'F', NULL, NULL, 'system:region:update', NULL, 3, 1, 1, NOW(), NOW()),
(2044, 'PLATFORM', '大区删除', 204, 'F', NULL, NULL, 'system:region:remove', NULL, 4, 1, 1, NOW(), NOW());

-- 三级按钮 - 操作日志
INSERT INTO `sys_menu` (`id`, `subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`) VALUES
(3011, 'PLATFORM', '日志查询', 301, 'F', NULL, NULL, 'log:operLog:list',   NULL, 1, 1, 1, NOW(), NOW()),
(3012, 'PLATFORM', '日志删除', 301, 'F', NULL, NULL, 'log:operLog:remove', NULL, 2, 1, 1, NOW(), NOW()),
(3013, 'PLATFORM', '日志导出', 301, 'F', NULL, NULL, 'log:operLog:export', NULL, 3, 1, 1, NOW(), NOW());

-- -------------------------------------------
-- 8. 平台管理员角色关联所有平台菜单
-- -------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, `id`, NOW(), NOW() FROM `sys_menu` WHERE `subject_type` = 'PLATFORM';

-- -------------------------------------------
-- 9. 初始化 PLATFORM 公司类型的菜单上限（平台拥有所有平台菜单）
-- -------------------------------------------
INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', `id`, NOW(), NOW() FROM `sys_menu` WHERE `subject_type` = 'PLATFORM';
