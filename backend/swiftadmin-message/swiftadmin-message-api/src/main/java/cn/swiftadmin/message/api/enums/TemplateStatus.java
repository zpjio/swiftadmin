package cn.swiftadmin.message.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 模板状态枚举
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Getter
@RequiredArgsConstructor
public enum TemplateStatus {

    /** 禁用 */
    DISABLED(0, "禁用"),

    /** 启用 */
    ENABLED(1, "启用"),

    /** 待审核 */
    PENDING(2, "待审核");

    @JsonValue
    private final Integer code;

    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static TemplateStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TemplateStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}