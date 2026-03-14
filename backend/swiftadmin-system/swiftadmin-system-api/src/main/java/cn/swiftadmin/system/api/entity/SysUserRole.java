package cn.swiftadmin.system.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色关联实体
 * <p>联合主键表，userId 标记为 @Id 表示主键字段，roleId 使用 @Column</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_user_role")
public class SysUserRole {

    /** 用户 ID */
    @Id
    private Long userId;

    /** 角色 ID */
    @Column
    private Long roleId;

}