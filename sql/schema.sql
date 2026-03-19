-- =============================================
-- 佳士售后系统 - 数据库DDL脚本
-- 数据库：jasic_after_sales
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_general_ci
-- 共17张表
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------
-- 1. 公司类型字典表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company_type`;
CREATE TABLE `sys_company_type` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code`    varchar(32)      NOT NULL                COMMENT '类型编码（PLATFORM/HQ_A/HQ_B/HQ_C/HQ_D/FIRST/SECOND）',
  `type_name`    varchar(64)      NOT NULL                COMMENT '类型名称',
  `subject_type` varchar(16)      NOT NULL                COMMENT '主体类型（PLATFORM/HQ/SERVICE）',
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '备注',
  `order_num`    int              DEFAULT 0               COMMENT '排序',
  `create_time`  datetime         NOT NULL                COMMENT '创建时间',
  `update_time`  datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司类型字典表';

-- -------------------------------------------
-- 2. 公司表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company`;
CREATE TABLE `sys_company` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_name`  varchar(128)     NOT NULL                COMMENT '公司名称',
  `company_code`  varchar(64)      DEFAULT NULL            COMMENT '公司编码',
  `type_code`     varchar(32)      NOT NULL                COMMENT '公司类型编码',
  `contact_name`  varchar(64)      NOT NULL                COMMENT '联系人',
  `contact_phone` varchar(20)      NOT NULL                COMMENT '联系电话',
  `address`       varchar(256)     NOT NULL                COMMENT '公司地址',
  `longitude`     decimal(10,6)    DEFAULT NULL            COMMENT '经度',
  `latitude`      decimal(10,6)    DEFAULT NULL            COMMENT '纬度',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '状态（1=正常，0=停用）',
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_code` (`company_code`),
  KEY `idx_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司表';

