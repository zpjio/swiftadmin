package cn.swiftadmin.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>
 * 错误码格式: XYYZZZ
 * <ul>
 *   <li>X - 模块编码 (1=公共, 2=认证, 3=用户, 4=系统, 5=文件, 6=消息, 7=定时任务, 9=HTTP)</li>
 *   <li>YY - 错误类型 (00=成功, 01=参数错误, 02=权限错误, 03=业务错误, 04=系统错误)</li>
 *   <li>ZZZ - 序号 (001-999)</li>
 * </ul>
 * </p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 成功 ====================
    /** 成功 */
    SUCCESS(0, "操作成功"),

    // ==================== 公共模块 (1xxxxx) ====================

    // 公共-参数错误 (101xxx)
    /** 参数校验失败 */
    PARAM_INVALID(101001, "参数校验失败"),
    /** 缺少必要参数 */
    PARAM_MISSING(101002, "缺少必要参数"),
    /** 参数格式错误 */
    PARAM_FORMAT_ERROR(101003, "参数格式错误"),
    /** 参数值超出范围 */
    PARAM_OUT_OF_RANGE(101004, "参数值超出允许范围"),

    // 公共-权限错误 (102xxx)
    /** 未登录或登录已过期 */
    UNAUTHORIZED(102001, "未登录或登录已过期"),
    /** 无权限访问 */
    PERMISSION_DENIED(102002, "无权限访问"),
    /** 无权访问该数据 */
    DATA_SCOPE_DENIED(102003, "无权访问该数据"),
    /** 数据权限校验失败 */
    DATA_PERMISSION_DENIED(102004, "数据权限校验失败"),

    // ==================== 认证模块 (2xxxxx) ====================

    // 认证-Token错误 (202xxx)
    /** Token 无效或已过期 */
    TOKEN_INVALID(202001, "Token 无效或已过期"),
    /** Token 已过期 */
    TOKEN_EXPIRED(202002, "Token 已过期"),
    /** Token 可能被盗用 */
    TOKEN_STOLEN(202003, "Token 异常，请重新登录"),
    /** Token 刷新过于频繁 */
    TOKEN_REFRESH_TOO_FREQUENT(202004, "Token 刷新过于频繁，请稍后再试"),
    /** Token 刷新次数超限 */
    TOKEN_REFRESH_LIMIT_EXCEEDED(202005, "Token 刷新次数已达上限，请明天再试"),

    // 认证-业务错误 (203xxx)
    /** 用户名或密码错误 */
    LOGIN_FAILED(203001, "用户名或密码错误"),
    /** 验证码错误或已过期 */
    CAPTCHA_ERROR(203002, "验证码错误或已过期"),
    /** 请先完成图形验证码验证 */
    CAPTCHA_REQUIRED(203003, "请先完成图形验证码验证"),
    /** 不支持的登录方式 */
    LOGIN_TYPE_NOT_SUPPORTED(203004, "不支持的登录方式"),
    /** 短信验证码错误或已过期 */
    SMS_CODE_INVALID(203005, "验证码错误或已过期"),
    /** 发送过于频繁 */
    SEND_TOO_FREQUENT(203006, "发送过于频繁，请稍后再试"),
    /** 操作次数已达上限 */
    RATE_LIMIT_EXCEEDED(203007, "操作次数已达上限，请稍后再试"),

    // 认证-社交登录错误 (204xxx)
    /** 授权码无效或已过期 */
    SOCIAL_AUTH_CODE_INVALID(204001, "授权码无效或已过期"),
    /** State 参数无效或已过期 */
    SOCIAL_STATE_INVALID(204002, "State 参数无效或已过期"),
    /** 第三方平台服务异常 */
    SOCIAL_PLATFORM_ERROR(204003, "第三方平台服务异常"),
    /** 该社交账号未绑定系统账号 */
    SOCIAL_ACCOUNT_NOT_BOUND(204004, "该社交账号未绑定系统账号"),
    /** 已绑定其他系统账号 */
    SOCIAL_ACCOUNT_ALREADY_BOUND(204005, "已绑定其他系统账号"),
    /** 不支持的社交平台 */
    SOCIAL_PLATFORM_NOT_SUPPORTED(204006, "不支持的社交平台"),

    // 认证-账户错误 (205xxx)
    /** 账户已锁定 */
    ACCOUNT_LOCKED(205001, "账户已锁定，请稍后再试"),
    /** 账户已被禁用 */
    ACCOUNT_DISABLED(205002, "账户已被禁用"),
    /** 密码已过期 */
    PASSWORD_EXPIRED(205003, "密码已过期，请修改密码"),
    /** 密码即将过期 */
    PASSWORD_EXPIRING_SOON(205004, "密码即将过期，请及时修改"),
    /** 密码强度不足 */
    PASSWORD_WEAK(205005, "密码强度不足"),
    /** 密码修改失败 */
    PASSWORD_CHANGE_FAILED(205006, "密码修改失败"),
    /** 原密码错误 */
    OLD_PASSWORD_ERROR(205007, "原密码错误"),

    // 认证-安全错误 (206xxx)
    /** IP 地址受限 */
    IP_RESTRICTED(206001, "IP 地址受限，请稍后再试"),
    /** 设备指纹不匹配 */
    DEVICE_FINGERPRINT_MISMATCH(206002, "设备环境异常，请重新登录"),
    /** 检测到异地登录 */
    ABNORMAL_LOCATION_LOGIN(206003, "检测到异地登录，请确认是否为本人操作"),

    // 认证-合规错误 (207xxx)
    /** 请先阅读并同意用户协议 */
    AGREEMENT_NOT_SIGNED(207001, "请先阅读并同意用户协议"),
    /** 请先阅读并同意隐私政策 */
    PRIVACY_POLICY_NOT_AGREED(207002, "请先阅读并同意隐私政策"),

    // ==================== 用户模块 (3xxxxx) ====================

    // 用户-业务错误 (303xxx)
    /** 用户不存在 */
    USER_NOT_FOUND(303001, "用户不存在"),
    /** 用户名已存在 */
    USER_EXISTS(303002, "用户名已存在"),
    /** 手机号已存在 */
    PHONE_EXISTS(303003, "手机号已被注册"),
    /** 邮箱已存在 */
    EMAIL_EXISTS(303004, "邮箱已被注册"),
    /** 手机号未注册 */
    USER_NOT_REGISTERED(303005, "手机号未注册，请先注册账号"),
    /** 用户状态异常 */
    USER_STATUS_ERROR(303006, "用户状态异常"),

    // ==================== 系统模块 (4xxxxx) ====================

    // 系统-系统错误 (404xxx)
    /** 系统异常 */
    SYSTEM_ERROR(404001, "系统异常，请稍后重试"),
    /** 服务暂不可用 */
    SERVICE_UNAVAILABLE(404002, "服务暂不可用"),
    /** 资源不存在 */
    NOT_FOUND(404003, "资源不存在"),
    /** 功能暂未开放 */
    FEATURE_NOT_AVAILABLE(404004, "功能暂未开放"),

    // ==================== 文件模块 (5xxxxx) ====================

    // 文件-系统错误 (504xxx)
    /** 文件上传失败 */
    FILE_UPLOAD_FAILED(504001, "文件上传失败"),
    /** 文件不存在 */
    FILE_NOT_FOUND(504002, "文件不存在"),
    /** 文件大小超出限制 */
    FILE_SIZE_EXCEEDED(504003, "文件大小超出限制"),
    /** 不支持的文件类型 */
    FILE_TYPE_NOT_SUPPORTED(504004, "不支持的文件类型"),
    /** 文件删除失败 */
    FILE_DELETE_FAILED(504005, "文件删除失败"),
    /** 文件名称无效 */
    FILE_NAME_INVALID(504006, "文件名称无效"),

    // ==================== 消息模块 (6xxxxx) ====================

    // 消息-业务错误 (603xxx)
    /** 消息发送失败 */
    MESSAGE_SEND_FAILED(603001, "消息发送失败"),
    /** 消息模板不存在 */
    MESSAGE_TEMPLATE_NOT_FOUND(603002, "消息模板不存在"),
    /** 消息渠道不可用 */
    MESSAGE_CHANNEL_UNAVAILABLE(603003, "消息渠道不可用"),
    /** 消息渠道配置错误 */
    MESSAGE_CHANNEL_CONFIG_ERROR(603004, "消息渠道配置错误"),

    // 消息-系统错误 (604xxx)
    /** 消息服务异常 */
    MESSAGE_SERVICE_ERROR(604001, "消息服务异常"),

    // ==================== 定时任务模块 (7xxxxx) ====================

    // 定时任务-业务错误 (703xxx)
    /** 任务不存在 */
    JOB_NOT_FOUND(703001, "任务不存在"),
    /** 任务已暂停 */
    JOB_PAUSED(703002, "任务已暂停"),
    /** 任务执行器不存在 */
    JOB_EXECUTOR_NOT_FOUND(703003, "任务执行器不存在"),
    /** Cron 表达式无效 */
    CRON_EXPRESSION_INVALID(703004, "Cron 表达式无效"),
    /** 任务正在执行中 */
    JOB_RUNNING(703005, "任务正在执行中"),
    /** 任务配置无效 */
    JOB_CONFIG_INVALID(703006, "任务配置无效"),
    /** 任务已存在 */
    JOB_EXISTS(703007, "任务已存在"),

    // 定时任务-系统错误 (704xxx)
    /** 任务执行异常 */
    JOB_EXECUTE_ERROR(704001, "任务执行异常"),

    // ==================== HTTP 相关 (9xxxxx) ====================

    /** 请求方法不支持 */
    HTTP_METHOD_NOT_ALLOWED(904001, "不支持的请求方法");

    /** 错误码 */
    private final int code;

    /** 错误消息 */
    private final String message;

    /**
     * 获取模块编码
     * @return 模块编码
     */
    public int getModule() {
        return code / 100000;
    }

    /**
     * 获取错误类型
     * @return 错误类型编码
     */
    public int getErrorType() {
        return (code / 1000) % 100;
    }

    /**
     * 获取序号
     * @return 序号
     */
    public int getSeq() {
        return code % 1000;
    }

    /**
     * 是否为业务错误
     * @return true 表示业务错误
     */
    public boolean isBusinessError() {
        return getErrorType() == 3;
    }

    /**
     * 是否为参数错误
     * @return true 表示参数错误
     */
    public boolean isParamError() {
        return getErrorType() == 1;
    }

    /**
     * 是否为权限错误
     * @return true 表示权限错误
     */
    public boolean isAuthError() {
        return getErrorType() == 2;
    }

    /**
     * 是否为系统错误
     * @return true 表示系统错误
     */
    public boolean isSystemError() {
        return getErrorType() == 4;
    }
}