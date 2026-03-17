package cn.swiftadmin.schedule.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** code -> JobStatus 映射缓存 */
    private static final Map<Integer, JobStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(JobStatus::getCode, Function.identity()));

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值，不存在返回 null
     */
    public static JobStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
