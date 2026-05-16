-- =============================================
-- 消息通知最终优化 Phase 6：旧配置废弃标记
-- 适用场景：测试环境或生产环境完成 sys_notify_* 通知模型升级后执行
-- 说明：
-- 1. 本脚本不删除历史 sys_config 数据，只给旧 wechat.notify.* 配置补充废弃说明。
-- 2. 新环境不再初始化旧 key；小程序订阅消息模板 ID 统一在 sys_notify_template_channel.config_json 维护。
-- 3. 可重复执行。
-- =============================================

SET NAMES utf8mb4;

SET @has_sys_config_table = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_config'
);

SET @ddl = IF(
  @has_sys_config_table = 0,
  'SELECT ''sys_config table does not exist; skip deprecated notify config marking'' AS upgrade_note',
  'UPDATE `sys_config`
   SET `remark` = CASE
     WHEN `remark` IS NULL OR TRIM(`remark`) = ''''
       THEN ''历史废弃通知配置，仅兼容旧环境；当前正式模板ID请改为 sys_notify_template_channel 维护，后续可删除''
     WHEN `remark` LIKE ''%历史废弃通知配置%''
       THEN `remark`
     ELSE CONCAT(`remark`, ''；历史废弃通知配置，仅兼容旧环境；当前正式模板ID请改为 sys_notify_template_channel 维护，后续可删除'')
   END
   WHERE `config_key` IN (
     ''wechat.notify.customer.repairFinished.templateId'',
     ''wechat.notify.customer.repairFinished.pagePath'',
     ''wechat.notify.customer.evaluationInvite.templateId'',
     ''wechat.notify.customer.evaluationInvite.pagePath'',
     ''wechat.notify.company.customerEvaluated.templateId'',
     ''wechat.notify.company.customerEvaluated.pagePath''
   )'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
