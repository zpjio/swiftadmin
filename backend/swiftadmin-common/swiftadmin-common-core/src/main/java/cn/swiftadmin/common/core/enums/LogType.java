package cn.swiftadmin.common.core.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** code -> LogType 映射缓存 */
    private static final Map<String, LogType> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(LogType::getCode, Function.identity()));

    public static LogType fromCode(String code) {
        return code != null ? CODE_MAP.get(code.toUpperCase()) : null;
    }

}