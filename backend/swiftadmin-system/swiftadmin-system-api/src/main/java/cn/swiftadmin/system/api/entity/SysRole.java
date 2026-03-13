package cn.swiftadmin.system.api.entity;

import cn.swiftadmin.common.core.enums.CommonStatus;
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
 * 角色实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_role")
public class SysRole {

    /** 角色 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 角色名称 */
    @Column
    private String name;

    /** 角色编码 */
    @Column
    private String code;

    /** 描述 */
    @Column
    private String description;

    /** 数据权限范围：1=全部，2=自定义，3=本部门，4=本部门及以下，5=仅本人 */
    @Column
    private Integer dataScope;

    /** 状态 */
    @Column
    private CommonStatus status;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

    /** 是否删除：0=未删除，1=已删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;

}