-- =============================================
-- 佳士售后系统 - CRM 一级二级关系来源快照增量脚本
-- 适用场景：已存在 CRM 公司快照与同步任务基础设施，需要补一级二级关系来源快照
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `crm_first_second_relation_snapshot` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`      bigint unsigned  DEFAULT NULL            COMMENT 'CRM原始关系主键',
  `first_cust_id`  bigint unsigned  DEFAULT NULL            COMMENT '一级CRM企业ID',
  `second_cust_id` bigint unsigned  NOT NULL                COMMENT '二级CRM企业ID',
  `crm_oper_time`  datetime         DEFAULT NULL            COMMENT 'CRM操作时间',
  `last_sync_time` datetime         DEFAULT NULL            COMMENT '最近同步时间',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_first_second_relation_second` (`second_cust_id`),
  UNIQUE KEY `uk_crm_first_second_relation_source` (`source_id`),
  KEY `idx_crm_first_second_relation_first` (`first_cust_id`),
  KEY `idx_crm_first_second_relation_oper_time` (`crm_oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CRM一级二级关系来源快照表';

INSERT INTO `sync_task` (`task_code`, `task_name`, `handler_code`, `cron_expression`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'CRM_FIRST_SECOND_RELATION_SNAPSHOT_SYNC', 'CRM一级二级关系来源快照同步', 'crmFirstSecondRelationSnapshotSync', '0 40 1 * * ?', 0,
       '默认内置 CRM 一级二级关系来源快照同步任务，启用后由 Quartz 按 Cron 调度', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sync_task` WHERE `handler_code` = 'crmFirstSecondRelationSnapshotSync'
);
