package cn.swiftadmin.common.security.service;

import cn.swiftadmin.common.security.domain.LoginUser;

import java.util.List;

/**
 * 权限预热服务接口
 * <p>在用户登录后预热常用权限数据，减少首次请求延迟</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface PermissionWarmupService {

    /**
     * 预热用户权限数据
     * <p>在登录成功后调用，预加载用户权限、角色、菜单等数据到缓存</p>
     *
     * @param userId 用户ID
     */
    void warmupUserPermission(Long userId);

    /**
     * 预热用户菜单数据
     *
     * @param userId 用户ID
     */
    void warmupUserMenus(Long userId);

    /**
     * 批量预热用户权限数据
     *
     * @param userIds 用户ID列表
     */
    void warmupUserPermissions(List<Long> userIds);

}