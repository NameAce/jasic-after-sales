-- =============================================
-- 佳士售后系统 - 工单核心实体增量升级脚本
-- 适用场景：已有业务数据的库，禁止重跑 schema.sql / init-data.sql
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

-- -------------------------------------------
-- 1. 新增表：work_order
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order` (
  `id`                          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`                    varchar(32)      NOT NULL                COMMENT '工单号',
  `customer_id`                 bigint unsigned  DEFAULT NULL            COMMENT '客户ID',
  `customer_name`               varchar(64)      NOT NULL                COMMENT '客户姓名',
  `customer_mobile`             varchar(20)      NOT NULL                COMMENT '客户手机号',
  `report_subject_type`         varchar(16)      NOT NULL                COMMENT '报修主体类型（CUSTOMER/COMPANY）',
  `report_company_id`           bigint unsigned  DEFAULT NULL            COMMENT '报修主体公司ID',
  `barcode`                     varchar(100)     DEFAULT NULL            COMMENT '机器条码',
  `product_code`                varchar(64)      DEFAULT NULL            COMMENT '物料编码',
  `product_model`               varchar(100)     DEFAULT NULL            COMMENT '机器型号',
  `brand_code`                  varchar(32)      DEFAULT NULL            COMMENT '品牌编码',
  `service_mode`                varchar(16)      NOT NULL                COMMENT '服务方式编码（MAIL=寄修，STORE=到店维修）',
  `warranty_status`             varchar(16)      DEFAULT NULL            COMMENT '质保状态',
  `fault_desc`                  text             DEFAULT NULL            COMMENT '客户报修描述',
  `sender_name`                 varchar(64)      DEFAULT NULL            COMMENT '寄件人姓名',
  `sender_mobile`               varchar(20)      DEFAULT NULL            COMMENT '寄件人手机号',
  `sender_address`              varchar(255)     DEFAULT NULL            COMMENT '寄件地址',
  `send_express_no`             varchar(64)      DEFAULT NULL            COMMENT '寄件快递单号',
  `main_status`                 varchar(32)      NOT NULL                COMMENT '主状态',
  `evaluate_status`             varchar(32)      NOT NULL                COMMENT '评价状态',
  `current_accept_subject_type` varchar(16)      NOT NULL                COMMENT '当前受理主体类型（SERVICE/HQ）',
  `current_accept_company_id`   bigint unsigned  NOT NULL                COMMENT '当前受理公司ID',
  `assigned_user_id`            bigint unsigned  DEFAULT NULL            COMMENT '当前维修员ID',
  `create_company_id`           bigint unsigned  NOT NULL                COMMENT '建单来源公司ID',
  `hq_company_id`               bigint unsigned  NOT NULL                COMMENT '归属总部ID',
  `has_transfer`                tinyint unsigned DEFAULT 0               COMMENT '是否发生过转单（1=是，0=否）',
  `transfer_count`              int unsigned     DEFAULT 0               COMMENT '转单次数',
  `return_method`               varchar(16)      DEFAULT NULL            COMMENT '机器返回方式（回寄/自提）',
  `return_express_no`           varchar(64)      DEFAULT NULL            COMMENT '回寄快递单号',
  `close_reason`                varchar(255)     DEFAULT NULL            COMMENT '关闭原因',
  `completed_time`              datetime         DEFAULT NULL            COMMENT '完成时间',
  `closed_time`                 datetime         DEFAULT NULL            COMMENT '关闭时间',
  `create_time`                 datetime         NOT NULL                COMMENT '创建时间',
  `update_time`                 datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_current_accept_company` (`current_accept_company_id`),
  KEY `idx_assigned_user` (`assigned_user_id`),
  KEY `idx_hq_company` (`hq_company_id`),
  KEY `idx_main_status` (`main_status`),
  KEY `idx_report_company` (`report_company_id`),
  KEY `idx_customer_mobile` (`customer_mobile`),
  KEY `idx_barcode` (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单主表';

-- -------------------------------------------
-- 2. 新增表：work_order_flow
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_flow` (
  `id`                  bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`       bigint unsigned  NOT NULL                COMMENT '工单ID',
  `action_type`         varchar(32)      NOT NULL                COMMENT '动作类型',
  `before_status`       varchar(32)      DEFAULT NULL            COMMENT '动作前主状态',
  `after_status`        varchar(32)      DEFAULT NULL            COMMENT '动作后主状态',
  `from_company_id`     bigint unsigned  DEFAULT NULL            COMMENT '来源公司ID',
  `to_company_id`       bigint unsigned  DEFAULT NULL            COMMENT '目标公司ID',
  `operator_company_id` bigint unsigned  NOT NULL                COMMENT '操作公司ID',
  `operator_user_id`    bigint unsigned  NOT NULL                COMMENT '操作人ID',
  `remark`              varchar(500)     DEFAULT NULL            COMMENT '备注',
  `create_time`         datetime         NOT NULL                COMMENT '创建时间',
  `update_time`         datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_action_time` (`work_order_id`, `create_time`),
  KEY `idx_to_company` (`to_company_id`),
  KEY `idx_operator_company` (`operator_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单流转历史表';

-- -------------------------------------------
-- 3. 新增表：work_order_participant
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_participant` (
  `id`                     bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`          bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`             bigint unsigned  NOT NULL                COMMENT '参与公司ID',
  `subject_type`           varchar(16)      NOT NULL                COMMENT '主体类型（SERVICE/HQ）',
  `participate_type`       varchar(32)      NOT NULL                COMMENT '参与类型（CREATE/CURRENT/HISTORY/HQ_OBSERVER）',
  `is_current_handler`     tinyint unsigned DEFAULT 0               COMMENT '是否当前受理方（1=是，0=否）',
  `is_readonly`            tinyint unsigned DEFAULT 1               COMMENT '是否只读（1=是，0=否）',
  `first_participate_time` datetime         NOT NULL                COMMENT '首次参与时间',
  `last_participate_time`  datetime         NOT NULL                COMMENT '最后参与时间',
  `create_time`            datetime         NOT NULL                COMMENT '创建时间',
  `update_time`            datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_company` (`work_order_id`, `company_id`),
  KEY `idx_company_current` (`company_id`, `is_current_handler`),
  KEY `idx_company_readonly` (`company_id`, `is_readonly`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单参与方快照表';

-- -------------------------------------------
-- 4. 新增表：work_order_quote
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_quote` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '报价公司ID',
  `quoted_by`        bigint unsigned  NOT NULL                COMMENT '报价人ID',
  `fault_judge`      varchar(255)     DEFAULT NULL            COMMENT '故障判定',
  `quote_amount`     decimal(10,2)    DEFAULT NULL            COMMENT '报价金额',
  `quote_desc`       varchar(500)     DEFAULT NULL            COMMENT '报价说明',
  `is_current_valid` tinyint unsigned DEFAULT 1               COMMENT '是否当前有效报价（1=是，0=否）',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_quote_time` (`work_order_id`, `create_time`),
  KEY `idx_quote_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单报价记录表';

-- -------------------------------------------
-- 5. 新增表：work_order_repair
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_repair` (
  `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`  bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`     bigint unsigned  NOT NULL                COMMENT '维修公司ID',
  `repair_user_id` bigint unsigned  NOT NULL                COMMENT '维修员ID',
  `repair_summary` varchar(255)     DEFAULT NULL            COMMENT '维修摘要',
  `repair_desc`    text             DEFAULT NULL            COMMENT '维修说明',
  `other_desc`     varchar(500)     DEFAULT NULL            COMMENT '其他说明',
  `is_finished`    tinyint unsigned DEFAULT 0               COMMENT '是否维修完成（1=是，0=否）',
  `finished_time`  datetime         DEFAULT NULL            COMMENT '完成时间',
  `create_time`    datetime         NOT NULL                COMMENT '创建时间',
  `update_time`    datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_repair_time` (`work_order_id`, `create_time`),
  KEY `idx_repair_company` (`company_id`),
  KEY `idx_repair_user` (`repair_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单维修登记表';

-- -------------------------------------------
-- 6. 新增表：work_order_fault
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_fault` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `repair_id`     bigint unsigned  NOT NULL                COMMENT '维修登记ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '登记公司ID',
  `fault_desc`    varchar(500)     NOT NULL                COMMENT '故障描述',
  `repair_desc`   varchar(500)     DEFAULT NULL            COMMENT '维修说明',
  `part_desc`     varchar(500)     DEFAULT NULL            COMMENT '配件信息',
  `image_urls`    text             DEFAULT NULL            COMMENT '图片地址集合',
  `sort_num`      int unsigned     DEFAULT 0               COMMENT '排序号',
  `created_by`    bigint unsigned  NOT NULL                COMMENT '登记人ID',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_repair_fault` (`repair_id`, `sort_num`),
  KEY `idx_work_order_fault_time` (`work_order_id`, `create_time`),
  KEY `idx_fault_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单故障点记录表';

-- -------------------------------------------
-- 7. 新增表：work_order_review
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_review` (
  `id`                 bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`      bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`         bigint unsigned  NOT NULL                COMMENT '复检公司ID',
  `review_user_id`     bigint unsigned  NOT NULL                COMMENT '复检人ID',
  `review_result`      varchar(32)      NOT NULL                COMMENT '复检结果',
  `review_desc`        varchar(500)     DEFAULT NULL            COMMENT '复检说明',
  `is_continue_repair` tinyint unsigned DEFAULT 0               COMMENT '是否继续维修（1=是，0=否）',
  `create_time`        datetime         NOT NULL                COMMENT '创建时间',
  `update_time`        datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_review_time` (`work_order_id`, `create_time`),
  KEY `idx_review_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单复检记录表';

-- -------------------------------------------
-- 8. 新增表：work_order_evaluation
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_evaluation` (
  `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned  NOT NULL                COMMENT '工单ID',
  `customer_id`   bigint unsigned  NOT NULL                COMMENT '客户ID',
  `company_id`    bigint unsigned  NOT NULL                COMMENT '被评价服务方公司ID',
  `score`         tinyint unsigned NOT NULL                COMMENT '评分',
  `tags`          varchar(255)     DEFAULT NULL            COMMENT '标签集合',
  `content`       varchar(1000)    DEFAULT NULL            COMMENT '评价内容',
  `create_time`   datetime         NOT NULL                COMMENT '创建时间',
  `update_time`   datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_eval` (`work_order_id`),
  KEY `idx_customer_eval` (`customer_id`),
  KEY `idx_eval_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单评价表';

-- -------------------------------------------
-- 9. 新增表：work_order_notify_event
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_notify_event` (
  `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id`    bigint unsigned  NOT NULL                COMMENT '工单ID',
  `company_id`       bigint unsigned  NOT NULL                COMMENT '业务归属公司ID',
  `event_type`       varchar(32)      NOT NULL                COMMENT '事件类型',
  `trigger_node`     varchar(32)      NOT NULL                COMMENT '触发节点',
  `receiver_type`    varchar(32)      NOT NULL                COMMENT '接收对象类型',
  `receiver_id`      bigint unsigned  NOT NULL                COMMENT '接收对象ID',
  `title_snapshot`   varchar(255)     DEFAULT NULL            COMMENT '标题快照',
  `content_snapshot` text             DEFAULT NULL            COMMENT '内容快照',
  `send_status`      varchar(16)      NOT NULL                COMMENT '发送状态',
  `send_time`        datetime         DEFAULT NULL            COMMENT '发送时间',
  `fail_reason`      varchar(500)     DEFAULT NULL            COMMENT '失败原因',
  `create_time`      datetime         NOT NULL                COMMENT '创建时间',
  `update_time`      datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_notify_time` (`work_order_id`, `create_time`),
  KEY `idx_receiver_status` (`receiver_id`, `send_status`),
  KEY `idx_notify_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单通知事件表';
