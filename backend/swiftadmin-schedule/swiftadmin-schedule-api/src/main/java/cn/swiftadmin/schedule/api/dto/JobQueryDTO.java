package cn.swiftadmin.schedule.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 定时任务查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobQueryDTO extends PageQuery {

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** Bean名称 */
    private String beanName;

    /** 状态：0=正常，1=暂停 */
    private Integer status;

    /** 创建时间开始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeBegin;

    /** 创建时间结束 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;

}
