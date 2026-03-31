-- =============================================
-- 佳士售后系统 - 工单寄修信息增量脚本
-- 适用场景：已执行过工单核心建表脚本的库
-- 可重复执行：是
-- =============================================

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order` ADD COLUMN `sender_name` varchar(64) DEFAULT NULL COMMENT ''寄件人姓名'' AFTER `fault_desc`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'sender_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order` ADD COLUMN `sender_mobile` varchar(20) DEFAULT NULL COMMENT ''寄件人手机号'' AFTER `sender_name`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'sender_mobile'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order` ADD COLUMN `sender_address` varchar(255) DEFAULT NULL COMMENT ''寄件地址'' AFTER `sender_mobile`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'sender_address'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `work_order` ADD COLUMN `send_express_no` varchar(64) DEFAULT NULL COMMENT ''寄件快递单号'' AFTER `sender_address`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order'
      AND column_name = 'send_express_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
