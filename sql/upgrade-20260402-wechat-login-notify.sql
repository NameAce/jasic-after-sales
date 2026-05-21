-- S8.3 真实微信登录与消息链路：用户微信绑定字段与配置占位

SET @wechat_phone_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'wechat_phone'
);

SET @wechat_phone_sql = IF(
  @wechat_phone_column_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `wechat_phone` varchar(20) DEFAULT NULL COMMENT ''微信授权手机号快照'' AFTER `openid`',
  'SELECT 1'
);
PREPARE stmt FROM @wechat_phone_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `group_key`, `remark`, `create_time`, `update_time`)
SELECT 'B端小程序AppID', 'wechat.mp.b.appid', '', 0, 'wechat', 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.b.appid');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `group_key`, `remark`, `create_time`, `update_time`)
SELECT 'B端小程序Secret', 'wechat.mp.b.secret', '', 0, 'wechat', 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.b.secret');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `group_key`, `remark`, `create_time`, `update_time`)
SELECT 'C端小程序AppID', 'wechat.mp.c.appid', '', 0, 'wechat', 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.c.appid');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `group_key`, `remark`, `create_time`, `update_time`)
SELECT 'C端小程序Secret', 'wechat.mp.c.secret', '', 0, 'wechat', 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.c.secret');
