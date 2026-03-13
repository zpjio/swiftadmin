package cn.swiftadmin.system.api.service;

import java.util.List;

/**
 * 权限缓存刷新服务接口
 * <p>处理权限变更时的缓存失效逻辑</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface PermissionCacheRefreshService {

    /**
     * 清除指定角色的所有用户权限缓存
     *
     * @param roleId 角色ID
     */
    void clearCacheByRoleId(Long roleId);

    /**
     * 清除多个角色的所有用户权限缓存
     *
     * @param roleIds 角色ID列表
     */
    void clearCacheByRoleIds(List<Long> roleIds);

    /**
     * 清除指定菜单关联的所有用户权限缓存
     *
     * @param menuId 菜单ID
     */
    void clearCacheByMenuId(Long menuId);

    /**
     * 清除多个菜单关联的所有用户权限缓存
     *
     * @param menuIds 菜单ID列表
     */
    void clearCacheByMenuIds(List<Long> menuIds);

    /**
     * 清除指定用户的权限缓存
     *
     * @param userId 用户ID
     */
    void clearCacheByUserId(Long userId);

    /**
     * 清除指定部门下所有用户的权限缓存
     *
     * @param deptId 部门ID
     */
    void clearCacheByDeptId(Long deptId);

    /**
     * 清除所有用户权限缓存
     */
    void clearAllCache();
}