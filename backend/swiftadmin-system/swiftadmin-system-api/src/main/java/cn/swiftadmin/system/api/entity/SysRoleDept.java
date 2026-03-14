package cn.swiftadmin.system.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色部门关联实体（自定义数据权限）
 * <p>联合主键表，roleId 标记为 @Id 表示主键字段，deptId 使用 @Column</p>
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
    @Id
    private Long roleId;

    /** 部门 ID */
    @Column
    private Long deptId;

}