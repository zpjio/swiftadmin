-- ============================================
-- SwiftAdmin 消息模块数据库表结构
-- 版本: 1.0.0
-- 创建时间: 2026-03-13
-- 说明: 消息推送模块表结构定义
-- ============================================

-- ============================================
-- 一、消息渠道配置表
-- ============================================

CREATE TABLE msg_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

-- ============================================
-- 二、消息模板表
-- ============================================

CREATE TABLE msg_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

-- ============================================
-- 三、消息发送记录表
-- ============================================

CREATE TABLE msg_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

-- ============================================
-- 四、消息订阅表
-- ============================================

CREATE TABLE msg_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    msg_type VARCHAR(50) NOT NULL COMMENT '消息类型',
    channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型',
    subscribed SMALLINT DEFAULT 1 COMMENT '是否订阅：1=订阅，0=取消',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_msg_channel (user_id, msg_type, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息订阅表';

CREATE INDEX idx_subscription_user_id ON msg_subscription(user_id);

-- ============================================
-- 五、站内信收件箱
-- ============================================

CREATE TABLE msg_user_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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