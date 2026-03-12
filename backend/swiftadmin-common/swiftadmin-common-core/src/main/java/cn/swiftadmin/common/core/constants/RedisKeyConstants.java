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

    // ==================== 用户相关 ====================

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

}