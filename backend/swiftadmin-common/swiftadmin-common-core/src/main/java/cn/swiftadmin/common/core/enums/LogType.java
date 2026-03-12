package cn.swiftadmin.common.core.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 日志类型枚举
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Getter
@AllArgsConstructor
public enum LogType {

    LOGIN("LOGIN", "登录日志"),
    OPERATION("OPERATION", "操作日志"),
    AUDIT("AUDIT", "审计日志");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    public static LogType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

}