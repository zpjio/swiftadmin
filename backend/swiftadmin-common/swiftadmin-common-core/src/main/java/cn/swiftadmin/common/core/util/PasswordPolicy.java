package cn.swiftadmin.common.core.util;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;

import java.util.regex.Pattern;

/**
 * 密码策略工具类
 * <p>实现密码强度校验、历史密码检查等安全策略</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public final class PasswordPolicy {

    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    /** 最小密码长度 */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** 最大密码长度 */
    private static final int MAX_PASSWORD_LENGTH = 32;

    /** 默认检查最近 5 次历史密码 */
    private static final int DEFAULT_HISTORY_CHECK_COUNT = 5;

    private PasswordPolicy() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @param username 用户名（用于检查密码是否包含用户名）
     * @throws BusinessException 密码强度不足时抛出
     */
    public static void validatePassword(String password, String username) {
        validatePasswordStrength(password, username);
    }

    /**
     * 验证密码强度（完整版）
     *
     * @param password 密码
     * @param username 用户名
     * @param historyPasswords 历史密码数组（可选）
     * @throws BusinessException 密码强度不足时抛出
     */
    public static void validatePassword(String password, String username, String... historyPasswords) {
        validatePasswordStrength(password, username);
        validateHistoryPassword(password, historyPasswords);
    }

    /**
     * 验证密码强度
     */
    private static void validatePasswordStrength(String password, String username) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK, "密码不能为空");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码长度至少为 " + MIN_PASSWORD_LENGTH + " 位");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码长度不能超过 " + MAX_PASSWORD_LENGTH + " 位");
        }

        if (!UPPER_CASE_PATTERN.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码必须包含至少一个大写字母");
        }

        if (!LOWER_CASE_PATTERN.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码必须包含至少一个小写字母");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码必须包含至少一个数字");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码必须包含至少一个特殊字符（如 !@#$%^&*）");
        }

        if (username != null && !username.isEmpty()) {
            String lowerPassword = password.toLowerCase();
            String lowerUsername = username.toLowerCase();
            if (lowerPassword.contains(lowerUsername)) {
                throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                        "密码不能包含用户名");
            }
        }

        if (containsSequentialCharacters(password, 3)) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码不能包含 3 位以上连续字符（如 abc、123）");
        }

        if (containsRepeatingCharacters(password, 3)) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                    "密码不能包含 3 位以上重复字符（如 aaa、111）");
        }
    }

    /**
     * 验证历史密码
     */
    private static void validateHistoryPassword(String password, String... historyPasswords) {
        if (historyPasswords == null || historyPasswords.length == 0) {
            return;
        }

        int checkCount = Math.min(historyPasswords.length, DEFAULT_HISTORY_CHECK_COUNT);
        for (int i = 0; i < checkCount; i++) {
            if (historyPasswords[i] != null && password.equals(historyPasswords[i])) {
                throw new BusinessException(ErrorCode.PASSWORD_WEAK,
                        "不能使用最近 " + checkCount + " 次内使用过的密码");
            }
        }
    }

    /**
     * 检查是否包含连续字符
     */
    private static boolean containsSequentialCharacters(String password, int minLength) {
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
    private static boolean containsRepeatingCharacters(String password, int minLength) {
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

    /**
     * 计算密码强度分数
     */
    public static int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        if (password.length() >= 8) score += 10;
        if (password.length() >= 10) score += 10;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        if (UPPER_CASE_PATTERN.matcher(password).find()) score += 10;
        if (LOWER_CASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 10;

        if (!containsSequentialCharacters(password, 3)) score += 10;
        if (!containsRepeatingCharacters(password, 3)) score += 10;

        return Math.min(score, 100);
    }

    /**
     * 获取密码强度等级
     */
    public static String getPasswordStrengthLevel(String password) {
        int score = calculatePasswordStrength(password);

        if (score >= 90) {
            return "very-strong";
        } else if (score >= 70) {
            return "strong";
        } else if (score >= 50) {
            return "medium";
        } else {
            return "weak";
        }
    }

    /**
     * 生成符合密码策略的随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < MIN_PASSWORD_LENGTH) {
            length = MIN_PASSWORD_LENGTH;
        }

        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        String allChars = upper + lower + digits + special;
        java.security.SecureRandom random = new java.security.SecureRandom();

        StringBuilder password = new StringBuilder(length);

        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        char[] passwordChars = password.toString().toCharArray();
        for (int i = 0; i < passwordChars.length; i++) {
            int j = random.nextInt(passwordChars.length);
            char temp = passwordChars[i];
            passwordChars[i] = passwordChars[j];
            passwordChars[j] = temp;
        }

        return new String(passwordChars);
    }
}