package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 审计日志查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQueryDTO extends PageQuery {

    /** 业务模块 */
    private String module;

    /** 操作类型 */
    private String operation;

    /** 操作用户名 */
    private String operUserName;

    /** 表名 */
    private String tableName;

    /** 开始时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    /** 结束时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

}