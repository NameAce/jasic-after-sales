-- =============================================
-- 浣冲＋鍞悗绯荤粺 - 鏁版嵁搴揇DL鑴氭湰
-- 鏁版嵁搴擄細jasic_after_sales
-- 瀛楃闆嗭細utf8mb4
-- 鎺掑簭瑙勫垯锛歶tf8mb4_general_ci
-- 鍏?1寮犺〃
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------
-- 1. 鍏徃绫诲瀷瀛楀吀琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company_type`;
CREATE TABLE `sys_company_type` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `type_code`    varchar(32)      NOT NULL                COMMENT '绫诲瀷缂栫爜锛圥LATFORM/HQ_A/HQ_B/HQ_C/HQ_D/FIRST/SECOND锛?,
  `type_name`    varchar(64)      NOT NULL                COMMENT '绫诲瀷鍚嶇О',
  `subject_type` varchar(16)      NOT NULL                COMMENT '涓讳綋绫诲瀷锛圥LATFORM/HQ/SERVICE锛?,
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `order_num`    int              DEFAULT 0               COMMENT '鎺掑簭',
  `create_time`  datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`  datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍏徃绫诲瀷瀛楀吀琛?;

-- -------------------------------------------
-- 2. 鍏徃琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company`;
CREATE TABLE `sys_company` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `company_name`  varchar(128)     NOT NULL                COMMENT '鍏徃鍚嶇О',
  `company_short_name` varchar(128) DEFAULT NULL           COMMENT '鍏徃绠€绉?',
  `company_code`  varchar(64)      DEFAULT NULL            COMMENT '鍏徃缂栫爜',
  `type_code`     varchar(32)      NOT NULL                COMMENT '鍏徃绫诲瀷缂栫爜',
  `contact_name`  varchar(64)      NOT NULL                COMMENT '鑱旂郴浜?,
  `contact_phone` varchar(20)      NOT NULL                COMMENT '鑱旂郴鐢佃瘽',
  `address`       varchar(256)     NOT NULL                COMMENT '鍏徃鍦板潃',
  `province_name` varchar(64)      DEFAULT NULL            COMMENT '鐪佷唤',
  `city_name`     varchar(64)      DEFAULT NULL            COMMENT '鍩庡競',
  `district_name` varchar(64)      DEFAULT NULL            COMMENT '鍖哄幙',
  `longitude`     decimal(10,6)    DEFAULT NULL            COMMENT '缁忓害',
  `latitude`      decimal(10,6)    DEFAULT NULL            COMMENT '绾害',
  `service_phone` varchar(32)      DEFAULT NULL            COMMENT '瀹㈡湇鐢佃瘽',
  `source_type`   varchar(16)      NOT NULL DEFAULT 'MANUAL' COMMENT '鏉ユ簮绫诲瀷',
  `sales_org`     varchar(64)      DEFAULT NULL            COMMENT '閿€鍞粍缁?',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_code` (`company_code`),
  UNIQUE KEY `uk_company_sales_org` (`sales_org`),
  KEY `idx_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍏徃琛?;

-- -------------------------------------------
-- 3. 澶у尯琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_region`;
CREATE TABLE `sys_region` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '鎵€灞炴€婚儴鍏徃ID',
  `region_name` varchar(64)      NOT NULL                COMMENT '澶у尯鍚嶇О',
  `region_code` varchar(32)      DEFAULT NULL            COMMENT '澶у尯缂栫爜',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='澶у尯琛?;

