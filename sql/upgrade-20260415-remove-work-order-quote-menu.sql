DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE m.`perms` = 'workorder:quote';

DELETE tcm
FROM `sys_type_code_menu` tcm
JOIN `sys_menu` m ON m.`id` = tcm.`menu_id`
WHERE m.`perms` = 'workorder:quote';

DELETE FROM `sys_menu`
WHERE `perms` = 'workorder:quote';
