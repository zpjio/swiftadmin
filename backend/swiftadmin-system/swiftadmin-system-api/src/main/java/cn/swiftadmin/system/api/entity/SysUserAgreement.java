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
 * 用户协议同意记录实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_user_agreement")
public class SysUserAgreement {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 用户 ID */
    @Column
    private Long userId;

    /** 协议版本 ID */
    @Column
    private Long agreementId;

    /** 协议类型 */
    @Column
    private String agreementType;

    /** 同意的版本号 */
    @Column
    private String agreedVersion;

    /** 同意时间 */
    @Column
    private LocalDateTime agreedTime;

    /** 同意时的 IP 地址 */
    @Column
    private String ipAddress;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

}