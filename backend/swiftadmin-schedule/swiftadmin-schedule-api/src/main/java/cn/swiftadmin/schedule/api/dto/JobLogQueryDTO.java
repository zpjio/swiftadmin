package cn.swiftadmin.schedule.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 定时任务日志查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobLogQueryDTO extends PageQuery {

    /** 任务ID */
    private Long jobId;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** 执行状态：0=执行中，1=成功，2=失败 */
    private Integer executeStatus;

    /** 执行类型：1=自动执行，2=手动触发 */
    private Integer executeType;

    /** 执行时间开始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executeTimeBegin;

    /** 执行时间结束 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executeTimeEnd;

}
