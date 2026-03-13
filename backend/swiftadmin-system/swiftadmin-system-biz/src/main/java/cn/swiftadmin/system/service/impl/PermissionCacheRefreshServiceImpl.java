package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.security.service.PermissionCacheService;
import cn.swiftadmin.system.api.service.PermissionCacheRefreshService;
import cn.swiftadmin.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static cn.swiftadmin.system.api.entity.table.SysUserTableDef.SYS_USER;

/**
 * 权限缓存刷新服务实现
 * <p>处理权限变更时的缓存失效逻辑</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheRefreshServiceImpl implements PermissionCacheRefreshService {

    private final PermissionCacheService permissionCacheService;
    private final SysUserMapper userMapper;

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

        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 清除这些用户的权限缓存
        for (Long userId : userIds) {
            permissionCacheService.clearUserPermissionCache(userId);
        }

        log.info("已清除 {} 个用户的权限缓存（角色变更：{}）", userIds.size(), roleIds);
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
        permissionCacheService.clearUserPermissionCache(userId);
        log.debug("已清除用户权限缓存：userId={}", userId);
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

        for (Long userId : userIds) {
            permissionCacheService.clearUserPermissionCache(userId);
        }

        log.info("已清除部门 {} 下 {} 个用户的权限缓存", deptId, userIds.size());
    }

    @Override
    public void clearAllCache() {
        permissionCacheService.clearAllPermissionCache();
        log.info("已清除所有用户权限缓存");
    }
}