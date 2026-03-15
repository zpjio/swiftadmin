package cn.swiftadmin.auth.api.entity;

import cn.swiftadmin.common.core.enums.LogStatus;
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
 * 登录日志实体
 * <p>记录用户登录行为，包括登录时间、IP 地址、登录结果等</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_login_log")
public class SysLoginLog {

    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    @Column
    private String username;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    /** 登录状态 */
    @Column
    private LogStatus status;

    @Column
    private String message;

    @Column
    private LocalDateTime loginTime;

}