package cn.swiftadmin.schedule.api.vo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务 VO
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
public class JobVO {

    /** 任务 ID */
    private Long id;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** Bean名称 */
    private String beanName;

    /** 方法名称 */
    private String methodName;

    /** 方法参数（JSON） */
    private String methodParams;

    /** Cron表达式 */
    private String cronExpression;

    /** 超时时间（秒） */
    private Integer timeout;

    /** 重试次数 */
    private Integer retryTimes;

    /** 重试间隔（秒） */
    private Integer retryInterval;

    /** 状态：0=正常，1=暂停 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 下次执行时间 */
    private LocalDateTime nextExecuteTime;

}
