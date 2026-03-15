package cn.swiftadmin.schedule.api.vo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务日志 VO
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
public class JobLogVO {

    /** 日志 ID */
    private Long id;

    /** 任务 ID */
    private Long jobId;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** 执行状态：0=执行中，1=成功，2=失败 */
    private Integer executeStatus;

    /** 执行时间 */
    private LocalDateTime executeTime;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 执行结果 */
    private String executeResult;

    /** 错误信息 */
    private String errorMsg;

    /** 异常堆栈 */
    private String stackTrace;

    /** 重试次数 */
    private Integer retryCount;

    /** 执行类型 */
    private Integer executeType;

    /** 创建时间 */
    private LocalDateTime createTime;

}
