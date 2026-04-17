-- 公司主档地址标准化改造
-- 1. 引入标准行政区划表
-- 2. 公司主档地址拆分为省/市/区县编码+名称、详细地址、完整地址、地理解析状态
-- 3. 老数据统一置为 FAILED，等待重新编辑后重算

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_area` (
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

ALTER TABLE `sys_company`
  CHANGE COLUMN `address` `detail_address` varchar(255) NOT NULL COMMENT '详细地址',
  ADD COLUMN `province_code` varchar(6) DEFAULT NULL COMMENT '省份编码' AFTER `contact_phone`,
  ADD COLUMN `city_code` varchar(6) DEFAULT NULL COMMENT '城市编码' AFTER `province_name`,
  ADD COLUMN `district_code` varchar(6) DEFAULT NULL COMMENT '区县编码' AFTER `city_name`,
  ADD COLUMN `full_address` varchar(255) DEFAULT NULL COMMENT '完整地址' AFTER `detail_address`,
  ADD COLUMN `geocode_status` varchar(16) NOT NULL DEFAULT 'FAILED' COMMENT '地理解析状态' AFTER `full_address`;

ALTER TABLE `sys_company`
  ADD KEY `idx_company_region` (`province_code`, `city_code`, `district_code`),
  ADD KEY `idx_company_geocode_status` (`geocode_status`);

UPDATE `sys_company`
SET `detail_address` = TRIM(`detail_address`),
    `province_code` = NULL,
    `city_code` = NULL,
    `district_code` = NULL,
    `full_address` = NULL,
    `longitude` = NULL,
    `latitude` = NULL,
    `geocode_status` = 'FAILED';
