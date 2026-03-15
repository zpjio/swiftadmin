package cn.swiftadmin.system.api.entity;

import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.enums.MenuType;
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
 * 菜单实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_menu")
public class SysMenu {

    /** 菜单 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 父菜单 ID */
    @Column
    private Long parentId;

    /** 菜单名称 */
    @Column
    private String name;

    /** 路由路径 */
    @Column
    private String path;

    /** 组件路径 */
    @Column
    private String component;

    /** 类型 */
    @Column
    private MenuType type;

    /** 图标 */
    @Column
    private String icon;

    /** 排序 */
    @Column
    private Integer sortOrder;

    /** 权限标识 */
    @Column
    private String permission;

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