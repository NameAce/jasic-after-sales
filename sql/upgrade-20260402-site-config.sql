-- S7.2 网点配置：关系删除留痕表

DROP TABLE IF EXISTS `hq_first_contract_record`;
CREATE TABLE `hq_first_contract_record` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`           bigint unsigned  NOT NULL                COMMENT '原签约关系ID',
  `hq_company_id`       bigint unsigned  NOT NULL                COMMENT '总部公司ID',
  `first_company_id`    bigint unsigned  NOT NULL                COMMENT '一级网点公司ID',
  `region_id`           bigint unsigned  DEFAULT NULL            COMMENT '大区ID',
  `contract_no`         varchar(64)      DEFAULT NULL            COMMENT '合同编号',
  `status`              tinyint unsigned DEFAULT 1               COMMENT '原关系状态（1=有效，0=终止）',
  `remark`              varchar(256)     DEFAULT NULL            COMMENT '原关系备注',
  `operation_type`      varchar(16)      NOT NULL                COMMENT '操作类型',
  `operator_user_id`    bigint unsigned  DEFAULT NULL            COMMENT '操作人ID',
  `operator_company_id` bigint unsigned  DEFAULT NULL            COMMENT '操作公司ID',
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_hq_company_id` (`hq_company_id`),
  KEY `idx_first_company_id` (`first_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='总部-一级签约关系记录表';

DROP TABLE IF EXISTS `first_second_relation_record`;
CREATE TABLE `first_second_relation_record` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`           bigint unsigned  NOT NULL                COMMENT '原从属关系ID',
  `first_company_id`    bigint unsigned  NOT NULL                COMMENT '一级网点公司ID',
  `second_company_id`   bigint unsigned  NOT NULL                COMMENT '二级网点公司ID',
  `status`              tinyint unsigned DEFAULT 1               COMMENT '原关系状态（1=有效，0=解除）',
  `remark`              varchar(256)     DEFAULT NULL            COMMENT '原关系备注',
  `operation_type`      varchar(16)      NOT NULL                COMMENT '操作类型',
  `operator_user_id`    bigint unsigned  DEFAULT NULL            COMMENT '操作人ID',
  `operator_company_id` bigint unsigned  DEFAULT NULL            COMMENT '操作公司ID',
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_first_company_id` (`first_company_id`),
  KEY `idx_second_company_id` (`second_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='一级-二级从属关系记录表';
