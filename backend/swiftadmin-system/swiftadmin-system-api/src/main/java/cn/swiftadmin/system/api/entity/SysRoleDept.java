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
 * 角色部门关联实体（自定义数据权限）
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_role_dept")
public class SysRoleDept {

    /** 角色 ID */
    @Id(keyType = KeyType.Auto)
    @Column
    private Long roleId;

    /** 部门 ID */
    @Id(keyType = KeyType.Auto)
    @Column
    private Long deptId;

}