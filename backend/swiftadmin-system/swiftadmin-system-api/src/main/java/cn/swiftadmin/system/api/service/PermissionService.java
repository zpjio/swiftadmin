package cn.swiftadmin.system.api.service;

import cn.swiftadmin.common.security.domain.LoginUser;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 * <p>提供用户权限和角色查询功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface PermissionService {

    // ==================== 数据权限范围常量 ====================

    /** 全部数据权限 */
    int DATA_SCOPE_ALL = 1;

    /** 自定义数据权限 */
    int DATA_SCOPE_CUSTOM = 2;

    /** 本部门数据权限 */
    int DATA_SCOPE_DEPT = 3;

    /** 本部门及以下数据权限 */
    int DATA_SCOPE_DEPT_AND_CHILD = 4;

    /** 仅本人数据权限 */
    int DATA_SCOPE_SELF = 5;

    // ==================== 权限查询方法 ====================

    /**
     * 根据用户 ID 获取权限标识集合
     *
     * @param userId 用户 ID
     * @return 权限标识集合
     */
    Set<String> getPermissionsByUserId(Long userId);

    /**
     * 根据用户 ID 获取角色标识集合
     *
     * @param userId 用户 ID
     * @return 角色标识集合
     */
    Set<String> getRolesByUserId(Long userId);

    /**
     * 获取用户的数据权限范围
     *
     * @param userId 用户 ID
     * @return 数据权限范围（取用户角色中最大的权限范围）
     */
    Integer getDataScopeByUserId(Long userId);

    /**
     * 获取用户的自定义部门ID列表
     *
     * @param userId 用户 ID
     * @return 部门ID列表
     */
    List<Long> getCustomDeptIdsByUserId(Long userId);

    /**
     * 构建登录用户权限信息
     *
     * @param userId 用户ID
     * @return 登录用户权限信息
     */
    LoginUser buildLoginUserPermission(Long userId);

    // ==================== 权限校验方法 ====================

    /**
     * 校验用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permission 权限标识
     * @return true=拥有
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * 校验用户是否拥有任意一个权限
     *
     * @param userId 用户ID
     * @param permissions 权限标识数组
     * @return true=拥有任意一个
     */
    boolean hasAnyPermission(Long userId, String... permissions);

    /**
     * 校验用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param role 角色编码
     * @return true=拥有
     */
    boolean hasRole(Long userId, String role);

    /**
     * 校验用户是否拥有任意一个角色
     *
     * @param userId 用户ID
     * @param roles 角色编码数组
     * @return true=拥有任意一个
     */
    boolean hasAnyRole(Long userId, String... roles);

    /**
     * 判断用户是否为超级管理员
     *
     * @param userId 用户ID
     * @return true=超级管理员
     */
    boolean isSuperAdmin(Long userId);

}