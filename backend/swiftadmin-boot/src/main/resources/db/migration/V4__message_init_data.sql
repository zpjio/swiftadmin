-- ============================================
-- SwiftAdmin 消息模块初始化数据
-- 版本: 1.0.0
-- 创建时间: 2026-03-13
-- 说明: 消息推送模块初始数据
-- ============================================

-- ============================================
-- 一、初始化默认渠道
-- ============================================

INSERT INTO msg_channel (channel_name, channel_type, channel_code, config, status, priority, remark) VALUES
('站内信', 'inbox', 'default_inbox', '{}', 1, 100, '默认站内信渠道'),
('系统短信', 'sms', 'default_sms', '{"provider":"aliyun","accessKeyId":"","accessKeySecret":"","signName":"","regionId":"cn-hangzhou"}', 0, 50, '阿里云短信渠道，需要配置AccessKey'),
('系统邮件', 'email', 'default_email', '{"host":"","port":465,"username":"","password":"","from":"","sslEnabled":true}', 0, 40, 'SMTP邮件渠道'),
('企业微信机器人', 'wecom', 'default_wecom', '{"webhookUrl":""}', 0, 30, '企业微信机器人Webhook'),
('钉钉机器人', 'dingtalk', 'default_dingtalk', '{"webhookUrl":"","secret":""}', 0, 30, '钉钉机器人Webhook'),
('飞书机器人', 'feishu', 'default_feishu', '{"webhookUrl":"","secret":""}', 0, 30, '飞书机器人Webhook');

-- ============================================
-- 二、初始化示例模板
-- ============================================

INSERT INTO msg_template (template_name, template_code, channel_type, subject, content, variables, status, remark) VALUES
('欢迎邮件', 'welcome_email', 'email', '欢迎加入 SwiftAdmin', '亲爱的 ${username}，欢迎加入 SwiftAdmin！您的账号已创建成功。', '["username"]', 1, '用户注册欢迎邮件'),
('验证码短信', 'verify_code_sms', 'sms', NULL, '您的验证码是 ${code}，有效期 ${expireMinutes} 分钟，请勿泄露给他人。', '["code","expireMinutes"]', 1, '通用验证码短信模板'),
('登录提醒', 'login_notice', 'inbox', NULL, '您的账号于 ${loginTime} 在 ${device} 上登录，如非本人操作，请及时修改密码。', '["loginTime","device"]', 1, '登录安全提醒'),
('密码修改通知', 'password_changed', 'email', '密码修改通知', '尊敬的 ${username}，您的密码已于 ${changeTime} 修改成功。如非本人操作，请立即联系管理员。', '["username","changeTime"]', 1, '密码修改成功通知'),
('系统公告', 'system_notice', 'inbox', '${title}', '${content}', '["title","content"]', 1, '系统公告模板'),
('订单状态变更', 'order_status', 'sms', NULL, '您的订单 ${orderNo} 状态已变更为 ${status}，请及时查看。', '["orderNo","status"]', 1, '订单状态变更通知');

-- ============================================
-- 三、初始化菜单
-- ============================================

-- 消息管理菜单（父菜单）
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(500, 0, '消息管理', '/message', 'LAYOUT', 1, 'message', 5, '', 1);

-- 消息模板管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(501, 500, '消息模板', '/message/template', 'views/message/template/index', 2, 'file-text', 1, 'message:template:list', 1);

-- 渠道管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(502, 500, '渠道管理', '/message/channel', 'views/message/channel/index', 2, 'api', 2, 'message:channel:list', 1);

-- 发送记录
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(503, 500, '发送记录', '/message/record', 'views/message/record/index', 2, 'history', 3, 'message:record:list', 1);

-- 站内信
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(504, 500, '站内信', '/message/inbox', 'views/message/inbox/index', 2, 'inbox', 4, 'message:inbox:list', 1);

-- ============================================
-- 四、初始化菜单按钮权限
-- ============================================

-- 模板管理按钮权限
INSERT INTO sys_menu (parent_id, name, type, permission, sort_order, status) VALUES
(501, '查询', 3, 'message:template:query', 1, 1),
(501, '新增', 3, 'message:template:add', 2, 1),
(501, '编辑', 3, 'message:template:edit', 3, 1),
(501, '删除', 3, 'message:template:delete', 4, 1);

-- 渠道管理按钮权限
INSERT INTO sys_menu (parent_id, name, type, permission, sort_order, status) VALUES
(502, '查询', 3, 'message:channel:query', 1, 1),
(502, '新增', 3, 'message:channel:add', 2, 1),
(502, '编辑', 3, 'message:channel:edit', 3, 1),
(502, '删除', 3, 'message:channel:delete', 4, 1),
(502, '测试', 3, 'message:channel:test', 5, 1);

-- 发送记录按钮权限
INSERT INTO sys_menu (parent_id, name, type, permission, sort_order, status) VALUES
(503, '查询', 3, 'message:record:query', 1, 1),
(503, '详情', 3, 'message:record:detail', 2, 1);

-- 站内信按钮权限
INSERT INTO sys_menu (parent_id, name, type, permission, sort_order, status) VALUES
(504, '查询', 3, 'message:inbox:query', 1, 1),
(504, '标记已读', 3, 'message:inbox:read', 2, 1),
(504, '删除', 3, 'message:inbox:delete', 3, 1),
(504, '发送', 3, 'message:inbox:send', 4, 1);

-- ============================================
-- 五、初始化字典类型
-- ============================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('消息渠道类型', 'msg_channel_type', 1, '消息渠道类型字典'),
('消息状态', 'msg_status', 1, '消息发送状态字典'),
('消息类型', 'msg_type', 1, '消息类型字典'),
('模板状态', 'msg_template_status', 1, '消息模板状态字典');

-- ============================================
-- 六、初始化字典数据
-- ============================================

-- 消息渠道类型
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status) VALUES
('msg_channel_type', '短信', 'sms', 1, 1),
('msg_channel_type', '邮件', 'email', 2, 1),
('msg_channel_type', '站内信', 'inbox', 3, 1),
('msg_channel_type', '企业微信', 'wecom', 4, 1),
('msg_channel_type', '钉钉', 'dingtalk', 5, 1),
('msg_channel_type', '飞书', 'feishu', 6, 1),
('msg_channel_type', 'Webhook', 'webhook', 7, 1);

-- 消息状态
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status) VALUES
('msg_status', '待发送', 'pending', 1, 1),
('msg_status', '发送中', 'sending', 2, 1),
('msg_status', '发送成功', 'success', 3, 1),
('msg_status', '发送失败', 'failed', 4, 1);

-- 消息类型
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status) VALUES
('msg_type', '系统通知', 'system', 1, 1),
('msg_type', '安全提醒', 'security', 2, 1),
('msg_type', '订单通知', 'order', 3, 1),
('msg_type', '营销活动', 'promotion', 4, 1),
('msg_type', '任务提醒', 'task', 5, 1),
('msg_type', '公告通知', 'notice', 6, 1),
('msg_type', '告警通知', 'alert', 7, 1);

-- 模板状态
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, sort_order, status) VALUES
('msg_template_status', '禁用', '0', 1, 1),
('msg_template_status', '启用', '1', 2, 1),
('msg_template_status', '待审核', '2', 3, 1);