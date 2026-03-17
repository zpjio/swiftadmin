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

    /** code -> LogStatus 映射缓存 */
    private static final Map<Integer, LogStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(LogStatus::getCode, Function.identity()));

    public static LogStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

}