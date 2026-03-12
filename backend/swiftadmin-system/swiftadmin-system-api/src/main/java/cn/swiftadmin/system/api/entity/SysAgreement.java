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
 * 用户协议版本实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_agreement")
public class SysAgreement {

    /** 主键 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 协议类型：agreement=用户协议，privacy=隐私政策 */
    @Column
    private String type;

    /** 版本号 */
    @Column
    private String version;

    /** 协议标题 */
    @Column
    private String title;

    /** 协议内容 */
    @Column
    private String content;

    /** 状态：0=停用，1=启用 */
    @Column
    private Integer status;

    /** 生效时间 */
    @Column
    private LocalDateTime effectiveDate;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

    /** 创建人 ID */
    @Column
    private Long createBy;

    /** 更新人 ID */
    @Column
    private Long updateBy;

    /** 删除标记 */
    @Column(isLogicDelete = true)
    private Integer deleted;

}