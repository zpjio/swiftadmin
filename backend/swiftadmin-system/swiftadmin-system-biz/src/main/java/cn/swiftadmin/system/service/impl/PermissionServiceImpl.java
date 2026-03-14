package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.PermissionCacheService;
import cn.swiftadmin.system.api.entity.SysDept;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.DeptService;
import cn.swiftadmin.system.api.service.PermissionService;
import cn.swiftadmin.system.api.service.UserService;
import cn.swiftadmin.system.mapper.SysMenuMapper;
import cn.swiftadmin.system.mapper.SysRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static cn.swiftadmin.system.api.entity.table.SysMenuTableDef.SYS_MENU;
import static cn.swiftadmin.system.api.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static cn.swiftadmin.system.api.entity.table.SysRoleTableDef.SYS_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysRoleDeptTableDef.SYS_ROLE_DEPT;
import static com.mybatisflex.core.query.QueryMethods.distinct;

/**
 * 权限服务实现类
 * <p>使用 MyBatis-Flex QueryWrapper 进行类型安全的数据库查询</p>
 * <p>集成 Redis 缓存提高性能</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final UserService userService;
    private final DeptService deptService;
    private final PermissionCacheService permissionCacheService;

    @Override
    public Set<String> getPermissionsByUserId(Long userId) {
        // 获取用户直接关联的角色ID（只查询启用状态的角色）
        QueryWrapper roleQuery = QueryWrapper.create()
                .select(SYS_USER_ROLE.ROLE_ID)
                .from(SYS_USER_ROLE)
                .innerJoin(SYS_ROLE).on(SYS_USER_ROLE.ROLE_ID.eq(SYS_ROLE.ID))
                .where(SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SYS_ROLE.STATUS.eq(CommonStatus.ENABLED));

        List<Long> roleIds = roleMapper.selectListByQueryAs(roleQuery, Long.class);

        if (CollectionUtils.isEmpty(roleIds)) {
            return new HashSet<>();
        }

        // 查询这些角色关联的所有菜单权限
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(distinct(SYS_MENU.PERMISSION))
                .from(SYS_MENU)
                .innerJoin(SYS_ROLE_MENU).on(SYS_MENU.ID.eq(SYS_ROLE_MENU.MENU_ID))
                .where(SYS_ROLE_MENU.ROLE_ID.in(roleIds))
                .and(SYS_MENU.PERMISSION.isNotNull())
                .and(SYS_MENU.PERMISSION.ne(""));

        List<String> permissions = menuMapper.selectListByQueryAs(queryWrapper, String.class);
        return new HashSet<>(permissions);
    }

    @Override
    public Set<String> getRolesByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(distinct(SYS_ROLE.CODE))
                .from(SYS_ROLE)
                .innerJoin(SYS_USER_ROLE).on(SYS_ROLE.ID.eq(SYS_USER_ROLE.ROLE_ID))
                .where(SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SYS_ROLE.STATUS.eq(CommonStatus.ENABLED));

        List<String> roles = roleMapper.selectListByQueryAs(queryWrapper, String.class);
        return new HashSet<>(roles);
    }

    @Override
    public Integer getDataScopeByUserId(Long userId) {
        log.info("=== getDataScopeByUserId called, userId={} ===", userId);

        // 获取用户所有角色的数据权限范围，取最大范围（数值最小）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_ROLE.DATA_SCOPE)
                .from(SYS_ROLE)
                .innerJoin(SYS_USER_ROLE).on(SYS_ROLE.ID.eq(SYS_USER_ROLE.ROLE_ID))
                .where(SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SYS_ROLE.STATUS.eq(1))
                .orderBy(SYS_ROLE.DATA_SCOPE.asc());

        log.info("执行查询: {}", queryWrapper.toSQL());
        List<Integer> dataScopes = roleMapper.selectListByQueryAs(queryWrapper, Integer.class);
        log.info("查询结果 dataScopes: {}", dataScopes);

        if (CollectionUtils.isEmpty(dataScopes)) {
            log.info("dataScopes 为空，返回默认值 DATA_SCOPE_SELF={}", DATA_SCOPE_SELF);
            return DATA_SCOPE_SELF; // 默认仅本人
        }

        // 返回最小的dataScope（权限范围最大）
        Integer result = dataScopes.get(0);
        log.info("返回 dataScope: {}", result);
        return result;
    }

    @Override
    public List<Long> getCustomDeptIdsByUserId(Long userId) {
        // 查询用户角色中dataScope=2的自定义部门
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(distinct(SYS_ROLE_DEPT.DEPT_ID))
                .from(SYS_ROLE_DEPT)
                .innerJoin(SYS_USER_ROLE).on(SYS_ROLE_DEPT.ROLE_ID.eq(SYS_USER_ROLE.ROLE_ID))
                .innerJoin(SYS_ROLE).on(SYS_USER_ROLE.ROLE_ID.eq(SYS_ROLE.ID))
                .where(SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SYS_ROLE.DATA_SCOPE.eq(DATA_SCOPE_CUSTOM))
                .and(SYS_ROLE.STATUS.eq(CommonStatus.ENABLED));

        return roleMapper.selectListByQueryAs(queryWrapper, Long.class);
    }

    @Override
    public LoginUser buildLoginUserPermission(Long userId) {
        // 先从缓存获取
        LoginUser cachedUser = permissionCacheService.getCachedUserPermission(userId);
        if (cachedUser != null) {
            log.debug("从缓存获取用户权限信息: userId={}", userId);
            return cachedUser;
        }

        // 缓存不存在，从数据库构建
        SysUser user = userService.getById(userId);
        if (user == null) {
            return null;
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setStatus(user.getStatus() != null ? user.getStatus().getCode() : null);

        // 查询部门名称和祖级路径
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                loginUser.setDeptName(dept.getName());
                loginUser.setDeptAncestors(dept.getAncestors());
            }
        }

        // 超级管理员标识
        loginUser.setSuperAdmin(user.getIsSuper() != null && user.getIsSuper().isYes());

        // 权限和角色
        Set<String> permissions = getPermissionsByUserId(userId);
        Set<String> roles = getRolesByUserId(userId);
        loginUser.setPermissions(permissions);
        loginUser.setRoles(roles);

        // 数据权限
        if (loginUser.isSuperAdmin()) {
            loginUser.setDataScope(DATA_SCOPE_ALL);
        } else {
            loginUser.setDataScope(getDataScopeByUserId(userId));
            if (loginUser.getDataScope() != null && loginUser.getDataScope() == DATA_SCOPE_CUSTOM) {
                loginUser.setCustomDeptIds(getCustomDeptIdsByUserId(userId));
            }
        }

        // 缓存用户权限信息
        permissionCacheService.cacheUserPermission(userId, loginUser);
        log.debug("构建并缓存用户权限信息: userId={}", userId);

        return loginUser;
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        Set<String> permissions = getPermissionsByUserId(userId);
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> userPermissions = getPermissionsByUserId(userId);
        if (CollectionUtils.isEmpty(userPermissions)) {
            return false;
        }
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(Long userId, String role) {
        Set<String> roles = getRolesByUserId(userId);
        return roles != null && roles.contains(role);
    }

    @Override
    public boolean hasAnyRole(Long userId, String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        Set<String> userRoles = getRolesByUserId(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            return false;
        }
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSuperAdmin(Long userId) {
        SysUser user = userService.getById(userId);
        return user != null && user.getIsSuper() != null && user.getIsSuper().isYes();
    }

}