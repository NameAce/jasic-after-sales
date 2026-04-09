-- 佳士售后系统 - 工单服务方式编码化增量脚本

ALTER TABLE `work_order`
  MODIFY COLUMN `service_mode` varchar(16) NOT NULL COMMENT '服务方式编码（MAIL=寄修，STORE=到店维修）';

UPDATE `work_order`
SET `service_mode` = CASE `service_mode`
    WHEN '寄修' THEN 'MAIL'
    WHEN '到店维修' THEN 'STORE'
    ELSE `service_mode`
END
WHERE `service_mode` IN ('寄修', '到店维修');
