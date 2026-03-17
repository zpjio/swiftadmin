package cn.swiftadmin.schedule.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** code -> JobExecuteStatus 映射缓存 */
    private static final Map<Integer, JobExecuteStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(JobExecuteStatus::getCode, Function.identity()));

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值，不存在返回 null
     */
    public static JobExecuteStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
