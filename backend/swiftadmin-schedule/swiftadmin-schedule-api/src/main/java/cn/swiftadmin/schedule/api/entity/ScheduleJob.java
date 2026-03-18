package cn.swiftadmin.schedule.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import cn.swiftadmin.schedule.api.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定时任务配置实体
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("schedule_job")
public class ScheduleJob {

    /** 任务ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 任务名称 */
    @Column
    private String jobName;

    /** 任务分组 */
    @Column
    private String jobGroup;

    /** Bean名称 */
    @Column
    private String beanName;

    /** 方法名称 */
    @Column
    private String methodName;

    /** 方法参数（JSON） */
    @Column
    private String methodParams;

    /** 任务类型：1=Bean，预留扩展分布式等类型 */
    @Column
    private Integer jobType;

    /** Cron表达式 */
    @Column
    private String cronExpression;

    /** 超时时间（秒） */
    @Column
    private Integer timeout;

    /** 重试次数 */
    @Column
    private Integer retryTimes;

    /** 重试间隔（秒） */
    @Column
    private Integer retryInterval;

    /** 状态：0=正常，1=暂停 */
    @Column
    private Integer status;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /** 创建人 */
    @Column
    private Long createBy;

    /** 更新人 */
    @Column
    private Long updateBy;

    /** 删除标识 */
    @Column(isLogicDelete = true)
    private Integer deleted;

    public JobType toJobType() {
        JobType type = JobType.fromCode(this.jobType);
        return type != null ? type : JobType.BEAN;
    }
}
