package cn.swiftadmin.common.security.util;

import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.exception.PermissionDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全上下文工具类
 * <p>提供获取当前登录用户信息的静态方法</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户，未登录返回 null
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，未登录返回 null
     */
    public static Long getCurrentUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 获取当前用户 ID（带默认值）
     *
     * @param defaultValue 默认值
     * @return 用户 ID，未登录返回默认值
     */
    public static Long getCurrentUserId(Long defaultValue) {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : defaultValue;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录返回 null
     */
    public static String getCurrentUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    /**
     * 获取当前用户昵称
     *
     * @return 昵称，未登录返回 null
     */
    public static String getCurrentNickname() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getNickname() : null;
    }

    /**
     * 获取当前用户部门 ID
     *
     * @return 部门 ID，未登录返回 null
     */
    public static Long getCurrentDeptId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getDeptId() : null;
    }

    /**
     * 获取当前用户部门 ID（带默认值）
     *
     * @param defaultValue 默认值
     * @return 部门 ID，未登录返回默认值
     */
    public static Long getCurrentDeptId(Long defaultValue) {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getDeptId() : defaultValue;
    }

    /**
     * 获取当前用户角色列表
     *
     * @return 角色列表，未登录返回空列表
     */
    public static List<String> getCurrentRoles() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return Collections.emptyList();
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.substring(5))
                .collect(Collectors.toList());
    }

    /**
     * 获取当前用户权限列表
     *
     * @return 权限列表，未登录返回空列表
     */
    public static List<String> getCurrentPermissions() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return Collections.emptyList();
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(permission -> !permission.startsWith("ROLE_"))
                .collect(Collectors.toList());
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return true=已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser;
    }

    /**
     * 判断当前用户是否为超级管理员
     *
     * @return true=超级管理员
     */
    public static boolean isSuperAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.isSuperAdmin();
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param role 角色名称
     * @return true=拥有该角色
     */
    public static boolean hasRole(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        String rolePrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rolePrefix.equals(authority.getAuthority()));
    }

    /**
     * 判断当前用户是否拥有指定权限
     *
     * @param permission 权限名称
     * @return true=拥有该权限
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
    }

    /**
     * 判断当前用户是否拥有任一角色
     *
     * @param roles 角色名称数组
     * @return true=拥有任一角色
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> {
                    for (String role : roles) {
                        String rolePrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        if (rolePrefix.equals(authority)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * 判断当前用户是否拥有所有权限
     *
     * @param permissions 权限名称数组
     * @return true=拥有所有权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        List<String> userPermissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断当前用户是否拥有任一权限
     *
     * @param permissions 权限名称数组
     * @return true=拥有任一权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        List<String> userPermissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取认证信息
     *
     * @return 认证信息，未登录返回 null
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 清除安全上下文
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 数据权限相关 ====================

    /**
     * 获取当前用户的数据权限范围
     *
     * @return 数据权限范围（1=全部，2=自定义，3=本部门，4=本部门及以下，5=仅本人）
     */
    public static Integer getDataScope() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getDataScope() : null;
    }

    /**
     * 判断当前用户是否有全部数据权限
     *
     * @return true=有全部数据权限
     */
    public static boolean hasAllDataScope() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.hasAllDataScope();
    }

    /**
     * 获取当前用户的自定义部门ID列表
     *
     * @return 自定义部门ID列表
     */
    public static List<Long> getCustomDeptIds() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getCustomDeptIds() : Collections.emptyList();
    }

    // ==================== 权限断言方法（失败抛异常） ====================

    /**
     * 断言当前用户已登录
     *
     * @throws PermissionDeniedException 如果未登录
     */
    public static void requireLogin() {
        if (!isAuthenticated()) {
            throw new PermissionDeniedException("用户未登录");
        }
    }

    /**
     * 断言当前用户拥有指定权限
     *
     * @param permission 权限标识
     * @throws PermissionDeniedException 如果无权限
     */
    public static void requirePermission(String permission) {
        requireLogin();
        // 超级管理员拥有所有权限
        if (isSuperAdmin()) {
            return;
        }
        if (!hasPermission(permission)) {
            throw new PermissionDeniedException("权限不足，需要权限：" + permission);
        }
    }

    /**
     * 断言当前用户拥有任意一个权限
     *
     * @param permissions 权限标识数组
     * @throws PermissionDeniedException 如果无权限
     */
    public static void requireAnyPermission(String... permissions) {
        requireLogin();
        // 超级管理员拥有所有权限
        if (isSuperAdmin()) {
            return;
        }
        if (!hasAnyPermission(permissions)) {
            throw new PermissionDeniedException("权限不足，需要权限：" + Arrays.toString(permissions));
        }
    }

    /**
     * 断言当前用户拥有指定角色
     *
     * @param role 角色编码
     * @throws PermissionDeniedException 如果无角色
     */
    public static void requireRole(String role) {
        requireLogin();
        // 超级管理员拥有所有角色
        if (isSuperAdmin()) {
            return;
        }
        if (!hasRole(role)) {
            throw new PermissionDeniedException("角色不足，需要角色：" + role);
        }
    }

    /**
     * 断言当前用户是超级管理员
     *
     * @throws PermissionDeniedException 如果不是超级管理员
     */
    public static void requireSuperAdmin() {
        requireLogin();
        if (!isSuperAdmin()) {
            throw new PermissionDeniedException("需要超级管理员权限");
        }
    }

    // ==================== 超级管理员相关 ====================

    /**
     * 判断指定用户是否为超级管理员
     *
     * @param loginUser 登录用户
     * @return true=超级管理员
     */
    public static boolean isSuperAdmin(LoginUser loginUser) {
        if (loginUser == null) {
            return false;
        }
        // 优先使用 isSuperAdmin 标识
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        // 兼容角色判断
        Set<String> roles = loginUser.getRoles();
        return roles != null && roles.contains("admin");
    }

    /**
     * 判断指定用户ID是否为超级管理员
     * <p>需要配合数据库查询使用，此方法仅做简单判断</p>
     *
     * @param userId 用户ID
     * @return true=可能是超级管理员（需要进一步验证）
     */
    public static boolean isSuperAdmin(Long userId) {
        // 简单判断：ID为1的用户通常是超级管理员
        // 实际判断应该查询数据库的 is_super 字段
        return userId != null && userId == 1L;
    }
}