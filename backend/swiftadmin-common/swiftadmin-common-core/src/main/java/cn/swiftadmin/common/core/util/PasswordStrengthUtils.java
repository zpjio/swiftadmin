package cn.swiftadmin.common.core.util;

import cn.swiftadmin.common.core.result.PasswordStrengthResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 密码强度校验工具类
 * <p>校验规则：中等强度 -8 位以上，包含大小写字母 + 数字</p>
 * <p>使用预编译正则和缓存提升性能</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public final class PasswordStrengthUtils {

    private PasswordStrengthUtils() {
    }

    /** 最小密码长度 */
    private static final int MIN_LENGTH = 8;

    /** 密码强度等级 */
    public static final String LEVEL_WEAK = "WEAK";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_STRONG = "STRONG";

    /** 常见弱密码列表 */
    private static final Set<String> WEAK_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "master", "dragon", "letmein", "login",
            "password1", "welcome", "admin", "root", "test"
    ));

    /** 大写字母正则（预编译） */
    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");

    /** 小写字母正则（预编译） */
    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");

    /** 数字正则（预编译） */
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

    /** 特殊字符正则（预编译） */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    /** 连续字符检测缓存 */
    private static final ConcurrentMap<String, Boolean> SEQUENCE_CACHE = new ConcurrentHashMap<>();

    /** 重复字符检测缓存 */
    private static final ConcurrentMap<String, Boolean> REPEAT_CACHE = new ConcurrentHashMap<>();

    /**
     * 校验密码强度
     *
     * @param password 密码
     * @return 校验结果
     */
    public static PasswordStrengthResult validate(String password) {
        return validate(password, null);
    }

    /**
     * 校验密码强度
     *
     * @param password 密码
     * @param username 用户名（用于检查密码是否包含用户名）
     * @return 校验结果
     */
    public static PasswordStrengthResult validate(String password, String username) {
        if (Objects.isNull(password) || password.isEmpty()) {
            return PasswordStrengthResult.fail("密码不能为空");
        }

        if (password.length() < MIN_LENGTH) {
            return PasswordStrengthResult.fail("密码长度不能少于 " + MIN_LENGTH + " 位");
        }

        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            return PasswordStrengthResult.fail("密码过于简单，请使用更复杂的密码");
        }

        if (username != null && !username.isEmpty() && password.equalsIgnoreCase(username)) {
            return PasswordStrengthResult.fail("密码不能与用户名相同");
        }

        boolean hasUpper = UPPER_CASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWER_CASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();

        if (!hasUpper) {
            return PasswordStrengthResult.fail("密码必须包含大写字母");
        }
        if (!hasLower) {
            return PasswordStrengthResult.fail("密码必须包含小写字母");
        }
        if (!hasDigit) {
            return PasswordStrengthResult.fail("密码必须包含数字");
        }

        int score = calculateScore(password, hasUpper, hasLower, hasDigit, hasSpecial);
        String level = determineLevel(score);

        return PasswordStrengthResult.ok(level, score);
    }

    /**
     * 计算密码强度分数
     */
    private static int calculateScore(String password, boolean hasUpper, boolean hasLower,
                                       boolean hasDigit, boolean hasSpecial) {
        int score = 0;

        score += Math.min(password.length() * 4, 40);

        if (hasUpper) score += 10;
        if (hasLower) score += 10;
        if (hasDigit) score += 10;
        if (hasSpecial) score += 15;

        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 5;

        return Math.min(score, 100);
    }

    /**
     * 确定密码强度等级
     */
    private static String determineLevel(int score) {
        if (score >= 75) {
            return LEVEL_STRONG;
        } else if (score >= 50) {
            return LEVEL_MEDIUM;
        } else {
            return LEVEL_WEAK;
        }
    }

    /**
     * 快速校验密码是否满足最低要求
     *
     * @param password 密码
     * @return true=满足要求
     */
    public static boolean isValid(String password) {
        return validate(password).isValid();
    }

    /**
     * 快速校验密码是否满足最低要求
     *
     * @param password 密码
     * @param username 用户名
     * @return true=满足要求
     */
    public static boolean isValid(String password, String username) {
        return validate(password, username).isValid();
    }

    /**
     * 检查是否包含连续字符（带缓存）
     *
     * @param password 密码
     * @param minLength 最小连续长度
     * @return true=包含连续字符
     */
    public static boolean containsSequentialCharacters(String password, int minLength) {
        String cacheKey = password + "_" + minLength;
        return SEQUENCE_CACHE.computeIfAbsent(cacheKey, key -> {
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
        });
    }

    /**
     * 检查是否包含重复字符（带缓存）
     *
     * @param password 密码
     * @param minLength 最小重复长度
     * @return true=包含重复字符
     */
    public static boolean containsRepeatingCharacters(String password, int minLength) {
        String cacheKey = password + "_" + minLength;
        return REPEAT_CACHE.computeIfAbsent(cacheKey, key -> {
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
        });
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        SEQUENCE_CACHE.clear();
        REPEAT_CACHE.clear();
    }

    /**
     * 生成密码强度建议
     *
     * @param password 密码
     * @return 建议列表
     */
    public static java.util.List<String> generateSuggestions(String password) {
        java.util.List<String> suggestions = new java.util.ArrayList<>();

        if (Objects.isNull(password) || password.isEmpty()) {
            suggestions.add("密码不能为空");
            return suggestions;
        }

        if (password.length() < 8) {
            suggestions.add("密码长度至少 8 位");
        } else if (password.length() < 12) {
            suggestions.add("建议密码长度达到 12 位以上");
        }

        if (!UPPER_CASE_PATTERN.matcher(password).find()) {
            suggestions.add("添加大写字母");
        }
        if (!LOWER_CASE_PATTERN.matcher(password).find()) {
            suggestions.add("添加小写字母");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            suggestions.add("添加数字");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            suggestions.add("添加特殊字符（如 !@#$%^&*）");
        }

        if (containsSequentialCharacters(password, 3)) {
            suggestions.add("避免使用连续字符（如 abc、123）");
        }
        if (containsRepeatingCharacters(password, 3)) {
            suggestions.add("避免使用重复字符（如 aaa、111）");
        }

        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            suggestions.add("使用更复杂的密码，避免常见弱密码");
        }

        return suggestions;
    }
}