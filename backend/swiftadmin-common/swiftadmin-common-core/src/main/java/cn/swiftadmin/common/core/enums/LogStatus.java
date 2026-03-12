package cn.swiftadmin.common.core.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 日志状态枚举
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Getter
@AllArgsConstructor
public enum LogStatus {

    FAILURE(0, "失败"),
    SUCCESS(1, "成功");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    public static LogStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

}