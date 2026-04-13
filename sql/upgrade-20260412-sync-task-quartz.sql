-- =============================================
-- 佳士售后系统 - 同步任务管理与 Quartz 调度增量脚本
-- 适用场景：已存在系统菜单与条码档案能力，需要补同步任务管理
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sync_task` (
  `id`              bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_code`       varchar(64)      NOT NULL                COMMENT '任务编码',
  `task_name`       varchar(128)     NOT NULL                COMMENT '任务名称',
  `handler_code`    varchar(64)      NOT NULL                COMMENT '处理器编码',
  `cron_expression` varchar(128)     NOT NULL                COMMENT 'Cron表达式',
  `status`          tinyint unsigned DEFAULT 1               COMMENT '状态（1=启用，0=停用）',
  `remark`          varchar(256)     DEFAULT NULL            COMMENT '备注',
  `create_time`     datetime         NOT NULL                COMMENT '创建时间',
  `update_time`     datetime         NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_task_code` (`task_code`),
  UNIQUE KEY `uk_sync_task_handler` (`handler_code`),
  KEY `idx_sync_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务表';

CREATE TABLE IF NOT EXISTS `sync_task_log` (
  `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`         bigint unsigned NOT NULL                COMMENT '任务ID',
  `status`          varchar(16)     NOT NULL                COMMENT '执行状态',
  `start_time`      datetime        NOT NULL                COMMENT '开始时间',
  `end_time`        datetime        DEFAULT NULL            COMMENT '结束时间',
  `data_start_time` datetime        DEFAULT NULL            COMMENT '数据开始时间',
  `data_end_time`   datetime        DEFAULT NULL            COMMENT '数据结束时间',
  `message`         varchar(1000)   DEFAULT NULL            COMMENT '执行信息',
  `create_time`     datetime        NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_task_log_task` (`task_id`, `id`),
  KEY `idx_sync_task_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务日志表';

INSERT INTO `sync_task` (`task_code`, `task_name`, `handler_code`, `cron_expression`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'MACHINE_BARCODE_SYNC', '条码档案同步', 'machineBarcodeSync', '0 0 2 * * ?', 0,
       '默认内置条码档案同步任务，启用后由 Quartz 按 Cron 调度', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sync_task` WHERE `handler_code` = 'machineBarcodeSync'
);

SET @sync_task_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `menu_type` = 'C'
    AND `path` = 'syncTask'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '同步任务', 1, 'C', 'syncTask', 'system/syncTask/index', NULL, 'el-icon-time', 9, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @sync_task_menu_id IS NULL;

SET @sync_task_menu_id = (
  SELECT `id`
  FROM `sys_menu`
  WHERE `subject_type` = 'PLATFORM'
    AND `menu_type` = 'C'
    AND `path` = 'syncTask'
  LIMIT 1
);

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '任务查询', @sync_task_menu_id, 'F', NULL, NULL, 'system:syncTask:list', NULL, 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @sync_task_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @sync_task_menu_id
      AND `perms` = 'system:syncTask:list'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '任务新增', @sync_task_menu_id, 'F', NULL, NULL, 'system:syncTask:add', NULL, 2, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @sync_task_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @sync_task_menu_id
      AND `perms` = 'system:syncTask:add'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '任务修改', @sync_task_menu_id, 'F', NULL, NULL, 'system:syncTask:update', NULL, 3, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @sync_task_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @sync_task_menu_id
      AND `perms` = 'system:syncTask:update'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '任务执行', @sync_task_menu_id, 'F', NULL, NULL, 'system:syncTask:execute', NULL, 4, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @sync_task_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @sync_task_menu_id
      AND `perms` = 'system:syncTask:execute'
  );

INSERT INTO `sys_menu` (`subject_type`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `is_visible`, `status`, `create_time`, `update_time`)
SELECT 'PLATFORM', '日志查询', @sync_task_menu_id, 'F', NULL, NULL, 'system:syncTask:log', NULL, 5, 1, 1, NOW(), NOW()
FROM DUAL
WHERE @sync_task_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `parent_id` = @sync_task_menu_id
      AND `perms` = 'system:syncTask:log'
  );

SET @platform_role_id = (
  SELECT `id`
  FROM `sys_role`
  WHERE `company_id` = 1
    AND `role_key` = 'platform_admin'
  LIMIT 1
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT @platform_role_id, m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE @platform_role_id IS NOT NULL
  AND m.`perms` IN (
    'system:syncTask:list',
    'system:syncTask:add',
    'system:syncTask:update',
    'system:syncTask:execute',
    'system:syncTask:log'
  )
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = @platform_role_id
      AND rm.`menu_id` = m.`id`
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT @platform_role_id, @sync_task_menu_id, NOW(), NOW()
FROM DUAL
WHERE @platform_role_id IS NOT NULL
  AND @sync_task_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = @platform_role_id
      AND rm.`menu_id` = @sync_task_menu_id
  );

INSERT INTO `sys_type_code_menu` (`type_code`, `menu_id`, `create_time`, `update_time`)
SELECT 'PLATFORM', m.`id`, NOW(), NOW()
FROM `sys_menu` m
WHERE (m.`id` = @sync_task_menu_id OR m.`perms` IN (
    'system:syncTask:list',
    'system:syncTask:add',
    'system:syncTask:update',
    'system:syncTask:execute',
    'system:syncTask:log'
  ))
  AND NOT EXISTS (
    SELECT 1 FROM `sys_type_code_menu` tcm
    WHERE tcm.`type_code` = 'PLATFORM'
      AND tcm.`menu_id` = m.`id`
  );
