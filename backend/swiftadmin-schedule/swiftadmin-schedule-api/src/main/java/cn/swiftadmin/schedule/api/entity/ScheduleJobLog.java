package cn.swiftadmin.schedule.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志实体
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("schedule_job_log")
public class ScheduleJobLog {

    /** 日志ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 任务ID */
    @Column
    private Long jobId;

    /** 任务名称 */
    @Column
    private String jobName;

    /** 任务分组 */
    @Column
    private String jobGroup;

    /** 执行状态：0=执行中，1=成功，2=失败 */
    @Column
    private Integer executeStatus;

    /** 执行时间 */
    @Column
    private LocalDateTime executeTime;

    /** 耗时（毫秒） */
    @Column
    private Long costTime;

    /** 执行结果 */
    @Column
    private String executeResult;

    /** 错误信息 */
    @Column
    private String errorMsg;

    /** 异常堆栈 */
    @Column
    private String stackTrace;

    /** 重试次数 */
    @Column
    private Integer retryCount;

    /** 执行类型：1=自动执行，2=手动触发 */
    @Column
    private Integer executeType;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 删除标识 */
    @Column(isLogicDelete = true)
    private Integer deleted;
}
