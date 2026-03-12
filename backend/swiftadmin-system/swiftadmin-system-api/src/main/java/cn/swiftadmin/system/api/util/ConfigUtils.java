package cn.swiftadmin.system.api.util;

import cn.swiftadmin.system.api.service.ConfigService;

/**
 * 系统配置工具类
 * <p>提供便捷的配置读取功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public final class ConfigUtils {

    private ConfigUtils() {
    }

    /** 配置服务实例（由 Spring 容器注入） */
    private static ConfigService configService;

    /**
     * 设置配置服务实例
     * <p>由 Spring 容器在启动时调用</p>
     *
     * @param service 配置服务
     */
    public static void setConfigService(ConfigService service) {
        configService = service;
    }

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值，不存在返回 null
     */
    public static String getValue(String key) {
        return getValue(key, null);
    }

    /**
     * 获取配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getValue(String key, String defaultValue) {
        if (configService == null) {
            return defaultValue;
        }
        return configService.getValue(key, defaultValue);
    }

    /**
     * 获取整数配置值
     *
     * @param key 配置键
     * @return 配置值，不存在返回 0
     */
    public static int getIntValue(String key) {
        return getIntValue(key, 0);
    }

    /**
     * 获取整数配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static int getIntValue(String key, int defaultValue) {
        if (configService == null) {
            return defaultValue;
        }
        return configService.getIntValue(key, defaultValue);
    }

    /**
     * 获取布尔配置值
     *
     * @param key 配置键
     * @return 配置值，不存在返回 false
     */
    public static boolean getBoolValue(String key) {
        return getBoolValue(key, false);
    }

    /**
     * 获取布尔配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static boolean getBoolValue(String key, boolean defaultValue) {
        if (configService == null) {
            return defaultValue;
        }
        return configService.getBoolValue(key, defaultValue);
    }

    /**
     * 获取长整型配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static long getLongValue(String key, long defaultValue) {
        String value = getValue(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取双精度浮点配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static double getDoubleValue(String key, double defaultValue) {
        String value = getValue(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ==================== 密码策略配置快捷方法 ====================

    /**
     * 获取密码最小长度
     *
     * @return 密码最小长度
     */
    public static int getPasswordMinLength() {
        return getIntValue("password.min_length", 8);
    }

    /**
     * 获取密码有效期（天）
     *
     * @return 密码有效期
     */
    public static int getPasswordExpireDays() {
        return getIntValue("password.expire_days", 90);
    }

    /**
     * 获取密码历史检查数量
     *
     * @return 密码历史检查数量
     */
    public static int getPasswordHistoryCount() {
        return getIntValue("password.history_count", 5);
    }

    /**
     * 获取登录失败锁定阈值
     *
     * @return 登录失败锁定阈值
     */
    public static int getLoginFailLockThreshold() {
        return getIntValue("login.fail_lock_threshold", 5);
    }

    /**
     * 获取账户锁定时长（分钟）
     *
     * @return 账户锁定时长
     */
    public static int getAccountLockMinutes() {
        return getIntValue("login.lock_minutes", 30);
    }

    // ==================== 系统配置快捷方法 ====================

    /**
     * 是否开启验证码
     *
     * @return true=开启
     */
    public static boolean isCaptchaEnabled() {
        return getBoolValue("system.captcha_enabled", true);
    }

    /**
     * 是否开启注册功能
     *
     * @return true=开启
     */
    public static boolean isRegisterEnabled() {
        return getBoolValue("system.register_enabled", false);
    }

    /**
     * 获取系统名称
     *
     * @return 系统名称
     */
    public static String getSystemName() {
        return getValue("system.name", "SwiftAdmin");
    }

}