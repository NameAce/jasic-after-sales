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

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT 'B端小程序AppID', 'wechat.mp.b.appid', '', 0, 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.b.appid');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT 'B端小程序Secret', 'wechat.mp.b.secret', '', 0, 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.b.secret');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT 'C端小程序AppID', 'wechat.mp.c.appid', '', 0, 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.c.appid');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT 'C端小程序Secret', 'wechat.mp.c.secret', '', 0, 'S8.3 真实微信登录链路配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.c.secret');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '客户维修完成通知模板ID', 'wechat.notify.customer.repairFinished.templateId', '', 0, 'S8.3 微信订阅消息配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.notify.customer.repairFinished.templateId');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '客户维修完成通知跳转页', 'wechat.notify.customer.repairFinished.pagePath', '', 0, 'S8.3 微信订阅消息配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.notify.customer.repairFinished.pagePath');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '客户评价邀请通知模板ID', 'wechat.notify.customer.evaluationInvite.templateId', '', 0, 'S8.3 微信订阅消息配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.notify.customer.evaluationInvite.templateId');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '客户评价邀请通知跳转页', 'wechat.notify.customer.evaluationInvite.pagePath', '', 0, 'S8.3 微信订阅消息配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.notify.customer.evaluationInvite.pagePath');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '公司侧客户评价结果通知模板ID', 'wechat.notify.company.customerEvaluated.templateId', '', 0, 'S8.3 微信订阅消息配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.notify.company.customerEvaluated.templateId');

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `update_time`)
SELECT '公司侧客户评价结果通知跳转页', 'wechat.notify.company.customerEvaluated.pagePath', '', 0, 'S8.3 微信订阅消息配置，默认留空', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.notify.company.customerEvaluated.pagePath');
