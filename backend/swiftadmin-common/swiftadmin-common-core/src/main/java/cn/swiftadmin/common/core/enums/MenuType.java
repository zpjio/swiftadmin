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
 * 菜单类型枚举
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Getter
@AllArgsConstructor
public enum MenuType {

    DIRECTORY(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    /** code -> MenuType 映射缓存 */
    private static final Map<Integer, MenuType> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(MenuType::getCode, Function.identity()));

    public static MenuType fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

}