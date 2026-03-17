package cn.swiftadmin.common.core.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 密码策略配置属性
 * <p>支持通过配置文件灵活配置密码复杂度要求</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "auth.password")
public class PasswordProperties {

    // ========== 长度配置 ==========

    /**
     * 最小密码长度
     */
    private int minLength = 8;

    /**
     * 最大密码长度
     */
    private int maxLength = 32;

    // ========== 复杂度配置 ==========

    /**
     * 是否要求包含大写字母
     */
    private boolean requireUppercase = true;

    /**
     * 是否要求包含小写字母
     */
    private boolean requireLowercase = true;

    /**
     * 是否要求包含数字
     */
    private boolean requireDigit = true;

    /**
     * 是否要求包含特殊字符
     */
    private boolean requireSpecialChar = false;

    /**
     * 允许的特殊字符集合
     */
    private String allowedSpecialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    // ========== 高级安全配置 ==========

    /**
     * 是否禁止包含用户名
     */
    private boolean forbidContainUsername = true;

    /**
     * 连续字符检测长度（0表示不检测）
     */
    private int maxSequentialChars = 3;

    /**
     * 重复字符检测长度（0表示不检测）
     */
    private int maxRepeatingChars = 3;

    /**
     * 历史密码检查次数（0表示不检查）
     */
    private int historyCheckCount = 5;

    // ========== 弱密码配置 ==========

    /**
     * 是否启用弱密码检测
     */
    private boolean enableWeakPasswordCheck = true;

    /**
     * 自定义弱密码列表（追加到默认列表）
     */
    private Set<String> customWeakPasswords = new HashSet<>();

    /**
     * 默认弱密码列表
     */
    private static final Set<String> DEFAULT_WEAK_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "master", "dragon", "letmein", "login",
            "password1", "welcome", "admin", "root", "test",
            "123456789", "12345678", "1234567", "123456",
            "111111", "000000", "password123", "admin123"
    ));

    /**
     * 获取完整的弱密码列表（默认 + 自定义）
     */
    public Set<String> getFullWeakPasswords() {
        Set<String> allWeakPasswords = new HashSet<>(DEFAULT_WEAK_PASSWORDS);
        allWeakPasswords.addAll(customWeakPasswords);
        return allWeakPasswords;
    }

    /**
     * 配置参数校验
     */
    @PostConstruct
    public void validate() {
        validateRange("minLength", minLength, 1, 128, "最小密码长度");
        validateRange("maxLength", maxLength, minLength, 128, "最大密码长度");
        validateRange("maxSequentialChars", maxSequentialChars, 0, 10, "连续字符检测长度");
        validateRange("maxRepeatingChars", maxRepeatingChars, 0, 10, "重复字符检测长度");
        validateRange("historyCheckCount", historyCheckCount, 0, 20, "历史密码检查次数");

        log.debug("密码策略配置: minLength={}, maxLength={}, requireUppercase={}, requireLowercase={}, requireDigit={}, requireSpecialChar={}",
                minLength, maxLength, requireUppercase, requireLowercase, requireDigit, requireSpecialChar);
    }

    private void validateRange(String paramName, int value, int min, int max, String description) {
        if (value < min || value > max) {
            String message = String.format(
                    "配置参数错误：%s (%s) 的值为 %d，有效范围为 [%d, %d]",
                    paramName, description, value, min, max);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
    }
}