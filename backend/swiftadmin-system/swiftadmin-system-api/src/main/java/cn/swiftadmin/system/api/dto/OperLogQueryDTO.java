package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQueryDTO extends PageQuery {

    /** 操作标题 */
    private String title;

    /** 操作人员 */
    private String operName;

    /** 业务类型 */
    private Integer businessType;

    /** 操作状态 */
    private Integer status;

    /** 开始时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    /** 结束时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

}