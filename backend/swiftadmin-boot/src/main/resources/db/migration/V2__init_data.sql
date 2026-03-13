-- ============================================
-- SwiftAdmin 初始化数据
-- 版本: 1.0.0
-- 创建时间: 2026-03-11
-- 说明: 整合所有初始化数据
-- ============================================

-- ============================================
-- 一、基础用户角色数据
-- ============================================

-- 默认管理员账号 (密码：admin123, BCrypt 加密)
INSERT INTO sys_user (username, password, nickname, email, phone, avatar, status, dept_id, is_super, login_fail_count, force_change_password)
VALUES ('admin', '$2b$10$xNZHyOGnnUT0j3s0.AC9q..fbsKu/HJkTFSdK/wUfh5nuAo6VQovC', '超级管理员', 'admin@swiftadmin.cn', '13800000000', 'https://app-assets-oss.oss-cn-beijing.aliyuncs.com/qianzhou-app/static/image/avatar/41.jpg', 1, 1, 1, 0, 0);

-- 默认角色
INSERT INTO sys_role (name, code, description, data_scope, status)
VALUES ('超级管理员', 'admin', '拥有系统全部权限', 1, 1);

-- 预置角色模板
INSERT INTO sys_role (name, code, description, data_scope, status) VALUES
('系统管理员', 'system_admin', '系统配置和用户管理', 1, 1),
('部门经理', 'dept_manager', '部门管理和部门用户管理', 3, 1),
('普通员工', 'employee', '基础功能使用权限', 5, 1),
('访客', 'visitor', '仅有查看权限', 5, 1);

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 默认部门
INSERT INTO sys_dept (name, parent_id, ancestors, sort_order, leader, phone, email, status)
VALUES ('总公司', 0, '0', 1, 'admin', '13800000000', 'admin@swiftadmin.cn', 1);

-- ============================================
-- 二、菜单权限数据
-- ============================================

-- 系统管理模块菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(1, 0, '系统管理', '/system', NULL, 1, 'mdi:cogs', 1, NULL, 1),
(2, 1, '用户管理', '/system/user', 'system/user/index', 2, 'mdi:user', 1, NULL, 1),
(3, 1, '角色管理', '/system/role', 'system/role/index', 2, 'mdi:account-cog', 2, NULL, 1),
(4, 1, '菜单管理', '/system/menu', 'system/menu/index', 2, 'mdi:view-list', 3, NULL, 1),
(5, 1, '部门管理', '/system/dept', 'system/dept/index', 2, 'mdi:account-group', 4, NULL, 1),
(6, 1, '字典管理', '/system/dict', 'system/dict/index', 2, 'mdi:toggle-switch', 5, NULL, 1),
(7, 1, '配置管理', '/system/config', 'system/config/index', 2, 'mdi:wrench', 6, NULL, 1);

-- 用户管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(8, 2, '用户查询', NULL, NULL, 3, NULL, 1, 'system:user:list', 1),
(9, 2, '用户新增', NULL, NULL, 3, NULL, 2, 'system:user:add', 1),
(10, 2, '用户修改', NULL, NULL, 3, NULL, 3, 'system:user:edit', 1),
(11, 2, '用户删除', NULL, NULL, 3, NULL, 4, 'system:user:delete', 1),
(12, 2, '重置密码', NULL, NULL, 3, NULL, 5, 'system:user:resetPwd', 1),
(13, 2, '导出用户', NULL, NULL, 3, NULL, 6, 'system:user:export', 1);

-- 角色管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(14, 3, '角色查询', NULL, NULL, 3, NULL, 1, 'system:role:list', 1),
(15, 3, '角色新增', NULL, NULL, 3, NULL, 2, 'system:role:add', 1),
(16, 3, '角色修改', NULL, NULL, 3, NULL, 3, 'system:role:edit', 1),
(17, 3, '角色删除', NULL, NULL, 3, NULL, 4, 'system:role:delete', 1);

-- 菜单管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(18, 4, '菜单查询', NULL, NULL, 3, NULL, 1, 'system:menu:list', 1),
(19, 4, '菜单新增', NULL, NULL, 3, NULL, 2, 'system:menu:add', 1),
(20, 4, '菜单修改', NULL, NULL, 3, NULL, 3, 'system:menu:edit', 1),
(21, 4, '菜单删除', NULL, NULL, 3, NULL, 4, 'system:menu:delete', 1);

-- 部门管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(22, 5, '部门查询', NULL, NULL, 3, NULL, 1, 'system:dept:list', 1),
(23, 5, '部门新增', NULL, NULL, 3, NULL, 2, 'system:dept:add', 1),
(24, 5, '部门修改', NULL, NULL, 3, NULL, 3, 'system:dept:edit', 1),
(25, 5, '部门删除', NULL, NULL, 3, NULL, 4, 'system:dept:delete', 1);

-- 字典管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(26, 6, '字典查询', NULL, NULL, 3, NULL, 1, 'system:dict:list', 1),
(27, 6, '字典新增', NULL, NULL, 3, NULL, 2, 'system:dict:add', 1),
(28, 6, '字典修改', NULL, NULL, 3, NULL, 3, 'system:dict:edit', 1),
(29, 6, '字典删除', NULL, NULL, 3, NULL, 4, 'system:dict:delete', 1);

