-- =============================================
-- 佳士售后系统 - CRM 签约快照增量脚本
-- 适用场景：已存在 CRM 公司映射和同步任务基础设施，需要补签约快照初始化
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `crm_hq_first_contract_snapshot` (
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

INSERT INTO `sync_task` (`task_code`, `task_name`, `handler_code`, `cron_expression`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'CRM_HQ_FIRST_CONTRACT_SNAPSHOT_SYNC', 'CRM签约快照同步', 'crmHqFirstContractSnapshotSync', '0 30 1 * * ?', 0,
       '默认内置 CRM 签约快照同步任务，启用后由 Quartz 按 Cron 调度', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sync_task` WHERE `handler_code` = 'crmHqFirstContractSnapshotSync'
);