-- -------------------------------------------
-- 4. 鎬婚儴-涓€绾х绾﹀叧绯昏〃
-- -------------------------------------------
DROP TABLE IF EXISTS `hq_first_contract`;
CREATE TABLE `hq_first_contract` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `hq_company_id`    bigint unsigned  NOT NULL                COMMENT '鎬婚儴鍏徃ID',
  `first_company_id` bigint unsigned  NOT NULL                COMMENT '涓€绾х綉鐐瑰叕鍙窱D',
  `region_id`        bigint unsigned  DEFAULT NULL            COMMENT '澶у尯ID锛堢绾︽椂缁戝畾锛?,
  `contract_no`      varchar(64)      DEFAULT NULL            COMMENT '鍚堝悓缂栧彿',
  `status`           tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鏈夋晥锛?=缁堟锛?,
  `remark`           varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hq_first` (`hq_company_id`, `first_company_id`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鎬婚儴-涓€绾х绾﹀叧绯昏〃';

-- -------------------------------------------
-- 5. 涓€绾?浜岀骇浠庡睘鍏崇郴琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `first_second_relation`;
CREATE TABLE `first_second_relation` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `first_company_id`  bigint unsigned  NOT NULL                COMMENT '涓€绾х綉鐐瑰叕鍙窱D',
  `second_company_id` bigint unsigned  NOT NULL                COMMENT '浜岀骇缃戠偣鍏徃ID',
  `status`            tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鏈夋晥锛?=瑙ｉ櫎锛?,
  `remark`            varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`       datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`       datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_first_second` (`first_company_id`, `second_company_id`),
  UNIQUE KEY `uk_second` (`second_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涓€绾?浜岀骇浠庡睘鍏崇郴琛?;

-- -------------------------------------------
-- 6. B绔憳宸ヨ〃
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `username`        varchar(64)      NOT NULL                COMMENT '鐧诲綍鐢ㄦ埛鍚?,
  `password`        varchar(128)     NOT NULL                COMMENT '瀵嗙爜锛圔Crypt鍔犲瘑锛?,
  `real_name`       varchar(64)      DEFAULT NULL            COMMENT '鐪熷疄濮撳悕',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '鎵嬫満鍙?,
  `email`           varchar(64)      DEFAULT NULL            COMMENT '閭',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '澶村儚URL',
  `openid`          varchar(64)      DEFAULT NULL            COMMENT '寰俊openid锛堝皬绋嬪簭鐧诲綍缁戝畾锛?,
  `wechat_phone`    varchar(20)      DEFAULT NULL            COMMENT '寰俊鎺堟潈鎵嬫満鍙峰揩鐓?,
  `sex`             tinyint unsigned DEFAULT 0               COMMENT '鎬у埆锛?=鏈煡锛?=鐢凤紝2=濂筹級',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `is_deleted`      tinyint unsigned DEFAULT 0               COMMENT '鏄惁鍒犻櫎锛堥€昏緫鍒犻櫎锛?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `last_login_time` datetime         DEFAULT NULL            COMMENT '鏈€鍚庣櫥褰曟椂闂?,
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='B绔憳宸ヨ〃';

-- -------------------------------------------
-- 7. 寰俊缁戝畾璁板綍琛?-- -------------------------------------------
DROP TABLE IF EXISTS `wechat_bind_record`;
CREATE TABLE `wechat_bind_record` (
  `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id`           bigint unsigned NOT NULL                COMMENT '鐢ㄦ埛ID',
  `operate_type`      varchar(32)     NOT NULL                COMMENT '鎿嶄綔绫诲瀷锛圔IND/UNBIND锛?,
  `operate_source`    varchar(32)     NOT NULL                COMMENT '鎿嶄綔鏉ユ簮锛圡P_BIND_LOGIN/PC_QR_BIND/PC_SELF_UNBIND锛?,
  `openid`            varchar(64)     NOT NULL                COMMENT '寰俊openid蹇収',
  `wechat_phone`      varchar(20)     DEFAULT NULL            COMMENT '寰俊鎺堟潈鎵嬫満鍙峰揩鐓?,
  `operator_user_id`  bigint unsigned NOT NULL                COMMENT '鎿嶄綔浜篒D',
  `operator_username` varchar(64)     NOT NULL                COMMENT '鎿嶄綔浜虹敤鎴峰悕',
  `operate_time`      datetime        NOT NULL                COMMENT '鎿嶄綔鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_operate_time` (`user_id`, `operate_time`),
  KEY `idx_openid_operate_time` (`openid`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='寰俊缁戝畾璁板綍琛?;

-- -------------------------------------------
-- 8. C绔鎴疯〃
-- -------------------------------------------
DROP TABLE IF EXISTS `c_user`;
CREATE TABLE `c_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `openid`          varchar(64)      NOT NULL                COMMENT '寰俊openid',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '鎵嬫満鍙凤紙寰俊鎺堟潈鑾峰彇锛?,
  `nickname`        varchar(64)      DEFAULT NULL            COMMENT '鏄电О',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '澶村儚URL',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `last_login_time` datetime         DEFAULT NULL            COMMENT '鏈€鍚庣櫥褰曟椂闂?,
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C绔鎴疯〃';

-- -------------------------------------------
-- 9. C绔鎴峰湴鍧€琛?-- -------------------------------------------
DROP TABLE IF EXISTS `customer_address`;
CREATE TABLE `customer_address` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `customer_id`    bigint unsigned  NOT NULL                COMMENT '瀹㈡埛ID',
  `contact_name`   varchar(64)      NOT NULL                COMMENT '鑱旂郴浜?,
  `contact_mobile` varchar(20)      NOT NULL                COMMENT '鑱旂郴鎵嬫満鍙?,
  `province`       varchar(64)      NOT NULL                COMMENT '鐪?,
  `city`           varchar(64)      NOT NULL                COMMENT '甯?,
  `county`         varchar(64)      DEFAULT NULL            COMMENT '鍖哄幙',
  `detail_address` varchar(255)     NOT NULL                COMMENT '璇︾粏鍦板潃',
  `is_default`     tinyint unsigned DEFAULT 0               COMMENT '鏄惁榛樿鍦板潃锛?=鏄紝0=鍚︼級',
  `create_time`    datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`    datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_customer_address_customer` (`customer_id`),
  KEY `idx_customer_address_default` (`customer_id`, `is_default`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C绔鎴峰湴鍧€琛?;

-- -------------------------------------------
-- 8. 鐢ㄦ埛-鍏徃鍏宠仈琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_company`;
CREATE TABLE `sys_user_company` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '鐢ㄦ埛ID',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '鍏徃ID',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '鏄惁榛樿鍏徃锛?=鏄紝0=鍚︼級',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_company` (`user_id`, `company_id`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛-鍏徃鍏宠仈琛?;

-- -------------------------------------------
-- 9. 瑙掕壊琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '褰掑睘鍏徃ID',
  `role_name`   varchar(64)      NOT NULL                COMMENT '瑙掕壊鍚嶇О',
  `role_key`    varchar(64)      NOT NULL                COMMENT '瑙掕壊鏍囪瘑',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '鏁版嵁鑼冨洿锛圓LL/REGION/SELF锛?,
  `role_type`   tinyint unsigned DEFAULT 0               COMMENT '瑙掕壊绫诲瀷锛?=鑷畾涔夎鑹诧紝1=鍏徃绠＄悊鍛樿鑹诧紝2=妯℃澘瑙掕壊锛?,
  `is_system`   tinyint unsigned DEFAULT 0               COMMENT '鏄惁绯荤粺瑙掕壊锛?=鏄紝涓嶅彲鍒犻櫎锛?,
  `status`      tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `order_num`   int              DEFAULT 0               COMMENT '鎺掑簭',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_role_key` (`company_id`, `role_key`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊琛?;

-- -------------------------------------------
-- 10. 瑙掕壊妯℃澘琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template`;
CREATE TABLE `sys_role_template` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `type_code`   varchar(32)      NOT NULL                COMMENT '鍏徃绫诲瀷缂栫爜',
  `role_name`   varchar(64)      NOT NULL                COMMENT '瑙掕壊鍚嶇О',
  `role_key`    varchar(64)      NOT NULL                COMMENT '瑙掕壊鏍囪瘑',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '鏁版嵁鑼冨洿锛圓LL/REGION/SELF锛?,
  `is_admin`    tinyint unsigned DEFAULT 0               COMMENT '鏄惁绠＄悊鍛樿鑹叉ā鏉匡紙1=鏄紝姣忕绫诲瀷鏈€澶氫竴涓級',
  `order_num`   int              DEFAULT 0               COMMENT '鎺掑簭',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_role_key` (`type_code`, `role_key`),
  KEY `idx_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊妯℃澘琛?;

-- -------------------------------------------
-- 11. 瑙掕壊妯℃澘-鑿滃崟鍏宠仈琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template_menu`;
CREATE TABLE `sys_role_template_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `template_id` bigint unsigned  NOT NULL                COMMENT '妯℃澘ID',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '鑿滃崟ID',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_menu` (`template_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊妯℃澘-鑿滃崟鍏宠仈琛?;

-- -------------------------------------------
-- 12. 鑿滃崟琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `subject_type`  varchar(16)      NOT NULL                COMMENT '鎵€灞炰富浣撶被鍨嬶紙PLATFORM/HQ/SERVICE锛?,
  `menu_name`     varchar(64)      NOT NULL                COMMENT '鑿滃崟鍚嶇О',
  `parent_id`     bigint unsigned  DEFAULT 0               COMMENT '涓婄骇鑿滃崟ID锛?涓洪《绾э級',
  `menu_type`     char(1)          NOT NULL                COMMENT '绫诲瀷锛圡=鐩綍锛孋=鑿滃崟锛孎=鎸夐挳锛?,
  `path`          varchar(128)     DEFAULT NULL            COMMENT '璺敱鍦板潃',
  `component`     varchar(128)     DEFAULT NULL            COMMENT '缁勪欢璺緞',
  `perms`         varchar(128)     DEFAULT NULL            COMMENT '鏉冮檺鏍囪瘑锛堝 system:user:list锛?,
  `icon`          varchar(64)      DEFAULT NULL            COMMENT '鍥炬爣',
  `order_num`     int              DEFAULT 0               COMMENT '鎺掑簭',
  `is_visible`    tinyint unsigned DEFAULT 1               COMMENT '鏄惁鍙锛?=鏄紝0=鍚︼級',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=姝ｅ父锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鑿滃崟琛?;

-- -------------------------------------------
-- 13. 鍏徃绫诲瀷-鑿滃崟涓婇檺琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_type_code_menu`;
CREATE TABLE `sys_type_code_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `type_code`   varchar(32)      NOT NULL                COMMENT '鍏徃绫诲瀷缂栫爜',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '鑿滃崟ID',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
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
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `role_id`     bigint unsigned  NOT NULL                COMMENT '瑙掕壊ID',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '鑿滃崟ID',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瑙掕壊-鑿滃崟鍏宠仈琛?;

-- -------------------------------------------
-- 15. 鐢ㄦ埛-瑙掕壊鍏宠仈琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '鐢ㄦ埛ID',
  `role_id`     bigint unsigned  NOT NULL                COMMENT '瑙掕壊ID',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛-瑙掕壊鍏宠仈琛?;

-- -------------------------------------------
-- 16. 鐢ㄦ埛-澶у尯鍏宠仈琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_region`;
CREATE TABLE `sys_user_region` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '鐢ㄦ埛ID',
  `region_id`   bigint unsigned  NOT NULL                COMMENT '澶у尯ID',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_region` (`user_id`, `region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛-澶у尯鍏宠仈琛?;

-- -------------------------------------------
-- 17. 瀛楀吀绫诲瀷琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `dict_name`   varchar(100)     NOT NULL                COMMENT '瀛楀吀鍚嶇О',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '瀛楀吀绫诲瀷',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀛楀吀绫诲瀷琛?;

-- -------------------------------------------
-- 18. 瀛楀吀鏁版嵁琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '瀛楀吀绫诲瀷',
  `dict_label`  varchar(100)     NOT NULL                COMMENT '瀛楀吀鏍囩',
  `dict_value`  varchar(100)     NOT NULL                COMMENT '瀛楀吀閿€?,
  `dict_sort`   int              DEFAULT 0               COMMENT '鎺掑簭',
  `css_class`   varchar(100)     DEFAULT NULL            COMMENT '鑷畾涔夋牱寮?,
  `list_class`  varchar(100)     DEFAULT NULL            COMMENT '鏍囩鏍峰紡',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '鏄惁榛樿锛?=鏄紝0=鍚︼級',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_value` (`dict_type`, `dict_value`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀛楀吀鏁版嵁琛?;

-- -------------------------------------------
-- 19. 鍙傛暟璁剧疆琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `config_name`  varchar(100)     NOT NULL                COMMENT '鍙傛暟鍚嶇О',
  `config_key`   varchar(100)     NOT NULL                COMMENT '鍙傛暟閿悕',
  `config_value` text             NOT NULL                COMMENT '鍙傛暟閿€?,
  `config_type`  tinyint unsigned DEFAULT 0               COMMENT '鏄惁鍐呯疆锛?=鏄紝0=鍚︼級',
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`  datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`  datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍙傛暟璁剧疆琛?;

-- -------------------------------------------
-- 20. 鎿嶄綔鏃ュ織琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `title`           varchar(64)      DEFAULT NULL            COMMENT '鎿嶄綔妯″潡',
  `oper_type`       tinyint unsigned DEFAULT 0               COMMENT '鎿嶄綔绫诲瀷锛?=鍏朵粬锛?=鏂板锛?=淇敼锛?=鍒犻櫎锛?=鎺堟潈锛?=瀵煎嚭锛?=鐧诲綍锛?=鐧诲嚭锛?=寮哄埗涓嬬嚎锛?,
  `method`          varchar(256)     DEFAULT NULL            COMMENT '璇锋眰鏂规硶锛堢被鍚?鏂规硶鍚嶏級',
  `request_method`  varchar(16)      DEFAULT NULL            COMMENT '璇锋眰鏂瑰紡锛圙ET/POST/PUT/DELETE锛?,
  `request_url`     varchar(256)     DEFAULT NULL            COMMENT '璇锋眰URL',
  `request_param`   text             DEFAULT NULL            COMMENT '璇锋眰鍙傛暟',
  `response_result` text             DEFAULT NULL            COMMENT '杩斿洖缁撴灉',
  `user_id`         bigint unsigned  DEFAULT NULL            COMMENT '鎿嶄綔浜篒D',
  `username`        varchar(64)      DEFAULT NULL            COMMENT '鎿嶄綔浜虹敤鎴峰悕',
  `company_id`      bigint unsigned  DEFAULT NULL            COMMENT '鎿嶄綔浜哄綋鍓嶅叕鍙窱D',
  `ip`              varchar(64)      DEFAULT NULL            COMMENT '鎿嶄綔IP',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鎿嶄綔鐘舵€侊紙1=鎴愬姛锛?=澶辫触锛?,
  `error_msg`       text             DEFAULT NULL            COMMENT '閿欒淇℃伅',
  `oper_time`       datetime         DEFAULT NULL            COMMENT '鎿嶄綔鏃堕棿',
  `cost_time`       bigint           DEFAULT 0               COMMENT '鑰楁椂锛堟绉掞級',
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鎿嶄綔鏃ュ織琛?;

-- -------------------------------------------
-- 21. 宸ュ崟涓昏〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id`                          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `order_no`                    varchar(32)      NOT NULL                COMMENT '宸ュ崟鍙?,
  `customer_id`                 bigint unsigned  DEFAULT NULL            COMMENT '瀹㈡埛ID',
  `customer_name`               varchar(64)      NOT NULL                COMMENT '瀹㈡埛濮撳悕',
  `customer_mobile`             varchar(20)      NOT NULL                COMMENT '瀹㈡埛鎵嬫満鍙?,
  `report_subject_type`         varchar(16)      NOT NULL                COMMENT '鎶ヤ慨涓讳綋绫诲瀷锛圕USTOMER/COMPANY锛?,
  `report_company_id`           bigint unsigned  DEFAULT NULL            COMMENT '鎶ヤ慨涓讳綋鍏徃ID',
  `barcode`                     varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒鏉＄爜',
  `product_code`                varchar(64)      DEFAULT NULL            COMMENT '鐗╂枡缂栫爜',
  `product_name`                varchar(128)     DEFAULT NULL            COMMENT '鍟嗗搧鍚嶇О',
  `product_model`               varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒鍨嬪彿',
  `machine_no`                  varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒灏忓彿',
  `brand_type`                  varchar(16)      DEFAULT NULL            COMMENT '鍝佺墝绫诲瀷',
  `brand_code`                  varchar(32)      DEFAULT NULL            COMMENT '鍝佺墝缂栫爜',
  `brand_name`                  varchar(64)      DEFAULT NULL            COMMENT '鍝佺墝鍚嶇О',
  `service_mode`                varchar(16)      NOT NULL                COMMENT '鏈嶅姟鏂瑰紡缂栫爜锛圡AIL=瀵勪慨锛孲TORE=鍒板簵缁翠慨锛?,
  `warranty_status`             varchar(16)      DEFAULT NULL            COMMENT '璐ㄤ繚鐘舵€?,
  `fault_desc`                  text             DEFAULT NULL            COMMENT '瀹㈡埛鎶ヤ慨鎻忚堪',
  `fault_remark`  varchar(500)     DEFAULT NULL            COMMENT '其它故障说明',
  `sender_name`                 varchar(64)      DEFAULT NULL            COMMENT '瀵勪欢浜哄鍚?,
  `sender_mobile`               varchar(20)      DEFAULT NULL            COMMENT '瀵勪欢浜烘墜鏈哄彿',
  `sender_address`              varchar(255)     DEFAULT NULL            COMMENT '瀵勪欢鍦板潃',
  `send_express_no`             varchar(64)      DEFAULT NULL            COMMENT '瀵勪欢蹇€掑崟鍙?,
  `main_status`                 varchar(32)      NOT NULL                COMMENT '涓荤姸鎬?,
  `evaluate_status`             varchar(32)      NOT NULL                COMMENT '璇勪环鐘舵€?,
  `current_accept_subject_type` varchar(16)      NOT NULL                COMMENT '褰撳墠鍙楃悊涓讳綋绫诲瀷锛圫ERVICE/HQ锛?,
  `current_accept_company_id`   bigint unsigned  NOT NULL                COMMENT '褰撳墠鍙楃悊鍏徃ID',
  `assigned_user_id`            bigint unsigned  DEFAULT NULL            COMMENT '褰撳墠缁翠慨鍛業D',
  `create_company_id`           bigint unsigned  NOT NULL                COMMENT '寤哄崟鏉ユ簮鍏徃ID',
  `create_entry_type`           varchar(32)      DEFAULT NULL            COMMENT '寤哄崟鍏ュ彛绫诲瀷',
  `hq_company_id`               bigint unsigned  NOT NULL                COMMENT '褰掑睘鎬婚儴ID',
  `fault_repair_config_id`      bigint unsigned  DEFAULT NULL            COMMENT '缁戝畾鐨勬晠闅滀笌缁翠慨閰嶇疆ID',
  `has_transfer`                tinyint unsigned DEFAULT 0               COMMENT '鏄惁鍙戠敓杩囪浆鍗曪紙1=鏄紝0=鍚︼級',
  `transfer_count`              int unsigned     DEFAULT 0               COMMENT '杞崟娆℃暟',
  `return_method`               varchar(16)      DEFAULT NULL            COMMENT '鏈哄櫒杩斿洖鏂瑰紡锛堝洖瀵?鑷彁锛?,
  `return_express_no`           varchar(64)      DEFAULT NULL            COMMENT '鍥炲瘎蹇€掑崟鍙?,
  `close_reason`                varchar(255)     DEFAULT NULL            COMMENT '鍏抽棴鍘熷洜',
  `completed_time`              datetime         DEFAULT NULL            COMMENT '瀹屾垚鏃堕棿',
  `closed_time`                 datetime         DEFAULT NULL            COMMENT '鍏抽棴鏃堕棿',
  `create_time`                 datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`                 datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_current_accept_company` (`current_accept_company_id`),
  KEY `idx_assigned_user` (`assigned_user_id`),
  KEY `idx_hq_company` (`hq_company_id`),
  KEY `idx_fault_repair_config` (`fault_repair_config_id`),
  KEY `idx_main_status` (`main_status`),
  KEY `idx_report_company` (`report_company_id`),
  KEY `idx_customer_mobile` (`customer_mobile`),
  KEY `idx_barcode` (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟涓昏〃';

-- -------------------------------------------
-- 22. 宸ュ崟闄勪欢琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `storage_type`      varchar(32)      NOT NULL COMMENT '瀛樺偍绫诲瀷',
  `bucket`            varchar(128)     NOT NULL COMMENT '瀛樺偍妗?,
  `object_key`        varchar(512)     NOT NULL COMMENT '瀵硅薄閿?,
  `original_name`     varchar(255)     NOT NULL COMMENT '鍘熷鏂囦欢鍚?,
  `content_type`      varchar(128)     DEFAULT NULL COMMENT '鍐呭绫诲瀷',
  `file_size`         bigint unsigned  NOT NULL COMMENT '鏂囦欢澶у皬',
  `file_ext`          varchar(32)      NOT NULL COMMENT '鎵╁睍鍚?,
  `file_hash`         varchar(128)     NOT NULL COMMENT '鏂囦欢鍝堝笇',
  `access_level`      varchar(32)      NOT NULL COMMENT '璁块棶绾у埆',
  `upload_user_id`    bigint unsigned  DEFAULT NULL COMMENT '涓婁紶鐢ㄦ埛ID',
  `upload_user_type`  varchar(32)      NOT NULL COMMENT '涓婁紶鐢ㄦ埛绫诲瀷',
  `upload_company_id` bigint unsigned  DEFAULT NULL COMMENT '涓婁紶鍏徃ID',
  `status`            varchar(32)      NOT NULL COMMENT '鏂囦欢鐘舵€?,
  `create_time`       datetime         NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`       datetime         NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_file_hash_key` (`file_hash`, `object_key`),
  KEY `idx_sys_file_upload_user` (`upload_user_id`, `upload_user_type`),
  KEY `idx_sys_file_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏂囦欢鍏冩暟鎹〃';

DROP TABLE IF EXISTS `sys_file_biz`;
CREATE TABLE `sys_file_biz` (
  `id`                 bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `file_id`            bigint unsigned  NOT NULL COMMENT '鏂囦欢ID',
  `biz_type`           varchar(64)      NOT NULL COMMENT '涓氬姟绫诲瀷',
  `biz_id`             bigint unsigned  NOT NULL COMMENT '涓氬姟ID',
  `sort_num`           int              NOT NULL DEFAULT 1 COMMENT '鎺掑簭鍙?,
  `is_primary`         tinyint unsigned NOT NULL DEFAULT 0 COMMENT '鏄惁涓绘枃浠?,
  `company_id`         bigint unsigned  DEFAULT NULL COMMENT '鍏徃ID',
  `operator_user_id`   bigint unsigned  DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  `operator_user_type` varchar(32)      DEFAULT NULL COMMENT '鎿嶄綔浜虹被鍨?,
  `remark`             varchar(255)     DEFAULT NULL COMMENT '澶囨敞',
  `create_time`        datetime         NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`        datetime         NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_biz_type_id_sort` (`biz_type`, `biz_id`, `sort_num`),
  KEY `idx_sys_file_biz_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏂囦欢涓氬姟鍏宠仈琛?;

-- -------------------------------------------
-- 22. 宸ュ崟娴佽浆鍘嗗彶琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_flow`;
CREATE TABLE `work_order_flow` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`       bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `action_type`         varchar(32)      NOT NULL                COMMENT '鍔ㄤ綔绫诲瀷',
  `before_status`       varchar(32)      DEFAULT NULL            COMMENT '鍔ㄤ綔鍓嶄富鐘舵€?,
  `after_status`        varchar(32)      DEFAULT NULL            COMMENT '鍔ㄤ綔鍚庝富鐘舵€?,
  `from_company_id`     bigint unsigned  DEFAULT NULL            COMMENT '鏉ユ簮鍏徃ID',
  `to_company_id`       bigint unsigned  DEFAULT NULL            COMMENT '鐩爣鍏徃ID',
  `operator_company_id` bigint unsigned  NOT NULL                COMMENT '鎿嶄綔鍏徃ID',
  `operator_user_id`    bigint unsigned  NOT NULL                COMMENT '鎿嶄綔浜篒D',
  `remark`              varchar(500)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`         datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`         datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_action_time` (`work_order_id`, `create_time`),
  KEY `idx_to_company` (`to_company_id`),
  KEY `idx_operator_company` (`operator_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟娴佽浆鍘嗗彶琛?;

-- -------------------------------------------
-- 23. 宸ュ崟鍙備笌鏂瑰揩鐓ц〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_participant`;
CREATE TABLE `work_order_participant` (
  `id`                     bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`          bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`             bigint unsigned  NOT NULL                COMMENT '鍙備笌鍏徃ID',
  `subject_type`           varchar(16)      NOT NULL                COMMENT '涓讳綋绫诲瀷锛圫ERVICE/HQ锛?,
  `participate_type`       varchar(32)      NOT NULL                COMMENT '鍙備笌绫诲瀷锛圕REATE/CURRENT/HISTORY/HQ_OBSERVER锛?,
  `is_current_handler`     tinyint unsigned DEFAULT 0               COMMENT '鏄惁褰撳墠鍙楃悊鏂癸紙1=鏄紝0=鍚︼級',
  `first_participate_time` datetime         NOT NULL                COMMENT '棣栨鍙備笌鏃堕棿',
  `last_participate_time`  datetime         NOT NULL                COMMENT '鏈€鍚庡弬涓庢椂闂?,
  `create_time`            datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`            datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_company` (`work_order_id`, `company_id`),
  KEY `idx_company_current` (`company_id`, `is_current_handler`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鍙備笌鏂瑰揩鐓ц〃';

-- -------------------------------------------
-- 24. 宸ュ崟鐢ㄦ埛绾у弬涓庝簨瀹炶〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_user_participant`;
CREATE TABLE `work_order_user_participant` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '鍙備笌鍏徃ID',
  `user_id`       bigint unsigned  NOT NULL                COMMENT '鍙備笌鐢ㄦ埛ID',
  `action_type`   varchar(32)      NOT NULL                COMMENT '鍙備笌鍔ㄤ綔绫诲瀷锛圱ECH_ACCEPT/QUOTE/REPAIR/REVIEW锛?,
  `action_time`   datetime         NOT NULL                COMMENT '鍔ㄤ綔鍙戠敓鏃堕棿',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_company_user` (`work_order_id`, `company_id`, `user_id`),
  KEY `idx_company_user_action_time` (`company_id`, `user_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鐢ㄦ埛绾у弬涓庝簨瀹炶〃';

-- -------------------------------------------
-- 25. 宸ュ崟鎶ヤ环璁板綍琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_quote`;
CREATE TABLE `work_order_quote` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '鎶ヤ环鍏徃ID',
  `quoted_by`        bigint unsigned  NOT NULL                COMMENT '鎶ヤ环浜篒D',
  `fault_judge`      varchar(255)     DEFAULT NULL            COMMENT '鏁呴殰鍒ゅ畾',
  `quote_amount`     decimal(10,2)    DEFAULT NULL            COMMENT '鎶ヤ环閲戦',
  `quote_desc`       varchar(500)     DEFAULT NULL            COMMENT '鎶ヤ环璇存槑',
  `is_current_valid` tinyint unsigned DEFAULT 1               COMMENT '鏄惁褰撳墠鏈夋晥鎶ヤ环锛?=鏄紝0=鍚︼級',
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_quote_time` (`work_order_id`, `create_time`),
  KEY `idx_quote_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鎶ヤ环璁板綍琛?;

-- -------------------------------------------
-- 26. 宸ュ崟缁翠慨鐧昏琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_repair`;
CREATE TABLE `work_order_repair` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`  bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`     bigint unsigned  NOT NULL                COMMENT '缁翠慨鍏徃ID',
  `repair_user_id` bigint unsigned  NOT NULL                COMMENT '缁翠慨鍛業D',
  `register_stage` varchar(32)      NOT NULL DEFAULT 'REPAIR' COMMENT '鐧昏闃舵锛圧EPAIR=缁翠慨鐧昏锛孯ECHECK=澶嶆鐧昏锛?,
  `is_finished`    tinyint unsigned DEFAULT 0               COMMENT '鏄惁缁翠慨瀹屾垚锛?=鏄紝0=鍚︼級',
  `finished_time`  datetime         DEFAULT NULL            COMMENT '瀹屾垚鏃堕棿',
  `create_time`    datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`    datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_repair_time` (`work_order_id`, `create_time`),
  KEY `idx_repair_company` (`company_id`),
  KEY `idx_repair_user` (`repair_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟缁翠慨鐧昏琛?;

-- -------------------------------------------
-- 27. 宸ュ崟鏁呴殰鐐硅褰曡〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_fault`;
CREATE TABLE `work_order_fault` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `repair_id`     bigint unsigned  NOT NULL                COMMENT '缁翠慨鐧昏ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '鐧昏鍏徃ID',
  `fault_desc`    varchar(500)     NOT NULL                COMMENT '鏁呴殰鎻忚堪',
  `fault_remark`  varchar(500)     DEFAULT NULL            COMMENT '其它故障说明',
  `repair_desc`   varchar(500)     DEFAULT NULL            COMMENT '缁翠慨璇存槑',
  `other_desc`    varchar(500)     DEFAULT NULL            COMMENT '鍏朵粬缁翠慨璇存槑',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `created_by`    bigint unsigned  NOT NULL                COMMENT '鐧昏浜篒D',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_repair_fault` (`repair_id`, `sort_num`),
  KEY `idx_work_order_fault_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鏁呴殰鐐硅褰曡〃';

-- -------------------------------------------
-- 28. 宸ュ崟鏁呴殰鐐归厤浠舵槑缁嗚〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_fault_part`;
CREATE TABLE `work_order_fault_part` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `fault_id`      bigint unsigned  NOT NULL                COMMENT '鏁呴殰鐐笽D',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '鐧昏鍏徃ID',
  `part_name`     varchar(500)     NOT NULL                COMMENT '閰嶄欢鍚嶇О',
  `part_qty`      int unsigned     NOT NULL                COMMENT '閰嶄欢鏁伴噺',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `created_by`    bigint unsigned  NOT NULL                COMMENT '鐧昏浜篒D',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_part` (`fault_id`, `sort_num`),
  KEY `idx_work_order_fault_part_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_part_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鏁呴殰鐐归厤浠舵槑缁嗚〃';

-- -------------------------------------------
-- 29. 宸ュ崟璇勪环琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_evaluation`;
CREATE TABLE `work_order_evaluation` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `customer_id`   bigint unsigned  NOT NULL                COMMENT '瀹㈡埛ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '琚瘎浠锋湇鍔℃柟鍏徃ID',
  `timeliness_score`   tinyint unsigned DEFAULT NULL       COMMENT '鏈嶅姟鏃舵晥璇勫垎',
  `quality_score`      tinyint unsigned DEFAULT NULL       COMMENT '缁翠慨璐ㄩ噺璇勫垎',
  `satisfaction_score` tinyint unsigned DEFAULT NULL       COMMENT '鏈嶅姟婊℃剰搴﹁瘎鍒?,
  `tags`          varchar(255)     DEFAULT NULL            COMMENT '鏍囩闆嗗悎',
  `content`       varchar(1000)    DEFAULT NULL            COMMENT '璇勪环鍐呭',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_eval` (`work_order_id`),
  KEY `idx_customer_eval` (`customer_id`),
  KEY `idx_eval_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟璇勪环琛?;

-- -------------------------------------------
-- 30. 宸ュ崟閫氱煡浜嬩欢琛?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_notify_event`;
CREATE TABLE `work_order_notify_event` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '涓氬姟褰掑睘鍏徃ID',
  `event_type`       varchar(32)      NOT NULL                COMMENT '浜嬩欢绫诲瀷',
  `trigger_node`     varchar(32)      NOT NULL                COMMENT '瑙﹀彂鑺傜偣',
  `receiver_type`    varchar(32)      NOT NULL                COMMENT '鎺ユ敹瀵硅薄绫诲瀷',
  `receiver_id`      bigint unsigned  NOT NULL                COMMENT '鎺ユ敹瀵硅薄ID',
  `title_snapshot`   varchar(255)     DEFAULT NULL            COMMENT '鏍囬蹇収',
  `content_snapshot` text             DEFAULT NULL            COMMENT '鍐呭蹇収',
  `send_status`      varchar(16)      NOT NULL                COMMENT '鍙戦€佺姸鎬?,
  `send_time`        datetime         DEFAULT NULL            COMMENT '鍙戦€佹椂闂?,
  `fail_reason`      varchar(500)     DEFAULT NULL            COMMENT '澶辫触鍘熷洜',
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
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
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `barcode`         varchar(100)     NOT NULL                COMMENT '鏈哄櫒鏉＄爜',
  `deliver_number`  varchar(50)      DEFAULT NULL            COMMENT '鍙戣揣鍗曞彿',
  `hq_company_id`   bigint unsigned  DEFAULT NULL            COMMENT '褰掑睘鎬婚儴ID',
  `cust_id`         varchar(64)      DEFAULT NULL            COMMENT 'CRM鍏徃ID',
  `sales_org`       varchar(64)      DEFAULT NULL            COMMENT '閿€鍞粍缁?,
  `product_code`    varchar(64)      DEFAULT NULL            COMMENT '鐗╂枡缂栫爜',
  `product_name`    varchar(128)     DEFAULT NULL            COMMENT '鍟嗗搧鍚嶇О',
  `product_model`   varchar(100)     DEFAULT NULL            COMMENT '浜у搧鍨嬪彿',
  `machine_no`      varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒灏忓彿',
  `brand_code`      varchar(32)      DEFAULT NULL            COMMENT '鍝佺墝缂栫爜',
  `scan_date`       datetime         DEFAULT NULL            COMMENT '鏉＄爜鎵弿鏃堕棿',
  `dealer_out_date` datetime         DEFAULT NULL            COMMENT '缁忛攢鍟嗘渶鏂板嚭搴撴棩鏈?,
  `crm_add_time`    datetime         DEFAULT NULL            COMMENT 'CRM鍒涘缓鏃堕棿',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `warranty_status` varchar(16)      DEFAULT NULL            COMMENT '璐ㄤ繚鐘舵€?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_machine_barcode_hq` (`hq_company_id`),
  KEY `idx_machine_barcode_product` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏈哄櫒鏉＄爜妗ｆ琛?;

-- 31. CRM 鍏徃鏄犲皠琛?-- -------------------------------------------
DROP TABLE IF EXISTS `crm_company_mapping`;
CREATE TABLE `crm_company_mapping` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `cust_id`       varchar(64)      DEFAULT NULL            COMMENT 'CRM鍏徃ID',
  `sales_org`     varchar(64)      DEFAULT NULL            COMMENT '閿€鍞粍缁?,
  `hq_company_id` bigint unsigned  DEFAULT NULL            COMMENT '褰掑睘鎬婚儴ID',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_company_mapping_cust` (`cust_id`),
  UNIQUE KEY `uk_crm_company_mapping_sales_org` (`sales_org`),
  KEY `idx_crm_company_mapping_hq` (`hq_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM鍏徃鏄犲皠琛?;

-- -------------------------------------------
-- 32. CRM 鎬婚儴-涓€绾х绾﹀揩鐓ц〃
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_hq_first_contract_snapshot`;
CREATE TABLE `crm_hq_first_contract_snapshot` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `kunnr`            varchar(64)      NOT NULL                COMMENT 'CRM瀹㈡埛缂栫爜',
  `cust_id`          bigint unsigned  DEFAULT NULL            COMMENT 'CRM浼佷笟ID',
  `crm_company_name` varchar(200)     DEFAULT NULL            COMMENT 'CRM浼佷笟鍚嶇О',
  `sales_org`        varchar(64)      NOT NULL                COMMENT '閿€鍞粍缁?,
  `region_code`      varchar(64)      DEFAULT NULL            COMMENT 'CRM澶у尯缂栫爜',
  `region_name`      varchar(100)     DEFAULT NULL            COMMENT 'CRM澶у尯鍚嶇О',
  `alive_flag`       tinyint          DEFAULT NULL            COMMENT 'CRM鏈夋晥鏍囪瘑',
  `crm_add_time`     datetime         DEFAULT NULL            COMMENT 'CRM鏂板鏃堕棿',
  `crm_oper_time`    datetime         DEFAULT NULL            COMMENT 'CRM鎿嶄綔鏃堕棿',
  `last_sync_time`   datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_hq_first_contract` (`kunnr`, `sales_org`),
  KEY `idx_crm_hq_first_contract_sales_org` (`sales_org`),
  KEY `idx_crm_hq_first_contract_region_code` (`region_code`),
  KEY `idx_crm_hq_first_contract_oper_time` (`crm_oper_time`),
  KEY `idx_crm_hq_first_contract_add_time` (`crm_add_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM鎬婚儴-涓€绾х绾﹀揩鐓ц〃';

-- -------------------------------------------
-- 28. 鏁呴殰涓庣淮淇厤缃〃
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config`;
CREATE TABLE `fault_repair_config` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '褰掑睘鎬婚儴ID',
  `product_code`  varchar(64)      DEFAULT NULL            COMMENT '鐗╂枡缂栫爜',
  `product_model` varchar(64)      DEFAULT NULL            COMMENT '浜у搧鍨嬪彿',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_product` (`company_id`, `product_code`, `product_model`),
  KEY `idx_fault_repair_config_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏁呴殰涓庣淮淇厤缃〃';

-- -------------------------------------------
-- 29. 鏁呴殰涓庣淮淇厤缃晠闅滈」琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_fault`;
CREATE TABLE `fault_repair_config_fault` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `config_id`   bigint unsigned NOT NULL                COMMENT '閰嶇疆ID',
  `fault_desc`  varchar(500)    NOT NULL                COMMENT '鏁呴殰鎻忚堪',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `create_time` datetime        NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime        NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_fault` (`config_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏁呴殰涓庣淮淇厤缃晠闅滈」琛?;

-- -------------------------------------------
-- 30. 鏁呴殰涓庣淮淇厤缃淮淇」琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_option`;
CREATE TABLE `fault_repair_config_option` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `fault_id`    bigint unsigned NOT NULL                COMMENT '鏁呴殰椤笽D',
  `repair_desc` varchar(500)    NOT NULL                COMMENT '缁翠慨璇存槑',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '鎺掑簭鍙?,
  `create_time` datetime        NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime        NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_option_fault` (`fault_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏁呴殰涓庣淮淇厤缃淮淇」琛?;

-- -------------------------------------------
-- 31a. CRM 鍏徃蹇収琛?-- -------------------------------------------
DROP TABLE IF EXISTS `crm_biz_company_snapshot`;
CREATE TABLE `crm_biz_company_snapshot` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `cust_id`             bigint unsigned  NOT NULL                COMMENT 'CRM瀹㈡埛ID',
  `cust_name`           varchar(200)     DEFAULT NULL            COMMENT '瀹㈡埛鍚嶇О',
  `juristic_cust_id`    varchar(50)      DEFAULT NULL            COMMENT '鑱旂郴浜?,
  `group_contact_phone` varchar(50)      DEFAULT NULL            COMMENT '鑱旂郴鐢佃瘽',
  `cellphone`           varchar(50)      DEFAULT NULL            COMMENT '鎵嬫満',
  `company_address`     varchar(200)     DEFAULT NULL            COMMENT '鍏徃鍦板潃',
  `sap_company_code`    varchar(64)      DEFAULT NULL            COMMENT 'SAP鍏徃缂栫爜',
  `cust_rage`           int              DEFAULT NULL            COMMENT '瀹㈡埛鑼冨洿',
  `company_short_name`  varchar(128)     DEFAULT NULL            COMMENT '鍏徃绠€绉?',
  `province_name`       varchar(64)      DEFAULT NULL            COMMENT '鐪佷唤',
  `city_name`           varchar(64)      DEFAULT NULL            COMMENT '鍩庡競',
  `district_name`       varchar(64)      DEFAULT NULL            COMMENT '鍖哄幙',
  `cust_state`          int              DEFAULT NULL            COMMENT '瀹㈡埛鐘舵€?,
  `add_date`            datetime         DEFAULT NULL            COMMENT 'CRM鏂板鏃堕棿',
  `oper_time`           datetime         DEFAULT NULL            COMMENT 'CRM鎿嶄綔鏃堕棿',
  `last_sync_time`      datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `create_time`         datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`         datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_company_snapshot_cust` (`cust_id`),
  KEY `idx_crm_biz_company_snapshot_sap_code` (`sap_company_code`),
  KEY `idx_crm_biz_company_snapshot_name` (`cust_name`),
  KEY `idx_crm_biz_company_snapshot_oper` (`oper_time`),
  KEY `idx_crm_biz_company_snapshot_add` (`add_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM鍏徃蹇収琛?;

-- -------------------------------------------
-- 31b. CRM 閿€鍞嚭搴撴壂鐮佸揩鐓ц〃
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_warehouse_scan_outstorage_snapshot`;
CREATE TABLE `crm_warehouse_scan_outstorage_snapshot` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `source_id`       bigint unsigned  NOT NULL                COMMENT 'CRM鍘熷涓婚敭',
  `ware_id`         bigint unsigned  DEFAULT NULL            COMMENT '鍑哄叆搴揑D',
  `warehouse_id`    bigint unsigned  DEFAULT NULL            COMMENT '浠撳簱ID',
  `scan_code`       varchar(30)      DEFAULT NULL            COMMENT '鏉＄爜',
  `scan_date`       datetime         DEFAULT NULL            COMMENT '鎵爜鏃堕棿',
  `cust_id`         bigint unsigned  DEFAULT NULL            COMMENT '浼佷笟ID',
  `product_numeric` varchar(50)      DEFAULT NULL            COMMENT '浜у搧缂栫爜',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '鏈€杩戝悓姝ユ椂闂?,
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_warehouse_scan_outstorage_source` (`source_id`),
  KEY `idx_crm_warehouse_scan_outstorage_code` (`scan_code`),
  KEY `idx_crm_warehouse_scan_outstorage_date` (`scan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM閿€鍞嚭搴撴壂鐮佸揩鐓ц〃';

-- -------------------------------------------
-- 32. 鍚屾浠诲姟琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task`;
CREATE TABLE `sync_task` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `task_code`       varchar(64)      NOT NULL                COMMENT '浠诲姟缂栫爜',
  `task_name`       varchar(128)     NOT NULL                COMMENT '浠诲姟鍚嶇О',
  `handler_code`    varchar(64)      NOT NULL                COMMENT '澶勭悊鍣ㄧ紪鐮?,
  `cron_expression` varchar(128)     NOT NULL                COMMENT 'Cron琛ㄨ揪寮?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '鐘舵€侊紙1=鍚敤锛?=鍋滅敤锛?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_task_code` (`task_code`),
  UNIQUE KEY `uk_sync_task_handler` (`handler_code`),
  KEY `idx_sync_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍚屾浠诲姟琛?;

-- -------------------------------------------
-- 33. 鍚屾浠诲姟鏃ュ織琛?-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task_log`;
CREATE TABLE `sync_task_log` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `task_id`         bigint unsigned NOT NULL                COMMENT '浠诲姟ID',
  `status`          varchar(16)     NOT NULL                COMMENT '鎵ц鐘舵€?,
  `start_time`      datetime        NOT NULL                COMMENT '寮€濮嬫椂闂?,
  `end_time`        datetime        DEFAULT NULL            COMMENT '缁撴潫鏃堕棿',
  `data_start_time` datetime        DEFAULT NULL            COMMENT '鏁版嵁寮€濮嬫椂闂?,
  `data_end_time`   datetime        DEFAULT NULL            COMMENT '鏁版嵁缁撴潫鏃堕棿',
  `message`         varchar(1000)   DEFAULT NULL            COMMENT '鎵ц淇℃伅',
  `create_time`     datetime        NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_sync_task_log_task` (`task_id`, `id`),
  KEY `idx_sync_task_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍚屾浠诲姟鏃ュ織琛?;

SET FOREIGN_KEY_CHECKS = 1;

