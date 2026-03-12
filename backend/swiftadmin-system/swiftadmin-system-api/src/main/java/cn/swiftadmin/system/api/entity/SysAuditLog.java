package cn.swiftadmin.system.api.entity;

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
 * 审计日志实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_audit_log")
public class SysAuditLog {

    /** 日志ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 模块名称 */
    @Column
    private String module;

    /** 操作类型（INSERT/UPDATE/DELETE） */
    @Column
    private String operation;

    /** 表名 */
    @Column
    private String tableName;

    /** 记录ID */
    @Column
    private String recordId;

    /** 记录名称/描述 */
    @Column
    private String recordName;

    /** 变更前数据（JSON） */
    @Column
    private String beforeData;

    /** 变更后数据（JSON） */
    @Column
    private String afterData;

    /** 变更字段详情（JSON） */
    @Column
    private String changedFields;

    /** 操作用户ID */
    @Column
    private Long operUserId;

    /** 操作用户名 */
    @Column
    private String operUserName;

    /** 操作IP */
    @Column
    private String operIp;

    /** 操作时间 */
    @Column
    private LocalDateTime operTime;

}