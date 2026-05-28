-- =============================================
-- 浣冲＋鍞悗绯荤粺 - 鏁版嵁搴揇DL鑴氭湰
-- 鏁版嵁搴擄細jasic_after_sales
-- 瀛楃闆嗭細utf8mb4
-- 鎺掑簭瑙勫垯锛歶tf8mb4_general_ci
-- 閸?1瀵姾銆?
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------
-- 1. 閸忣剙寰冪猾璇茬€风€涙鍚€鐞?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company_type`;
CREATE TABLE `sys_company_type` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `type_code`    varchar(32)      NOT NULL                COMMENT '缁鐎风紓鏍垳閿涘湧LATFORM/HQ_A/HQ_B/HQ_C/HQ_D/FIRST/SECOND閿?,
  `type_name`    varchar(64)      NOT NULL                COMMENT '绫诲瀷鍚嶇О',
  `subject_type` varchar(16)      NOT NULL                COMMENT '娑撹缍嬬猾璇茬€烽敍鍦ATFORM/HQ/SERVICE閿?,
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `order_num`    int              DEFAULT 0               COMMENT '鎺掑簭',
  `create_time`  datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`  datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閸忣剙寰冪猾璇茬€风€涙鍚€鐞?;

-- -------------------------------------------
-- 2. 涓浗琛屾斂鍖哄垝鏍囧噯琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_area`;
CREATE TABLE `sys_area` (
  `area_code`    varchar(6)       NOT NULL                COMMENT '琛屾斂鍖虹紪鐮?,
  `area_name`    varchar(64)      NOT NULL                COMMENT '琛屾斂鍖哄悕绉?,
  `parent_code`  varchar(6)       NOT NULL                COMMENT '鐖剁骇缂栫爜',
  `area_level`   varchar(16)      NOT NULL                COMMENT '灞傜骇(PROVINCE/CITY/DISTRICT)',
  `full_name`    varchar(255)     DEFAULT NULL            COMMENT '瀹屾暣鍚嶇О',
  `sort_num`     int              DEFAULT 0               COMMENT '鎺掑簭',
  `status`       tinyint unsigned DEFAULT 1               COMMENT '鐘舵€?1=鍚敤,0=鍋滅敤)',
  `create_time`  datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`  datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`area_code`),
  KEY `idx_sys_area_parent` (`parent_code`, `sort_num`),
  KEY `idx_sys_area_level` (`area_level`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='涓浗琛屾斂鍖哄垝鏍囧噯琛?;

-- -------------------------------------------
-- 3. 鍏徃琛?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_company`;
CREATE TABLE `sys_company` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `company_name`     varchar(128)     NOT NULL                COMMENT '鍏徃鍚嶇О',
  `company_short_name` varchar(128)   DEFAULT NULL            COMMENT '鍏徃绠€绉?,
  `company_code`     varchar(64)      DEFAULT NULL            COMMENT '鍏徃缂栫爜',
  `type_code`        varchar(32)      NOT NULL                COMMENT '鍏徃绫诲瀷缂栫爜',
  `contact_name`     varchar(64)      NOT NULL                COMMENT '鑱旂郴浜?,
  `contact_phone`    varchar(20)      NOT NULL                COMMENT '鑱旂郴鐢佃瘽',
  `province_code`    varchar(6)       NOT NULL                COMMENT '鐪佷唤缂栫爜',
  `province_name`    varchar(64)      NOT NULL                COMMENT '鐪佷唤鍚嶇О',
  `city_code`        varchar(6)       NOT NULL                COMMENT '鍩庡競缂栫爜',
  `city_name`        varchar(64)      NOT NULL                COMMENT '鍩庡競鍚嶇О',
  `district_code`    varchar(6)       NOT NULL                COMMENT '鍖哄幙缂栫爜',
  `district_name`    varchar(64)      NOT NULL                COMMENT '鍖哄幙鍚嶇О',
  `detail_address`   varchar(255)     NOT NULL                COMMENT '璇︾粏鍦板潃',
  `full_address`     varchar(255)     DEFAULT NULL            COMMENT '瀹屾暣鍦板潃',
  `geocode_status`   varchar(16)      NOT NULL                COMMENT '鍦扮悊瑙ｆ瀽鐘舵€?,
  `longitude`        decimal(10,6)    DEFAULT NULL            COMMENT '缁忓害',
  `latitude`         decimal(10,6)    DEFAULT NULL            COMMENT '绾害',
  `service_phone`    varchar(32)      DEFAULT NULL            COMMENT '瀹㈡湇鐢佃瘽',
  `source_type`      varchar(16)      NOT NULL DEFAULT 'MANUAL' COMMENT '鏉ユ簮绫诲瀷',
  `sales_org`        varchar(64)      DEFAULT NULL            COMMENT '閿€鍞粍缁?,
  `status`           tinyint unsigned DEFAULT 1               COMMENT '鐘舵€?1=姝ｅ父,0=鍋滅敤)',
  `remark`           varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_code` (`company_code`),
  UNIQUE KEY `uk_company_sales_org` (`sales_org`),
  KEY `idx_type_code` (`type_code`),
  KEY `idx_company_region` (`province_code`, `city_code`, `district_code`),
  KEY `idx_company_geocode_status` (`geocode_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍏徃琛?;

-- -------------------------------------------
-- 4. 婢堆冨隘鐞?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='婢堆冨隘鐞?;

-- -------------------------------------------
-- 4. 鎬婚儴-涓€绾х绾﹀叧绯昏〃
-- -------------------------------------------
DROP TABLE IF EXISTS `hq_first_contract`;
CREATE TABLE `hq_first_contract` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `hq_company_id`    bigint unsigned  NOT NULL                COMMENT '鎬婚儴鍏徃ID',
  `first_company_id` bigint unsigned  NOT NULL                COMMENT '涓€绾х綉鐐瑰叕鍙窱D',
  `region_id`        bigint unsigned  DEFAULT NULL            COMMENT '婢堆冨隘ID閿涘牏顒风痪锔芥缂佹垵鐣鹃敍?,
  `contract_no`      varchar(64)      DEFAULT NULL            COMMENT '鍚堝悓缂栧彿',
  `status`           tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閺堝鏅ラ敍?=缂佸牊顒涢敍?,
  `remark`           varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hq_first` (`hq_company_id`, `first_company_id`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鎬婚儴-涓€绾х绾﹀叧绯昏〃';

-- -------------------------------------------
-- 5. 娑撯偓缁?娴滃瞼楠囨禒搴＄潣閸忓磭閮寸悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `first_second_relation`;
CREATE TABLE `first_second_relation` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `first_company_id`  bigint unsigned  NOT NULL                COMMENT '涓€绾х綉鐐瑰叕鍙窱D',
  `second_company_id` bigint unsigned  NOT NULL                COMMENT '浜岀骇缃戠偣鍏徃ID',
  `status`            tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閺堝鏅ラ敍?=鐟欙綁娅庨敍?,
  `remark`            varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`       datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`       datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_first_second` (`first_company_id`, `second_company_id`),
  UNIQUE KEY `uk_second` (`second_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='娑撯偓缁?娴滃瞼楠囨禒搴＄潣閸忓磭閮寸悰?;

-- -------------------------------------------
-- 6. B绔憳宸ヨ〃
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `username`        varchar(64)      NOT NULL                COMMENT '閻ц缍嶉悽銊﹀煕閸?,
  `password`        varchar(128)     NOT NULL                COMMENT '鐎靛棛鐖滈敍鍦擟rypt閸旂姴鐦戦敍?,
  `real_name`       varchar(64)      DEFAULT NULL            COMMENT '鐪熷疄濮撳悕',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '閹靛婧€閸?,
  `email`           varchar(64)      DEFAULT NULL            COMMENT '閭',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '澶村儚URL',
  `openid`          varchar(64)      DEFAULT NULL            COMMENT '瀵邦喕淇妎penid閿涘牆鐨粙瀣碍閻ц缍嶇紒鎴濈暰閿?,
  `wechat_phone`    varchar(20)      DEFAULT NULL            COMMENT '瀵邦喕淇婇幒鍫熸綀閹靛婧€閸欏嘲鎻╅悡?,
  `sex`             tinyint unsigned DEFAULT 0               COMMENT '閹冨焼閿?=閺堫亞鐓￠敍?=閻㈠嚖绱?=婵傜绱?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=濮濓絽鐖堕敍?=閸嬫粎鏁ら敍?,
  `is_deleted`      tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯侀崚鐘绘珟閿涘牓鈧槒绶崚鐘绘珟閿?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `last_login_time` datetime         DEFAULT NULL            COMMENT '閺堚偓閸氬海娅ヨぐ鏇熸闂?,
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='B绔憳宸ヨ〃';

-- -------------------------------------------
-- 7. 瀵邦喕淇婄紒鎴濈暰鐠佹澘缍嶇悰?-- -------------------------------------------
DROP TABLE IF EXISTS `wechat_bind_record`;
CREATE TABLE `wechat_bind_record` (
  `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id`           bigint unsigned NOT NULL                COMMENT '鐢ㄦ埛ID',
  `operate_type`      varchar(32)     NOT NULL                COMMENT '閹垮秳缍旂猾璇茬€烽敍鍦擨ND/UNBIND閿?,
  `operate_source`    varchar(32)     NOT NULL                COMMENT '閹垮秳缍旈弶銉︾爱閿涘湣P_BIND_LOGIN/PC_QR_BIND/PC_SELF_UNBIND閿?,
  `openid`            varchar(64)     NOT NULL                COMMENT '寰俊openid蹇収',
  `wechat_phone`      varchar(20)     DEFAULT NULL            COMMENT '瀵邦喕淇婇幒鍫熸綀閹靛婧€閸欏嘲鎻╅悡?,
  `operator_user_id`  bigint unsigned NOT NULL                COMMENT '鎿嶄綔浜篒D',
  `operator_username` varchar(64)     NOT NULL                COMMENT '鎿嶄綔浜虹敤鎴峰悕',
  `operate_time`      datetime        NOT NULL                COMMENT '鎿嶄綔鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_operate_time` (`user_id`, `operate_time`),
  KEY `idx_openid_operate_time` (`openid`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀵邦喕淇婄紒鎴濈暰鐠佹澘缍嶇悰?;

-- -------------------------------------------
-- 8. C绔鎴疯〃
-- -------------------------------------------
DROP TABLE IF EXISTS `c_user`;
CREATE TABLE `c_user` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `openid`          varchar(64)      NOT NULL                COMMENT '寰俊openid',
  `phone`           varchar(20)      DEFAULT NULL            COMMENT '閹靛婧€閸欏嚖绱欏顔讳繆閹哄牊娼堥懢宄板絿閿?,
  `nickname`        varchar(64)      DEFAULT NULL            COMMENT '鏄电О',
  `avatar`          varchar(256)     DEFAULT NULL            COMMENT '澶村儚URL',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=濮濓絽鐖堕敍?=閸嬫粎鏁ら敍?,
  `last_login_time` datetime         DEFAULT NULL            COMMENT '閺堚偓閸氬海娅ヨぐ鏇熸闂?,
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C绔鎴疯〃';

-- -------------------------------------------
-- 9. C缁旑垰顓归幋宄版勾閸р偓鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `customer_address`;
CREATE TABLE `customer_address` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `customer_id`    bigint unsigned  NOT NULL                COMMENT '瀹㈡埛ID',
  `contact_name`   varchar(64)      NOT NULL                COMMENT '閼辨梻閮存禍?,
  `contact_mobile` varchar(20)      NOT NULL                COMMENT '閼辨梻閮撮幍瀣簚閸?,
  `province`       varchar(64)      NOT NULL                COMMENT '閻?,
  `city`           varchar(64)      NOT NULL                COMMENT '鐢?,
  `county`         varchar(64)      DEFAULT NULL            COMMENT '鍖哄幙',
  `detail_address` varchar(255)     NOT NULL                COMMENT '璇︾粏鍦板潃',
  `is_default`     tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯佹妯款吇閸︽澘娼冮敍?=閺勵垽绱?=閸氾讣绱?,
  `create_time`    datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`    datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_customer_address_customer` (`customer_id`),
  KEY `idx_customer_address_default` (`customer_id`, `is_default`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='C缁旑垰顓归幋宄版勾閸р偓鐞?;

-- -------------------------------------------
-- 8. 閻劍鍩?閸忣剙寰冮崗瀹犱粓鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user_company`;
CREATE TABLE `sys_user_company` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id`     bigint unsigned  NOT NULL                COMMENT '鐢ㄦ埛ID',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '鍏徃ID',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯佹妯款吇閸忣剙寰冮敍?=閺勵垽绱?=閸氾讣绱?,
  `is_primary_account` tinyint unsigned DEFAULT 0        COMMENT '鏄惁鍏徃涓昏处鍙凤紙1=鏄紝0=鍚︼級',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_company` (`user_id`, `company_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_company_primary_account` (`company_id`, `is_primary_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閻劍鍩?閸忣剙寰冮崗瀹犱粓鐞?;

-- -------------------------------------------
-- 9. 鐟欐帟澹婄悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `company_id`  bigint unsigned  NOT NULL                COMMENT '褰掑睘鍏徃ID',
  `role_name`   varchar(64)      NOT NULL                COMMENT '瑙掕壊鍚嶇О',
  `role_key`    varchar(64)      NOT NULL                COMMENT '瑙掕壊鏍囪瘑',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '閺佺増宓侀懠鍐ㄦ纯閿涘湏LL/REGION/SELF閿?,
  `role_type`   tinyint unsigned DEFAULT 0               COMMENT '鐟欐帟澹婄猾璇茬€烽敍?=閼奉亜鐣炬稊澶庮潡閼硅绱?=閸忣剙寰冪粻锛勬倞閸涙顫楅懝璇х礉2=濡剝婢樼憴鎺曞閿?,
  `is_system`   tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯佺化鑽ょ埠鐟欐帟澹婇敍?=閺勵垽绱濇稉宥呭讲閸掔娀娅庨敍?,
  `status`      tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=濮濓絽鐖堕敍?=閸嬫粎鏁ら敍?,
  `order_num`   int              DEFAULT 0               COMMENT '鎺掑簭',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_role_key` (`company_id`, `role_key`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐟欐帟澹婄悰?;

-- -------------------------------------------
-- 10. 鐟欐帟澹婂Ο鈩冩緲鐞?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template`;
CREATE TABLE `sys_role_template` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `type_code`   varchar(32)      NOT NULL                COMMENT '鍏徃绫诲瀷缂栫爜',
  `role_name`   varchar(64)      NOT NULL                COMMENT '瑙掕壊鍚嶇О',
  `role_key`    varchar(64)      NOT NULL                COMMENT '瑙掕壊鏍囪瘑',
  `data_scope`  varchar(16)      DEFAULT 'SELF'          COMMENT '閺佺増宓侀懠鍐ㄦ纯閿涘湏LL/REGION/SELF閿?,
  `is_admin`    tinyint unsigned DEFAULT 0               COMMENT '鏄惁绠＄悊鍛樿鑹叉ā鏉匡紙1=鏄紝姣忕绫诲瀷鏈€澶氫竴涓級',
  `order_num`   int              DEFAULT 0               COMMENT '鎺掑簭',
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_role_key` (`type_code`, `role_key`),
  KEY `idx_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐟欐帟澹婂Ο鈩冩緲鐞?;

-- -------------------------------------------
-- 11. 鐟欐帟澹婂Ο鈩冩緲-閼挎粌宕熼崗瀹犱粓鐞?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_role_template_menu`;
CREATE TABLE `sys_role_template_menu` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `template_id` bigint unsigned  NOT NULL                COMMENT '濡剝婢業D',
  `menu_id`     bigint unsigned  NOT NULL                COMMENT '鑿滃崟ID',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_menu` (`template_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐟欐帟澹婂Ο鈩冩緲-閼挎粌宕熼崗瀹犱粓鐞?;

-- -------------------------------------------
-- 12. 閼挎粌宕熺悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `subject_type`  varchar(16)      NOT NULL                COMMENT '閹碘偓鐏炵偘瀵屾担鎾惰閸ㄥ绱橮LATFORM/HQ/SERVICE閿?,
  `menu_name`     varchar(64)      NOT NULL                COMMENT '鑿滃崟鍚嶇О',
  `parent_id`     bigint unsigned  DEFAULT 0               COMMENT '娑撳﹦楠囬懣婊冨礋ID閿?娑撴椽銆婄痪褝绱?,
  `menu_type`     char(1)          NOT NULL                COMMENT '缁鐎烽敍鍦?閻╊喖缍嶉敍瀛?閼挎粌宕熼敍瀛?閹稿鎸抽敍?,
  `path`          varchar(128)     DEFAULT NULL            COMMENT '璺敱鍦板潃',
  `component`     varchar(128)     DEFAULT NULL            COMMENT '缁勪欢璺緞',
  `perms`         varchar(128)     DEFAULT NULL            COMMENT '閺夊啴妾洪弽鍥槕閿涘牆顩?system:user:list閿?,
  `icon`          varchar(64)      DEFAULT NULL            COMMENT '鍥炬爣',
  `order_num`     int              DEFAULT 0               COMMENT '鎺掑簭',
  `is_visible`    tinyint unsigned DEFAULT 1               COMMENT '閺勵垰鎯侀崣顖濐潌閿?=閺勵垽绱?=閸氾讣绱?,
  `status`        tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=濮濓絽鐖堕敍?=閸嬫粎鏁ら敍?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閼挎粌宕熺悰?;

-- -------------------------------------------
-- 13. 閸忣剙寰冪猾璇茬€?閼挎粌宕熸稉濠囨鐞?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閸忣剙寰冪猾璇茬€?閼挎粌宕熸稉濠囨鐞?;

-- -------------------------------------------
-- 14. 鐟欐帟澹?閼挎粌宕熼崗瀹犱粓鐞?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐟欐帟澹?閼挎粌宕熼崗瀹犱粓鐞?;

-- -------------------------------------------
-- 15. 閻劍鍩?鐟欐帟澹婇崗瀹犱粓鐞?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閻劍鍩?鐟欐帟澹婇崗瀹犱粓鐞?;

-- -------------------------------------------
-- 16. 閻劍鍩?婢堆冨隘閸忓疇浠堢悰?
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閻劍鍩?婢堆冨隘閸忓疇浠堢悰?;

-- -------------------------------------------
-- 17. 鐎涙鍚€缁鐎风悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `dict_name`   varchar(100)     NOT NULL                COMMENT '瀛楀吀鍚嶇О',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '瀛楀吀绫诲瀷',
  `status`      tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閸氼垳鏁ら敍?=閸嬫粎鏁ら敍?,
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐎涙鍚€缁鐎风悰?;

-- -------------------------------------------
-- 18. 鐎涙鍚€閺佺増宓佺悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `dict_type`   varchar(100)     NOT NULL                COMMENT '瀛楀吀绫诲瀷',
  `dict_label`  varchar(100)     NOT NULL                COMMENT '瀛楀吀鏍囩',
  `dict_value`  varchar(100)     NOT NULL                COMMENT '鐎涙鍚€闁款喖鈧?,
  `dict_sort`   int              DEFAULT 0               COMMENT '鎺掑簭',
  `css_class`   varchar(100)     DEFAULT NULL            COMMENT '閼奉亜鐣炬稊澶嬬壉瀵?,
  `list_class`  varchar(100)     DEFAULT NULL            COMMENT '鏍囩鏍峰紡',
  `is_default`  tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯佹妯款吇閿?=閺勵垽绱?=閸氾讣绱?,
  `status`      tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閸氼垳鏁ら敍?=閸嬫粎鏁ら敍?,
  `remark`      varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time` datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_value` (`dict_type`, `dict_value`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐎涙鍚€閺佺増宓佺悰?;

-- -------------------------------------------
-- 19. 閸欏倹鏆熺拋鍓х枂鐞?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `config_name`  varchar(100)     NOT NULL                COMMENT '鍙傛暟鍚嶇О',
  `config_key`   varchar(100)     NOT NULL                COMMENT '鍙傛暟閿悕',
  `config_value` text             NOT NULL                COMMENT '閸欏倹鏆熼柨顔尖偓?,
  `config_type`  tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯侀崘鍛枂閿?=閺勵垽绱?=閸氾讣绱?,
  `group_key`    varchar(64)      NOT NULL DEFAULT 'org'  COMMENT '配置分组标识',
  `remark`       varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`  datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`  datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閸欏倹鏆熺拋鍓х枂鐞?;

-- -------------------------------------------
-- 20. 閹垮秳缍旈弮銉ョ箶鐞?
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `title`           varchar(64)      DEFAULT NULL            COMMENT '鎿嶄綔妯″潡',
  `oper_type`       tinyint unsigned DEFAULT 0               COMMENT '閹垮秳缍旂猾璇茬€烽敍?=閸忔湹绮敍?=閺傛澘顤冮敍?=娣囶喗鏁奸敍?=閸掔娀娅庨敍?=閹哄牊娼堥敍?=鐎电厧鍤敍?=閻ц缍嶉敍?=閻ц鍤敍?=瀵搫鍩楁稉瀣殠閿?,
  `method`          varchar(256)     DEFAULT NULL            COMMENT '鐠囬攱鐪伴弬瑙勭《閿涘牏琚崥?閺傝纭堕崥宥忕礆',
  `request_method`  varchar(16)      DEFAULT NULL            COMMENT '鐠囬攱鐪伴弬鐟扮础閿涘湙ET/POST/PUT/DELETE閿?,
  `request_url`     varchar(256)     DEFAULT NULL            COMMENT '璇锋眰URL',
  `request_param`   text             DEFAULT NULL            COMMENT '璇锋眰鍙傛暟',
  `response_result` text             DEFAULT NULL            COMMENT '杩斿洖缁撴灉',
  `user_id`         bigint unsigned  DEFAULT NULL            COMMENT '鎿嶄綔浜篒D',
  `username`        varchar(64)      DEFAULT NULL            COMMENT '鎿嶄綔浜虹敤鎴峰悕',
  `company_id`      bigint unsigned  DEFAULT NULL            COMMENT '鎿嶄綔浜哄綋鍓嶅叕鍙窱D',
  `ip`              varchar(64)      DEFAULT NULL            COMMENT '鎿嶄綔IP',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '閹垮秳缍旈悩鑸碘偓渚婄礄1=閹存劕濮涢敍?=婢惰精瑙﹂敍?,
  `error_msg`       text             DEFAULT NULL            COMMENT '閿欒淇℃伅',
  `oper_time`       datetime         DEFAULT NULL            COMMENT '鎿嶄綔鏃堕棿',
  `cost_time`       bigint           DEFAULT 0               COMMENT '鑰楁椂锛堟绉掞級',
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閹垮秳缍旈弮銉ョ箶鐞?;

-- -------------------------------------------
-- 21. 宸ュ崟涓昏〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id`                          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `order_no`                    varchar(32)      NOT NULL                COMMENT '瀹搞儱宕熼崣?,
  `customer_id`                 bigint unsigned  DEFAULT NULL            COMMENT '瀹㈡埛ID',
  `customer_name`               varchar(64)      NOT NULL                COMMENT '瀹㈡埛濮撳悕',
  `customer_mobile`             varchar(20)      NOT NULL                COMMENT '鐎广垺鍩涢幍瀣簚閸?,
  `report_subject_type`         varchar(16)      NOT NULL                COMMENT '閹躲儰鎱ㄦ稉璁崇秼缁鐎烽敍鍦昒STOMER/COMPANY閿?,
  `report_company_id`           bigint unsigned  DEFAULT NULL            COMMENT '鎶ヤ慨涓讳綋鍏徃ID',
  `barcode`                     varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒鏉＄爜',
  `product_code`                varchar(64)      DEFAULT NULL            COMMENT '鐗╂枡缂栫爜',
  `product_name`                varchar(128)     DEFAULT NULL            COMMENT '鍟嗗搧鍚嶇О',
  `product_model`               varchar(100)     DEFAULT NULL            COMMENT '鏈哄櫒鍨嬪彿',
  `machine_no`                  varchar(100)     DEFAULT NULL            COMMENT '閺堝搫娅掔亸蹇撳娇',
  `brand_type`                  varchar(16)      DEFAULT NULL            COMMENT '鍝佺墝绫诲瀷',
  `brand_code`                  varchar(32)      DEFAULT NULL            COMMENT '鍝佺墝缂栫爜',
  `brand_name`                  varchar(64)      DEFAULT NULL            COMMENT '鍝佺墝鍚嶇О',
  `service_mode`                varchar(16)      NOT NULL                COMMENT '閺堝秴濮熼弬鐟扮础缂傛牜鐖滈敍鍦IL=鐎靛嫪鎱ㄩ敍瀛睺ORE=閸掓澘绨电紒缈犳叏閿?,
  `warranty_status`             varchar(16)      DEFAULT NULL            COMMENT '鐠愩劋绻氶悩鑸碘偓?,
  `fault_desc`                  text             DEFAULT NULL            COMMENT '瀹㈡埛鎶ヤ慨鎻忚堪',
  `fault_remark`  varchar(500)     DEFAULT NULL            COMMENT '鍏跺畠鏁呴殰璇存槑',
  `sender_name`                 varchar(64)      DEFAULT NULL            COMMENT '鐎靛嫪娆㈡禍鍝勵潣閸?,
  `sender_mobile`               varchar(20)      DEFAULT NULL            COMMENT '瀵勪欢浜烘墜鏈哄彿',
  `sender_address`              varchar(255)     DEFAULT NULL            COMMENT '瀵勪欢鍦板潃',
  `send_express_no`             varchar(64)      DEFAULT NULL            COMMENT '鐎靛嫪娆㈣箛顐︹偓鎺戝礋閸?,
  `main_status`                 varchar(32)      NOT NULL                COMMENT '娑撹崵濮搁幀?,
  `evaluate_status`             varchar(32)      NOT NULL                COMMENT '鐠囧嫪鐜悩鑸碘偓?,
  `current_accept_subject_type` varchar(16)      NOT NULL                COMMENT '瑜版挸澧犻崣妤冩倞娑撹缍嬬猾璇茬€烽敍鍦獷RVICE/HQ閿?,
  `current_accept_company_id`   bigint unsigned  NOT NULL                COMMENT '褰撳墠鍙楃悊鍏徃ID',
  `assigned_user_id`            bigint unsigned  DEFAULT NULL            COMMENT '褰撳墠缁翠慨鍛業D',
  `create_company_id`           bigint unsigned  NOT NULL                COMMENT '寤哄崟鏉ユ簮鍏徃ID',
  `create_entry_type`           varchar(32)      DEFAULT NULL            COMMENT '寤哄崟鍏ュ彛绫诲瀷',
  `hq_company_id`               bigint unsigned  NOT NULL                COMMENT '褰掑睘鎬婚儴ID',
  `fault_repair_config_id`      bigint unsigned  DEFAULT NULL            COMMENT '缁戝畾鐨勬晠闅滀笌缁翠慨閰嶇疆ID',
  `has_transfer`                tinyint unsigned DEFAULT 0               COMMENT '鏄惁鍙戠敓杩囪浆鍗曪紙1=鏄紝0=鍚︼級',
  `transfer_count`              int unsigned     DEFAULT 0               COMMENT '杞崟娆℃暟',
  `return_method`               varchar(16)      DEFAULT NULL            COMMENT '閺堝搫娅掓潻鏂挎礀閺傜懓绱￠敍鍫濇礀鐎?閼奉亝褰侀敍?,
  `return_express_no`           varchar(64)      DEFAULT NULL            COMMENT '閸ョ偛鐦庤箛顐︹偓鎺戝礋閸?,
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
  KEY `idx_customer_id` (`customer_id`, `id`),
  KEY `idx_customer_mobile` (`customer_mobile`),
  KEY `idx_barcode` (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟涓昏〃';

-- -------------------------------------------
-- 22. 平台反馈单
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_feedback`;
CREATE TABLE `sys_feedback` (
  `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submitter_type`       varchar(64)     NOT NULL COMMENT '提交主体类型（CUSTOMER/SERVICE_COMPANY_USER）',
  `submitter_id`         bigint unsigned NOT NULL COMMENT '提交人ID',
  `submitter_name`       varchar(128)    DEFAULT NULL COMMENT '提交人姓名快照',
  `submit_company_id`    bigint unsigned DEFAULT NULL COMMENT '提交网点ID',
  `submit_source_type`   varchar(64)     NOT NULL COMMENT '提交来源类型（CUSTOMER_WORK_ORDER/CUSTOMER_DIRECT/SERVICE_COMPANY）',
  `submit_source_name`   varchar(128)    NOT NULL COMMENT '提交来源名称快照',
  `contact_phone`        varchar(32)     DEFAULT NULL COMMENT '联系电话快照',
  `related_work_order_id` bigint unsigned DEFAULT NULL COMMENT '关联工单ID',
  `hq_company_id`        bigint unsigned NOT NULL COMMENT '归属总部ID',
  `content`              varchar(500)    NOT NULL COMMENT '反馈内容',
  `status`               varchar(32)     NOT NULL COMMENT '反馈状态（UNACCEPTED/ACCEPTED）',
  `accept_user_id`       bigint unsigned DEFAULT NULL COMMENT '受理人系统用户ID',
  `accept_user_name`     varchar(128)    DEFAULT NULL COMMENT '受理人姓名快照',
  `accept_time`          datetime        DEFAULT NULL COMMENT '受理时间',
  `accept_reply`         varchar(200)    DEFAULT NULL COMMENT '受理回复',
  `create_time`          datetime        NOT NULL COMMENT '创建时间',
  `update_time`          datetime        NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台反馈单';

-- -------------------------------------------
-- 23. 瀹搞儱宕熼梽鍕鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id`                bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `storage_type`      varchar(32)      NOT NULL COMMENT '瀛樺偍绫诲瀷',
  `bucket`            varchar(128)     NOT NULL COMMENT '鐎涙ê鍋嶅?,
  `object_key`        varchar(512)     NOT NULL COMMENT '鐎电钖勯柨?,
  `original_name`     varchar(255)     NOT NULL COMMENT '閸樼喎顫愰弬鍥︽閸?,
  `content_type`      varchar(128)     DEFAULT NULL COMMENT '鍐呭绫诲瀷',
  `file_size`         bigint unsigned  NOT NULL COMMENT '鏂囦欢澶у皬',
  `file_ext`          varchar(32)      NOT NULL COMMENT '閹碘晛鐫嶉崥?,
  `file_hash`         varchar(128)     NOT NULL COMMENT '鏂囦欢鍝堝笇',
  `access_level`      varchar(32)      NOT NULL COMMENT '璁块棶绾у埆',
  `upload_user_id`    bigint unsigned  DEFAULT NULL COMMENT '涓婁紶鐢ㄦ埛ID',
  `upload_user_type`  varchar(32)      NOT NULL COMMENT '涓婁紶鐢ㄦ埛绫诲瀷',
  `upload_company_id` bigint unsigned  DEFAULT NULL COMMENT '涓婁紶鍏徃ID',
  `status`            varchar(32)      NOT NULL COMMENT '閺傚洣娆㈤悩鑸碘偓?,
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
  `biz_id`             bigint unsigned  NOT NULL COMMENT '娑撴艾濮烮D',
  `sort_num`           int              NOT NULL DEFAULT 1 COMMENT '閹烘帒绨崣?,
  `is_primary`         tinyint unsigned NOT NULL DEFAULT 0 COMMENT '閺勵垰鎯佹稉缁樻瀮娴?,
  `company_id`         bigint unsigned  DEFAULT NULL COMMENT '鍏徃ID',
  `operator_user_id`   bigint unsigned  DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  `operator_user_type` varchar(32)      DEFAULT NULL COMMENT '閹垮秳缍旀禍铏硅閸?,
  `remark`             varchar(255)     DEFAULT NULL COMMENT '澶囨敞',
  `create_time`        datetime         NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`        datetime         NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_biz_type_id_sort` (`biz_type`, `biz_id`, `sort_num`),
  KEY `idx_sys_file_biz_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閺傚洣娆㈡稉姘閸忓疇浠堢悰?;

-- -------------------------------------------
-- 22. 瀹搞儱宕熷ù浣芥祮閸樺棗褰剁悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_flow`;
CREATE TABLE `work_order_flow` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`       bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `action_type`         varchar(32)      NOT NULL                COMMENT '鍔ㄤ綔绫诲瀷',
  `before_status`       varchar(32)      DEFAULT NULL            COMMENT '閸斻劋缍旈崜宥勫瘜閻樿埖鈧?,
  `after_status`        varchar(32)      DEFAULT NULL            COMMENT '閸斻劋缍旈崥搴濆瘜閻樿埖鈧?,
  `from_company_id`     bigint unsigned  DEFAULT NULL            COMMENT '鏉ユ簮鍏徃ID',
  `to_company_id`       bigint unsigned  DEFAULT NULL            COMMENT '鐩爣鍏徃ID',
  `operator_company_id` bigint unsigned  NOT NULL                COMMENT '鎿嶄綔鍏徃ID',
  `operator_user_id`    bigint unsigned  NOT NULL                COMMENT '鎿嶄綔浜篒D',
  `remark`              varchar(500)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`         datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`         datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_action_time` (`work_order_id`, `create_time`),
  KEY `idx_action_to_company_order_time` (`action_type`, `to_company_id`, `work_order_id`, `create_time`),
  KEY `idx_to_company` (`to_company_id`),
  KEY `idx_operator_company` (`operator_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀹搞儱宕熷ù浣芥祮閸樺棗褰剁悰?;

-- -------------------------------------------
-- 23. 宸ュ崟鍙備笌鏂瑰揩鐓ц〃
-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_participant`;
CREATE TABLE `work_order_participant` (
  `id`                     bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`          bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`             bigint unsigned  NOT NULL                COMMENT '鍙備笌鍏徃ID',
  `subject_type`           varchar(16)      NOT NULL                COMMENT '娑撹缍嬬猾璇茬€烽敍鍦獷RVICE/HQ閿?,
  `participate_type`       varchar(32)      NOT NULL                COMMENT '閸欏倷绗岀猾璇茬€烽敍鍦昍EATE/CURRENT/HISTORY/HQ_OBSERVER閿?,
  `is_current_handler`     tinyint unsigned DEFAULT 0               COMMENT '鏄惁褰撳墠鍙楃悊鏂癸紙1=鏄紝0=鍚︼級',
  `first_participate_time` datetime         NOT NULL                COMMENT '棣栨鍙備笌鏃堕棿',
  `last_participate_time`  datetime         NOT NULL                COMMENT '閺堚偓閸氬骸寮稉搴㈡闂?,
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
  `action_type`   varchar(32)      NOT NULL                COMMENT '閸欏倷绗岄崝銊ょ稊缁鐎烽敍鍦盓CH_ACCEPT/QUOTE/REPAIR/REVIEW閿?,
  `action_time`   datetime         NOT NULL                COMMENT '鍔ㄤ綔鍙戠敓鏃堕棿',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_company_user` (`work_order_id`, `company_id`, `user_id`),
  KEY `idx_company_user_action_time` (`company_id`, `user_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鐢ㄦ埛绾у弬涓庝簨瀹炶〃';

-- -------------------------------------------
-- 25. 瀹搞儱宕熼幎銉ょ幆鐠佹澘缍嶇悰?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_quote`;
CREATE TABLE `work_order_quote` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '鎶ヤ环鍏徃ID',
  `quoted_by`        bigint unsigned  NOT NULL                COMMENT '鎶ヤ环浜篒D',
  `fault_judge`      varchar(255)     DEFAULT NULL            COMMENT '鏁呴殰鍒ゅ畾',
  `quote_amount`     decimal(10,2)    DEFAULT NULL            COMMENT '鎶ヤ环閲戦',
  `quote_desc`       varchar(500)     DEFAULT NULL            COMMENT '鎶ヤ环璇存槑',
  `is_current_valid` tinyint unsigned DEFAULT 1               COMMENT '閺勵垰鎯佽ぐ鎾冲閺堝鏅ラ幎銉ょ幆閿?=閺勵垽绱?=閸氾讣绱?,
  `create_time`      datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`      datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_quote_time` (`work_order_id`, `create_time`),
  KEY `idx_quote_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀹搞儱宕熼幎銉ょ幆鐠佹澘缍嶇悰?;

-- -------------------------------------------
-- 26. 瀹搞儱宕熺紒缈犳叏閻ф槒顔囩悰?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_repair`;
CREATE TABLE `work_order_repair` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id`  bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `company_id`     bigint unsigned  NOT NULL                COMMENT '缁翠慨鍏徃ID',
  `repair_user_id` bigint unsigned  NOT NULL                COMMENT '缁翠慨鍛業D',
  `register_stage` varchar(32)      NOT NULL DEFAULT 'REPAIR' COMMENT '閻ф槒顔囬梼鑸殿唽閿涘湩EPAIR=缂佺繝鎱ㄩ惂鏄忣唶閿涘ECHECK=婢跺秵顥呴惂鏄忣唶閿?,
  `is_finished`    tinyint unsigned DEFAULT 0               COMMENT '閺勵垰鎯佺紒缈犳叏鐎瑰本鍨氶敍?=閺勵垽绱?=閸氾讣绱?,
  `finished_time`  datetime         DEFAULT NULL            COMMENT '瀹屾垚鏃堕棿',
  `create_time`    datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`    datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_repair_time` (`work_order_id`, `create_time`),
  KEY `idx_repair_company` (`company_id`),
  KEY `idx_repair_user` (`repair_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀹搞儱宕熺紒缈犳叏閻ф槒顔囩悰?;

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
  `fault_remark`  varchar(500)     DEFAULT NULL            COMMENT '鍏跺畠鏁呴殰璇存槑',
  `repair_desc`   varchar(500)     DEFAULT NULL            COMMENT '缁翠慨璇存槑',
  `other_desc`    varchar(500)     DEFAULT NULL            COMMENT '鍏朵粬缁翠慨璇存槑',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '閹烘帒绨崣?,
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
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '閹烘帒绨崣?,
  `created_by`    bigint unsigned  NOT NULL                COMMENT '鐧昏浜篒D',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_part` (`fault_id`, `sort_num`),
  KEY `idx_work_order_fault_part_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_part_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宸ュ崟鏁呴殰鐐归厤浠舵槑缁嗚〃';

-- -------------------------------------------
-- 29. 瀹搞儱宕熺拠鍕幆鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `work_order_evaluation`;
CREATE TABLE `work_order_evaluation` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '宸ュ崟ID',
  `customer_id`   bigint unsigned  NOT NULL                COMMENT '瀹㈡埛ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '琚瘎浠锋湇鍔℃柟鍏徃ID',
  `timeliness_score`   tinyint unsigned DEFAULT NULL       COMMENT '鏈嶅姟鏃舵晥璇勫垎',
  `quality_score`      tinyint unsigned DEFAULT NULL       COMMENT '缁翠慨璐ㄩ噺璇勫垎',
  `satisfaction_score` tinyint unsigned DEFAULT NULL       COMMENT '閺堝秴濮熷鈩冨壈鎼达箒鐦庨崚?,
  `tags`          varchar(255)     DEFAULT NULL            COMMENT '鏍囩闆嗗悎',
  `content`       varchar(1000)    DEFAULT NULL            COMMENT '璇勪环鍐呭',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_eval` (`work_order_id`),
  KEY `idx_customer_eval` (`customer_id`),
  KEY `idx_eval_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='瀹搞儱宕熺拠鍕幆鐞?;

-- -------------------------------------------
-- 30. Notify scene and target config tables
-- -------------------------------------------
DROP TABLE IF EXISTS `notify_scene_target`;
DROP TABLE IF EXISTS `notify_scene`;
CREATE TABLE `notify_scene` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `scene_code`  varchar(64)     NOT NULL COMMENT '閫氱煡鍦烘櫙缂栫爜',
  `scene_name`  varchar(128)    NOT NULL COMMENT '閫氱煡鍦烘櫙鍚嶇О',
  `biz_type`    varchar(64)     NOT NULL COMMENT '涓氬姟绫诲瀷',
  `event_code`  varchar(64)     NOT NULL COMMENT '浜嬩欢缂栫爜',
  `status`      tinyint(1)      NOT NULL DEFAULT 1 COMMENT '鐘舵€侊細1鍚敤锛?鍋滅敤',
  `remark`      varchar(255)    DEFAULT NULL COMMENT '澶囨敞',
  `create_time` datetime        NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime        NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_code` (`scene_code`),
  KEY `idx_biz_type_status` (`biz_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閫氱煡鍦烘櫙琛?;

CREATE TABLE `notify_scene_target` (
  `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `scene_code`           varchar(64)     NOT NULL COMMENT '閫氱煡鍦烘櫙缂栫爜',
  `target_type`          varchar(32)     NOT NULL COMMENT '閫氱煡鐩爣绫诲瀷',
  `enabled`              tinyint(1)      NOT NULL DEFAULT 0 COMMENT '鐩爣寮€鍏筹細1鍚敤锛?鍋滅敤',
  `title_template`       varchar(128)    DEFAULT NULL COMMENT '鏍囬妯℃澘',
  `content_template`     varchar(512)    DEFAULT NULL COMMENT '鍐呭妯℃澘',
  `route_type`           varchar(64)     DEFAULT NULL COMMENT '璺宠浆绫诲瀷',
  `route_value_template` varchar(256)    DEFAULT NULL COMMENT '璺宠浆鍊兼ā鏉?,
  `config_json`          text            DEFAULT NULL COMMENT '鐩爣涓撳睘鍙傛暟 JSON',
  `remark`               varchar(255)    DEFAULT NULL COMMENT '澶囨敞',
  `create_time`          datetime        NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`          datetime        NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_target` (`scene_code`, `target_type`),
  KEY `idx_target_type_enabled` (`target_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閫氱煡鍦烘櫙鐩爣閰嶇疆琛?;

DROP TABLE IF EXISTS `sys_notify_message_log`;
DROP TABLE IF EXISTS `sys_notify_message`;
DROP TABLE IF EXISTS `sys_notify_event`;
CREATE TABLE `sys_notify_event` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `event_key`       varchar(128)    NOT NULL COMMENT '骞傜瓑閿?,
  `event_type`      varchar(64)     NOT NULL COMMENT '浜嬩欢绫诲瀷',
  `biz_type`        varchar(64)     NOT NULL COMMENT '涓氬姟绫诲瀷',
  `biz_id`          bigint unsigned NOT NULL COMMENT '涓氬姟ID',
  `biz_no`          varchar(64)     NOT NULL COMMENT '涓氬姟缂栧彿',
  `operator_id`     bigint unsigned DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  `receiver_id`     bigint unsigned DEFAULT NULL COMMENT '鎺ユ敹浜篒D',
  `payload_json`    text            NOT NULL COMMENT '浜嬩欢杞借嵎',
  `status`          varchar(32)     NOT NULL COMMENT '浜嬩欢鐘舵€?,
  `retry_count`     int unsigned    NOT NULL DEFAULT 0 COMMENT '閲嶈瘯娆℃暟',
  `next_retry_time` datetime        DEFAULT NULL COMMENT '涓嬫閲嶈瘯鏃堕棿',
  `error_message`   varchar(500)    DEFAULT NULL COMMENT '鏈€杩戜竴娆″け璐ヤ俊鎭?,
  `create_time`     datetime        NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime        NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_key` (`event_key`),
  KEY `idx_status_retry_time` (`status`, `next_retry_time`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閫氱煡浜嬩欢琛?;

CREATE TABLE `sys_notify_message` (
  `id`                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `event_id`            bigint unsigned NOT NULL COMMENT '鏉ユ簮浜嬩欢ID',
  `message_type`        varchar(32)     NOT NULL COMMENT '娑堟伅绫诲瀷',
  `event_type`          varchar(64)     NOT NULL COMMENT '浜嬩欢绫诲瀷',
  `biz_type`            varchar(64)     NOT NULL COMMENT '涓氬姟绫诲瀷',
  `biz_id`              bigint unsigned NOT NULL COMMENT '涓氬姟ID',
  `biz_no`              varchar(64)     NOT NULL COMMENT '涓氬姟缂栧彿',
  `receiver_id`         bigint unsigned NOT NULL COMMENT '鎺ユ敹浜篒D',
  `receiver_company_id` bigint unsigned NOT NULL COMMENT '鎺ユ敹鍏徃ID',
  `receiver_name`       varchar(64)     NOT NULL COMMENT '鎺ユ敹浜哄悕绉板揩鐓?,
  `title`               varchar(128)    NOT NULL COMMENT '鏍囬',
  `summary`             varchar(255)    NOT NULL COMMENT '鎽樿',
  `route_type`          varchar(32)     NOT NULL COMMENT '璺宠浆绫诲瀷',
  `route_value`         varchar(128)    NOT NULL COMMENT '璺宠浆鍊?,
  `todo_status`         varchar(32)     NOT NULL COMMENT '寰呭姙鐘舵€?,
  `invalid_reason`      varchar(64)     DEFAULT NULL COMMENT '澶辨晥鍘熷洜',
  `read_time`           datetime        DEFAULT NULL COMMENT '宸茶鏃堕棿',
  `done_time`           datetime        DEFAULT NULL COMMENT '宸插鐞嗘椂闂?,
  `invalid_time`        datetime        DEFAULT NULL COMMENT '澶辨晥鏃堕棿',
  `ext_json`            text            DEFAULT NULL COMMENT '鎵╁睍瀛楁',
  `create_time`         datetime        NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`         datetime        NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_status_time` (`receiver_id`, `todo_status`, `create_time`),
  KEY `idx_receiver_company_status_time` (`receiver_company_id`, `receiver_id`, `todo_status`, `create_time`),
  KEY `idx_biz_receiver` (`biz_type`, `biz_id`, `receiver_company_id`, `receiver_id`),
  KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閫氱煡娑堟伅琛?;

CREATE TABLE `sys_notify_message_log` (
  `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `message_id`     bigint unsigned NOT NULL COMMENT '娑堟伅ID',
  `action_type`    varchar(32)     NOT NULL COMMENT '鍔ㄤ綔绫诲瀷',
  `action_user_id` bigint unsigned DEFAULT NULL COMMENT '鍔ㄤ綔鎵ц浜?,
  `remark`         varchar(255)    DEFAULT NULL COMMENT '澶囨敞',
  `snapshot_json`  text            DEFAULT NULL COMMENT '蹇収',
  `create_time`    datetime        NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `update_time`    datetime        NOT NULL COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_message_time` (`message_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閫氱煡娑堟伅鏃ュ織琛?;

-- -------------------------------------------
-- 32. 閺堝搫娅掗弶锛勭垳濡楋絾顢嶇悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `machine_barcode`;
CREATE TABLE `machine_barcode` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `barcode`         varchar(100)     NOT NULL                COMMENT '鏈哄櫒鏉＄爜',
  `deliver_number`  varchar(50)      DEFAULT NULL            COMMENT '鍙戣揣鍗曞彿',
  `hq_company_id`   bigint unsigned  DEFAULT NULL            COMMENT '褰掑睘鎬婚儴ID',
  `cust_id`         varchar(64)      DEFAULT NULL            COMMENT 'CRM鍏徃ID',
  `sales_org`       varchar(64)      DEFAULT NULL            COMMENT '闁库偓閸烆喚绮嶇紒?,
  `product_code`    varchar(64)      DEFAULT NULL            COMMENT '鐗╂枡缂栫爜',
  `product_name`    varchar(128)     DEFAULT NULL            COMMENT '鍟嗗搧鍚嶇О',
  `product_model`   varchar(100)     DEFAULT NULL            COMMENT '浜у搧鍨嬪彿',
  `machine_no`      varchar(100)     DEFAULT NULL            COMMENT '閺堝搫娅掔亸蹇撳娇',
  `brand_code`      varchar(32)      DEFAULT NULL            COMMENT '鍝佺墝缂栫爜',
  `scan_date`       datetime         DEFAULT NULL            COMMENT '鏉＄爜鎵弿鏃堕棿',
  `dealer_out_date` datetime         DEFAULT NULL            COMMENT '缂佸繘鏀㈤崯鍡樻付閺傛澘鍤惔鎾存）閺?,
  `crm_add_time`    datetime         DEFAULT NULL            COMMENT 'CRM鍒涘缓鏃堕棿',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '閺堚偓鏉╂垵鎮撳銉︽闂?,
  `warranty_status` varchar(16)      DEFAULT NULL            COMMENT '鐠愩劋绻氶悩鑸碘偓?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閸氼垳鏁ら敍?=閸嬫粎鏁ら敍?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_machine_barcode_hq` (`hq_company_id`),
  KEY `idx_machine_barcode_product` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閺堝搫娅掗弶锛勭垳濡楋絾顢嶇悰?;

-- 31. CRM 閸忣剙寰冮弰鐘茬殸鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `crm_company_mapping`;
CREATE TABLE `crm_company_mapping` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `cust_id`       varchar(64)      DEFAULT NULL            COMMENT 'CRM鍏徃ID',
  `sales_org`     varchar(64)      DEFAULT NULL            COMMENT '闁库偓閸烆喚绮嶇紒?,
  `hq_company_id` bigint unsigned  DEFAULT NULL            COMMENT '褰掑睘鎬婚儴ID',
  `status`        tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閸氼垳鏁ら敍?=閸嬫粎鏁ら敍?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_company_mapping_cust` (`cust_id`),
  UNIQUE KEY `uk_crm_company_mapping_sales_org` (`sales_org`),
  KEY `idx_crm_company_mapping_hq` (`hq_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM閸忣剙寰冮弰鐘茬殸鐞?;

-- -------------------------------------------
-- 32. CRM 鎬婚儴-涓€绾х绾﹀揩鐓ц〃
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_hq_first_contract_snapshot`;
CREATE TABLE `crm_hq_first_contract_snapshot` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `kunnr`            varchar(64)      NOT NULL                COMMENT 'CRM瀹㈡埛缂栫爜',
  `cust_id`          bigint unsigned  DEFAULT NULL            COMMENT 'CRM娴间椒绗烮D',
  `crm_company_name` varchar(200)     DEFAULT NULL            COMMENT 'CRM浼佷笟鍚嶇О',
  `sales_org`        varchar(64)      NOT NULL                COMMENT '闁库偓閸烆喚绮嶇紒?,
  `region_code`      varchar(64)      DEFAULT NULL            COMMENT 'CRM澶у尯缂栫爜',
  `region_name`      varchar(100)     DEFAULT NULL            COMMENT 'CRM澶у尯鍚嶇О',
  `alive_flag`       tinyint          DEFAULT NULL            COMMENT 'CRM鏈夋晥鏍囪瘑',
  `crm_add_time`     datetime         DEFAULT NULL            COMMENT 'CRM鏂板鏃堕棿',
  `crm_oper_time`    datetime         DEFAULT NULL            COMMENT 'CRM鎿嶄綔鏃堕棿',
  `last_sync_time`   datetime         DEFAULT NULL            COMMENT '閺堚偓鏉╂垵鎮撳銉︽闂?,
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
  `status`        tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閸氼垳鏁ら敍?=閸嬫粎鏁ら敍?,
  `remark`        varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`   datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`   datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_product` (`company_id`, `product_code`, `product_model`),
  KEY `idx_fault_repair_config_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏁呴殰涓庣淮淇厤缃〃';

-- -------------------------------------------
-- 29. 閺佸懘娈版稉搴ｆ樊娣囶噣鍘ょ純顔芥櫊闂呮粓銆嶇悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_fault`;
CREATE TABLE `fault_repair_config_fault` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `config_id`   bigint unsigned NOT NULL                COMMENT '闁板秶鐤咺D',
  `fault_desc`  varchar(500)    NOT NULL                COMMENT '鏁呴殰鎻忚堪',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '閹烘帒绨崣?,
  `create_time` datetime        NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime        NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_config_fault` (`config_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閺佸懘娈版稉搴ｆ樊娣囶噣鍘ょ純顔芥櫊闂呮粓銆嶇悰?;

-- -------------------------------------------
-- 30. 閺佸懘娈版稉搴ｆ樊娣囶噣鍘ょ純顔炬樊娣囶噣銆嶇悰?
-- -------------------------------------------
DROP TABLE IF EXISTS `fault_repair_config_option`;
CREATE TABLE `fault_repair_config_option` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `fault_id`    bigint unsigned NOT NULL                COMMENT '鏁呴殰椤笽D',
  `repair_desc` varchar(500)    NOT NULL                COMMENT '缁翠慨璇存槑',
  `sort_num`    int unsigned    DEFAULT 0               COMMENT '閹烘帒绨崣?,
  `create_time` datetime        NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime        NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_fault_repair_option_fault` (`fault_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閺佸懘娈版稉搴ｆ樊娣囶噣鍘ょ純顔炬樊娣囶噣銆嶇悰?;

-- -------------------------------------------
-- 31a. CRM 閸忣剙寰冭箛顐ゅ弾鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `crm_biz_company_snapshot`;
CREATE TABLE `crm_biz_company_snapshot` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `cust_id`             bigint unsigned  NOT NULL                COMMENT 'CRM瀹㈡埛ID',
  `cust_name`           varchar(200)     DEFAULT NULL            COMMENT '瀹㈡埛鍚嶇О',
  `juristic_cust_id`    varchar(50)      DEFAULT NULL            COMMENT '閼辨梻閮存禍?,
  `group_contact_phone` varchar(50)      DEFAULT NULL            COMMENT '鑱旂郴鐢佃瘽',
  `cellphone`           varchar(50)      DEFAULT NULL            COMMENT '閹靛婧€',
  `company_address`     varchar(200)     DEFAULT NULL            COMMENT '鍏徃鍦板潃',
  `sap_company_code`    varchar(64)      DEFAULT NULL            COMMENT 'SAP鍏徃缂栫爜',
  `cust_rage`           int              DEFAULT NULL            COMMENT '瀹㈡埛鑼冨洿',
  `company_short_name`  varchar(128)     DEFAULT NULL            COMMENT '閸忣剙寰冪粻鈧粔?',
  `province_name`       varchar(64)      DEFAULT NULL            COMMENT '鐪佷唤',
  `city_name`           varchar(64)      DEFAULT NULL            COMMENT '鍩庡競',
  `district_name`       varchar(64)      DEFAULT NULL            COMMENT '鍖哄幙',
  `cust_state`          int              DEFAULT NULL            COMMENT '鐎广垺鍩涢悩鑸碘偓?,
  `add_date`            datetime         DEFAULT NULL            COMMENT 'CRM鏂板鏃堕棿',
  `oper_time`           datetime         DEFAULT NULL            COMMENT 'CRM鎿嶄綔鏃堕棿',
  `last_sync_time`      datetime         DEFAULT NULL            COMMENT '閺堚偓鏉╂垵鎮撳銉︽闂?,
  `create_time`         datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`         datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_biz_company_snapshot_cust` (`cust_id`),
  KEY `idx_crm_biz_company_snapshot_sap_code` (`sap_company_code`),
  KEY `idx_crm_biz_company_snapshot_name` (`cust_name`),
  KEY `idx_crm_biz_company_snapshot_oper` (`oper_time`),
  KEY `idx_crm_biz_company_snapshot_add` (`add_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM閸忣剙寰冭箛顐ゅ弾鐞?;

-- -------------------------------------------
-- 31a. CRM 涓€绾т簩绾у叧绯绘潵婧愬揩鐓ц〃
-- -------------------------------------------
DROP TABLE IF EXISTS `crm_first_second_relation_snapshot`;
CREATE TABLE `crm_first_second_relation_snapshot` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `source_id`      bigint unsigned  DEFAULT NULL            COMMENT 'CRM鍘熷鍏崇郴涓婚敭',
  `first_cust_id`  bigint unsigned  DEFAULT NULL            COMMENT '涓€绾RM浼佷笟ID',
  `second_cust_id` bigint unsigned  NOT NULL                COMMENT '浜岀骇CRM浼佷笟ID',
  `crm_oper_time`  datetime         DEFAULT NULL            COMMENT 'CRM鎿嶄綔鏃堕棿',
  `last_sync_time` datetime         DEFAULT NULL            COMMENT '閺堚偓鏉╂垵鎮撳銉︽闂?,
  `create_time`    datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`    datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_first_second_relation_second` (`second_cust_id`),
  UNIQUE KEY `uk_crm_first_second_relation_source` (`source_id`),
  KEY `idx_crm_first_second_relation_first` (`first_cust_id`),
  KEY `idx_crm_first_second_relation_oper_time` (`crm_oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM涓€绾т簩绾у叧绯绘潵婧愬揩鐓ц〃';

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
  `cust_id`         bigint unsigned  DEFAULT NULL            COMMENT '娴间椒绗烮D',
  `product_numeric` varchar(50)      DEFAULT NULL            COMMENT '浜у搧缂栫爜',
  `last_sync_time`  datetime         DEFAULT NULL            COMMENT '閺堚偓鏉╂垵鎮撳銉︽闂?,
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_warehouse_scan_outstorage_source` (`source_id`),
  KEY `idx_crm_warehouse_scan_outstorage_code` (`scan_code`),
  KEY `idx_crm_warehouse_scan_outstorage_date` (`scan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM閿€鍞嚭搴撴壂鐮佸揩鐓ц〃';

-- -------------------------------------------
-- 32. 閸氬本顒炴禒璇插鐞?-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task`;
CREATE TABLE `sync_task` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `task_code`       varchar(64)      NOT NULL                COMMENT '浠诲姟缂栫爜',
  `task_name`       varchar(128)     NOT NULL                COMMENT '浠诲姟鍚嶇О',
  `handler_code`    varchar(64)      NOT NULL                COMMENT '婢跺嫮鎮婇崳銊х椽閻?,
  `cron_expression` varchar(128)     NOT NULL                COMMENT 'Cron鐞涖劏鎻?,
  `status`          tinyint unsigned DEFAULT 1               COMMENT '閻樿埖鈧緤绱?=閸氼垳鏁ら敍?=閸嬫粎鏁ら敍?,
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '澶囨敞',
  `create_time`     datetime         NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  `update_time`     datetime         NOT NULL                COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_task_code` (`task_code`),
  UNIQUE KEY `uk_sync_task_handler` (`handler_code`),
  KEY `idx_sync_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閸氬本顒炴禒璇插鐞?;

-- -------------------------------------------
-- 33. 閸氬本顒炴禒璇插閺冦儱绻旂悰?-- -------------------------------------------
DROP TABLE IF EXISTS `sync_task_log`;
CREATE TABLE `sync_task_log` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `task_id`         bigint unsigned NOT NULL                COMMENT '娴犺濮烮D',
  `status`          varchar(16)     NOT NULL                COMMENT '閹笛嗩攽閻樿埖鈧?,
  `start_time`      datetime        NOT NULL                COMMENT '瀵偓婵妞傞梻?,
  `end_time`        datetime        DEFAULT NULL            COMMENT '缁撴潫鏃堕棿',
  `data_start_time` datetime        DEFAULT NULL            COMMENT '閺佺増宓佸鈧慨瀣闂?,
  `data_end_time`   datetime        DEFAULT NULL            COMMENT '鏁版嵁缁撴潫鏃堕棿',
  `trigger_type`    varchar(16)     NOT NULL DEFAULT 'SCHEDULED' COMMENT '瑙﹀彂绫诲瀷锛圡ANUAL/SCHEDULED锛?,
  `trigger_user_id` bigint unsigned NOT NULL DEFAULT 0       COMMENT '瑙﹀彂浜篒D锛?琛ㄧず绯荤粺浠诲姟',
  `message`         varchar(1000)   DEFAULT NULL            COMMENT '鎵ц淇℃伅',
  `create_time`     datetime        NOT NULL                COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_sync_task_log_task` (`task_id`, `id`),
  KEY `idx_sync_task_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='閸氬本顒炴禒璇插閺冦儱绻旂悰?;

SET FOREIGN_KEY_CHECKS = 1;


