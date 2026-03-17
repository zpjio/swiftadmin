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
 * 通用状态枚举
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Getter
@AllArgsConstructor
public enum CommonStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    /** code -> CommonStatus 映射缓存 */
    private static final Map<Integer, CommonStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(CommonStatus::getCode, Function.identity()));

    public static CommonStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }

}