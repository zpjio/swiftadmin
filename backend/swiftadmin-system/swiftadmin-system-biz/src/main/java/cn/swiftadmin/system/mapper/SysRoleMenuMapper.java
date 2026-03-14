package cn.swiftadmin.system.mapper;

import cn.swiftadmin.system.api.entity.SysRoleMenu;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色菜单关联 Mapper
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 插入角色菜单关联（联合主键表）
     */
    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

}