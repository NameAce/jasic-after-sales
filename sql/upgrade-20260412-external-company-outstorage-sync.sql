-- =============================================
-- 佳士售后系统 - CRM 公司快照与销售出库扫码同步增量脚本
-- 适用场景：已存在同步任务基础设施，需要补公司快照导入与销售出库扫码投影
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `crm_biz_company_snapshot` (
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

CREATE TABLE IF NOT EXISTS `crm_warehouse_scan_outstorage_snapshot` (
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

INSERT INTO `sync_task` (`task_code`, `task_name`, `handler_code`, `cron_expression`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'BIZ_COMPANY_SNAPSHOT_SYNC', 'CRM公司快照同步', 'bizCompanySnapshotSync', '0 0 1 * * ?', 0,
       '默认内置 CRM 公司快照同步任务，启用后由 Quartz 按 Cron 调度', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sync_task` WHERE `handler_code` = 'bizCompanySnapshotSync'
);

INSERT INTO `sync_task` (`task_code`, `task_name`, `handler_code`, `cron_expression`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'WAREHOUSE_SCAN_OUTSTORAGE_SYNC', '销售出库扫码同步', 'warehouseScanOutstorageSync', '0 0 3 * * ?', 0,
       '默认内置销售出库扫码同步任务，启用后由 Quartz 按 Cron 调度', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sync_task` WHERE `handler_code` = 'warehouseScanOutstorageSync'
);
