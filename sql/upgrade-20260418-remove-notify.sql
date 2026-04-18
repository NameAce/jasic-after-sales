-- =============================================
-- 佳士售后系统 - 移除消息通知相关数据库对象
-- 适用场景：已落库通知表或通知配置的环境
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `sys_notify_message_log`;
DROP TABLE IF EXISTS `sys_notify_message`;
DROP TABLE IF EXISTS `sys_notify_event`;
DROP TABLE IF EXISTS `work_order_notify_event`;

DELETE FROM `sys_config`
WHERE `config_key` IN (
  'wechat.notify.customer.repairFinished.templateId',
  'wechat.notify.customer.repairFinished.pagePath',
  'wechat.notify.customer.evaluationInvite.templateId',
  'wechat.notify.customer.evaluationInvite.pagePath',
  'wechat.notify.company.customerEvaluated.templateId',
  'wechat.notify.company.customerEvaluated.pagePath'
);
