-- 工单用户级历史参与事实表

CREATE TABLE IF NOT EXISTS `work_order_user_participant` (
  `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` bigint unsigned NOT NULL COMMENT '工单ID',
  `company_id`    bigint unsigned NOT NULL COMMENT '参与公司ID',
  `user_id`       bigint unsigned NOT NULL COMMENT '参与用户ID',
  `action_type`   varchar(32)     NOT NULL COMMENT '参与动作类型（TECH_ACCEPT/QUOTE/REPAIR/REVIEW）',
  `action_time`   datetime        NOT NULL COMMENT '动作发生时间',
  `create_time`   datetime        NOT NULL COMMENT '创建时间',
  `update_time`   datetime        NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_company_user` (`work_order_id`, `company_id`, `user_id`),
  KEY `idx_company_user_action_time` (`company_id`, `user_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单用户级参与事实表';