-- 配置管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(30, 7, '配置查询', NULL, NULL, 3, NULL, 1, 'system:config:list', 1),
(31, 7, '配置新增', NULL, NULL, 3, NULL, 2, 'system:config:add', 1),
(32, 7, '配置修改', NULL, NULL, 3, NULL, 3, 'system:config:edit', 1),
(33, 7, '配置删除', NULL, NULL, 3, NULL, 4, 'system:config:delete', 1);

-- 文件管理菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(40, 1, '文件管理', '/system/file', 'system/file/index', 2, 'mdi:folder', 7, NULL, 1);

-- 文件管理权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(41, 40, '文件查询', NULL, NULL, 3, NULL, 1, 'system:file:query', 1),
(42, 40, '文件上传', NULL, NULL, 3, NULL, 2, 'system:file:upload', 1),
(43, 40, '文件下载', NULL, NULL, 3, NULL, 3, 'system:file:download', 1),
(44, 40, '文件删除', NULL, NULL, 3, NULL, 4, 'system:file:delete', 1);

-- 系统监控模块菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(50, 0, '系统监控', '/monitor', NULL, 1, 'mdi:monitor', 2, NULL, 1);

-- 在线用户菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(51, 50, '在线用户', '/monitor/online', 'monitor/online/index', 2, 'mdi:walk', 1, NULL, 1);

-- 在线用户按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(52, 51, '在线查询', NULL, NULL, 3, NULL, 1, 'monitor:online:list', 1),
(53, 51, '强制退出', NULL, NULL, 3, NULL, 2, 'monitor:online:forceLogout', 1);

-- 操作日志菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(54, 50, '操作日志', '/monitor/operlog', 'monitor/operlog/index', 2, 'mdi:history', 2, NULL, 1);

-- 操作日志按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(55, 54, '日志查询', NULL, NULL, 3, NULL, 1, 'monitor:operlog:list', 1),
(56, 54, '日志删除', NULL, NULL, 3, NULL, 2, 'monitor:operlog:delete', 1),
(57, 54, '日志导出', NULL, NULL, 3, NULL, 3, 'monitor:operlog:export', 1);

-- 登录日志菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(58, 50, '登录日志', '/monitor/loginlog', 'monitor/loginlog/index', 2, 'mdi:bike', 3, NULL, 1);

-- 登录日志按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(59, 58, '日志查询', NULL, NULL, 3, NULL, 1, 'monitor:loginlog:list', 1),
(60, 58, '日志删除', NULL, NULL, 3, NULL, 2, 'monitor:loginlog:delete', 1),
(61, 58, '日志导出', NULL, NULL, 3, NULL, 3, 'monitor:loginlog:export', 1);

-- ============================================
-- 三、字典数据
-- ============================================

-- 用户状态字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('用户状态', 'sys_user_status', 1, '用户状态字典');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
('sys_user_status', '正常', '1', 1, 1, '用户正常状态'),
('sys_user_status', '禁用', '0', 2, 1, '用户禁用状态');

-- 性别字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('性别', 'sys_user_sex', 1, '性别字典');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
('sys_user_sex', '男', '1', 1, 1, '男性'),
('sys_user_sex', '女', '2', 2, 1, '女性'),
('sys_user_sex', '未知', '0', 3, 1, '未知性别');

-- 菜单类型字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('菜单类型', 'sys_menu_type', 1, '菜单类型字典');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
('sys_menu_type', '目录', '1', 1, 1, '目录类型菜单'),
('sys_menu_type', '菜单', '2', 2, 1, '页面类型菜单'),
('sys_menu_type', '按钮', '3', 3, 1, '按钮类型权限');

-- 数据范围字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('数据范围', 'sys_data_scope', 1, '数据范围字典');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
('sys_data_scope', '全部数据', '1', 1, 1, '可查看所有数据'),
('sys_data_scope', '自定义数据', '2', 2, 1, '可查看自定义部门数据'),
('sys_data_scope', '本部门数据', '3', 3, 1, '仅可查看本部门数据'),
('sys_data_scope', '本部门及以下', '4', 4, 1, '可查看本部门及子部门数据'),
('sys_data_scope', '仅本人数据', '5', 5, 1, '仅可查看本人数据');

-- ============================================
-- 五、系统配置数据
-- ============================================

-- 密码策略配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('password.expire_days', '90', '密码有效期（天）', 'password', '密码过期时间，超过此天数强制修改密码'),
('password.history_count', '5', '密码历史检查数量', 'password', '修改密码时检查最近N次历史密码，防止重复'),
('password.force_change_on_reset', 'true', '管理员重置后强制修改', 'password', '管理员重置密码后是否强制用户修改'),
('password.min_length', '8', '密码最小长度', 'password', '密码最小长度要求'),
('password.expire_warning_days', '7', '密码过期提前提醒天数', 'password', '密码过期前N天开始提醒');

