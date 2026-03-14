package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.security.service.PermissionCacheService;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.PermissionCacheRefreshService;
import cn.swiftadmin.system.api.service.UserService;
import cn.swiftadmin.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static cn.swiftadmin.system.api.entity.table.SysUserTableDef.SYS_USER;

/**
 * 权限缓存刷新服务实现
 * <p>处理权限变更时的缓存失效逻辑，同时强制用户下线以重新获取权限</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheRefreshServiceImpl implements PermissionCacheRefreshService {

    private final PermissionCacheService permissionCacheService;
    private final UserSessionService userSessionService;
    private final SysUserMapper userMapper;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void clearCacheByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        clearCacheByRoleIds(Collections.singletonList(roleId));
    }

    @Override
    public void clearCacheByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        // 查询拥有这些角色的用户ID
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_USER_ROLE.USER_ID)
                .from(SYS_USER_ROLE)
                .where(SYS_USER_ROLE.ROLE_ID.in(roleIds));

        List<Long> userIds = userMapper.selectListByQueryAs(queryWrapper, Long.class);

        log.info("查询角色 {} 关联的用户ID: {}", roleIds, userIds);

        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 批量查询用户信息（优化：避免 N+1 查询）
        Map<Long, String> userIdToUsername = batchGetUsernames(userIds);

        // 清除缓存并强制下线
        for (Long userId : userIds) {
            clearUserAllCache(userId, userIdToUsername.get(userId));
            removeUserSessions(userId);
        }

        log.info("已清除 {} 个用户的权限缓存并强制下线（角色变更：{}）", userIds.size(), roleIds);
    }

    @Override
    public void clearCacheByMenuId(Long menuId) {
        if (menuId == null) {
            return;
        }
        clearCacheByMenuIds(Collections.singletonList(menuId));
    }

    @Override
    public void clearCacheByMenuIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }

        // 查询拥有这些菜单的角色ID
        QueryWrapper roleQuery = QueryWrapper.create()
                .select(SYS_ROLE_MENU.ROLE_ID)
                .from(SYS_ROLE_MENU)
                .where(SYS_ROLE_MENU.MENU_ID.in(menuIds));

        List<Long> roleIds = userMapper.selectListByQueryAs(roleQuery, Long.class);

        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        // 清除拥有这些角色的用户缓存
        clearCacheByRoleIds(roleIds);

        log.info("已清除菜单关联的用户权限缓存（菜单变更：{}）", menuIds);
    }

    @Override
    public void clearCacheByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        clearUserAllCache(userId, null);
        removeUserSessions(userId);
        log.debug("已清除用户权限缓存并强制下线：userId={}", userId);
    }

    @Override
    public void clearCacheByDeptId(Long deptId) {
        if (deptId == null) {
            return;
        }

        // 查询部门下的所有用户
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_USER.ID)
                .from(SYS_USER)
                .where(SYS_USER.DEPT_ID.eq(deptId));

        List<Long> userIds = userMapper.selectListByQueryAs(queryWrapper, Long.class);

        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 批量查询用户信息（优化：避免 N+1 查询）
        Map<Long, String> userIdToUsername = batchGetUsernames(userIds);

        for (Long userId : userIds) {
            clearUserAllCache(userId, userIdToUsername.get(userId));
            removeUserSessions(userId);
        }

        log.info("已清除部门 {} 下 {} 个用户的权限缓存并强制下线", deptId, userIds.size());
    }

    @Override
    public void clearAllCache() {
        permissionCacheService.clearAllPermissionCache();
        log.info("已清除所有用户权限缓存");
    }

    /**
     * 批量获取用户ID到用户名的映射
     */
    private Map<Long, String> batchGetUsernames(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        QueryWrapper qw = QueryWrapper.create()
                .select(SYS_USER.ID, SYS_USER.USERNAME)
                .from(SYS_USER)
                .where(SYS_USER.ID.in(userIds));
        List<SysUser> users = userMapper.selectListByQuery(qw);
        return users.stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername, (a, b) -> a));
    }

    /**
     * 清除用户所有权限相关缓存
     * <p>包括 user:permission:{userId} 和 user:info:{username}</p>
     *
     * @param userId   用户ID
     * @param username 用户名（可为null，此时会查询数据库）
     */
    private void clearUserAllCache(Long userId, String username) {
        // 1. 清除 user:permission:{userId} 权限缓存
        permissionCacheService.clearUserPermissionCache(userId);

        // 2. 清除 user:info:{username} 用户信息缓存
        String actualUsername = username;
        if (actualUsername == null) {
            SysUser user = userService.getById(userId);
            actualUsername = user != null ? user.getUsername() : null;
        }
        if (actualUsername != null) {
            String userInfoKey = RedisKeyConstants.USER_INFO + actualUsername;
            Boolean deleted = redisTemplate.delete(userInfoKey);
            log.debug("已清除用户信息缓存: {}, 删除结果: {}", userInfoKey, deleted);
        }
    }

    /**
     * 删除用户所有会话，强制下线
     */
    private void removeUserSessions(Long userId) {
        List<String> sessionIds = userSessionService.getUserSessionIds(userId);
        log.debug("用户 {} 的会话数量: {}", userId, sessionIds.size());
        if (!sessionIds.isEmpty()) {
            for (String sessionId : sessionIds) {
                userSessionService.removeSession(sessionId);
            }
            log.info("已删除用户 {} 的 {} 个会话", userId, sessionIds.size());
        }
    }
}