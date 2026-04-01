-- =============================================
-- 佳士售后系统 - 机器条码档案增量升级脚本
-- 适用场景：已有业务数据的库，禁止重跑 schema.sql / init-data.sql
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `machine_barcode` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `barcode`         varchar(64)      NOT NULL                COMMENT '机器条码',
  `hq_company_id`   bigint unsigned  NOT NULL                COMMENT '归属总部ID',
  `product_code`    varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_model`   varchar(64)      DEFAULT NULL            COMMENT '产品型号',
  `brand_code`      varchar(32)      DEFAULT NULL            COMMENT '品牌编码',
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
