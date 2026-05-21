-- 系统配置分组改造：为 sys_config 增加 group_key，并补齐当前代码已读取但初始化缺失的配置项。
-- 本脚本只做单表字段增强、历史数据分组回填和缺失配置占位，不创建 sys_config_group，也不恢复 wechat.notify.* 旧通知链路。

SET @sys_config_group_key_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_config'
    AND COLUMN_NAME = 'group_key'
);

SET @sys_config_group_key_sql = IF(
  @sys_config_group_key_column_exists = 0,
  'ALTER TABLE `sys_config` ADD COLUMN `group_key` varchar(64) NOT NULL DEFAULT ''org'' COMMENT ''配置分组标识'' AFTER `config_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sys_config_group_key_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已确认的组织配置归入 org，当前仅包含公司管理员初始密码。
UPDATE `sys_config`
SET `group_key` = 'org'
WHERE `config_key` = 'org.company.adminInitPassword';

-- 已确认的微信配置归入 wechat，包含 B/C 端小程序接入参数和 B 端绑定页路径。
UPDATE `sys_config`
SET `group_key` = 'wechat'
WHERE `config_key` IN (
  'wechat.mp.b.appid',
  'wechat.mp.b.secret',
  'wechat.mp.c.appid',
  'wechat.mp.c.secret',
  'wechat.mp.b.bind.pagePath'
);

-- 工单业务默认参数归入 work_order，当前用于非佳士无码报修等场景的默认总部归属。
UPDATE `sys_config`
SET `group_key` = 'work_order'
WHERE `config_key` = 'default.hq.company.id';

-- 历史 wechat.notify.* 参数只做 legacy 隔离，不重新接入现有消息通知正式链路。
UPDATE `sys_config`
SET `group_key` = 'legacy'
WHERE `config_key` LIKE 'wechat.notify.%';

-- B 端微信绑定页路径当前由代码读取，默认值留空，由具体部署环境按需配置。
INSERT INTO `sys_config` (
  `config_name`,
  `config_key`,
  `config_value`,
  `config_type`,
  `group_key`,
  `remark`,
  `create_time`,
  `update_time`
)
SELECT
  'B端微信绑定页路径',
  'wechat.mp.b.bind.pagePath',
  '',
  0,
  'wechat',
  'B端微信绑定二维码或绑定链接使用的小程序页面路径，默认留空，由具体环境按需配置',
  NOW(),
  NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_config` WHERE `config_key` = 'wechat.mp.b.bind.pagePath'
);

-- 默认归属总部 ID 当前由工单无码报修等链路读取，默认值留空，避免在初始化脚本中写死环境 ID。
INSERT INTO `sys_config` (
  `config_name`,
  `config_key`,
  `config_value`,
  `config_type`,
  `group_key`,
  `remark`,
  `create_time`,
  `update_time`
)
SELECT
  '默认归属总部ID',
  'default.hq.company.id',
  '',
  0,
  'work_order',
  '非佳士无码报修等场景下的默认归属总部ID，默认留空，由具体环境按需配置',
  NOW(),
  NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_config` WHERE `config_key` = 'default.hq.company.id'
);
