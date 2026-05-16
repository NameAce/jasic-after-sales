-- =============================================
-- 佳士售后系统 - 移除消息通知相关数据库对象
-- 适用场景：已落库通知表或通知配置的环境
-- 说明：旧 wechat.notify.* 配置只标记为历史废弃，不强制删除历史数据。
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `sys_notify_message_log`;
DROP TABLE IF EXISTS `sys_notify_message`;
DROP TABLE IF EXISTS `sys_notify_event`;
DROP TABLE IF EXISTS `work_order_notify_event`;

UPDATE `sys_config`
SET `remark` = CASE
  WHEN `remark` IS NULL OR TRIM(`remark`) = ''
    THEN '历史废弃通知配置，仅兼容旧环境；当前正式模板ID请改为 sys_notify_template_channel 维护，后续可删除'
  WHEN `remark` LIKE '%历史废弃通知配置%'
    THEN `remark`
  ELSE CONCAT(`remark`, '；历史废弃通知配置，仅兼容旧环境；当前正式模板ID请改为 sys_notify_template_channel 维护，后续可删除')
END
WHERE `config_key` IN (
  'wechat.notify.customer.repairFinished.templateId',
  'wechat.notify.customer.repairFinished.pagePath',
  'wechat.notify.customer.evaluationInvite.templateId',
  'wechat.notify.customer.evaluationInvite.pagePath',
  'wechat.notify.company.customerEvaluated.templateId',
  'wechat.notify.company.customerEvaluated.pagePath'
);
