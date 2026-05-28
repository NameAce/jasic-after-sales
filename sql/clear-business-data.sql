-- =============================================
-- 佳士售后系统 - 清空业务数据脚本
-- 适用场景：
-- 1. 需要保留平台、菜单、字典、配置、公司、账号等基础数据
-- 2. 需要清空工单、客户、附件、通知运行时数据、日志和同步执行痕迹
--
-- 使用说明：
-- 1. 执行前务必先备份数据库
-- 2. 本脚本默认不会删除初始化配置类数据，也不会删除 OSS 等对象存储中的物理文件
-- 3. 如需一并清空条码档案、CRM 快照、公司地址等基础业务档案，请按需打开文末注释块
-- =============================================

SET NAMES utf8mb4;

-- 记录当前外键检查开关，脚本结束时恢复，避免后续会话被误伤。

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- 兼容不同环境升级进度不一致的情况：如果表存在则清空，不存在则跳过。

DROP PROCEDURE IF EXISTS truncate_table_if_exists;

DELIMITER $$
CREATE PROCEDURE truncate_table_if_exists(IN p_table_name VARCHAR(128))
BEGIN
    DECLARE v_table_count INT DEFAULT 0;

    -- 仅检查当前数据库，避免误操作到其它 schema 的同名表。

    SELECT COUNT(1)
    INTO v_table_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name;

    IF v_table_count > 0 THEN
        SET @truncate_sql = CONCAT('TRUNCATE TABLE `', p_table_name, '`');
        PREPARE stmt FROM @truncate_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

-- -------------------------------------------
-- 1. 清空工单主业务数据
-- 说明：
-- 1. 先清子表，再清主表，便于后续即使补上外键也能继续复用本脚本
-- 2. 这里覆盖工单状态流转、参与方、报价、维修、故障、评价等售后过程数据
-- -------------------------------------------
CALL truncate_table_if_exists('work_order_fault_part');
CALL truncate_table_if_exists('work_order_fault');
CALL truncate_table_if_exists('work_order_repair');
CALL truncate_table_if_exists('work_order_quote');
CALL truncate_table_if_exists('work_order_user_participant');
CALL truncate_table_if_exists('work_order_participant');
CALL truncate_table_if_exists('work_order_flow');
CALL truncate_table_if_exists('work_order_evaluation');
CALL truncate_table_if_exists('work_order');
CALL truncate_table_if_exists('sys_feedback');

-- -------------------------------------------
-- 2. 清空客户侧业务数据
-- 说明：
-- 1. 保留 B 端账号、公司、角色、权限，只清 C 端客户和客户地址
-- 2. 微信绑定操作记录属于运行痕迹，和客户数据一起清空
-- -------------------------------------------
CALL truncate_table_if_exists('customer_address');
CALL truncate_table_if_exists('wechat_bind_record');
CALL truncate_table_if_exists('c_user');

-- -------------------------------------------
-- 3. 清空附件业务数据
-- 说明：
-- 1. 先清业务关联，再清文件元数据
-- 2. 本脚本不会删除对象存储中的物理文件，如需回收存储空间需另行处理
-- -------------------------------------------
CALL truncate_table_if_exists('sys_file_biz');
CALL truncate_table_if_exists('sys_file');

-- -------------------------------------------
-- 4. 清空通知运行时数据
-- 说明：
-- 1. 保留通知场景和目标配置，只清通知事件、待办消息、消息日志和派发记录
-- 2. `sys_notify_dispatch` 来自通知多目标升级，老环境可能没有该表，所以走兼容调用
-- -------------------------------------------
CALL truncate_table_if_exists('sys_notify_message_log');
CALL truncate_table_if_exists('sys_notify_message');
CALL truncate_table_if_exists('sys_notify_dispatch');
CALL truncate_table_if_exists('sys_notify_event');

-- -------------------------------------------
-- 5. 清空日志与同步执行痕迹
-- 说明：
-- 1. 保留同步任务定义 `sync_task`，不清空任务配置
-- 2. 仅清运行日志、执行日志等可再生数据
-- -------------------------------------------
CALL truncate_table_if_exists('sys_oper_log');
CALL truncate_table_if_exists('sync_task_log');

-- -------------------------------------------
-- 6. 按需清空的扩展业务档案
-- 说明：
-- 1. 以下数据通常是基础业务档案、同步快照或组织侧配置数据
-- 2. 默认注释掉，避免误删后影响条码识别、CRM 映射、公司地址或组织关系查询
-- 3. 如果目标是把环境还原成“仅保留平台初始化数据”的空库，可按需取消注释
-- -------------------------------------------
-- CALL truncate_table_if_exists('machine_barcode');
-- CALL truncate_table_if_exists('crm_company_mapping');
-- CALL truncate_table_if_exists('crm_biz_company_snapshot');
-- CALL truncate_table_if_exists('crm_first_second_relation_snapshot');
-- CALL truncate_table_if_exists('crm_hq_first_contract_snapshot');
-- CALL truncate_table_if_exists('crm_warehouse_scan_outstorage_snapshot');
-- CALL truncate_table_if_exists('company_address');
-- CALL truncate_table_if_exists('hq_first_contract_record');
-- CALL truncate_table_if_exists('first_second_relation_record');

-- 清理辅助过程并恢复会话级开关，避免影响后续手工执行的 SQL。

DROP PROCEDURE IF EXISTS truncate_table_if_exists;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
