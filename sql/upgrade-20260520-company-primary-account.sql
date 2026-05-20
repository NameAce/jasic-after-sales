-- 公司主账号标记补齐
-- 目标：
-- 1. 在 `sys_user_company` 上增加 `is_primary_account` 字段，明确区分公司主账号与子账号。
-- 2. 对历史数据进行一次性回填：优先取公司下最早绑定管理员角色的用户；若不存在，再兜底取最早关联用户。

SET @has_primary_account_column = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user_company'
      AND COLUMN_NAME = 'is_primary_account'
);

SET @sql_add_primary_account_column = IF(
    @has_primary_account_column = 0,
    'ALTER TABLE `sys_user_company` ADD COLUMN `is_primary_account` tinyint unsigned DEFAULT 0 COMMENT ''是否公司主账号（1=是，0=否）'' AFTER `is_default`',
    'SELECT 1'
);

PREPARE stmt_add_primary_account_column FROM @sql_add_primary_account_column;
EXECUTE stmt_add_primary_account_column;
DEALLOCATE PREPARE stmt_add_primary_account_column;

SET @has_primary_account_index = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user_company'
      AND INDEX_NAME = 'idx_company_primary_account'
);

SET @sql_add_primary_account_index = IF(
    @has_primary_account_index = 0,
    'ALTER TABLE `sys_user_company` ADD KEY `idx_company_primary_account` (`company_id`, `is_primary_account`)',
    'SELECT 1'
);

PREPARE stmt_add_primary_account_index FROM @sql_add_primary_account_index;
EXECUTE stmt_add_primary_account_index;
DEALLOCATE PREPARE stmt_add_primary_account_index;

-- 第一优先级：对还没有主账号标记的公司，优先把最早绑定公司管理员角色的用户标记为主账号。
UPDATE `sys_user_company` uc
INNER JOIN (
    SELECT MIN(candidate.id) AS relation_id
    FROM `sys_user_company` candidate
    INNER JOIN `sys_user_role` ur
        ON ur.`user_id` = candidate.`user_id`
    INNER JOIN `sys_role` r
        ON r.`id` = ur.`role_id`
       AND r.`company_id` = candidate.`company_id`
    LEFT JOIN (
        SELECT `company_id`
        FROM `sys_user_company`
        WHERE `is_primary_account` = 1
        GROUP BY `company_id`
    ) existing
        ON existing.`company_id` = candidate.`company_id`
    WHERE existing.`company_id` IS NULL
      AND r.`role_type` = 1
    GROUP BY candidate.`company_id`
) chosen
    ON chosen.`relation_id` = uc.`id`
SET uc.`is_primary_account` = 1;

-- 第二优先级：如果历史公司没有管理员角色数据，再兜底把最早关联到公司的用户标记为主账号。
UPDATE `sys_user_company` uc
INNER JOIN (
    SELECT MIN(candidate.id) AS relation_id
    FROM `sys_user_company` candidate
    LEFT JOIN (
        SELECT `company_id`
        FROM `sys_user_company`
        WHERE `is_primary_account` = 1
        GROUP BY `company_id`
    ) existing
        ON existing.`company_id` = candidate.`company_id`
    WHERE existing.`company_id` IS NULL
    GROUP BY candidate.`company_id`
) chosen
    ON chosen.`relation_id` = uc.`id`
SET uc.`is_primary_account` = 1;
