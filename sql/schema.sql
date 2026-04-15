-- =============================================
-- 佳士售后系统 - 数据库DDL脚本
-- 数据库：jasic_after_sales
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_general_ci
-- 共21张表
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
  `wechat_phone`    varchar(20)      DEFAULT NULL            COMMENT '微信授权手机号快照',
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
-- 7. 微信绑定记录表
-- -------------------------------------------
DROP TABLE IF EXISTS `wechat_bind_record`;
CREATE TABLE `wechat_bind_record` (
  `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           bigint unsigned NOT NULL                COMMENT '用户ID',
  `operate_type`      varchar(32)     NOT NULL                COMMENT '操作类型（BIND/UNBIND）',
  `operate_source`    varchar(32)     NOT NULL                COMMENT '操作来源（MP_BIND_LOGIN/PC_QR_BIND/PC_SELF_UNBIND）',
  `openid`            varchar(64)     NOT NULL                COMMENT '微信openid快照',
  `wechat_phone`      varchar(20)     DEFAULT NULL            COMMENT '微信授权手机号快照',
  `operator_user_id`  bigint unsigned NOT NULL                COMMENT '操作人ID',
  `operator_username` varchar(64)     NOT NULL                COMMENT '操作人用户名',
  `operate_time`      datetime        NOT NULL                COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_operate_time` (`user_id`, `operate_time`),
  KEY `idx_openid_operate_time` (`openid`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='微信绑定记录表';

-- -------------------------------------------
-- 8. C端客户表
-- -------------------------------------------
DROP TABLE IF EXISTS `c_user`;
CREATE TABLE `c_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid`          varchar(64)      NOT NULL                COMMENT '微信openid',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '手机号（微信授权获取）',
  `nickname`        varchar(64)      DEFAULT NULL            COMMENT '昵称',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '头像URL',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '状态（1=正常，0=停用）',
  `last_login_time` datetime         DEFAULT NULL            COMMENT '最后登录时间',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C端客户表';

-- -------------------------------------------
-- 9. C端客户地址表
-- -------------------------------------------
DROP TABLE IF EXISTS `customer_address`;
CREATE TABLE `customer_address` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id`    bigint unsigned  NOT NULL                COMMENT '客户ID',
  `contact_name`   varchar(64)      NOT NULL                COMMENT '联系人',
  `contact_mobile` varchar(20)      NOT NULL                COMMENT '联系手机号',
  `province`       varchar(64)      NOT NULL                COMMENT '省',
  `city`           varchar(64)      NOT NULL                COMMENT '市',
  `county`         varchar(64)      DEFAULT NULL            COMMENT '区县',
  `detail_address` varchar(255)     NOT NULL                COMMENT '详细地址',
  `is_default`     tinyint unsigned DEFAULT 0               COMMENT '是否默认地址（1=是，0=否）',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_customer_address_customer` (`customer_id`),
  KEY `idx_customer_address_default` (`customer_id`, `is_default`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C端客户地址表';

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
-- 17. 字典类型表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
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
-- 18. 字典数据表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
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
-- 19. 参数设置表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
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
-- 20. 操作日志表
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

-- -------------------------------------------
-- 21. 工单主表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id`                          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`                    varchar(32)      NOT NULL                COMMENT '工单号',
  `customer_id`                 bigint unsigned  DEFAULT NULL            COMMENT '客户ID',
  `customer_name`               varchar(64)      NOT NULL                COMMENT '客户姓名',
  `customer_mobile`             varchar(20)      NOT NULL                COMMENT '客户手机号',
  `report_subject_type`         varchar(16)      NOT NULL                COMMENT '报修主体类型（CUSTOMER/COMPANY）',
  `report_company_id`           bigint unsigned  DEFAULT NULL            COMMENT '报修主体公司ID',
  `barcode`                     varchar(100)     DEFAULT NULL            COMMENT '机器条码',
  `product_code`                varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_name`                varchar(128)     DEFAULT NULL            COMMENT '商品名称',
  `product_model`               varchar(100)     DEFAULT NULL            COMMENT '机器型号',
  `machine_no`                  varchar(100)     DEFAULT NULL            COMMENT '机器小号',
  `brand_type`                  varchar(16)      DEFAULT NULL            COMMENT '品牌类型',
  `brand_code`                  varchar(32)      DEFAULT NULL            COMMENT '品牌编码',
  `brand_name`                  varchar(64)      DEFAULT NULL            COMMENT '品牌名称',
  `service_mode`                varchar(16)      NOT NULL                COMMENT '服务方式编码（MAIL=寄修，STORE=到店维修）',
  `warranty_status`             varchar(16)      DEFAULT NULL            COMMENT '质保状态',
  `fault_desc`                  text             DEFAULT NULL            COMMENT '客户报修描述',
  `fault_remark`                varchar(500)     DEFAULT NULL            COMMENT '客户故障备注',
  `sender_name`                 varchar(64)      DEFAULT NULL            COMMENT '寄件人姓名',
  `sender_mobile`               varchar(20)      DEFAULT NULL            COMMENT '寄件人手机号',
  `sender_address`              varchar(255)     DEFAULT NULL            COMMENT '寄件地址',
  `send_express_no`             varchar(64)      DEFAULT NULL            COMMENT '寄件快递单号',
  `main_status`                 varchar(32)      NOT NULL                COMMENT '主状态',
  `evaluate_status`             varchar(32)      NOT NULL                COMMENT '评价状态',
  `current_accept_subject_type` varchar(16)      NOT NULL                COMMENT '当前受理主体类型（SERVICE/HQ）',
  `current_accept_company_id`   bigint unsigned  NOT NULL                COMMENT '当前受理公司ID',
  `assigned_user_id`            bigint unsigned  DEFAULT NULL            COMMENT '当前维修员ID',
  `create_company_id`           bigint unsigned  NOT NULL                COMMENT '建单来源公司ID',
  `create_entry_type`           varchar(32)      DEFAULT NULL            COMMENT '建单入口类型',
  `hq_company_id`               bigint unsigned  NOT NULL                COMMENT '归属总部ID',
  `has_transfer`                tinyint unsigned DEFAULT 0               COMMENT '是否发生过转单（1=是，0=否）',
  `transfer_count`              int unsigned     DEFAULT 0               COMMENT '转单次数',
  `return_method`               varchar(16)      DEFAULT NULL            COMMENT '机器返回方式（回寄/自提）',
  `return_express_no`           varchar(64)      DEFAULT NULL            COMMENT '回寄快递单号',
  `close_reason`                varchar(255)     DEFAULT NULL            COMMENT '关闭原因',
  `completed_time`              datetime         DEFAULT NULL            COMMENT '完成时间',
  `closed_time`                 datetime         DEFAULT NULL            COMMENT '关闭时间',
  `create_time`                 datetime         NOT NULL                COMMENT '创建时间',
  `update_time`                 datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_current_accept_company` (`current_accept_company_id`),
  KEY `idx_assigned_user` (`assigned_user_id`),
  KEY `idx_hq_company` (`hq_company_id`),
  KEY `idx_main_status` (`main_status`),
  KEY `idx_report_company` (`report_company_id`),
  KEY `idx_customer_mobile` (`customer_mobile`),
  KEY `idx_barcode` (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单主表';

-- -------------------------------------------
-- 22. 工单附件表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `storage_type`      varchar(32)      NOT NULL COMMENT '存储类型',
  `bucket`            varchar(128)     NOT NULL COMMENT '存储桶',
  `object_key`        varchar(512)     NOT NULL COMMENT '对象键',
  `original_name`     varchar(255)     NOT NULL COMMENT '原始文件名',
  `content_type`      varchar(128)     DEFAULT NULL COMMENT '内容类型',
  `file_size`         bigint unsigned  NOT NULL COMMENT '文件大小',
  `file_ext`          varchar(32)      NOT NULL COMMENT '扩展名',
  `file_hash`         varchar(128)     NOT NULL COMMENT '文件哈希',
  `access_level`      varchar(32)      NOT NULL COMMENT '访问级别',
  `upload_user_id`    bigint unsigned  DEFAULT NULL COMMENT '上传用户ID',
  `upload_user_type`  varchar(32)      NOT NULL COMMENT '上传用户类型',
  `upload_company_id` bigint unsigned  DEFAULT NULL COMMENT '上传公司ID',
  `status`            varchar(32)      NOT NULL COMMENT '文件状态',
  `create_time`       datetime         NOT NULL COMMENT '创建时间',
  `update_time`       datetime         NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_file_hash_key` (`file_hash`, `object_key`),
  KEY `idx_sys_file_upload_user` (`upload_user_id`, `upload_user_type`),
  KEY `idx_sys_file_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件元数据表';

DROP TABLE IF EXISTS `sys_file_biz`;
CREATE TABLE `sys_file_biz` (
  `id`                 bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_id`            bigint unsigned  NOT NULL COMMENT '文件ID',
  `biz_type`           varchar(64)      NOT NULL COMMENT '业务类型',
  `biz_id`             bigint unsigned  NOT NULL COMMENT '业务ID',
  `sort_num`           int              NOT NULL DEFAULT 1 COMMENT '排序号',
  `is_primary`         tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否主文件',
  `company_id`         bigint unsigned  DEFAULT NULL COMMENT '公司ID',
  `operator_user_id`   bigint unsigned  DEFAULT NULL COMMENT '操作人ID',
  `operator_user_type` varchar(32)      DEFAULT NULL COMMENT '操作人类型',
  `remark`             varchar(255)     DEFAULT NULL COMMENT '备注',
  `create_time`        datetime         NOT NULL COMMENT '创建时间',
  `update_time`        datetime         NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_biz_type_id_sort` (`biz_type`, `biz_id`, `sort_num`),
  KEY `idx_sys_file_biz_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件业务关联表';

-- -------------------------------------------
-- 22. 工单流转历史表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_flow`;
CREATE TABLE `work_order_flow` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`       bigint unsigned  NOT NULL                COMMENT '工单ID',
  `action_type`         varchar(32)      NOT NULL                COMMENT '动作类型',
  `before_status`       varchar(32)      DEFAULT NULL            COMMENT '动作前主状态',
  `after_status`        varchar(32)      DEFAULT NULL            COMMENT '动作后主状态',
  `from_company_id`     bigint unsigned  DEFAULT NULL            COMMENT '来源公司ID',
  `to_company_id`       bigint unsigned  DEFAULT NULL            COMMENT '目标公司ID',
  `operator_company_id` bigint unsigned  NOT NULL                COMMENT '操作公司ID',
  `operator_user_id`    bigint unsigned  NOT NULL                COMMENT '操作人ID',
  `remark`              varchar(500)     DEFAULT NULL            COMMENT '备注',
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_action_time` (`work_order_id`, `create_time`),
  KEY `idx_to_company` (`to_company_id`),
  KEY `idx_operator_company` (`operator_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单流转历史表';

-- -------------------------------------------
-- 23. 工单参与方快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_participant`;
CREATE TABLE `work_order_participant` (
  `id`                     bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`          bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`             bigint unsigned  NOT NULL                COMMENT '参与公司ID',
  `subject_type`           varchar(16)      NOT NULL                COMMENT '主体类型（SERVICE/HQ）',
  `participate_type`       varchar(32)      NOT NULL                COMMENT '参与类型（CREATE/CURRENT/HISTORY/HQ_OBSERVER）',
  `is_current_handler`     tinyint unsigned DEFAULT 0               COMMENT '是否当前受理方（1=是，0=否）',
  `first_participate_time` datetime         NOT NULL                COMMENT '首次参与时间',
  `last_participate_time`  datetime         NOT NULL                COMMENT '最后参与时间',
  `create_time`            datetime         NOT NULL                COMMENT '创建时间',
  `update_time`            datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_company` (`work_order_id`, `company_id`),
  KEY `idx_company_current` (`company_id`, `is_current_handler`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单参与方快照表';

-- -------------------------------------------
-- 24. 工单用户级参与事实表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_user_participant`;
CREATE TABLE `work_order_user_participant` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '参与公司ID',
  `user_id`       bigint unsigned  NOT NULL                COMMENT '参与用户ID',
  `action_type`   varchar(32)      NOT NULL                COMMENT '参与动作类型（TECH_ACCEPT/QUOTE/REPAIR/REVIEW）',
  `action_time`   datetime         NOT NULL                COMMENT '动作发生时间',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_company_user` (`work_order_id`, `company_id`, `user_id`),
  KEY `idx_company_user_action_time` (`company_id`, `user_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单用户级参与事实表';

-- -------------------------------------------
-- 25. 工单报价记录表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_quote`;
CREATE TABLE `work_order_quote` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '报价公司ID',
  `quoted_by`        bigint unsigned  NOT NULL                COMMENT '报价人ID',
  `fault_judge`      varchar(255)     DEFAULT NULL            COMMENT '故障判定',
  `quote_amount`     decimal(10,2)    DEFAULT NULL            COMMENT '报价金额',
  `quote_desc`       varchar(500)     DEFAULT NULL            COMMENT '报价说明',
  `is_current_valid` tinyint unsigned DEFAULT 1               COMMENT '是否当前有效报价（1=是，0=否）',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_quote_time` (`work_order_id`, `create_time`),
  KEY `idx_quote_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单报价记录表';

-- -------------------------------------------
-- 26. 工单维修登记表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_repair`;
CREATE TABLE `work_order_repair` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`  bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`     bigint unsigned  NOT NULL                COMMENT '维修公司ID',
  `repair_user_id` bigint unsigned  NOT NULL                COMMENT '维修员ID',
  `register_stage` varchar(32)      NOT NULL DEFAULT 'REPAIR' COMMENT '登记阶段（REPAIR=维修登记，RECHECK=复检登记）',
  `is_finished`    tinyint unsigned DEFAULT 0               COMMENT '是否维修完成（1=是，0=否）',
  `finished_time`  datetime         DEFAULT NULL            COMMENT '完成时间',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_repair_time` (`work_order_id`, `create_time`),
  KEY `idx_repair_company` (`company_id`),
  KEY `idx_repair_user` (`repair_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单维修登记表';

-- -------------------------------------------
-- 27. 工单故障点记录表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_fault`;
CREATE TABLE `work_order_fault` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `repair_id`     bigint unsigned  NOT NULL                COMMENT '维修登记ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '登记公司ID',
  `fault_desc`    varchar(500)     NOT NULL                COMMENT '故障描述',
  `repair_desc`   varchar(500)     DEFAULT NULL            COMMENT '维修说明',
  `other_desc`    varchar(500)     DEFAULT NULL            COMMENT '其他维修说明',
  `part_name`     varchar(500)     DEFAULT NULL            COMMENT '配件名称',
  `part_qty`      int unsigned     DEFAULT NULL            COMMENT '配件数量',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '排序号',
  `created_by`    bigint unsigned  NOT NULL                COMMENT '登记人ID',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_repair_fault` (`repair_id`, `sort_num`),
  KEY `idx_work_order_fault_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单故障点记录表';

-- -------------------------------------------
-- 29. 工单评价表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_evaluation`;
CREATE TABLE `work_order_evaluation` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `customer_id`   bigint unsigned  NOT NULL                COMMENT '客户ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '被评价服务方公司ID',
  `timeliness_score`   tinyint unsigned DEFAULT NULL       COMMENT '服务时效评分',
  `quality_score`      tinyint unsigned DEFAULT NULL       COMMENT '维修质量评分',
  `satisfaction_score` tinyint unsigned DEFAULT NULL       COMMENT '服务满意度评分',
  `tags`          varchar(255)     DEFAULT NULL            COMMENT '标签集合',
  `content`       varchar(1000)    DEFAULT NULL            COMMENT '评价内容',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_eval` (`work_order_id`),
  KEY `idx_customer_eval` (`customer_id`),
  KEY `idx_eval_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单评价表';

-- -------------------------------------------
-- 30. 工单通知事件表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_notify_event`;
CREATE TABLE `work_order_notify_event` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '业务归属公司ID',
  `event_type`       varchar(32)      NOT NULL                COMMENT '事件类型',
  `trigger_node`     varchar(32)      NOT NULL                COMMENT '触发节点',
  `receiver_type`    varchar(32)      NOT NULL                COMMENT '接收对象类型',
  `receiver_id`      bigint unsigned  NOT NULL                COMMENT '接收对象ID',
  `title_snapshot`   varchar(255)     DEFAULT NULL            COMMENT '标题快照',
  `content_snapshot` text             DEFAULT NULL            COMMENT '内容快照',
  `send_status`      varchar(16)      NOT NULL                COMMENT '发送状态',
  `send_time`        datetime         DEFAULT NULL            COMMENT '发送时间',
  `fail_reason`      varchar(500)     DEFAULT NULL            COMMENT '失败原因',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_notify_time` (`work_order_id`, `create_time`),
  KEY `idx_receiver_status` (`receiver_id`, `send_status`),
  KEY `idx_notify_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单通知事件表';

-- -------------------------------------------
-- 30. 机器条码档案表
-- -------------------------------------------
DROP TABLE IF EXISTS `machine_barcode`;
CREATE TABLE `machine_barcode` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode`         varchar(100)     NOT NULL                COMMENT '机器条码',
  `deliver_number`  varchar(50)      DEFAULT NULL            COMMENT '发货单号',
  `hq_company_id`   bigint unsigned  DEFAULT NULL            COMMENT '归属总部ID',
  `cust_id`         varchar(64)      DEFAULT NULL            COMMENT 'CRM公司ID',
  `sales_org`       varchar(64)      DEFAULT NULL            COMMENT '销售组织',
  `product_code`    varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_name`    varchar(128)     DEFAULT NULL            COMMENT '商品名称',
  `product_model`   varchar(100)     DEFAULT NULL            COMMENT '产品型号',
  `machine_no`      varchar(100)     DEFAULT NULL            COMMENT '机器小号',
  `brand_code`      varchar(32)      DEFAULT NULL            COMMENT '品牌编码',
  `scan_date`       datetime         DEFAULT NULL            COMMENT '条码扫描时间',
  `dealer_out_date` datetime         DEFAULT NULL            COMMENT '经销商最新出库日期',
  `crm_add_time`    datetime         DEFAULT NULL            COMMENT 'CRM创建时间',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '最近同步时间',
  `warranty_status` varchar(16)      DEFAULT NULL            COMMENT '质保状态',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_machine_barcode_hq` (`hq_company_id`),
  KEY `idx_machine_barcode_product` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机器条码档案表';

-- 31. CRM 公司映射表
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_company_mapping`;
CREATE TABLE `crm_company_mapping` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cust_id`       varchar(64)      DEFAULT NULL            COMMENT 'CRM公司ID',
  `sales_org`     varchar(64)      DEFAULT NULL            COMMENT '销售组织',
  `hq_company_id` bigint unsigned  DEFAULT NULL            COMMENT '归属总部ID',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_company_mapping_cust` (`cust_id`),
  UNIQUE KEY `uk_crm_company_mapping_sales_org` (`sales_org`),
  KEY `idx_crm_company_mapping_hq` (`hq_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM公司映射表';

-- -------------------------------------------
-- 32. CRM 总部-一级签约快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_hq_first_contract_snapshot`;
CREATE TABLE `crm_hq_first_contract_snapshot` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kunnr`            varchar(64)      NOT NULL                COMMENT 'CRM客户编码',
  `cust_id`          bigint unsigned  DEFAULT NULL            COMMENT 'CRM企业ID',
  `crm_company_name` varchar(200)     DEFAULT NULL            COMMENT 'CRM企业名称',
  `sales_org`        varchar(64)      NOT NULL                COMMENT '销售组织',
  `region_code`      varchar(64)      DEFAULT NULL            COMMENT 'CRM大区编码',
  `region_name`      varchar(100)     DEFAULT NULL            COMMENT 'CRM大区名称',
  `alive_flag`       tinyint          DEFAULT NULL            COMMENT 'CRM有效标识',
  `crm_add_time`     datetime         DEFAULT NULL            COMMENT 'CRM新增时间',
  `crm_oper_time`    datetime         DEFAULT NULL            COMMENT 'CRM操作时间',
  `last_sync_time`   datetime         DEFAULT NULL            COMMENT '最近同步时间',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_hq_first_contract` (`kunnr`, `sales_org`),
  KEY `idx_crm_hq_first_contract_sales_org` (`sales_org`),
  KEY `idx_crm_hq_first_contract_region_code` (`region_code`),
  KEY `idx_crm_hq_first_contract_oper_time` (`crm_oper_time`),
  KEY `idx_crm_hq_first_contract_add_time` (`crm_add_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM总部-一级签约快照表';

-- -------------------------------------------
-- 28. 故障与维修配置表
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config`;
CREATE TABLE `fault_repair_config` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '归属总部ID',
  `product_code`  varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_model` varchar(64)      DEFAULT NULL            COMMENT '产品型号',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fault_repair_config_product` (`company_id`, `product_code`, `product_model`),
  KEY `idx_fault_repair_config_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置表';

-- -------------------------------------------
-- 29. 故障与维修配置故障项表
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_fault`;
CREATE TABLE `fault_repair_config_fault` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id`   bigint unsigned NOT NULL                COMMENT '配置ID',
  `fault_desc`  varchar(500)    NOT NULL                COMMENT '故障描述',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '排序号',
  `create_time` datetime        NOT NULL                COMMENT '创建时间',
  `update_time` datetime        NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_fault` (`config_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置故障项表';

-- -------------------------------------------
-- 30. 故障与维修配置维修项表
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_option`;
CREATE TABLE `fault_repair_config_option` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fault_id`    bigint unsigned NOT NULL                COMMENT '故障项ID',
  `repair_desc` varchar(500)    NOT NULL                COMMENT '维修说明',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '排序号',
  `create_time` datetime        NOT NULL                COMMENT '创建时间',
  `update_time` datetime        NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_option_fault` (`fault_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置维修项表';

-- -------------------------------------------
-- 31a. CRM 公司快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_biz_company_snapshot`;
CREATE TABLE `crm_biz_company_snapshot` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cust_id`             bigint unsigned  NOT NULL                COMMENT 'CRM客户ID',
  `cust_name`           varchar(200)     DEFAULT NULL            COMMENT '客户名称',
  `juristic_cust_id`    varchar(50)      DEFAULT NULL            COMMENT '联系人',
  `group_contact_phone` varchar(50)      DEFAULT NULL            COMMENT '联系电话',
  `cellphone`           varchar(50)      DEFAULT NULL            COMMENT '手机',
  `company_address`     varchar(200)     DEFAULT NULL            COMMENT '公司地址',
  `cust_state`          int              DEFAULT NULL            COMMENT '客户状态',
  `add_date`            datetime         DEFAULT NULL            COMMENT 'CRM新增时间',
  `oper_time`           datetime         DEFAULT NULL            COMMENT 'CRM操作时间',
  `last_sync_time`      datetime         DEFAULT NULL            COMMENT '最近同步时间',
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_company_snapshot_cust` (`cust_id`),
  KEY `idx_crm_biz_company_snapshot_name` (`cust_name`),
  KEY `idx_crm_biz_company_snapshot_oper` (`oper_time`),
  KEY `idx_crm_biz_company_snapshot_add` (`add_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM公司快照表';

-- -------------------------------------------
-- 31b. CRM 销售出库扫码快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_warehouse_scan_outstorage_snapshot`;
CREATE TABLE `crm_warehouse_scan_outstorage_snapshot` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`       bigint unsigned  NOT NULL                COMMENT 'CRM原始主键',
  `ware_id`         bigint unsigned  DEFAULT NULL            COMMENT '出入库ID',
  `warehouse_id`    bigint unsigned  DEFAULT NULL            COMMENT '仓库ID',
  `scan_code`       varchar(30)      DEFAULT NULL            COMMENT '条码',
  `scan_date`       datetime         DEFAULT NULL            COMMENT '扫码时间',
  `cust_id`         bigint unsigned  DEFAULT NULL            COMMENT '企业ID',
  `product_numeric` varchar(50)      DEFAULT NULL            COMMENT '产品编码',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '最近同步时间',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_warehouse_scan_outstorage_source` (`source_id`),
  KEY `idx_crm_warehouse_scan_outstorage_code` (`scan_code`),
  KEY `idx_crm_warehouse_scan_outstorage_date` (`scan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM销售出库扫码快照表';

-- -------------------------------------------
-- 32. 同步任务表
-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task`;
CREATE TABLE `sync_task` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_code`       varchar(64)      NOT NULL                COMMENT '任务编码',
  `task_name`       varchar(128)     NOT NULL                COMMENT '任务名称',
  `handler_code`    varchar(64)      NOT NULL                COMMENT '处理器编码',
  `cron_expression` varchar(128)     NOT NULL                COMMENT 'Cron表达式',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_task_code` (`task_code`),
  UNIQUE KEY `uk_sync_task_handler` (`handler_code`),
  KEY `idx_sync_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务表';

-- -------------------------------------------
-- 33. 同步任务日志表
-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task_log`;
CREATE TABLE `sync_task_log` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`         bigint unsigned NOT NULL                COMMENT '任务ID',
  `status`          varchar(16)     NOT NULL                COMMENT '执行状态',
  `start_time`      datetime        NOT NULL                COMMENT '开始时间',
  `end_time`        datetime        DEFAULT NULL            COMMENT '结束时间',
  `data_start_time` datetime        DEFAULT NULL            COMMENT '数据开始时间',
  `data_end_time`   datetime        DEFAULT NULL            COMMENT '数据结束时间',
  `message`         varchar(1000)   DEFAULT NULL            COMMENT '执行信息',
  `create_time`     datetime        NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_task_log_task` (`task_id`, `id`),
  KEY `idx_sync_task_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务日志表';

SET FOREIGN_KEY_CHECKS = 1;

