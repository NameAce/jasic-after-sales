-- =============================================
-- 佳士售后系统 - C端客户地址簿增量脚本
-- 适用场景：已执行过基础建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `customer_address` (
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
