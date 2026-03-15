-- ============================================
-- SwiftAdmin 定时任务模块
-- 版本: 1.0.0
-- 创建时间: 2026-03-15
-- 说明: 定时任务管理模块（任务配置、执行日志）
-- ============================================

-- ============================================
-- 一、定时任务核心表
-- ============================================

-- 定时任务表
CREATE TABLE schedule_job (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '任务ID',
    job_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    job_group VARCHAR(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
    bean_name VARCHAR(200) COMMENT 'Bean名称',
    method_name VARCHAR(100) COMMENT '方法名称',
    method_params TEXT COMMENT '方法参数（JSON）',
    job_type TINYINT DEFAULT 1 COMMENT '任务类型：1=Bean',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
    timeout INT DEFAULT 300 COMMENT '超时时间（秒）',
    retry_times INT DEFAULT 0 COMMENT '重试次数',
    retry_interval INT DEFAULT 0 COMMENT '重试间隔（秒）',
    status TINYINT DEFAULT 0 COMMENT '状态：0=正常，1=暂停',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted SMALLINT DEFAULT 0 COMMENT '删除标识',
    UNIQUE KEY uk_job_name_group (job_name, job_group),
    KEY idx_job_status (status),
    KEY idx_job_group (job_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

-- 定时任务执行日志表
CREATE TABLE schedule_job_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    job_id BIGINT NOT NULL COMMENT '任务ID',
    job_name VARCHAR(100) COMMENT '任务名称',
    job_group VARCHAR(50) COMMENT '任务分组',
    execute_status TINYINT DEFAULT 0 COMMENT '执行状态：0=执行中，1=成功，2=失败',
    execute_time TIMESTAMP COMMENT '执行时间',
    cost_time BIGINT COMMENT '耗时（毫秒）',
    execute_result TEXT COMMENT '执行结果',
    error_msg TEXT COMMENT '错误信息',
    stack_trace TEXT COMMENT '异常堆栈',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    execute_type TINYINT DEFAULT 1 COMMENT '执行类型：1=自动执行，2=手动触发',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted SMALLINT DEFAULT 0 COMMENT '删除标识',
    KEY idx_job_id (job_id),
    KEY idx_execute_time (execute_time),
    KEY idx_execute_status (execute_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

-- ============================================
-- 二、菜单和权限数据
-- ============================================

-- 定时任务管理一级菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(300, 0, '定时任务', '/schedule', NULL, 1, 'mdi:clock', 300, NULL, 1);

-- 任务管理二级菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(301, 300, '任务管理', '/schedule/job', 'schedule/job/index', 2, 'mdi:robot', 1, NULL, 1);

-- 任务管理按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(302, 301, '任务列表', NULL, NULL, 3, NULL, 1, 'schedule:job:list', 1),
(303, 301, '新增任务', NULL, NULL, 3, NULL, 2, 'schedule:job:add', 1),
(304, 301, '编辑任务', NULL, NULL, 3, NULL, 3, 'schedule:job:edit', 1),
(305, 301, '删除任务', NULL, NULL, 3, NULL, 4, 'schedule:job:delete', 1),
(306, 301, '启用任务', NULL, NULL, 3, NULL, 5, 'schedule:job:enable', 1),
(307, 301, '暂停任务', NULL, NULL, 3, NULL, 6, 'schedule:job:disable', 1),
(308, 301, '手动触发', NULL, NULL, 3, NULL, 7, 'schedule:job:trigger', 1);

-- 执行日志二级菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(309, 300, '执行日志', '/schedule/log', 'schedule/log/index', 2, 'mdi:file-document', 2, NULL, 1);

-- 执行日志按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, type, icon, sort_order, permission, status) VALUES
(310, 309, '日志列表', NULL, NULL, 3, NULL, 1, 'schedule:log:list', 1),
(311, 309, '查看日志', NULL, NULL, 3, NULL, 2, 'schedule:log:query', 1);

-- ============================================
-- 三、预置定时任务数据
-- ============================================

-- 社交 Token 自动刷新任务
INSERT INTO schedule_job (
    id,
    job_name,
    job_group,
    bean_name,
    method_name,
    method_params,
    job_type,
    cron_expression,
    timeout,
    retry_times,
    retry_interval,
    status,
    remark,
    create_time,
    update_time,
    create_by,
    update_by,
    deleted
) VALUES (
    400,                                    -- 任务ID，注意避免与现有数据冲突
    '社交Token自动刷新',                     -- 任务名称
    'DEFAULT',                              -- 任务分组
    'socialTokenRefreshTask',               -- Bean 名称
    'refreshExpiringTokens',                -- 方法名称（无参）
    NULL,                                   -- 方法参数（无参）
    1,                                      -- 任务类型：1=Bean（JobType.BEAN）
    '0 0 * * * ?',                          -- Cron：每小时整点执行
    300,                                    -- 超时时间（秒）
    0,                                      -- 重试次数
    0,                                      -- 重试间隔（秒）
    0,                                      -- 状态：0=正常，1=暂停
    '每小时刷新即将过期的社交Token',          -- 备注
    NOW(),                                  -- 创建时间
    NOW(),                                  -- 更新时间
    1,                                      -- 创建人（可根据实际调整）
    1,                                      -- 更新人
    0                                       -- 未删除
);

