package cn.swiftadmin.common.core.util;

import org.springframework.util.StringUtils;

/**
 * 敏感信息脱敏工具类
 * <p>用于日志输出时脱敏用户名、IP、手机号等敏感信息</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public final class SensitiveUtils {

    private SensitiveUtils() {
    }

    /**
     * 用户名脱敏
     * <p>规则：保留首尾字符，中间用 *** 替代</p>
     * <p>示例：zhangsan → zha***n，李四 → 李***四</p>
     *
     * @param username 用户名
     * @return 脱敏后的用户名
     */
    public static String maskUsername(String username) {
        if (!StringUtils.hasLength(username)) {
            return "***";
        }
        if (username.length() <= 2) {
            return "***";
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }

    /**
     * IP 地址脱敏
     * <p>规则：保留前三段，最后一段用 * 替代</p>
     * <p>示例：192.168.1.100 → 192.168.1.*</p>
     *
     * @param ip IP 地址
     * @return 脱敏后的 IP
     */
    public static String maskIp(String ip) {
        if (!StringUtils.hasLength(ip)) {
            return "***.*.*.*";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
        }
        return "***.*.*.*";
    }

    /**
     * 手机号脱敏
     * <p>规则：保留前 3 位和后 4 位，中间用 **** 替代</p>
     * <p>示例：13812345678 → 138****5678</p>
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasLength(phone)) {
            return "***";
        }
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏
     * <p>规则：保留前 3 位和@后面的部分，中间用 *** 替代</p>
     * <p>示例：zhangsan@example.com → zha***@example.com</p>
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (!StringUtils.hasLength(email)) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (prefix.length() <= 3) {
            return "***" + domain;
        }
        return prefix.substring(0, 3) + "***" + domain;
    }

    /**
     * 密码脱敏（用于日志）
     * <p>规则：固定返回 ********</p>
     *
     * @param password 密码
     * @return 固定脱敏字符串
     */
    public static String maskPassword(String password) {
        return "********";
    }

}
