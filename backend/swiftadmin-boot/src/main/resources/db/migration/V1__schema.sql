-- ============================================
-- SwiftAdmin 数据库表结构
-- 版本: 1.0.0
-- 创建时间: 2026-03-11
-- 说明: 整合所有表结构定义
-- ============================================

-- ============================================
-- 一、核心用户权限表
-- ============================================

-- 用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    status SMALLINT DEFAULT 1,
    dept_id BIGINT,
    is_super TINYINT DEFAULT 0 COMMENT '是否超级管理员: 0否 1是',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_time DATETIME COMMENT '账户锁定时间',
    password_update_time TIMESTAMP COMMENT '密码最后更新时间',
    force_change_password SMALLINT DEFAULT 0 COMMENT '是否强制修改密码：0=否，1=是',
    wecom_open_id VARCHAR(100) COMMENT '企业微信 OpenID',
    dingtalk_open_id VARCHAR(100) COMMENT '钉钉 OpenID',
    feishu_open_id VARCHAR(100) COMMENT '飞书 OpenID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE INDEX idx_user_dept ON sys_user(dept_id);
CREATE INDEX idx_user_status ON sys_user(status);
CREATE INDEX idx_user_phone ON sys_user(phone);
CREATE INDEX idx_sys_user_lock_time ON sys_user(lock_time);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    data_scope TINYINT DEFAULT 1 COMMENT '数据权限范围: 1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(255),
    component VARCHAR(255),
    type SMALLINT COMMENT '类型：1=目录，2=菜单，3=按钮',
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    permission VARCHAR(100),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE INDEX idx_menu_parent ON sys_menu(parent_id);

-- 部门表
CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    ancestors VARCHAR(255) COMMENT '祖级路径(逗号分隔)',
    name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    leader VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE INDEX idx_dept_parent ON sys_dept(parent_id);
CREATE INDEX idx_dept_ancestors ON sys_dept(ancestors);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 角色部门关联表（自定义数据权限）
CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色部门关联表';

CREATE INDEX idx_role_dept_role ON sys_role_dept(role_id);
CREATE INDEX idx_role_dept_dept ON sys_role_dept(dept_id);

-- ============================================
-- 二、认证相关表
-- ============================================

-- 登录日志表
CREATE TABLE sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent TEXT,
    status SMALLINT,
    message VARCHAR(255),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 短信验证码表
CREATE TABLE sys_sms_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    code VARCHAR(6) NOT NULL COMMENT '验证码',
    scene VARCHAR(20) NOT NULL COMMENT '使用场景：login|register|bind',
    status SMALLINT DEFAULT 1 COMMENT '状态：1=未使用，0=已使用',
    expire_at TIMESTAMP NOT NULL COMMENT '过期时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ip_address VARCHAR(50) COMMENT '请求 IP'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码表';

CREATE INDEX idx_sms_phone_scene ON sys_sms_code(phone, scene);
CREATE INDEX idx_sms_expire_at ON sys_sms_code(expire_at);

