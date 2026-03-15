package cn.swiftadmin.schedule.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务执行状态枚举
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Getter
@AllArgsConstructor
public enum JobExecuteStatus {

    /** 执行中 */
    RUNNING(0, "执行中"),

    /** 成功 */
    SUCCESS(1, "成功"),

    /** 失败 */
    FAILED(2, "失败");

    private final Integer code;
    private final String desc;

    public static JobExecuteStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobExecuteStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
