-- S8.4 C端客户手机号唯一约束

SET @c_user_idx_phone_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'c_user'
    AND INDEX_NAME = 'idx_phone'
);

SET @drop_c_user_idx_phone_sql = IF(
  @c_user_idx_phone_exists > 0,
  'ALTER TABLE `c_user` DROP INDEX `idx_phone`',
  'SELECT 1'
);
PREPARE stmt FROM @drop_c_user_idx_phone_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c_user_uk_phone_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'c_user'
    AND INDEX_NAME = 'uk_phone'
);

SET @add_c_user_uk_phone_sql = IF(
  @c_user_uk_phone_exists = 0,
  'ALTER TABLE `c_user` ADD UNIQUE KEY `uk_phone` (`phone`)',
  'SELECT 1'
);
PREPARE stmt FROM @add_c_user_uk_phone_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
