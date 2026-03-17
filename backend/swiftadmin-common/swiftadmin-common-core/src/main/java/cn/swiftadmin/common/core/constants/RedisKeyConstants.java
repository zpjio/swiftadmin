package cn.swiftadmin.common.core.constants;

/**
 * Redis Key 常量定义
 * <p>统一管理 Redis Key 命名规范，避免硬编码</p>
 * <p>命名规范：业务模块:功能:标识</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    // ==================== 认证相关 ====================

    /** 登录 Token 前缀 */
    public static final String LOGIN_TOKEN = "auth:token:";

    /** 登录会话额外信息前缀 */
    public static final String LOGIN_SESSION_EXTRA = "auth:session:extra:";

    /** 登录失败次数 */
    public static final String LOGIN_FAIL_COUNT = "auth:fail:count:";

    /** 刷新 Token 前缀 */
    public static final String REFRESH_TOKEN = "auth:refresh:";

    /** 验证码前缀 */
    public static final String CAPTCHA = "auth:captcha:";

    /** 短信验证码前缀 */
    public static final String SMS_CODE = "auth:sms:";

    /** OAuth State 前缀 */
    public static final String OAUTH_STATE = "oauth:state:";

    /** OAuth 临时 Token 前缀 */
    public static final String OAUTH_TEMP_TOKEN = "oauth:temp:";

    /** JWT 黑名单前缀 */
    public static final String JWT_BLACKLIST = "jwt:blacklist:";

    /** Token 刷新频率前缀 */
    public static final String TOKEN_REFRESH_FREQ = "token:refresh:freq:";

    /** Token 刷新次数前缀 */
    public static final String TOKEN_REFRESH_COUNT = "token:refresh:count:";

    /** Token 上下文 IP 前缀 */
    public static final String TOKEN_CONTEXT_IP = "token:context:ip:";

    /** Token 上下文 UA 前缀 */
    public static final String TOKEN_CONTEXT_UA = "token:context:ua:";

    /** Token IP 变化次数前缀 */
    public static final String TOKEN_IP_CHANGE_COUNT = "token:ip:change:";

    /** 临时 Token 已使用前缀 */
    public static final String TEMP_TOKEN_USED = "temp:token:used:";

    // ==================== 用户相关 ====================

    /** 用户信息缓存前缀 */
    public static final String USER_INFO = "user:info:";

    /** 用户权限缓存前缀 */
    public static final String USER_PERMISSION = "user:permission:";

    /** 用户 Token 列表 */
    public static final String USER_TOKENS = "user:tokens:";

    /** 在线用户 */
    public static final String ONLINE_USER = "user:online:";

    // ==================== 系统配置 ====================

    /** 系统配置缓存前缀 */
    public static final String SYS_CONFIG = "sys:config:";

    /** 字典缓存前缀 */
    public static final String SYS_DICT = "sys:dict:";

    // ==================== 限流相关 ====================

    /** IP 限流前缀 */
    public static final String RATE_LIMIT_IP = "rate:ip:";

    /** 接口限流前缀 */
    public static final String RATE_LIMIT_API = "rate:api:";

    /** IP 登录尝试次数前缀 */
    public static final String IP_LOGIN_ATTEMPTS = "auth:ip:attempts:";

    /** IP 登录黑名单前缀 */
    public static final String IP_LOGIN_BLACKLIST = "auth:ip:blacklist:";

    // ==================== 第三方平台 ====================

    /** 企业微信 Access Token */
    public static final String WECOM_ACCESS_TOKEN = "wecom:access_token";

    /** 钉钉 Access Token */
    public static final String DINGTALK_ACCESS_TOKEN = "dingtalk:access_token";

    // ==================== 工具方法 ====================

    /**
     * 构建登录 Token Key
     */
    public static String loginToken(String sessionId) {
        return LOGIN_TOKEN + sessionId;
    }

    /**
     * 构建登录会话额外信息 Key
     */
    public static String loginSessionExtra(String sessionId) {
        return LOGIN_SESSION_EXTRA + sessionId;
    }

    /**
     * 构建用户信息缓存 Key
     */
    public static String userInfo(Long userId) {
        return USER_INFO + userId;
    }

    /**
     * 构建用户权限缓存 Key
     */
    public static String userPermission(Long userId) {
        return USER_PERMISSION + userId;
    }

    /**
     * 构建验证码 Key
     */
    public static String captcha(String uuid) {
        return CAPTCHA + uuid;
    }

    /**
     * 构建短信验证码 Key
     */
    public static String smsCode(String phone) {
        return SMS_CODE + phone;
    }

    /**
     * 构建登录失败次数 Key
     */
    public static String loginFailCount(String username) {
        return LOGIN_FAIL_COUNT + username;
    }

    /**
     * 构建系统配置 Key
     */
    public static String sysConfig(String key) {
        return SYS_CONFIG + key;
    }

    /**
     * 构建 IP 限流 Key
     */
    public static String rateLimitIp(String ip) {
        return RATE_LIMIT_IP + ip;
    }

    /**
     * 构建 JWT 黑名单 Key
     */
    public static String jwtBlacklist(String jti) {
        return JWT_BLACKLIST + jti;
    }

    /**
     * 构建 Token 刷新频率 Key
     */
    public static String tokenRefreshFreq(String username) {
        return TOKEN_REFRESH_FREQ + username;
    }

    /**
     * 构建 Token 刷新次数 Key
     */
    public static String tokenRefreshCount(String username) {
        return TOKEN_REFRESH_COUNT + username;
    }

    /**
     * 构建 Token 上下文 IP Key
     */
    public static String tokenContextIp(String jti) {
        return TOKEN_CONTEXT_IP + jti;
    }

    /**
     * 构建 Token 上下文 UA Key
     */
    public static String tokenContextUa(String jti) {
        return TOKEN_CONTEXT_UA + jti;
    }

    /**
     * 构建临时 Token 已使用 Key
     */
    public static String tempTokenUsed(String jti) {
        return TEMP_TOKEN_USED + jti;
    }

    /**
     * 构建 Token IP 变化次数 Key
     */
    public static String tokenIpChangeCount(String jti) {
        return TOKEN_IP_CHANGE_COUNT + jti;
    }

    /**
     * 构建 IP 登录尝试次数 Key
     */
    public static String ipLoginAttempts(String ip) {
        return IP_LOGIN_ATTEMPTS + ip;
    }

    /**
     * 构建 IP 登录黑名单 Key
     */
    public static String ipLoginBlacklist(String ip) {
        return IP_LOGIN_BLACKLIST + ip;
    }

    /**
     * 构建 Token 每日刷新次数 Key
     *
     * @param username 用户名
     * @param date 日期（格式：yyyy-MM-dd）
     * @return Redis Key
     */
    public static String tokenRefreshDailyCount(String username, String date) {
        return TOKEN_REFRESH_COUNT + username + ":daily:" + date;
    }

    /**
     * 构建 OAuth State Key
     *
     * @param state OAuth 状态码
     * @return Redis Key
     */
    public static String oauthState(String state) {
        return OAUTH_STATE + state;
    }

    /**
     * 构建 OAuth 临时 Token Key
     *
     * @param tempToken 临时 Token
     * @return Redis Key
     */
    public static String oauthTempToken(String tempToken) {
        return OAUTH_TEMP_TOKEN + tempToken;
    }

}