package cn.swiftadmin.system.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限包菜单关联实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_permission_package_menu")
public class SysPermissionPackageMenu {

    /** ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 权限包 ID */
    @Column
    private Long packageId;

    /** 菜单 ID */
    @Column
    private Long menuId;

}