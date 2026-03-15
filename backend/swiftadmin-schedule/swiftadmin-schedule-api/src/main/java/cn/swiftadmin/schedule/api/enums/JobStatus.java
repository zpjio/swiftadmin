package cn.swiftadmin.schedule.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Getter
@AllArgsConstructor
public enum JobStatus {

    /** 正常 */
    NORMAL(0, "正常"),

    /** 暂停 */
    PAUSED(1, "暂停");

    private final Integer code;
    private final String desc;

    public static JobStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
