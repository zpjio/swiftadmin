package cn.swiftadmin.system.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色菜单关联实体
 * <p>联合主键表，roleId 标记为 @Id 表示主键字段，menuId 使用 @Column</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_role_menu")
public class SysRoleMenu {

    /** 角色 ID */
    @Id
    private Long roleId;

    /** 菜单 ID */
    @Column
    private Long menuId;

}