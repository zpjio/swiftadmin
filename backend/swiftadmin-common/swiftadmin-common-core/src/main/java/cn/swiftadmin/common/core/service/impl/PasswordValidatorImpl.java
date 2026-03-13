package cn.swiftadmin.common.core.service.impl;

import cn.swiftadmin.common.core.config.PasswordProperties;
import cn.swiftadmin.common.core.result.PasswordStrengthResult;
import cn.swiftadmin.common.core.service.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 密码验证服务实现
 * <p>基于可配置的密码策略进行密码复杂度校验</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordValidatorImpl implements PasswordValidator {

    private final PasswordProperties properties;

    /** 预编译正则表达式 */
    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    /** 密码强度等级 */
    public static final String LEVEL_WEAK = "WEAK";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_STRONG = "STRONG";

    @Override
    public PasswordStrengthResult validate(String password) {
        return validate(password, null);
    }

    @Override
    public PasswordStrengthResult validate(String password, String username) {
        return validate(password, username, (String[]) null);
    }

    @Override
    public PasswordStrengthResult validate(String password, String username, String... historyPasswords) {
        // 1. 非空检查
        if (!StringUtils.hasLength(password)) {
            return PasswordStrengthResult.fail("密码不能为空");
        }

        // 2. 长度检查
        if (password.length() < properties.getMinLength()) {
            return PasswordStrengthResult.fail("密码长度不能少于 " + properties.getMinLength() + " 位");
        }

        if (password.length() > properties.getMaxLength()) {
            return PasswordStrengthResult.fail("密码长度不能超过 " + properties.getMaxLength() + " 位");
        }

        // 3. 弱密码检查
        if (properties.isEnableWeakPasswordCheck()) {
            Set<String> weakPasswords = properties.getFullWeakPasswords();
            if (weakPasswords.contains(password.toLowerCase())) {
                return PasswordStrengthResult.fail("密码过于简单，请使用更复杂的密码");
            }
        }

        // 4. 复杂度检查
        boolean hasUpper = UPPER_CASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWER_CASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();

        if (properties.isRequireUppercase() && !hasUpper) {
            return PasswordStrengthResult.fail("密码必须包含大写字母");
        }

        if (properties.isRequireLowercase() && !hasLower) {
            return PasswordStrengthResult.fail("密码必须包含小写字母");
        }

        if (properties.isRequireDigit() && !hasDigit) {
            return PasswordStrengthResult.fail("密码必须包含数字");
        }

        if (properties.isRequireSpecialChar() && !hasSpecial) {
            return PasswordStrengthResult.fail("密码必须包含特殊字符");
        }

        // 5. 用户名检查
        if (properties.isForbidContainUsername() && StringUtils.hasText(username)) {
            if (password.toLowerCase().contains(username.toLowerCase())) {
                return PasswordStrengthResult.fail("密码不能包含用户名");
            }
        }

        // 6. 连续字符检查
        int maxSequential = properties.getMaxSequentialChars();
        if (maxSequential > 0 && containsSequentialCharacters(password, maxSequential)) {
            return PasswordStrengthResult.fail("密码不能包含 " + maxSequential + " 位以上连续字符");
        }

        // 7. 重复字符检查
        int maxRepeating = properties.getMaxRepeatingChars();
        if (maxRepeating > 0 && containsRepeatingCharacters(password, maxRepeating)) {
            return PasswordStrengthResult.fail("密码不能包含 " + maxRepeating + " 位以上重复字符");
        }

        // 8. 历史密码检查
        if (historyPasswords != null && historyPasswords.length > 0) {
            int checkCount = Math.min(historyPasswords.length, properties.getHistoryCheckCount());
            for (int i = 0; i < checkCount; i++) {
                if (historyPasswords[i] != null && password.equals(historyPasswords[i])) {
                    return PasswordStrengthResult.fail("不能使用最近 " + checkCount + " 次内使用过的密码");
                }
            }
        }

        // 计算强度分数和等级
        int score = calculateScore(password, hasUpper, hasLower, hasDigit, hasSpecial);
        String level = determineLevel(score);

        return PasswordStrengthResult.ok(level, score);
    }

    @Override
    public boolean isValid(String password) {
        return validate(password).isValid();
    }

    @Override
    public boolean isValid(String password, String username) {
        return validate(password, username).isValid();
    }

    @Override
    public int calculateStrengthScore(String password) {
        if (!StringUtils.hasLength(password)) {
            return 0;
        }

        boolean hasUpper = UPPER_CASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWER_CASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();

        return calculateScore(password, hasUpper, hasLower, hasDigit, hasSpecial);
    }

    @Override
    public String getStrengthLevel(String password) {
        return determineLevel(calculateStrengthScore(password));
    }

    /**
     * 计算密码强度分数
     */
    private int calculateScore(String password, boolean hasUpper, boolean hasLower,
                               boolean hasDigit, boolean hasSpecial) {
        int score = 0;

        // 长度加分
        score += Math.min(password.length() * 4, 40);

        // 字符类型加分
        if (hasUpper) score += 10;
        if (hasLower) score += 10;
        if (hasDigit) score += 10;
        if (hasSpecial) score += 15;

        // 长度额外加分
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 5;

        // 复杂度奖励
        int typeCount = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        if (typeCount >= 3) score += 10;

        return Math.min(score, 100);
    }

    /**
     * 确定密码强度等级
     */
    private String determineLevel(int score) {
        if (score >= 75) {
            return LEVEL_STRONG;
        } else if (score >= 50) {
            return LEVEL_MEDIUM;
        } else {
            return LEVEL_WEAK;
        }
    }

    /**
     * 检查是否包含连续字符
     */
    private boolean containsSequentialCharacters(String password, int minLength) {
        if (password.length() < minLength) {
            return false;
        }

        char[] chars = password.toLowerCase().toCharArray();
        int sequentialCount = 1;

        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == chars[i - 1] + 1) {
                sequentialCount++;
                if (sequentialCount >= minLength) {
                    return true;
                }
            } else {
                sequentialCount = 1;
            }
        }

        return false;
    }

    /**
     * 检查是否包含重复字符
     */
    private boolean containsRepeatingCharacters(String password, int minLength) {
        if (password.length() < minLength) {
            return false;
        }

        char[] chars = password.toCharArray();
        int repeatCount = 1;

        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == chars[i - 1]) {
                repeatCount++;
                if (repeatCount >= minLength) {
                    return true;
                }
            } else {
                repeatCount = 1;
            }
        }

        return false;
    }
}