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
 * 权限包实体
 * <p>权限包用于打包一组菜单权限，方便快速分配给角色</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_permission_package")
public class SysPermissionPackage {

    /** 权限包 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 权限包名称 */
    @Column
    private String name;

    /** 权限包编码 */
    @Column
    private String code;

    /** 描述 */
    @Column
    private String description;

    /** 排序 */
    @Column
    private Integer sortOrder;

    /** 状态：1=正常，0=禁用 */
    @Column
    private Integer status;

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