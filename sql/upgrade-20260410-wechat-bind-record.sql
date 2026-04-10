-- S8.x 微信解绑与绑定记录：移除 unionid，新增绑定记录表

SET @sys_user_unionid_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'unionid'
);

SET @drop_sys_user_unionid_sql = IF(
  @sys_user_unionid_exists > 0,
  'ALTER TABLE `sys_user` DROP COLUMN `unionid`',
  'SELECT 1'
);
PREPARE stmt FROM @drop_sys_user_unionid_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c_user_unionid_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'c_user'
    AND COLUMN_NAME = 'unionid'
);

SET @drop_c_user_unionid_sql = IF(
  @c_user_unionid_exists > 0,
  'ALTER TABLE `c_user` DROP COLUMN `unionid`',
  'SELECT 1'
);
PREPARE stmt FROM @drop_c_user_unionid_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `wechat_bind_record` (
  `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           bigint unsigned NOT NULL                COMMENT '用户ID',
  `operate_type`      varchar(32)     NOT NULL                COMMENT '操作类型（BIND/UNBIND）',
  `operate_source`    varchar(32)     NOT NULL                COMMENT '操作来源（MP_BIND_LOGIN/PC_QR_BIND/PC_SELF_UNBIND）',
  `openid`            varchar(64)     NOT NULL                COMMENT '微信openid快照',
  `wechat_phone`      varchar(20)     DEFAULT NULL            COMMENT '微信授权手机号快照',
  `operator_user_id`  bigint unsigned NOT NULL                COMMENT '操作人ID',
  `operator_username` varchar(64)     NOT NULL                COMMENT '操作人用户名',
  `operate_time`      datetime        NOT NULL                COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_operate_time` (`user_id`, `operate_time`),
  KEY `idx_openid_operate_time` (`openid`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='微信绑定记录表';
