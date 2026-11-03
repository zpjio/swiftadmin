-- 仍使用种子密码的默认管理员，下次登录必须改密。
-- 已经改过密码的环境不会被这条语句碰到。
UPDATE sys_user
SET force_change_password = 1
WHERE id = 1
  AND username = 'admin'
  AND password = '$2b$10$xNZHyOGnnUT0j3s0.AC9q..fbsKu/HJkTFSdK/wUfh5nuAo6VQovC'
  AND deleted = 0;

-- 运行指标权限。授予超级管理员角色；其他角色需要显式分配。
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status)
VALUES (70, 50, '运行指标', NULL, NULL, 3, NULL, 3, 'monitor:metrics', 1);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 70);
