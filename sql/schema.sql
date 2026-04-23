-- =============================================
-- 佳士售后系统 - 数据库DDL脚本
-- 数据库：jasic_after_sales
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_general_ci
-- 鍏?1寮犺〃
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------
-- 1. 鍏徃绫诲瀷瀛楀吀琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company_type`;
CREATE TABLE `sys_company_type` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code`    varchar(32)      NOT NULL                COMMENT '绫诲瀷缂栫爜锛圥LATFORM/HQ_A/HQ_B/HQ_C/HQ_D/FIRST/SECOND锛?,
  `type_name`    varchar(64)      NOT NULL                COMMENT '类型名称',
  `subject_type` varchar(16)      NOT NULL                COMMENT '涓讳綋绫诲瀷锛圥LATFORM/HQ/SERVICE锛?,
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '备注',
  `order_num`    int              DEFAULT 0               COMMENT '排序',
  `create_time`  datetime         NOT NULL                COMMENT '创建时间',
  `update_time`  datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍏徃绫诲瀷瀛楀吀琛?;

-- -------------------------------------------
-- 2. 中国行政区划标准表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_area`;
CREATE TABLE `sys_area` (
  `area_code`    varchar(6)       NOT NULL                COMMENT '行政区编码',
  `area_name`    varchar(64)      NOT NULL                COMMENT '行政区名称',
  `parent_code`  varchar(6)       NOT NULL                COMMENT '父级编码',
  `area_level`   varchar(16)      NOT NULL                COMMENT '层级(PROVINCE/CITY/DISTRICT)',
  `full_name`    varchar(255)     DEFAULT NULL            COMMENT '完整名称',
  `sort_num`     int              DEFAULT 0               COMMENT '排序',
  `status`       tinyint unsigned DEFAULT 1               COMMENT '状态(1=启用,0=停用)',
  `create_time`  datetime         NOT NULL                COMMENT '创建时间',
  `update_time`  datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`area_code`),
  KEY `idx_sys_area_parent` (`parent_code`, `sort_num`),
  KEY `idx_sys_area_level` (`area_level`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='中国行政区划标准表';

-- -------------------------------------------
-- 3. 公司表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company`;
CREATE TABLE `sys_company` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_name`     varchar(128)     NOT NULL                COMMENT '公司名称',
  `company_short_name` varchar(128)   DEFAULT NULL            COMMENT '公司简称',
  `company_code`     varchar(64)      DEFAULT NULL            COMMENT '公司编码',
  `type_code`        varchar(32)      NOT NULL                COMMENT '公司类型编码',
  `contact_name`     varchar(64)      NOT NULL                COMMENT '联系人',
  `contact_phone`    varchar(20)      NOT NULL                COMMENT '联系电话',
  `province_code`    varchar(6)       NOT NULL                COMMENT '省份编码',
  `province_name`    varchar(64)      NOT NULL                COMMENT '省份名称',
  `city_code`        varchar(6)       NOT NULL                COMMENT '城市编码',
  `city_name`        varchar(64)      NOT NULL                COMMENT '城市名称',
  `district_code`    varchar(6)       NOT NULL                COMMENT '区县编码',
  `district_name`    varchar(64)      NOT NULL                COMMENT '区县名称',
  `detail_address`   varchar(255)     NOT NULL                COMMENT '详细地址',
  `full_address`     varchar(255)     DEFAULT NULL            COMMENT '完整地址',
  `geocode_status`   varchar(16)      NOT NULL                COMMENT '地理解析状态',
  `longitude`        decimal(10,6)    DEFAULT NULL            COMMENT '经度',
  `latitude`         decimal(10,6)    DEFAULT NULL            COMMENT '纬度',
  `service_phone`    varchar(32)      DEFAULT NULL            COMMENT '客服电话',
  `source_type`      varchar(16)      NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  `sales_org`        varchar(64)      DEFAULT NULL            COMMENT '销售组织',
  `status`           tinyint unsigned DEFAULT 1               COMMENT '状态(1=正常,0=停用)',
  `remark`           varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_code` (`company_code`),
  UNIQUE KEY `uk_company_sales_org` (`sales_org`),
  KEY `idx_type_code` (`type_code`),
  KEY `idx_company_region` (`province_code`, `city_code`, `district_code`),
  KEY `idx_company_geocode_status` (`geocode_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司表';

-- -------------------------------------------
-- 4. 澶у尯琛?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='澶у尯琛?;

-- -------------------------------------------
-- 4. 总部-一级签约关系表
-- -------------------------------------------
DROP TABLE IF EXISTS `hq_first_contract`;
CREATE TABLE `hq_first_contract` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `hq_company_id`    bigint unsigned  NOT NULL                COMMENT '总部公司ID',
  `first_company_id` bigint unsigned  NOT NULL                COMMENT '一级网点公司ID',
  `region_id`        bigint unsigned  DEFAULT NULL            COMMENT '澶у尯ID锛堢绾︽椂缁戝畾锛?,
  `contract_no`      varchar(64)      DEFAULT NULL            COMMENT '合同编号',
  `status`           tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鏈夋晥锛?=缁堟锛?,
  `remark`           varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hq_first` (`hq_company_id`, `first_company_id`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='总部-一级签约关系表';

-- -------------------------------------------
-- 5. 涓€绾?浜岀骇浠庡睘鍏崇郴琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `first_second_relation`;
CREATE TABLE `first_second_relation` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `first_company_id`  bigint unsigned  NOT NULL                COMMENT '一级网点公司ID',
  `second_company_id` bigint unsigned  NOT NULL                COMMENT '二级网点公司ID',
  `status`            tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鏈夋晥锛?=瑙ｉ櫎锛?,
  `remark`            varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`       datetime         NOT NULL                COMMENT '创建时间',
  `update_time`       datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_first_second` (`first_company_id`, `second_company_id`),
  UNIQUE KEY `uk_second` (`second_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涓€绾?浜岀骇浠庡睘鍏崇郴琛?;

-- -------------------------------------------
-- 6. B端员工表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        varchar(64)      NOT NULL                COMMENT '鐧诲綍鐢ㄦ埛鍚?,
  `password`        varchar(128)     NOT NULL                COMMENT '瀵嗙爜锛圔Crypt鍔犲瘑锛?,
  `real_name`       varchar(64)      DEFAULT NULL            COMMENT '真实姓名',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '鎵嬫満鍙?,
  `email`           varchar(64)      DEFAULT NULL            COMMENT '邮箱',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '头像URL',
  `openid`          varchar(64)      DEFAULT NULL            COMMENT '寰俊openid锛堝皬绋嬪簭鐧诲綍缁戝畾锛?,
  `wechat_phone`    varchar(20)      DEFAULT NULL            COMMENT '寰俊鎺堟潈鎵嬫満鍙峰揩鐓?,
  `sex`             tinyint unsigned DEFAULT 0               COMMENT '鎬у埆锛?=鏈煡锛?=鐢凤紝2=濂筹級',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `is_deleted`      tinyint unsigned DEFAULT 0               COMMENT '鏄惁鍒犻櫎锛堥€昏緫鍒犻櫎锛?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `last_login_time` datetime         DEFAULT NULL            COMMENT '鏈€鍚庣櫥褰曟椂闂?,
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='B端员工表';

-- -------------------------------------------
-- 7. 寰俊缁戝畾璁板綍琛?-- -------------------------------------------
DROP TABLE IF EXISTS `wechat_bind_record`;
CREATE TABLE `wechat_bind_record` (
  `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           bigint unsigned NOT NULL                COMMENT '用户ID',
  `operate_type`      varchar(32)     NOT NULL                COMMENT '鎿嶄綔绫诲瀷锛圔IND/UNBIND锛?,
  `operate_source`    varchar(32)     NOT NULL                COMMENT '鎿嶄綔鏉ユ簮锛圡P_BIND_LOGIN/PC_QR_BIND/PC_SELF_UNBIND锛?,
  `openid`            varchar(64)     NOT NULL                COMMENT '微信openid快照',
  `wechat_phone`      varchar(20)     DEFAULT NULL            COMMENT '寰俊鎺堟潈鎵嬫満鍙峰揩鐓?,
  `operator_user_id`  bigint unsigned NOT NULL                COMMENT '操作人ID',
  `operator_username` varchar(64)     NOT NULL                COMMENT '操作人用户名',
  `operate_time`      datetime        NOT NULL                COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_operate_time` (`user_id`, `operate_time`),
  KEY `idx_openid_operate_time` (`openid`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='寰俊缁戝畾璁板綍琛?;

-- -------------------------------------------
-- 8. C端客户表
-- -------------------------------------------
DROP TABLE IF EXISTS `c_user`;
CREATE TABLE `c_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid`          varchar(64)      NOT NULL                COMMENT '微信openid',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '鎵嬫満鍙凤紙寰俊鎺堟潈鑾峰彇锛?,
  `nickname`        varchar(64)      DEFAULT NULL            COMMENT '昵称',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '头像URL',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `last_login_time` datetime         DEFAULT NULL            COMMENT '鏈€鍚庣櫥褰曟椂闂?,
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C端客户表';

-- -------------------------------------------
-- 9. C绔鎴峰湴鍧€琛?-- -------------------------------------------
DROP TABLE IF EXISTS `customer_address`;
CREATE TABLE `customer_address` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id`    bigint unsigned  NOT NULL                COMMENT '客户ID',
  `contact_name`   varchar(64)      NOT NULL                COMMENT '鑱旂郴浜?,
  `contact_mobile` varchar(20)      NOT NULL                COMMENT '鑱旂郴鎵嬫満鍙?,
  `province`       varchar(64)      NOT NULL                COMMENT '鐪?,
  `city`           varchar(64)      NOT NULL                COMMENT '甯?,
  `county`         varchar(64)      DEFAULT NULL            COMMENT '区县',
  `detail_address` varchar(255)     NOT NULL                COMMENT '详细地址',
  `is_default`     tinyint unsigned DEFAULT 0               COMMENT '鏄惁榛樿鍦板潃锛?=鏄紝0=鍚︼級',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_customer_address_customer` (`customer_id`),
  KEY `idx_customer_address_default` (`customer_id`, `is_default`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C绔鎴峰湴鍧€琛?;

-- -------------------------------------------
-- 8. 鐢ㄦ埛-鍏徃鍏宠仈琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_company`;
CREATE TABLE `sys_user_company` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '用户ID',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '公司ID',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '鏄惁榛樿鍏徃锛?=鏄紝0=鍚︼級',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_company` (`user_id`, `company_id`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛-鍏徃鍏宠仈琛?;

-- -------------------------------------------
-- 9. 瑙掕壊琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '归属公司ID',
  `role_name`   varchar(64)      NOT NULL                COMMENT '角色名称',
  `role_key`    varchar(64)      NOT NULL                COMMENT '角色标识',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '鏁版嵁鑼冨洿锛圓LL/REGION/SELF锛?,
  `role_type`   tinyint unsigned DEFAULT 0               COMMENT '瑙掕壊绫诲瀷锛?=鑷畾涔夎鑹诧紝1=鍏徃绠＄悊鍛樿鑹诧紝2=妯℃澘瑙掕壊锛?,
  `is_system`   tinyint unsigned DEFAULT 0               COMMENT '鏄惁绯荤粺瑙掕壊锛?=鏄紝涓嶅彲鍒犻櫎锛?,
  `status`      tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `order_num`   int              DEFAULT 0               COMMENT '排序',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_role_key` (`company_id`, `role_key`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊琛?;

-- -------------------------------------------
-- 10. 瑙掕壊妯℃澘琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template`;
CREATE TABLE `sys_role_template` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_code`   varchar(32)      NOT NULL                COMMENT '公司类型编码',
  `role_name`   varchar(64)      NOT NULL                COMMENT '角色名称',
  `role_key`    varchar(64)      NOT NULL                COMMENT '角色标识',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '鏁版嵁鑼冨洿锛圓LL/REGION/SELF锛?,
  `is_admin`    tinyint unsigned DEFAULT 0               COMMENT '是否管理员角色模板（1=是，每种类型最多一个）',
  `order_num`   int              DEFAULT 0               COMMENT '排序',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_role_key` (`type_code`, `role_key`),
  KEY `idx_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊妯℃澘琛?;

-- -------------------------------------------
-- 11. 瑙掕壊妯℃澘-鑿滃崟鍏宠仈琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template_menu`;
CREATE TABLE `sys_role_template_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint unsigned  NOT NULL                COMMENT '妯℃澘ID',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '菜单ID',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_menu` (`template_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊妯℃澘-鑿滃崟鍏宠仈琛?;

-- -------------------------------------------
-- 12. 鑿滃崟琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_type`  varchar(16)      NOT NULL                COMMENT '鎵€灞炰富浣撶被鍨嬶紙PLATFORM/HQ/SERVICE锛?,
  `menu_name`     varchar(64)      NOT NULL                COMMENT '菜单名称',
  `parent_id`     bigint unsigned  DEFAULT 0               COMMENT '涓婄骇鑿滃崟ID锛?涓洪《绾э級',
  `menu_type`     char(1)          NOT NULL                COMMENT '绫诲瀷锛圡=鐩綍锛孋=鑿滃崟锛孎=鎸夐挳锛?,
  `path`          varchar(128)     DEFAULT NULL            COMMENT '路由地址',
  `component`     varchar(128)     DEFAULT NULL            COMMENT '组件路径',
  `perms`         varchar(128)     DEFAULT NULL            COMMENT '鏉冮檺鏍囪瘑锛堝 system:user:list锛?,
  `icon`          varchar(64)      DEFAULT NULL            COMMENT '图标',
  `order_num`     int              DEFAULT 0               COMMENT '排序',
  `is_visible`    tinyint unsigned DEFAULT 1               COMMENT '鏄惁鍙锛?=鏄紝0=鍚︼級',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鑿滃崟琛?;

-- -------------------------------------------
-- 13. 鍏徃绫诲瀷-鑿滃崟涓婇檺琛?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍏徃绫诲瀷-鑿滃崟涓婇檺琛?;

-- -------------------------------------------
-- 14. 瑙掕壊-鑿滃崟鍏宠仈琛?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊-鑿滃崟鍏宠仈琛?;

-- -------------------------------------------
-- 15. 鐢ㄦ埛-瑙掕壊鍏宠仈琛?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛-瑙掕壊鍏宠仈琛?;

-- -------------------------------------------
-- 16. 鐢ㄦ埛-澶у尯鍏宠仈琛?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛-澶у尯鍏宠仈琛?;

-- -------------------------------------------
-- 17. 瀛楀吀绫诲瀷琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_name`   varchar(100)     NOT NULL                COMMENT '字典名称',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '字典类型',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀛楀吀绫诲瀷琛?;

-- -------------------------------------------
-- 18. 瀛楀吀鏁版嵁琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '字典类型',
  `dict_label`  varchar(100)     NOT NULL                COMMENT '字典标签',
  `dict_value`  varchar(100)     NOT NULL                COMMENT '瀛楀吀閿€?,
  `dict_sort`   int              DEFAULT 0               COMMENT '排序',
  `css_class`   varchar(100)     DEFAULT NULL            COMMENT '鑷畾涔夋牱寮?,
  `list_class`  varchar(100)     DEFAULT NULL            COMMENT '标签样式',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '鏄惁榛樿锛?=鏄紝0=鍚︼級',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time` datetime         NOT NULL                COMMENT '创建时间',
  `update_time` datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_value` (`dict_type`, `dict_value`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀛楀吀鏁版嵁琛?;

-- -------------------------------------------
-- 19. 鍙傛暟璁剧疆琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name`  varchar(100)     NOT NULL                COMMENT '参数名称',
  `config_key`   varchar(100)     NOT NULL                COMMENT '参数键名',
  `config_value` text             NOT NULL                COMMENT '鍙傛暟閿€?,
  `config_type`  tinyint unsigned DEFAULT 0               COMMENT '鏄惁鍐呯疆锛?=鏄紝0=鍚︼級',
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`  datetime         NOT NULL                COMMENT '创建时间',
  `update_time`  datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍙傛暟璁剧疆琛?;

-- -------------------------------------------
-- 20. 鎿嶄綔鏃ュ織琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`           varchar(64)      DEFAULT NULL            COMMENT '操作模块',
  `oper_type`       tinyint unsigned DEFAULT 0               COMMENT '鎿嶄綔绫诲瀷锛?=鍏朵粬锛?=鏂板锛?=淇敼锛?=鍒犻櫎锛?=鎺堟潈锛?=瀵煎嚭锛?=鐧诲綍锛?=鐧诲嚭锛?=寮哄埗涓嬬嚎锛?,
  `method`          varchar(256)     DEFAULT NULL            COMMENT '璇锋眰鏂规硶锛堢被鍚?鏂规硶鍚嶏級',
  `request_method`  varchar(16)      DEFAULT NULL            COMMENT '璇锋眰鏂瑰紡锛圙ET/POST/PUT/DELETE锛?,
  `request_url`     varchar(256)     DEFAULT NULL            COMMENT '请求URL',
  `request_param`   text             DEFAULT NULL            COMMENT '请求参数',
  `response_result` text             DEFAULT NULL            COMMENT '返回结果',
  `user_id`         bigint unsigned  DEFAULT NULL            COMMENT '操作人ID',
  `username`        varchar(64)      DEFAULT NULL            COMMENT '操作人用户名',
  `company_id`      bigint unsigned  DEFAULT NULL            COMMENT '操作人当前公司ID',
  `ip`              varchar(64)      DEFAULT NULL            COMMENT '操作IP',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鎿嶄綔鐘舵€侊紙1=鎴愬姛锛?=澶辫触锛?,
  `error_msg`       text             DEFAULT NULL            COMMENT '错误信息',
  `oper_time`       datetime         DEFAULT NULL            COMMENT '操作时间',
  `cost_time`       bigint           DEFAULT 0               COMMENT '耗时（毫秒）',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鎿嶄綔鏃ュ織琛?;

-- -------------------------------------------
-- 21. 工单主表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id`                          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`                    varchar(32)      NOT NULL                COMMENT '宸ュ崟鍙?,
  `customer_id`                 bigint unsigned  DEFAULT NULL            COMMENT '客户ID',
  `customer_name`               varchar(64)      NOT NULL                COMMENT '客户姓名',
  `customer_mobile`             varchar(20)      NOT NULL                COMMENT '瀹㈡埛鎵嬫満鍙?,
  `report_subject_type`         varchar(16)      NOT NULL                COMMENT '鎶ヤ慨涓讳綋绫诲瀷锛圕USTOMER/COMPANY锛?,
  `report_company_id`           bigint unsigned  DEFAULT NULL            COMMENT '报修主体公司ID',
  `barcode`                     varchar(100)     DEFAULT NULL            COMMENT '机器条码',
  `product_code`                varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_name`                varchar(128)     DEFAULT NULL            COMMENT '商品名称',
  `product_model`               varchar(100)     DEFAULT NULL            COMMENT '机器型号',
  `machine_no`                  varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒灏忓彿',
  `brand_type`                  varchar(16)      DEFAULT NULL            COMMENT '品牌类型',
  `brand_code`                  varchar(32)      DEFAULT NULL            COMMENT '品牌编码',
  `brand_name`                  varchar(64)      DEFAULT NULL            COMMENT '品牌名称',
  `service_mode`                varchar(16)      NOT NULL                COMMENT '鏈嶅姟鏂瑰紡缂栫爜锛圡AIL=瀵勪慨锛孲TORE=鍒板簵缁翠慨锛?,
  `warranty_status`             varchar(16)      DEFAULT NULL            COMMENT '璐ㄤ繚鐘舵€?,
  `fault_desc`                  text             DEFAULT NULL            COMMENT '客户报修描述',
  `fault_remark`  varchar(500)     DEFAULT NULL            COMMENT '其它故障说明',
  `sender_name`                 varchar(64)      DEFAULT NULL            COMMENT '瀵勪欢浜哄鍚?,
  `sender_mobile`               varchar(20)      DEFAULT NULL            COMMENT '寄件人手机号',
  `sender_address`              varchar(255)     DEFAULT NULL            COMMENT '寄件地址',
  `send_express_no`             varchar(64)      DEFAULT NULL            COMMENT '瀵勪欢蹇€掑崟鍙?,
  `main_status`                 varchar(32)      NOT NULL                COMMENT '涓荤姸鎬?,
  `evaluate_status`             varchar(32)      NOT NULL                COMMENT '璇勪环鐘舵€?,
  `current_accept_subject_type` varchar(16)      NOT NULL                COMMENT '褰撳墠鍙楃悊涓讳綋绫诲瀷锛圫ERVICE/HQ锛?,
  `current_accept_company_id`   bigint unsigned  NOT NULL                COMMENT '当前受理公司ID',
  `assigned_user_id`            bigint unsigned  DEFAULT NULL            COMMENT '当前维修员ID',
  `create_company_id`           bigint unsigned  NOT NULL                COMMENT '建单来源公司ID',
  `create_entry_type`           varchar(32)      DEFAULT NULL            COMMENT '建单入口类型',
  `hq_company_id`               bigint unsigned  NOT NULL                COMMENT '归属总部ID',
  `fault_repair_config_id`      bigint unsigned  DEFAULT NULL            COMMENT '绑定的故障与维修配置ID',
  `has_transfer`                tinyint unsigned DEFAULT 0               COMMENT '是否发生过转单（1=是，0=否）',
  `transfer_count`              int unsigned     DEFAULT 0               COMMENT '转单次数',
  `return_method`               varchar(16)      DEFAULT NULL            COMMENT '鏈哄櫒杩斿洖鏂瑰紡锛堝洖瀵?鑷彁锛?,
  `return_express_no`           varchar(64)      DEFAULT NULL            COMMENT '鍥炲瘎蹇€掑崟鍙?,
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
  KEY `idx_fault_repair_config` (`fault_repair_config_id`),
  KEY `idx_main_status` (`main_status`),
  KEY `idx_report_company` (`report_company_id`),
  KEY `idx_customer_id` (`customer_id`, `id`),
  KEY `idx_customer_mobile` (`customer_mobile`),
  KEY `idx_barcode` (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单主表';

-- -------------------------------------------
-- 22. 宸ュ崟闄勪欢琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `storage_type`      varchar(32)      NOT NULL COMMENT '存储类型',
  `bucket`            varchar(128)     NOT NULL COMMENT '瀛樺偍妗?,
  `object_key`        varchar(512)     NOT NULL COMMENT '瀵硅薄閿?,
  `original_name`     varchar(255)     NOT NULL COMMENT '鍘熷鏂囦欢鍚?,
  `content_type`      varchar(128)     DEFAULT NULL COMMENT '内容类型',
  `file_size`         bigint unsigned  NOT NULL COMMENT '文件大小',
  `file_ext`          varchar(32)      NOT NULL COMMENT '鎵╁睍鍚?,
  `file_hash`         varchar(128)     NOT NULL COMMENT '文件哈希',
  `access_level`      varchar(32)      NOT NULL COMMENT '访问级别',
  `upload_user_id`    bigint unsigned  DEFAULT NULL COMMENT '上传用户ID',
  `upload_user_type`  varchar(32)      NOT NULL COMMENT '上传用户类型',
  `upload_company_id` bigint unsigned  DEFAULT NULL COMMENT '上传公司ID',
  `status`            varchar(32)      NOT NULL COMMENT '鏂囦欢鐘舵€?,
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
  `biz_id`             bigint unsigned  NOT NULL COMMENT '涓氬姟ID',
  `sort_num`           int              NOT NULL DEFAULT 1 COMMENT '鎺掑簭鍙?,
  `is_primary`         tinyint unsigned NOT NULL DEFAULT 0 COMMENT '鏄惁涓绘枃浠?,
  `company_id`         bigint unsigned  DEFAULT NULL COMMENT '公司ID',
  `operator_user_id`   bigint unsigned  DEFAULT NULL COMMENT '操作人ID',
  `operator_user_type` varchar(32)      DEFAULT NULL COMMENT '鎿嶄綔浜虹被鍨?,
  `remark`             varchar(255)     DEFAULT NULL COMMENT '备注',
  `create_time`        datetime         NOT NULL COMMENT '创建时间',
  `update_time`        datetime         NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_biz_type_id_sort` (`biz_type`, `biz_id`, `sort_num`),
  KEY `idx_sys_file_biz_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏂囦欢涓氬姟鍏宠仈琛?;

-- -------------------------------------------
-- 22. 宸ュ崟娴佽浆鍘嗗彶琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_flow`;
CREATE TABLE `work_order_flow` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`       bigint unsigned  NOT NULL                COMMENT '工单ID',
  `action_type`         varchar(32)      NOT NULL                COMMENT '动作类型',
  `before_status`       varchar(32)      DEFAULT NULL            COMMENT '鍔ㄤ綔鍓嶄富鐘舵€?,
  `after_status`        varchar(32)      DEFAULT NULL            COMMENT '鍔ㄤ綔鍚庝富鐘舵€?,
  `from_company_id`     bigint unsigned  DEFAULT NULL            COMMENT '来源公司ID',
  `to_company_id`       bigint unsigned  DEFAULT NULL            COMMENT '目标公司ID',
  `operator_company_id` bigint unsigned  NOT NULL                COMMENT '操作公司ID',
  `operator_user_id`    bigint unsigned  NOT NULL                COMMENT '操作人ID',
  `remark`              varchar(500)     DEFAULT NULL            COMMENT '备注',
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_action_time` (`work_order_id`, `create_time`),
  KEY `idx_action_to_company_order_time` (`action_type`, `to_company_id`, `work_order_id`, `create_time`),
  KEY `idx_to_company` (`to_company_id`),
  KEY `idx_operator_company` (`operator_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟娴佽浆鍘嗗彶琛?;

-- -------------------------------------------
-- 23. 工单参与方快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_participant`;
CREATE TABLE `work_order_participant` (
  `id`                     bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`          bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`             bigint unsigned  NOT NULL                COMMENT '参与公司ID',
  `subject_type`           varchar(16)      NOT NULL                COMMENT '涓讳綋绫诲瀷锛圫ERVICE/HQ锛?,
  `participate_type`       varchar(32)      NOT NULL                COMMENT '鍙備笌绫诲瀷锛圕REATE/CURRENT/HISTORY/HQ_OBSERVER锛?,
  `is_current_handler`     tinyint unsigned DEFAULT 0               COMMENT '是否当前受理方（1=是，0=否）',
  `first_participate_time` datetime         NOT NULL                COMMENT '首次参与时间',
  `last_participate_time`  datetime         NOT NULL                COMMENT '鏈€鍚庡弬涓庢椂闂?,
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
  `action_type`   varchar(32)      NOT NULL                COMMENT '鍙備笌鍔ㄤ綔绫诲瀷锛圱ECH_ACCEPT/QUOTE/REPAIR/REVIEW锛?,
  `action_time`   datetime         NOT NULL                COMMENT '动作发生时间',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_company_user` (`work_order_id`, `company_id`, `user_id`),
  KEY `idx_company_user_action_time` (`company_id`, `user_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单用户级参与事实表';

-- -------------------------------------------
-- 25. 宸ュ崟鎶ヤ环璁板綍琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_quote`;
CREATE TABLE `work_order_quote` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '报价公司ID',
  `quoted_by`        bigint unsigned  NOT NULL                COMMENT '报价人ID',
  `fault_judge`      varchar(255)     DEFAULT NULL            COMMENT '故障判定',
  `quote_amount`     decimal(10,2)    DEFAULT NULL            COMMENT '报价金额',
  `quote_desc`       varchar(500)     DEFAULT NULL            COMMENT '报价说明',
  `is_current_valid` tinyint unsigned DEFAULT 1               COMMENT '鏄惁褰撳墠鏈夋晥鎶ヤ环锛?=鏄紝0=鍚︼級',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_quote_time` (`work_order_id`, `create_time`),
  KEY `idx_quote_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鎶ヤ环璁板綍琛?;

-- -------------------------------------------
-- 26. 宸ュ崟缁翠慨鐧昏琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_repair`;
CREATE TABLE `work_order_repair` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`  bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`     bigint unsigned  NOT NULL                COMMENT '维修公司ID',
  `repair_user_id` bigint unsigned  NOT NULL                COMMENT '维修员ID',
  `register_stage` varchar(32)      NOT NULL DEFAULT 'REPAIR' COMMENT '鐧昏闃舵锛圧EPAIR=缁翠慨鐧昏锛孯ECHECK=澶嶆鐧昏锛?,
  `is_finished`    tinyint unsigned DEFAULT 0               COMMENT '鏄惁缁翠慨瀹屾垚锛?=鏄紝0=鍚︼級',
  `finished_time`  datetime         DEFAULT NULL            COMMENT '完成时间',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_repair_time` (`work_order_id`, `create_time`),
  KEY `idx_repair_company` (`company_id`),
  KEY `idx_repair_user` (`repair_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟缁翠慨鐧昏琛?;

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
  `fault_remark`  varchar(500)     DEFAULT NULL            COMMENT '其它故障说明',
  `repair_desc`   varchar(500)     DEFAULT NULL            COMMENT '维修说明',
  `other_desc`    varchar(500)     DEFAULT NULL            COMMENT '其他维修说明',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `created_by`    bigint unsigned  NOT NULL                COMMENT '登记人ID',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_repair_fault` (`repair_id`, `sort_num`),
  KEY `idx_work_order_fault_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单故障点记录表';

-- -------------------------------------------
-- 28. 工单故障点配件明细表
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_fault_part`;
CREATE TABLE `work_order_fault_part` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `fault_id`      bigint unsigned  NOT NULL                COMMENT '故障点ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '登记公司ID',
  `part_name`     varchar(500)     NOT NULL                COMMENT '配件名称',
  `part_qty`      int unsigned     NOT NULL                COMMENT '配件数量',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `created_by`    bigint unsigned  NOT NULL                COMMENT '登记人ID',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_part` (`fault_id`, `sort_num`),
  KEY `idx_work_order_fault_part_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_part_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单故障点配件明细表';

-- -------------------------------------------
-- 29. 宸ュ崟璇勪环琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_evaluation`;
CREATE TABLE `work_order_evaluation` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `customer_id`   bigint unsigned  NOT NULL                COMMENT '客户ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '被评价服务方公司ID',
  `timeliness_score`   tinyint unsigned DEFAULT NULL       COMMENT '服务时效评分',
  `quality_score`      tinyint unsigned DEFAULT NULL       COMMENT '维修质量评分',
  `satisfaction_score` tinyint unsigned DEFAULT NULL       COMMENT '鏈嶅姟婊℃剰搴﹁瘎鍒?,
  `tags`          varchar(255)     DEFAULT NULL            COMMENT '标签集合',
  `content`       varchar(1000)    DEFAULT NULL            COMMENT '评价内容',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_eval` (`work_order_id`),
  KEY `idx_customer_eval` (`customer_id`),
  KEY `idx_eval_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟璇勪环琛?;

-- -------------------------------------------
-- 30. 宸ュ崟閫氱煡浜嬩欢琛?-- -------------------------------------------
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
  `send_status`      varchar(16)      NOT NULL                COMMENT '鍙戦€佺姸鎬?,
  `send_time`        datetime         DEFAULT NULL            COMMENT '鍙戦€佹椂闂?,
  `fail_reason`      varchar(500)     DEFAULT NULL            COMMENT '失败原因',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_notify_time` (`work_order_id`, `create_time`),
  KEY `idx_receiver_status` (`receiver_id`, `send_status`),
  KEY `idx_notify_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟閫氱煡浜嬩欢琛?;

-- -------------------------------------------
-- 30. 鏈哄櫒鏉＄爜妗ｆ琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `machine_barcode`;
CREATE TABLE `machine_barcode` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode`         varchar(100)     NOT NULL                COMMENT '机器条码',
  `deliver_number`  varchar(50)      DEFAULT NULL            COMMENT '发货单号',
  `hq_company_id`   bigint unsigned  DEFAULT NULL            COMMENT '归属总部ID',
  `cust_id`         varchar(64)      DEFAULT NULL            COMMENT 'CRM公司ID',
  `sales_org`       varchar(64)      DEFAULT NULL            COMMENT '閿€鍞粍缁?,
  `product_code`    varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_name`    varchar(128)     DEFAULT NULL            COMMENT '商品名称',
  `product_model`   varchar(100)     DEFAULT NULL            COMMENT '产品型号',
  `machine_no`      varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒灏忓彿',
  `brand_code`      varchar(32)      DEFAULT NULL            COMMENT '品牌编码',
  `scan_date`       datetime         DEFAULT NULL            COMMENT '条码扫描时间',
  `dealer_out_date` datetime         DEFAULT NULL            COMMENT '缁忛攢鍟嗘渶鏂板嚭搴撴棩鏈?,
  `crm_add_time`    datetime         DEFAULT NULL            COMMENT 'CRM创建时间',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `warranty_status` varchar(16)      DEFAULT NULL            COMMENT '璐ㄤ繚鐘舵€?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_machine_barcode_hq` (`hq_company_id`),
  KEY `idx_machine_barcode_product` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏈哄櫒鏉＄爜妗ｆ琛?;

-- 31. CRM 鍏徃鏄犲皠琛?-- -------------------------------------------
DROP TABLE IF EXISTS `crm_company_mapping`;
CREATE TABLE `crm_company_mapping` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cust_id`       varchar(64)      DEFAULT NULL            COMMENT 'CRM公司ID',
  `sales_org`     varchar(64)      DEFAULT NULL            COMMENT '閿€鍞粍缁?,
  `hq_company_id` bigint unsigned  DEFAULT NULL            COMMENT '归属总部ID',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_company_mapping_cust` (`cust_id`),
  UNIQUE KEY `uk_crm_company_mapping_sales_org` (`sales_org`),
  KEY `idx_crm_company_mapping_hq` (`hq_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM鍏徃鏄犲皠琛?;

-- -------------------------------------------
-- 32. CRM 总部-一级签约快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_hq_first_contract_snapshot`;
CREATE TABLE `crm_hq_first_contract_snapshot` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kunnr`            varchar(64)      NOT NULL                COMMENT 'CRM客户编码',
  `cust_id`          bigint unsigned  DEFAULT NULL            COMMENT 'CRM浼佷笟ID',
  `crm_company_name` varchar(200)     DEFAULT NULL            COMMENT 'CRM企业名称',
  `sales_org`        varchar(64)      NOT NULL                COMMENT '閿€鍞粍缁?,
  `region_code`      varchar(64)      DEFAULT NULL            COMMENT 'CRM大区编码',
  `region_name`      varchar(100)     DEFAULT NULL            COMMENT 'CRM大区名称',
  `alive_flag`       tinyint          DEFAULT NULL            COMMENT 'CRM有效标识',
  `crm_add_time`     datetime         DEFAULT NULL            COMMENT 'CRM新增时间',
  `crm_oper_time`    datetime         DEFAULT NULL            COMMENT 'CRM操作时间',
  `last_sync_time`   datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
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
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_product` (`company_id`, `product_code`, `product_model`),
  KEY `idx_fault_repair_config_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故障与维修配置表';

-- -------------------------------------------
-- 29. 鏁呴殰涓庣淮淇厤缃晠闅滈」琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_fault`;
CREATE TABLE `fault_repair_config_fault` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id`   bigint unsigned NOT NULL                COMMENT '閰嶇疆ID',
  `fault_desc`  varchar(500)    NOT NULL                COMMENT '故障描述',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `create_time` datetime        NOT NULL                COMMENT '创建时间',
  `update_time` datetime        NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_fault` (`config_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏁呴殰涓庣淮淇厤缃晠闅滈」琛?;

-- -------------------------------------------
-- 30. 鏁呴殰涓庣淮淇厤缃淮淇」琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_option`;
CREATE TABLE `fault_repair_config_option` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fault_id`    bigint unsigned NOT NULL                COMMENT '故障项ID',
  `repair_desc` varchar(500)    NOT NULL                COMMENT '维修说明',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `create_time` datetime        NOT NULL                COMMENT '创建时间',
  `update_time` datetime        NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_option_fault` (`fault_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏁呴殰涓庣淮淇厤缃淮淇」琛?;

-- -------------------------------------------
-- 31a. CRM 鍏徃蹇収琛?-- -------------------------------------------
DROP TABLE IF EXISTS `crm_biz_company_snapshot`;
CREATE TABLE `crm_biz_company_snapshot` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cust_id`             bigint unsigned  NOT NULL                COMMENT 'CRM客户ID',
  `cust_name`           varchar(200)     DEFAULT NULL            COMMENT '客户名称',
  `juristic_cust_id`    varchar(50)      DEFAULT NULL            COMMENT '鑱旂郴浜?,
  `group_contact_phone` varchar(50)      DEFAULT NULL            COMMENT '联系电话',
  `cellphone`           varchar(50)      DEFAULT NULL            COMMENT '鎵嬫満',
  `company_address`     varchar(200)     DEFAULT NULL            COMMENT '公司地址',
  `sap_company_code`    varchar(64)      DEFAULT NULL            COMMENT 'SAP公司编码',
  `cust_rage`           int              DEFAULT NULL            COMMENT '客户范围',
  `company_short_name`  varchar(128)     DEFAULT NULL            COMMENT '鍏徃绠€绉?',
  `province_name`       varchar(64)      DEFAULT NULL            COMMENT '省份',
  `city_name`           varchar(64)      DEFAULT NULL            COMMENT '城市',
  `district_name`       varchar(64)      DEFAULT NULL            COMMENT '区县',
  `cust_state`          int              DEFAULT NULL            COMMENT '瀹㈡埛鐘舵€?,
  `add_date`            datetime         DEFAULT NULL            COMMENT 'CRM新增时间',
  `oper_time`           datetime         DEFAULT NULL            COMMENT 'CRM操作时间',
  `last_sync_time`      datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_company_snapshot_cust` (`cust_id`),
  KEY `idx_crm_biz_company_snapshot_sap_code` (`sap_company_code`),
  KEY `idx_crm_biz_company_snapshot_name` (`cust_name`),
  KEY `idx_crm_biz_company_snapshot_oper` (`oper_time`),
  KEY `idx_crm_biz_company_snapshot_add` (`add_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM鍏徃蹇収琛?;

-- -------------------------------------------
-- 31a. CRM 一级二级关系来源快照表
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_first_second_relation_snapshot`;
CREATE TABLE `crm_first_second_relation_snapshot` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`      bigint unsigned  DEFAULT NULL            COMMENT 'CRM原始关系主键',
  `first_cust_id`  bigint unsigned  DEFAULT NULL            COMMENT '一级CRM企业ID',
  `second_cust_id` bigint unsigned  NOT NULL                COMMENT '二级CRM企业ID',
  `crm_oper_time`  datetime         DEFAULT NULL            COMMENT 'CRM操作时间',
  `last_sync_time` datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_first_second_relation_second` (`second_cust_id`),
  UNIQUE KEY `uk_crm_first_second_relation_source` (`source_id`),
  KEY `idx_crm_first_second_relation_first` (`first_cust_id`),
  KEY `idx_crm_first_second_relation_oper_time` (`crm_oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM一级二级关系来源快照表';

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
  `cust_id`         bigint unsigned  DEFAULT NULL            COMMENT '浼佷笟ID',
  `product_numeric` varchar(50)      DEFAULT NULL            COMMENT '产品编码',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_warehouse_scan_outstorage_source` (`source_id`),
  KEY `idx_crm_warehouse_scan_outstorage_code` (`scan_code`),
  KEY `idx_crm_warehouse_scan_outstorage_date` (`scan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM销售出库扫码快照表';

-- -------------------------------------------
-- 32. 鍚屾浠诲姟琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task`;
CREATE TABLE `sync_task` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_code`       varchar(64)      NOT NULL                COMMENT '任务编码',
  `task_name`       varchar(128)     NOT NULL                COMMENT '任务名称',
  `handler_code`    varchar(64)      NOT NULL                COMMENT '澶勭悊鍣ㄧ紪鐮?,
  `cron_expression` varchar(128)     NOT NULL                COMMENT 'Cron琛ㄨ揪寮?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_task_code` (`task_code`),
  UNIQUE KEY `uk_sync_task_handler` (`handler_code`),
  KEY `idx_sync_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍚屾浠诲姟琛?;

-- -------------------------------------------
-- 33. 鍚屾浠诲姟鏃ュ織琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task_log`;
CREATE TABLE `sync_task_log` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`         bigint unsigned NOT NULL                COMMENT '浠诲姟ID',
  `status`          varchar(16)     NOT NULL                COMMENT '鎵ц鐘舵€?,
  `start_time`      datetime        NOT NULL                COMMENT '寮€濮嬫椂闂?,
  `end_time`        datetime        DEFAULT NULL            COMMENT '结束时间',
  `data_start_time` datetime        DEFAULT NULL            COMMENT '鏁版嵁寮€濮嬫椂闂?,
  `data_end_time`   datetime        DEFAULT NULL            COMMENT '数据结束时间',
  `message`         varchar(1000)   DEFAULT NULL            COMMENT '执行信息',
  `create_time`     datetime        NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_task_log_task` (`task_id`, `id`),
  KEY `idx_sync_task_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍚屾浠诲姟鏃ュ織琛?;

SET FOREIGN_KEY_CHECKS = 1;

