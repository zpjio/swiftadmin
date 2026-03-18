package cn.swiftadmin.auth.api.constant;

/**
 * 认证相关常量
 * <p>统一管理认证模块的常量定义，避免魔法值</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public final class AuthConstants {

    private AuthConstants() {
        throw new IllegalStateException("Constant class");
    }

    // ========== Token 相关常量 ==========

    /** Token 前缀：Bearer */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Token 类型：Access Token */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** Token 类型：Refresh Token */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** Token 类型：Temp Token */
    public static final String TOKEN_TYPE_TEMP = "temp";

    /** Access Token 默认有效期：2 小时（毫秒） */
    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION = 7200000L;

    /** Refresh Token 默认有效期：7 天（毫秒） */
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION = 604800000L;

    /** Temp Token 默认有效期：10 分钟（毫秒） */
    public static final long DEFAULT_TEMP_TOKEN_EXPIRATION = 600000L;

    /** Temp Token 最大使用次数 */
    public static final int TEMP_TOKEN_MAX_USES = 1;

    // ========== 登录相关常量 ==========

    /** 登录成功状态 */
    public static final int LOGIN_STATUS_SUCCESS = 1;

    /** 登录失败状态 */
    public static final int LOGIN_STATUS_FAILURE = 0;

    /** 用户状态：正常 */
    public static final int USER_STATUS_NORMAL = 1;

    /** 用户状态：禁用 */
    public static final int USER_STATUS_DISABLED = 0;

    /** 强制修改密码标识：是 */
    public static final int FORCE_CHANGE_PASSWORD_YES = 1;

    /** 强制修改密码标识：否 */
    public static final int FORCE_CHANGE_PASSWORD_NO = 0;

    // ========== 短信验证码相关常量 ==========

    /** 验证码场景：登录 */
    public static final String SMS_SCENE_LOGIN = "login";

    /** 验证码场景：注册 */
    public static final String SMS_SCENE_REGISTER = "register";

    /** 验证码场景：绑定 */
    public static final String SMS_SCENE_BIND = "bind";

    /** 验证码状态：未使用 */
    public static final int SMS_CODE_STATUS_UNUSED = 1;

    /** 验证码状态：已使用 */
    public static final int SMS_CODE_STATUS_USED = 0;

    /** 验证码默认有效期：5 分钟 */
    public static final int DEFAULT_SMS_CODE_EXPIRE_MINUTES = 5;

    /** 验证码默认长度：6 位 */
    public static final int DEFAULT_SMS_CODE_LENGTH = 6;

    // ========== 安全策略相关常量 ==========

    /** 默认最大登录失败次数 */
    public static final int DEFAULT_MAX_LOGIN_FAIL_COUNT = 5;

    /** 默认账户锁定时间：15 分钟 */
    public static final int DEFAULT_LOCK_MINUTES = 15;

    /** 默认 Token 刷新最小间隔：5 秒 */
    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 5;

    /** 默认每日最大 Token 刷新次数：50 次 */
    public static final int DEFAULT_MAX_REFRESH_COUNT_PER_DAY = 50;

    /** 默认 OAuth State 有效期：10 分钟 */
    public static final int DEFAULT_OAUTH_STATE_EXPIRE_MINUTES = 10;

    /** 默认临时 Token 有效期：10 分钟 */
    public static final int DEFAULT_TEMP_TOKEN_EXPIRE_MINUTES = 10;

    // ========== IP 限流相关常量 ==========

    /** 默认 IP 登录限流阈值：每小时 20 次 */
    public static final int DEFAULT_IP_LOGIN_LIMIT_PER_HOUR = 20;

    /** 默认 IP 黑名单锁定时间：30 分钟 */
    public static final int DEFAULT_IP_BLACKLIST_LOCK_MINUTES = 30;

}