-- -------------------------------------------
-- 3. 大区表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_region`;
CREATE TABLE `sys_region` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '所属总部公司ID',
  `region_name` varchar(64)      NOT NULL                COMMENT '大区名称',
  `region_code` varchar(32)      DEFAULT NULL            COMMENT '大区编码',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='大区表';

-- -------------------------------------------
-- 4. 总部-一级签约关系表
-- -------------------------------------------
DROP TABLE IF EXISTS `hq_first_contract`;
CREATE TABLE `hq_first_contract` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `hq_company_id`    bigint unsigned  NOT NULL                COMMENT '总部公司ID',
  `first_company_id` bigint unsigned  NOT NULL                COMMENT '一级网点公司ID',
  `region_id`        bigint unsigned  DEFAULT NULL            COMMENT '大区ID（签约时绑定）',
  `contract_no`      varchar(64)      DEFAULT NULL            COMMENT '合同编号',
  `status`           tinyint unsigned DEFAULT 1               COMMENT '状态（1=有效，0=终止）',
  `remark`           varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hq_first` (`hq_company_id`, `first_company_id`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='总部-一级签约关系表';

-- -------------------------------------------
-- 5. 一级-二级从属关系表
-- -------------------------------------------
DROP TABLE IF EXISTS `first_second_relation`;
CREATE TABLE `first_second_relation` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `first_company_id`  bigint unsigned  NOT NULL                COMMENT '一级网点公司ID',
  `second_company_id` bigint unsigned  NOT NULL                COMMENT '二级网点公司ID',
  `status`            tinyint unsigned DEFAULT 1               COMMENT '状态（1=有效，0=解除）',
  `remark`            varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`       datetime         NOT NULL                COMMENT '创建时间',
  `update_time`       datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_first_second` (`first_company_id`, `second_company_id`),
  UNIQUE KEY `uk_second` (`second_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='一级-二级从属关系表';

-- -------------------------------------------
-- 6. B端员工表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        varchar(64)      NOT NULL                COMMENT '登录用户名',
  `password`        varchar(128)     NOT NULL                COMMENT '密码（BCrypt加密）',
  `real_name`       varchar(64)      DEFAULT NULL            COMMENT '真实姓名',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '手机号',
  `email`           varchar(64)      DEFAULT NULL            COMMENT '邮箱',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '头像URL',
  `openid`          varchar(64)      DEFAULT NULL            COMMENT '微信openid（小程序登录绑定）',
  `unionid`         varchar(64)      DEFAULT NULL            COMMENT '微信unionid',
  `sex`             tinyint unsigned DEFAULT 0               COMMENT '性别（0=未知，1=男，2=女）',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '状态（1=正常，0=停用）',
  `is_deleted`      tinyint unsigned DEFAULT 0               COMMENT '是否删除（逻辑删除）',
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `last_login_time` datetime         DEFAULT NULL            COMMENT '最后登录时间',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='B端员工表';

-- -------------------------------------------
-- 7. C端客户表
-- -------------------------------------------
DROP TABLE IF EXISTS `c_user`;
CREATE TABLE `c_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid`          varchar(64)      NOT NULL                COMMENT '微信openid',
  `unionid`         varchar(64)      DEFAULT NULL            COMMENT '微信unionid',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '手机号（微信授权获取）',
  `nickname`        varchar(64)      DEFAULT NULL            COMMENT '昵称',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '头像URL',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '状态（1=正常，0=停用）',
  `last_login_time` datetime         DEFAULT NULL            COMMENT '最后登录时间',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C端客户表';

-- -------------------------------------------
-- 8. 用户-公司关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_company`;
CREATE TABLE `sys_user_company` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '用户ID',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '公司ID',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '是否默认公司（1=是，0=否）',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_company` (`user_id`, `company_id`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-公司关联表';

-- -------------------------------------------
-- 9. 角色表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '归属公司ID',
  `role_name`   varchar(64)      NOT NULL                COMMENT '角色名称',
  `role_key`    varchar(64)      NOT NULL                COMMENT '角色标识',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '数据范围（ALL/REGION/SELF）',
  `role_type`   tinyint unsigned DEFAULT 0               COMMENT '角色类型（0=自定义角色，1=公司管理员角色，2=模板角色）',
  `is_system`   tinyint unsigned DEFAULT 0               COMMENT '是否系统角色（1=是，不可删除）',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '状态（1=正常，0=停用）',
  `order_num`   int              DEFAULT 0               COMMENT '排序',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_role_key` (`company_id`, `role_key`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- -------------------------------------------
-- 10. 角色模板表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template`;
CREATE TABLE `sys_role_template` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code`   varchar(32)      NOT NULL                COMMENT '公司类型编码',
  `role_name`   varchar(64)      NOT NULL                COMMENT '角色名称',
  `role_key`    varchar(64)      NOT NULL                COMMENT '角色标识',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '数据范围（ALL/REGION/SELF）',
  `is_admin`    tinyint unsigned DEFAULT 0               COMMENT '是否管理员角色模板（1=是，每种类型最多一个）',
  `order_num`   int              DEFAULT 0               COMMENT '排序',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_role_key` (`type_code`, `role_key`),
  KEY `idx_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色模板表';

-- -------------------------------------------
-- 11. 角色模板-菜单关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template_menu`;
CREATE TABLE `sys_role_template_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint unsigned  NOT NULL                COMMENT '模板ID',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '菜单ID',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_menu` (`template_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色模板-菜单关联表';

-- -------------------------------------------
-- 12. 菜单表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_type`  varchar(16)      NOT NULL                COMMENT '所属主体类型（PLATFORM/HQ/SERVICE）',
  `menu_name`     varchar(64)      NOT NULL                COMMENT '菜单名称',
  `parent_id`     bigint unsigned  DEFAULT 0               COMMENT '上级菜单ID（0为顶级）',
  `menu_type`     char(1)          NOT NULL                COMMENT '类型（M=目录，C=菜单，F=按钮）',
  `path`          varchar(128)     DEFAULT NULL            COMMENT '路由地址',
  `component`     varchar(128)     DEFAULT NULL            COMMENT '组件路径',
  `perms`         varchar(128)     DEFAULT NULL            COMMENT '权限标识（如 system:user:list）',
  `icon`          varchar(64)      DEFAULT NULL            COMMENT '图标',
  `order_num`     int              DEFAULT 0               COMMENT '排序',
  `is_visible`    tinyint unsigned DEFAULT 1               COMMENT '是否可见（1=是，0=否）',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '状态（1=正常，0=停用）',
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

-- -------------------------------------------
-- 13. 公司类型-菜单上限表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_type_code_menu`;
CREATE TABLE `sys_type_code_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code`   varchar(32)      NOT NULL                COMMENT '公司类型编码',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '菜单ID',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code_menu` (`type_code`, `menu_id`),
  KEY `idx_type_code` (`type_code`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司类型-菜单上限表';

-- -------------------------------------------
-- 14. 角色-菜单关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`     bigint unsigned  NOT NULL                COMMENT '角色ID',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '菜单ID',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色-菜单关联表';

-- -------------------------------------------
-- 15. 用户-角色关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '用户ID',
  `role_id`     bigint unsigned  NOT NULL                COMMENT '角色ID',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-角色关联表';

-- -------------------------------------------
-- 16. 用户-大区关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_region`;
CREATE TABLE `sys_user_region` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '用户ID',
  `region_id`   bigint unsigned  NOT NULL                COMMENT '大区ID',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_region` (`user_id`, `region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-大区关联表';

-- -------------------------------------------
-- 17. 操作日志表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`           varchar(64)      DEFAULT NULL            COMMENT '操作模块',
  `oper_type`       tinyint unsigned DEFAULT 0               COMMENT '操作类型（0=其他，1=新增，2=修改，3=删除，4=授权，5=导出，6=登录，7=登出，8=强制下线）',
  `method`          varchar(256)     DEFAULT NULL            COMMENT '请求方法（类名.方法名）',
  `request_method`  varchar(16)      DEFAULT NULL            COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `request_url`     varchar(256)     DEFAULT NULL            COMMENT '请求URL',
  `request_param`   text             DEFAULT NULL            COMMENT '请求参数',
  `response_result` text             DEFAULT NULL            COMMENT '返回结果',
  `user_id`         bigint unsigned  DEFAULT NULL            COMMENT '操作人ID',
  `username`        varchar(64)      DEFAULT NULL            COMMENT '操作人用户名',
  `company_id`      bigint unsigned  DEFAULT NULL            COMMENT '操作人当前公司ID',
  `ip`              varchar(64)      DEFAULT NULL            COMMENT '操作IP',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '操作状态（1=成功，0=失败）',
  `error_msg`       text             DEFAULT NULL            COMMENT '错误信息',
  `oper_time`       datetime         DEFAULT NULL            COMMENT '操作时间',
  `cost_time`       bigint           DEFAULT 0               COMMENT '耗时（毫秒）',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
