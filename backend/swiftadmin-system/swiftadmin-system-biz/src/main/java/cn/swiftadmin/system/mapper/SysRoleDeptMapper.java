package cn.swiftadmin.system.mapper;

import cn.swiftadmin.system.api.entity.SysRoleDept;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色部门关联 Mapper
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /**
     * 插入角色部门关联（联合主键表）
     */
    @Insert("INSERT INTO sys_role_dept (role_id, dept_id) VALUES (#{roleId}, #{deptId})")
    int insertRoleDept(@Param("roleId") Long roleId, @Param("deptId") Long deptId);

}