-- 系统基础配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('system.name', 'SwiftAdmin', '系统名称', 'system', '系统显示名称'),
('system.version', '1.0.0', '系统版本', 'system', '系统版本号'),
('system.copyright', '© 2026 SwiftAdmin. All rights reserved.', '版权信息', 'system', '页脚版权信息'),
('system.logo', '/static/logo.png', '系统Logo', 'system', '系统Logo路径');

-- 文件上传配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('upload.max_size', '10485760', '文件上传最大限制（字节）', 'upload', '单个文件上传最大限制，默认10MB'),
('upload.allow_types', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx', '允许上传的文件类型', 'upload', '允许上传的文件扩展名，逗号分隔'),
('upload.save_path', '/upload', '文件保存路径', 'upload', '文件上传保存的根路径');

-- 验证码配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('captcha.enable', 'true', '验证码开关', 'captcha', '是否开启验证码验证'),
('captcha.type', 'math', '验证码类型', 'captcha', '验证码类型：math=数学运算，char=字符'),
('captcha.length', '4', '验证码长度', 'captcha', '验证码字符长度'),
('captcha.expire_seconds', '300', '验证码有效期（秒）', 'captcha', '验证码过期时间');

-- 登录配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('login.max_retry', '5', '登录最大重试次数', 'login', '登录失败最大重试次数'),
('login.lock_minutes', '30', '账户锁定时间（分钟）', 'login', '超过最大重试次数后锁定时间'),
('login.single_device', 'false', '单设备登录', 'login', '是否限制同一账号只能在一个设备登录');

-- 会话配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('session.timeout', '7200', '会话超时时间（秒）', 'session', '用户无操作会话超时时间'),
('session.max_concurrent', '1', '最大并发会话数', 'session', '同一用户最大并发会话数');

-- 日志配置
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('log.oper.keep_days', '90', '操作日志保留天数', 'log', '操作日志保留天数，超过自动清理'),
('log.login.keep_days', '180', '登录日志保留天数', 'log', '登录日志保留天数，超过自动清理');

-- ============================================
-- 六、角色菜单关联数据
-- ============================================

-- 超级管理员角色菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
 (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
 (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13),
 (1, 14), (1, 15), (1, 16), (1, 17),
 (1, 18), (1, 19), (1, 20), (1, 21),
 (1, 22), (1, 23), (1, 24), (1, 25),
 (1, 26), (1, 27), (1, 28), (1, 29),
 (1, 30), (1, 31), (1, 32), (1, 33),
 (1, 40), (1, 41), (1, 42), (1, 43), (1, 44),
 (1, 50), (1, 51), (1, 52), (1, 53),
 (1, 54), (1, 55), (1, 56), (1, 57),
 (1, 58), (1, 59), (1, 60), (1, 61);

-- 系统管理员角色菜单权限(role_id=2): 系统管理全部权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
 (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7),
 (2, 8), (2, 9), (2, 10), (2, 11), (2, 12), (2, 13),
 (2, 14), (2, 15), (2, 16), (2, 17),
 (2, 18), (2, 19), (2, 20), (2, 21),
 (2, 22), (2, 23), (2, 24), (2, 25),
 (2, 26), (2, 27), (2, 28), (2, 29),
 (2, 30), (2, 31), (2, 32), (2, 33),
 (2, 40), (2, 41), (2, 42), (2, 43), (2, 44);

-- 部门经理角色菜单权限(role_id=3): 部门管理+用户管理
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
 (3, 1), (3, 2), (3, 5),
 (3, 8), (3, 9), (3, 10), (3, 11), (3, 12),
 (3, 22), (3, 23), (3, 24), (3, 25);

-- 普通员工角色菜单权限(role_id=4): 用户查看+文件管理
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
 (4, 1), (4, 2), (4, 40),
 (4, 8), (4, 41), (4, 42), (4, 43);

-- 访客角色菜单权限(role_id=5): 仅查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
 (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 7),
 (5, 8), (5, 14), (5, 18), (5, 22), (5, 26), (5, 30),
 (5, 40), (5, 41), (5, 50), (5, 51), (5, 52);

-- ============================================
-- 七、配置类型字典
-- ============================================

-- 配置类型字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('配置类型', 'sys_config_type', 1, '系统配置类型字典');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
('sys_config_type', '系统配置', 'system', 1, 1, '系统基础配置'),
('sys_config_type', '密码策略', 'password', 2, 1, '密码安全策略配置'),
('sys_config_type', '文件上传', 'upload', 3, 1, '文件上传相关配置'),
('sys_config_type', '验证码', 'captcha', 4, 1, '验证码相关配置'),
('sys_config_type', '登录配置', 'login', 5, 1, '登录相关配置'),
('sys_config_type', '会话配置', 'session', 6, 1, '会话相关配置'),
('sys_config_type', '日志配置', 'log', 7, 1, '日志相关配置'),
('sys_config_type', '邮件服务', 'email', 8, 1, '邮件服务配置'),
('sys_config_type', '短信服务', 'sms', 9, 1, '短信服务配置');