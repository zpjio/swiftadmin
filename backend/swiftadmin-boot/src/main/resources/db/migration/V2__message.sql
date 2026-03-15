-- ============================================
-- SwiftAdmin 消息模块
-- 版本: 1.0.0
-- 创建时间: 2026-03-13
-- 说明: 消息推送模块（渠道、模板、记录、订阅、站内信）
-- ============================================

-- ============================================
-- 一、表结构定义
-- ============================================

-- 消息渠道配置表
CREATE TABLE msg_channel (
    id BIGINT NOT NULL PRIMARY KEY,
    channel_name VARCHAR(50) NOT NULL COMMENT '渠道名称',
    channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型：sms/email/inbox/wecom/dingtalk/feishu/webhook',
    channel_code VARCHAR(50) NOT NULL COMMENT '渠道编码',
    config TEXT NOT NULL COMMENT '配置信息（JSON）',
    status SMALLINT DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
    priority INT DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
    remark VARCHAR(255) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    UNIQUE KEY uk_channel_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息渠道配置表';

CREATE INDEX idx_channel_type ON msg_channel(channel_type);
CREATE INDEX idx_channel_status ON msg_channel(status);

-- 消息模板表
CREATE TABLE msg_template (
    id BIGINT NOT NULL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型',
    subject VARCHAR(200) COMMENT '主题（邮件使用）',
    content TEXT NOT NULL COMMENT '模板内容',
    variables VARCHAR(500) COMMENT '变量列表（JSON数组）',
    status SMALLINT DEFAULT 1 COMMENT '状态：1=启用，0=禁用，2=待审核',
    remark VARCHAR(255) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted SMALLINT DEFAULT 0,
    UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

CREATE INDEX idx_template_channel_type ON msg_template(channel_type);
CREATE INDEX idx_template_status ON msg_template(status);

-- 消息发送记录表
CREATE TABLE msg_record (
    id BIGINT NOT NULL PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL COMMENT '消息ID（UUID）',
    channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型',
    channel_id BIGINT COMMENT '渠道ID',
    template_id BIGINT COMMENT '模板ID',
    template_code VARCHAR(50) COMMENT '模板编码',
    to_address VARCHAR(500) NOT NULL COMMENT '接收地址',
    subject VARCHAR(200) COMMENT '主题',
    content TEXT COMMENT '消息内容',
    variables TEXT COMMENT '模板变量（JSON）',
    biz_type VARCHAR(50) COMMENT '业务类型',
    biz_id VARCHAR(100) COMMENT '业务ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/sending/success/failed',
    error_msg TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    send_time TIMESTAMP COMMENT '发送时间',
    arrive_time TIMESTAMP COMMENT '到达时间',
    cost_time INT COMMENT '耗时（毫秒）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表';

CREATE INDEX idx_record_channel_type ON msg_record(channel_type);
CREATE INDEX idx_record_to_address ON msg_record(to_address(50));
CREATE INDEX idx_record_status ON msg_record(status);
CREATE INDEX idx_record_biz ON msg_record(biz_type, biz_id);
CREATE INDEX idx_record_create_time ON msg_record(create_time);

-- 消息订阅表
CREATE TABLE msg_subscription (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    msg_type VARCHAR(50) NOT NULL COMMENT '消息类型',
    channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型',
    subscribed SMALLINT DEFAULT 1 COMMENT '是否订阅：1=订阅，0=取消',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_msg_channel (user_id, msg_type, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息订阅表';

CREATE INDEX idx_subscription_user_id ON msg_subscription(user_id);

-- 站内信收件箱
CREATE TABLE msg_user_inbox (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_id VARCHAR(50) NOT NULL COMMENT '消息ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    msg_type VARCHAR(50) COMMENT '消息类型：system/notice/alert/promotion',
    priority INT DEFAULT 0 COMMENT '优先级',
    is_read SMALLINT DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
    read_time TIMESTAMP COMMENT '阅读时间',
    is_star SMALLINT DEFAULT 0 COMMENT '是否星标：0=否，1=是',
    sender_id BIGINT COMMENT '发送者ID',
    sender_name VARCHAR(50) COMMENT '发送者名称',
    expire_time TIMESTAMP COMMENT '过期时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信收件箱';

CREATE INDEX idx_inbox_user_id ON msg_user_inbox(user_id);
CREATE INDEX idx_inbox_user_read ON msg_user_inbox(user_id, is_read);
CREATE INDEX idx_inbox_message_id ON msg_user_inbox(message_id);
CREATE INDEX idx_inbox_create_time ON msg_user_inbox(create_time);

-- ============================================
-- 二、初始化数据
-- ============================================

-- 初始化默认渠道
INSERT INTO msg_channel (id, channel_name, channel_type, channel_code, config, status, priority, remark) VALUES
(1, '站内信', 'inbox', 'default_inbox', '{}', 1, 100, '默认站内信渠道'),
(2, '系统短信', 'sms', 'default_sms', '{"provider":"aliyun","accessKeyId":"","accessKeySecret":"","signName":"","regionId":"cn-hangzhou"}', 0, 50, '阿里云短信渠道，需要配置AccessKey'),
(3, '系统邮件', 'email', 'default_email', '{"host":"","port":465,"username":"","password":"","from":"","sslEnabled":true}', 0, 40, 'SMTP邮件渠道'),
(4, '企业微信机器人', 'wecom', 'default_wecom', '{"webhookUrl":""}', 0, 30, '企业微信机器人Webhook'),
(5, '钉钉机器人', 'dingtalk', 'default_dingtalk', '{"webhookUrl":"","secret":""}', 0, 30, '钉钉机器人Webhook'),
(6, '飞书机器人', 'feishu', 'default_feishu', '{"webhookUrl":"","secret":""}', 0, 30, '飞书机器人Webhook');

-- 初始化示例模板
INSERT INTO msg_template (id, template_name, template_code, channel_type, subject, content, variables, status, remark) VALUES
(1, '欢迎邮件', 'welcome_email', 'email', '欢迎加入 SwiftAdmin', '亲爱的 ${username}，欢迎加入 SwiftAdmin！您的账号已创建成功。', '["username"]', 1, '用户注册欢迎邮件'),
(2, '验证码短信', 'verify_code_sms', 'sms', NULL, '您的验证码是 ${code}，有效期 ${expireMinutes} 分钟，请勿泄露给他人。', '["code","expireMinutes"]', 1, '通用验证码短信模板'),
(3, '登录提醒', 'login_notice', 'inbox', NULL, '您的账号于 ${loginTime} 在 ${device} 上登录，如非本人操作，请及时修改密码。', '["loginTime","device"]', 1, '登录安全提醒'),
(4, '密码修改通知', 'password_changed', 'email', '密码修改通知', '尊敬的 ${username}，您的密码已于 ${changeTime} 修改成功。如非本人操作，请立即联系管理员。', '["username","changeTime"]', 1, '密码修改成功通知'),
(5, '系统公告', 'system_notice', 'inbox', '${title}', '${content}', '["title","content"]', 1, '系统公告模板'),
(6, '订单状态变更', 'order_status', 'sms', NULL, '您的订单 ${orderNo} 状态已变更为 ${status}，请及时查看。', '["orderNo","status"]', 1, '订单状态变更通知');

-- 初始化菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(500, 0, '消息管理', '/message', 'LAYOUT', 1, 'message', 5, '', 1),
(501, 500, '消息模板', '/message/template', 'views/message/template/index', 2, 'file-text', 1, 'message:template:list', 1),
(502, 500, '渠道管理', '/message/channel', 'views/message/channel/index', 2, 'api', 2, 'message:channel:list', 1),
(503, 500, '发送记录', '/message/record', 'views/message/record/index', 2, 'history', 3, 'message:record:list', 1),
(504, 500, '站内信', '/message/inbox', 'views/message/inbox/index', 2, 'inbox', 4, 'message:inbox:list', 1);

-- 初始化菜单按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
-- 模板管理按钮权限
(505, 501, '查询', NULL, NULL, 3, NULL, 1, 'message:template:query', 1),
(506, 501, '新增', NULL, NULL, 3, NULL, 2, 'message:template:add', 1),
(507, 501, '编辑', NULL, NULL, 3, NULL, 3, 'message:template:edit', 1),
(508, 501, '删除', NULL, NULL, 3, NULL, 4, 'message:template:delete', 1),
-- 渠道管理按钮权限
(509, 502, '查询', NULL, NULL, 3, NULL, 1, 'message:channel:query', 1),
(510, 502, '新增', NULL, NULL, 3, NULL, 2, 'message:channel:add', 1),
(511, 502, '编辑', NULL, NULL, 3, NULL, 3, 'message:channel:edit', 1),
(512, 502, '删除', NULL, NULL, 3, NULL, 4, 'message:channel:delete', 1),
(513, 502, '测试', NULL, NULL, 3, NULL, 5, 'message:channel:test', 1),
-- 发送记录按钮权限
(514, 503, '查询', NULL, NULL, 3, NULL, 1, 'message:record:query', 1),
(515, 503, '详情', NULL, NULL, 3, NULL, 2, 'message:record:detail', 1),
-- 站内信按钮权限
(516, 504, '查询', NULL, NULL, 3, NULL, 1, 'message:inbox:query', 1),
(517, 504, '标记已读', NULL, NULL, 3, NULL, 2, 'message:inbox:read', 1),
(518, 504, '删除', NULL, NULL, 3, NULL, 3, 'message:inbox:delete', 1),
(519, 504, '发送', NULL, NULL, 3, NULL, 4, 'message:inbox:send', 1);

-- 初始化字典类型
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(6, '消息渠道类型', 'msg_channel_type', 1, '消息渠道类型字典'),
(7, '消息状态', 'msg_status', 1, '消息发送状态字典'),
(8, '消息类型', 'msg_type', 1, '消息类型字典'),
(9, '模板状态', 'msg_template_status', 1, '消息模板状态字典');

-- 初始化字典数据
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort_order, status) VALUES
-- 消息渠道类型
(23, 'msg_channel_type', '短信', 'sms', 1, 1),
(24, 'msg_channel_type', '邮件', 'email', 2, 1),
(25, 'msg_channel_type', '站内信', 'inbox', 3, 1),
(26, 'msg_channel_type', '企业微信', 'wecom', 4, 1),
(27, 'msg_channel_type', '钉钉', 'dingtalk', 5, 1),
(28, 'msg_channel_type', '飞书', 'feishu', 6, 1),
(29, 'msg_channel_type', 'Webhook', 'webhook', 7, 1),
-- 消息状态
(30, 'msg_status', '待发送', 'pending', 1, 1),
(31, 'msg_status', '发送中', 'sending', 2, 1),
(32, 'msg_status', '发送成功', 'success', 3, 1),
(33, 'msg_status', '发送失败', 'failed', 4, 1),
-- 消息类型
(34, 'msg_type', '系统通知', 'system', 1, 1),
(35, 'msg_type', '安全提醒', 'security', 2, 1),
(36, 'msg_type', '订单通知', 'order', 3, 1),
(37, 'msg_type', '营销活动', 'promotion', 4, 1),
(38, 'msg_type', '任务提醒', 'task', 5, 1),
(39, 'msg_type', '公告通知', 'notice', 6, 1),
(40, 'msg_type', '告警通知', 'alert', 7, 1),
-- 模板状态
(41, 'msg_template_status', '禁用', '0', 1, 1),
(42, 'msg_template_status', '启用', '1', 2, 1),
(43, 'msg_template_status', '待审核', '2', 3, 1);