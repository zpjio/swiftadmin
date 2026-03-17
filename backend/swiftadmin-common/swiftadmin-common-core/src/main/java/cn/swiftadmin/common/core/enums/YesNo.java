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
 * 是否枚举
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Getter
@AllArgsConstructor
public enum YesNo {

    NO(0, "否"),
    YES(1, "是");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    /** code -> YesNo 映射缓存 */
    private static final Map<Integer, YesNo> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(YesNo::getCode, Function.identity()));

    public static YesNo fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public boolean isYes() {
        return this == YES;
    }

    public boolean isNo() {
        return this == NO;
    }

}