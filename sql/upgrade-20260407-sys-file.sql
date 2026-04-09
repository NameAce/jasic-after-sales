-- 佳士售后系统 - 文件中台增量脚本
-- 作者: Codex
-- 日期: 2026-04-07

CREATE TABLE IF NOT EXISTS `sys_file` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `storage_type` varchar(32) NOT NULL COMMENT '存储类型',
    `bucket` varchar(128) NOT NULL COMMENT '存储桶',
    `object_key` varchar(512) NOT NULL COMMENT '对象键',
    `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
    `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
    `file_size` bigint unsigned NOT NULL COMMENT '文件大小',
    `file_ext` varchar(32) NOT NULL COMMENT '扩展名',
    `file_hash` varchar(128) NOT NULL COMMENT '文件哈希',
    `access_level` varchar(32) NOT NULL COMMENT '访问级别',
    `upload_user_id` bigint unsigned DEFAULT NULL COMMENT '上传用户ID',
    `upload_user_type` varchar(32) NOT NULL COMMENT '上传用户类型',
    `upload_company_id` bigint unsigned DEFAULT NULL COMMENT '上传公司ID',
    `status` varchar(32) NOT NULL COMMENT '文件状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_file_hash_key` (`file_hash`, `object_key`),
    KEY `idx_sys_file_upload_user` (`upload_user_id`, `upload_user_type`),
    KEY `idx_sys_file_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件元数据表';

CREATE TABLE IF NOT EXISTS `sys_file_biz` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_id` bigint unsigned NOT NULL COMMENT '文件ID',
    `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
    `biz_id` bigint unsigned NOT NULL COMMENT '业务ID',
    `sort_num` int NOT NULL DEFAULT 1 COMMENT '排序号',
    `is_primary` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否主文件',
    `company_id` bigint unsigned DEFAULT NULL COMMENT '公司ID',
    `operator_user_id` bigint unsigned DEFAULT NULL COMMENT '操作人ID',
    `operator_user_type` varchar(32) DEFAULT NULL COMMENT '操作人类型',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_file_biz_type_id_sort` (`biz_type`, `biz_id`, `sort_num`),
    KEY `idx_sys_file_biz_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件业务关联表';

DROP TABLE IF EXISTS `work_order_attachment`;
