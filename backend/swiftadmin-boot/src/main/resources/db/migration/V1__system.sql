-- ============================================
-- SwiftAdmin 系统模块
-- 版本: 1.0.0
-- 创建时间: 2026-03-11
-- 说明: 系统核心模块（用户、角色、菜单、部门、字典、配置、日志、文件）
-- ============================================

-- ============================================
-- 一、核心用户权限表
-- ============================================

-- 用户表
CREATE TABLE sys_user (
    id BIGINT NOT NULL PRIMARY KEY,
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
    id BIGINT NOT NULL PRIMARY KEY,
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
    id BIGINT NOT NULL PRIMARY KEY,
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
    id BIGINT NOT NULL PRIMARY KEY,
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
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent TEXT,
    status SMALLINT,
    message VARCHAR(255),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 短信验证码表
CREATE TABLE sys_sms_code (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
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
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
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
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
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
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
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
    id BIGINT NOT NULL PRIMARY KEY,
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
    id BIGINT NOT NULL PRIMARY KEY,
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

-- 操作日志表（合并审计日志功能）
CREATE TABLE sys_oper_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    -- 基础信息
    title VARCHAR(100) COMMENT '操作标题',
    business_type TINYINT DEFAULT 0 COMMENT '业务类型：0=其它，1=新增，2=修改，3=删除，4=授权，5=导出，6=导入，7=强退，8=生成代码，9=清空数据',
    method VARCHAR(200) COMMENT '方法名称',
    request_method VARCHAR(20) COMMENT '请求方式',
    operator_type TINYINT DEFAULT 0 COMMENT '操作类别：0=其它，1=后台用户，2=手机端用户',
    -- 操作人信息
    oper_user_id BIGINT COMMENT '操作用户ID',
    oper_name VARCHAR(50) COMMENT '操作人员',
    dept_name VARCHAR(50) COMMENT '部门名称',
    oper_ip VARCHAR(128) COMMENT '主机地址',
    oper_location VARCHAR(255) COMMENT '操作地点',
    -- 请求信息
    oper_url VARCHAR(500) COMMENT '请求URL',
    oper_param TEXT COMMENT '请求参数',
    json_result TEXT COMMENT '返回参数',
    -- 数据变更（审计功能）
    table_name VARCHAR(100) COMMENT '操作表名',
    record_id VARCHAR(100) COMMENT '记录ID',
    record_name VARCHAR(200) COMMENT '记录名称/描述',
    before_data TEXT COMMENT '变更前数据（JSON）',
    after_data TEXT COMMENT '变更后数据（JSON）',
    changed_fields TEXT COMMENT '变更字段详情（JSON）',
    -- 执行信息
    status TINYINT DEFAULT 0 COMMENT '操作状态：0=正常，1=异常',
    error_msg TEXT COMMENT '错误消息',
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    cost_time BIGINT DEFAULT 0 COMMENT '消耗时间(毫秒)',
    -- 索引
    INDEX idx_oper_name (oper_name),
    INDEX idx_oper_time (oper_time),
    INDEX idx_business_type (business_type),
    INDEX idx_table_record (table_name, record_id),
    INDEX idx_oper_user_id (oper_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录表';

-- ============================================
-- 五、文件管理表
-- ============================================

-- 系统文件表
CREATE TABLE sys_file (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '文件ID',
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
-- 六、用户协议表
-- ============================================

-- 用户协议版本表
CREATE TABLE sys_agreement (
    id BIGINT NOT NULL PRIMARY KEY,
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
    id BIGINT NOT NULL PRIMARY KEY,
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

-- ============================================
-- 七、初始化数据
-- ============================================

-- ============================================
-- 7.1 基础用户角色数据
-- ============================================

-- 默认管理员账号 (密码：admin123, BCrypt 加密)
INSERT INTO sys_user (id, username, password, nickname, email, phone, avatar, status, dept_id, is_super, login_fail_count, force_change_password)
VALUES (1, 'admin', '$2b$10$xNZHyOGnnUT0j3s0.AC9q..fbsKu/HJkTFSdK/wUfh5nuAo6VQovC', '超级管理员', 'admin@swiftadmin.cn', '13800000000', 'https://app-assets-oss.oss-cn-beijing.aliyuncs.com/qianzhou-app/static/image/avatar/41.jpg', 1, 1, 1, 0, 0);

-- 默认角色
INSERT INTO sys_role (id, name, code, description, data_scope, status)
VALUES (1, '超级管理员', 'admin', '拥有系统全部权限', 1, 1);

-- 预置角色模板
INSERT INTO sys_role (id, name, code, description, data_scope, status) VALUES
(2, '系统管理员', 'system_admin', '系统配置和用户管理', 1, 1),
(3, '部门经理', 'dept_manager', '部门管理和部门用户管理', 3, 1),
(4, '普通员工', 'employee', '基础功能使用权限', 5, 1),
(5, '访客', 'visitor', '仅有查看权限', 5, 1);

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 默认部门
INSERT INTO sys_dept (id, name, parent_id, ancestors, sort_order, leader, phone, email, status)
VALUES (1, '总公司', 0, '0', 1, 'admin', '13800000000', 'admin@swiftadmin.cn', 1);

-- ============================================
-- 7.2 菜单权限数据
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
-- 7.3 字典数据
-- ============================================

-- 用户状态字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(1, '用户状态', 'sys_user_status', 1, '用户状态字典');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
(1, 'sys_user_status', '正常', '1', 1, 1, '用户正常状态'),
(2, 'sys_user_status', '禁用', '0', 2, 1, '用户禁用状态');

-- 性别字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(2, '性别', 'sys_user_sex', 1, '性别字典');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
(3, 'sys_user_sex', '男', '1', 1, 1, '男性'),
(4, 'sys_user_sex', '女', '2', 2, 1, '女性'),
(5, 'sys_user_sex', '未知', '0', 3, 1, '未知性别');

-- 菜单类型字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(3, '菜单类型', 'sys_menu_type', 1, '菜单类型字典');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
(6, 'sys_menu_type', '目录', '1', 1, 1, '目录类型菜单'),
(7, 'sys_menu_type', '菜单', '2', 2, 1, '页面类型菜单'),
(8, 'sys_menu_type', '按钮', '3', 3, 1, '按钮类型权限');

-- 数据范围字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(4, '数据范围', 'sys_data_scope', 1, '数据范围字典');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
(9, 'sys_data_scope', '全部数据', '1', 1, 1, '可查看所有数据'),
(10, 'sys_data_scope', '自定义数据', '2', 2, 1, '可查看自定义部门数据'),
(11, 'sys_data_scope', '本部门数据', '3', 3, 1, '仅可查看本部门数据'),
(12, 'sys_data_scope', '本部门及以下', '4', 4, 1, '可查看本部门及子部门数据'),
(13, 'sys_data_scope', '仅本人数据', '5', 5, 1, '仅可查看本人数据');

-- 配置类型字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(5, '配置类型', 'sys_config_type', 1, '系统配置类型字典');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort_order, status, remark) VALUES
(14, 'sys_config_type', '系统配置', 'system', 1, 1, '系统基础配置'),
(15, 'sys_config_type', '密码策略', 'password', 2, 1, '密码安全策略配置'),
(16, 'sys_config_type', '文件上传', 'upload', 3, 1, '文件上传相关配置'),
(17, 'sys_config_type', '验证码', 'captcha', 4, 1, '验证码相关配置'),
(18, 'sys_config_type', '登录配置', 'login', 5, 1, '登录相关配置'),
(19, 'sys_config_type', '会话配置', 'session', 6, 1, '会话相关配置'),
(20, 'sys_config_type', '日志配置', 'log', 7, 1, '日志相关配置'),
(21, 'sys_config_type', '邮件服务', 'email', 8, 1, '邮件服务配置'),
(22, 'sys_config_type', '短信服务', 'sms', 9, 1, '短信服务配置');

-- ============================================
-- 7.4 系统配置数据
-- ============================================

-- 密码策略配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(1, 'password.expire_days', '90', '密码有效期（天）', 'password', '密码过期时间，超过此天数强制修改密码'),
(2, 'password.history_count', '5', '密码历史检查数量', 'password', '修改密码时检查最近N次历史密码，防止重复'),
(3, 'password.force_change_on_reset', 'true', '管理员重置后强制修改', 'password', '管理员重置密码后是否强制用户修改'),
(4, 'password.min_length', '8', '密码最小长度', 'password', '密码最小长度要求'),
(5, 'password.expire_warning_days', '7', '密码过期提前提醒天数', 'password', '密码过期前N天开始提醒');

-- 系统基础配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(6, 'system.name', 'SwiftAdmin', '系统名称', 'system', '系统显示名称'),
(7, 'system.version', '1.0.0', '系统版本', 'system', '系统版本号'),
(8, 'system.copyright', '© 2026 SwiftAdmin. All rights reserved.', '版权信息', 'system', '页脚版权信息'),
(9, 'system.logo', '/static/logo.png', '系统Logo', 'system', '系统Logo路径');

-- 文件上传配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(10, 'upload.max_size', '10485760', '文件上传最大限制（字节）', 'upload', '单个文件上传最大限制，默认10MB'),
(11, 'upload.allow_types', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx', '允许上传的文件类型', 'upload', '允许上传的文件扩展名，逗号分隔'),
(12, 'upload.save_path', '/upload', '文件保存路径', 'upload', '文件上传保存的根路径');

-- 验证码配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(13, 'captcha.enable', 'true', '验证码开关', 'captcha', '是否开启验证码验证'),
(14, 'captcha.type', 'math', '验证码类型', 'captcha', '验证码类型：math=数学运算，char=字符'),
(15, 'captcha.length', '4', '验证码长度', 'captcha', '验证码字符长度'),
(16, 'captcha.expire_seconds', '300', '验证码有效期（秒）', 'captcha', '验证码过期时间');

-- 登录配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(17, 'login.max_retry', '5', '登录最大重试次数', 'login', '登录失败最大重试次数'),
(18, 'login.lock_minutes', '30', '账户锁定时间（分钟）', 'login', '超过最大重试次数后锁定时间'),
(19, 'login.single_device', 'false', '单设备登录', 'login', '是否限制同一账号只能在一个设备登录');

-- 会话配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(20, 'session.timeout', '7200', '会话超时时间（秒）', 'session', '用户无操作会话超时时间'),
(21, 'session.max_concurrent', '1', '最大并发会话数', 'session', '同一用户最大并发会话数');

-- 日志配置
INSERT INTO sys_config (id, config_key, config_value, config_name, config_type, remark) VALUES
(22, 'log.oper.keep_days', '90', '操作日志保留天数', 'log', '操作日志保留天数，超过自动清理'),
(23, 'log.login.keep_days', '180', '登录日志保留天数', 'log', '登录日志保留天数，超过自动清理');

-- ============================================
-- 7.5 角色菜单关联数据
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