package cn.swiftadmin.schedule.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 定时任务创建/更新 DTO
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
public class JobDTO {

    private Long id;

    @NotBlank(message = "任务名称不能为空", groups = {Create.class})
    @Size(max = 100, message = "任务名称长度不能超过 100 个字符")
    private String jobName;

    @NotBlank(message = "任务分组不能为空", groups = {Create.class})
    @Size(max = 50, message = "任务分组长度不能超过 50 个字符")
    private String jobGroup;

    @NotBlank(message = "Bean名称不能为空", groups = {Create.class})
    @Size(max = 200, message = "Bean名称长度不能超过 200 个字符")
    private String beanName;

    @NotBlank(message = "方法名称不能为空", groups = {Create.class})
    @Size(max = 100, message = "方法名称长度不能超过 100 个字符")
    private String methodName;

    private String methodParams;

    @NotBlank(message = "Cron表达式不能为空", groups = {Create.class})
    @Size(max = 100, message = "Cron表达式长度不能超过 100 个字符")
    private String cronExpression;

    @NotNull(message = "超时时间不能为空", groups = {Create.class})
    private Integer timeout;

    @NotNull(message = "重试次数不能为空", groups = {Create.class})
    private Integer retryTimes;

    @NotNull(message = "重试间隔不能为空", groups = {Create.class})
    private Integer retryInterval;

    @Min(value = 0, message = "状态必须为 0 或 1")
    @Max(value = 1, message = "状态必须为 0 或 1")
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    public interface Create {}
    public interface Update {}
}