-- 用户社交账号绑定表
CREATE TABLE sys_social_bind (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '系统用户 ID',
    source VARCHAR(20) NOT NULL COMMENT '来源：wecom|dingtalk|feishu',
    open_id VARCHAR(100) NOT NULL COMMENT '第三方用户 ID',
    union_id VARCHAR(100) COMMENT '第三方统一 ID',
    access_token VARCHAR(500) COMMENT '访问 Token',
    refresh_token VARCHAR(500) COMMENT '刷新 Token',
    token_expires_at TIMESTAMP COMMENT 'Token 过期时间',
    extra_info TEXT COMMENT '额外信息（昵称、头像等，JSON 格式）',
    bind_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_social_source_open_id (source, open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户社交账号绑定表';

CREATE INDEX idx_social_user_id ON sys_social_bind(user_id);

-- 密码历史记录表
CREATE TABLE sys_password_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希值（BCrypt）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码历史记录表';

CREATE INDEX idx_password_history_user ON sys_password_history(user_id);

-- ============================================
-- 三、系统配置表
-- ============================================

-- 系统配置表
CREATE TABLE sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_type VARCHAR(20) DEFAULT 'system' COMMENT '配置类型',
    remark VARCHAR(255) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE INDEX idx_config_key ON sys_config(config_key);

-- 字典类型表
CREATE TABLE sys_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL UNIQUE COMMENT '字典类型',
    status SMALLINT DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted SMALLINT DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典数据表
CREATE TABLE sys_dict_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status SMALLINT DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted SMALLINT DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

CREATE INDEX idx_dict_type ON sys_dict_data(dict_type);
CREATE INDEX idx_dict_data_status ON sys_dict_data(status);

-- ============================================
-- 四、日志表
-- ============================================

-- 操作日志表
CREATE TABLE sys_oper_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    title VARCHAR(100) COMMENT '操作标题',
    business_type TINYINT DEFAULT 0 COMMENT '业务类型：0=其它，1=新增，2=修改，3=删除，4=授权，5=导出，6=导入，7=强退，8=生成代码，9=清空数据',
    method VARCHAR(200) COMMENT '方法名称',
    request_method VARCHAR(20) COMMENT '请求方式',
    operator_type TINYINT DEFAULT 0 COMMENT '操作类别：0=其它，1=后台用户，2=手机端用户',
    oper_name VARCHAR(50) COMMENT '操作人员',
    dept_name VARCHAR(50) COMMENT '部门名称',
    oper_url VARCHAR(500) COMMENT '请求URL',
    oper_ip VARCHAR(128) COMMENT '主机地址',
    oper_location VARCHAR(255) COMMENT '操作地点',
    oper_param TEXT COMMENT '请求参数',
    json_result TEXT COMMENT '返回参数',
    status TINYINT DEFAULT 0 COMMENT '操作状态：0=正常，1=异常',
    error_msg TEXT COMMENT '错误消息',
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    cost_time BIGINT DEFAULT 0 COMMENT '消耗时间(毫秒)',
    INDEX idx_oper_name (oper_name),
    INDEX idx_oper_time (oper_time),
    INDEX idx_business_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录表';

-- 审计日志表
CREATE TABLE sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    module VARCHAR(50) NOT NULL COMMENT '模块名称',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型（INSERT/UPDATE/DELETE）',
    table_name VARCHAR(100) COMMENT '表名',
    record_id VARCHAR(100) COMMENT '记录ID',
    record_name VARCHAR(200) COMMENT '记录名称/描述',
    before_data TEXT COMMENT '变更前数据（JSON）',
    after_data TEXT COMMENT '变更后数据（JSON）',
    changed_fields TEXT COMMENT '变更字段详情（JSON）',
    oper_user_id BIGINT COMMENT '操作用户ID',
    oper_user_name VARCHAR(50) COMMENT '操作用户名',
    oper_ip VARCHAR(128) COMMENT '操作IP',
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_module (module),
    INDEX idx_operation (operation),
    INDEX idx_table_name (table_name),
    INDEX idx_record_id (record_id),
    INDEX idx_oper_user_id (oper_user_id),
    INDEX idx_oper_time (oper_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 五、文件管理表
-- ============================================

-- 系统文件表
CREATE TABLE sys_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称（原始文件名）',
    storage_name VARCHAR(100) NOT NULL COMMENT '存储文件名（UUID）',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(100) COMMENT '文件类型（MIME类型）',
    file_extension VARCHAR(20) COMMENT '文件扩展名',
    storage_type VARCHAR(20) DEFAULT 'local' COMMENT '存储类型（local/oss）',
    file_md5 VARCHAR(32) COMMENT '文件MD5',
    access_url VARCHAR(500) COMMENT '访问URL',
    biz_type VARCHAR(50) COMMENT '业务类型（avatar/attachment等）',
    biz_id BIGINT COMMENT '业务ID',
    upload_user_id BIGINT COMMENT '上传用户ID',
    upload_user_name VARCHAR(100) COMMENT '上传用户名',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    status SMALLINT DEFAULT 1 COMMENT '状态（0禁用 1正常）',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted SMALLINT DEFAULT 0 COMMENT '删除标志',
    INDEX idx_storage_name (storage_name),
    INDEX idx_biz_type (biz_type),
    INDEX idx_upload_user_id (upload_user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统文件表';

-- ============================================
-- 七、用户协议表
-- ============================================

-- 用户协议版本表
CREATE TABLE sys_agreement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL COMMENT '协议类型：agreement=用户协议，privacy=隐私政策',
    version VARCHAR(20) NOT NULL COMMENT '版本号',
    title VARCHAR(100) NOT NULL COMMENT '协议标题',
    content TEXT NOT NULL COMMENT '协议内容',
    status SMALLINT DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    effective_date TIMESTAMP COMMENT '生效时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人 ID',
    update_by BIGINT COMMENT '更新人 ID',
    deleted SMALLINT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_type_version (type, version),
    INDEX idx_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议版本表';

-- 用户协议同意记录表
CREATE TABLE sys_user_agreement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    agreement_id BIGINT NOT NULL COMMENT '协议版本 ID',
    agreement_type VARCHAR(20) NOT NULL COMMENT '协议类型：agreement=用户协议，privacy=隐私政策',
    agreed_version VARCHAR(20) NOT NULL COMMENT '同意的版本号',
    agreed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '同意时间',
    ip_address VARCHAR(50) COMMENT '同意时的 IP 地址',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_type (user_id, agreement_type),
    INDEX idx_user_id (user_id),
    INDEX idx_agreement_id (agreement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议同意记录表';
