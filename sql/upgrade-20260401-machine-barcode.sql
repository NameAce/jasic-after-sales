-- =============================================
-- 佳士售后系统 - 机器条码档案增量升级脚本
-- 适用场景：已有业务数据的库，禁止重跑 schema.sql / init-data.sql
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `machine_barcode` (
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

ALTER TABLE `machine_barcode`
  MODIFY COLUMN `barcode` varchar(100) NOT NULL COMMENT '机器条码',
  MODIFY COLUMN `hq_company_id` bigint unsigned DEFAULT NULL COMMENT '归属总部ID',
  MODIFY COLUMN `product_model` varchar(100) DEFAULT NULL COMMENT '产品型号',
  MODIFY COLUMN `machine_no` varchar(100) DEFAULT NULL COMMENT '机器小号',
  MODIFY COLUMN `scan_date` datetime DEFAULT NULL COMMENT '条码扫描时间';

ALTER TABLE `machine_barcode`
  ADD COLUMN IF NOT EXISTS `deliver_number` varchar(50) DEFAULT NULL COMMENT '发货单号' AFTER `barcode`,
  ADD COLUMN IF NOT EXISTS `cust_id` varchar(64) DEFAULT NULL COMMENT 'CRM公司ID' AFTER `hq_company_id`,
  ADD COLUMN IF NOT EXISTS `sales_org` varchar(64) DEFAULT NULL COMMENT '销售组织' AFTER `cust_id`,
  ADD COLUMN IF NOT EXISTS `product_name` varchar(128) DEFAULT NULL COMMENT '商品名称' AFTER `product_code`,
  ADD COLUMN IF NOT EXISTS `machine_no` varchar(100) DEFAULT NULL COMMENT '机器小号' AFTER `product_model`,
  ADD COLUMN IF NOT EXISTS `scan_date` datetime DEFAULT NULL COMMENT '条码扫描时间' AFTER `brand_code`,
  ADD COLUMN IF NOT EXISTS `dealer_out_date` datetime DEFAULT NULL COMMENT '经销商最新出库日期' AFTER `scan_date`,
  ADD COLUMN IF NOT EXISTS `crm_add_time` datetime DEFAULT NULL COMMENT 'CRM创建时间' AFTER `dealer_out_date`,
  ADD COLUMN IF NOT EXISTS `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间' AFTER `crm_add_time`;
