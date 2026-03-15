package cn.swiftadmin.schedule.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务类型枚举
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Getter
@AllArgsConstructor
public enum JobType {

    /** Bean 方式 */
    BEAN(1, "Bean");

    private final Integer code;
    private final String desc;

    public static JobType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
