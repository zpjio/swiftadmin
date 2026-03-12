package cn.swiftadmin.common.security.service;

import cn.swiftadmin.common.security.domain.LoginUser;

import java.util.List;
import java.util.Set;

/**
 * 权限缓存服务接口
 * <p>提供权限信息的缓存管理</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface PermissionCacheService {

    /**
     * 缓存用户权限信息
     *
     * @param userId 用户ID
     * @param loginUser 登录用户信息
     */
    void cacheUserPermission(Long userId, LoginUser loginUser);

    /**
     * 获取缓存的用户权限信息
     *
     * @param userId 用户ID
     * @return 登录用户信息，不存在返回null
     */
    LoginUser getCachedUserPermission(Long userId);

    /**
     * 获取用户权限标识集合
     *
     * @param userId 用户ID
     * @return 权限标识集合
     */
    Set<String> getUserPermissions(Long userId);

    /**
     * 获取用户角色标识集合
     *
     * @param userId 用户ID
     * @return 角色标识集合
     */
    Set<String> getUserRoles(Long userId);

    /**
     * 获取用户数据权限范围
     *
     * @param userId 用户ID
     * @return 数据权限范围
     */
    Integer getUserDataScope(Long userId);

    /**
     * 获取用户自定义部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> getUserCustomDeptIds(Long userId);

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    void clearUserPermissionCache(Long userId);

    /**
     * 清除所有用户权限缓存
     */
    void clearAllPermissionCache();

    /**
     * 刷新用户权限缓存
     *
     * @param userId 用户ID
     * @return 刷新后的登录用户信息
     */
    LoginUser refreshUserPermission(Long userId);

}