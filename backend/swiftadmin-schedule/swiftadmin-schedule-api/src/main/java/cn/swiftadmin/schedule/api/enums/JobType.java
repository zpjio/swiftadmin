package cn.swiftadmin.schedule.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** code -> JobType 映射缓存 */
    private static final Map<Integer, JobType> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(JobType::getCode, Function.identity()));

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值，不存在返回 null
     */
    public static JobType fromCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
