package cn.swiftadmin.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码强度校验结果
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordStrengthResult {

    /** 是否通过校验 */
    private boolean valid;

    /** 密码强度等级：WEAK|MEDIUM|STRONG */
    private String level;

    /** 强度分数（0-100） */
    private int score;

    /** 错误提示信息 */
    private String message;

    /** 是否通过 */
    public static PasswordStrengthResult ok(String level, int score) {
        return PasswordStrengthResult.builder()
                .valid(true)
                .level(level)
                .score(score)
                .build();
    }

    /** 校验失败 */
    public static PasswordStrengthResult fail(String message) {
        return PasswordStrengthResult.builder()
                .valid(false)
                .level("WEAK")
                .score(0)
                .message(message)
                .build();
    }

}