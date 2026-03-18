package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.PermissionCacheService;
import cn.swiftadmin.common.security.service.PermissionWarmupService;
import cn.swiftadmin.system.api.entity.SysMenu;
import cn.swiftadmin.system.api.service.MenuService;
import cn.swiftadmin.system.api.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限预热服务实现
 * <p>在用户登录后预热常用权限数据，减少首次请求延迟</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionWarmupServiceImpl implements PermissionWarmupService {

    private final PermissionService permissionService;
    private final PermissionCacheService permissionCacheService;
    private final MenuService menuService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    @Async("permissionWarmupExecutor")
    public void warmupUserPermission(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            log.debug("开始预热用户权限数据: userId={}", userId);

            // 1. 检查缓存是否已存在
            LoginUser cachedUser = permissionCacheService.getCachedUserPermission(userId);
            if (cachedUser != null) {
                log.debug("用户权限缓存已存在，跳过预热: userId={}", userId);
                return;
            }

            // 2. 构建并缓存用户权限信息
            LoginUser loginUser = permissionService.buildLoginUserPermission(userId);
            if (loginUser != null) {
                permissionCacheService.cacheUserPermission(userId, loginUser);
                log.info("用户权限预热完成: userId={}, permissions={}, roles={}",
                        userId,
                        loginUser.getPermissions() != null ? loginUser.getPermissions().size() : 0,
                        loginUser.getRoles() != null ? loginUser.getRoles().size() : 0);
            }
        } catch (Exception e) {
            log.error("用户权限预热失败: userId={}", userId, e);
        }
    }

    @Override
    @Async("permissionWarmupExecutor")
    public void warmupUserMenus(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            log.debug("开始预热用户菜单数据: userId={}", userId);

            String cacheKey = RedisKeyConstants.userMenus(userId);

            // 1. 检查缓存是否已存在
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("用户菜单缓存已存在，跳过预热: userId={}", userId);
                return;
            }

            // 2. 获取用户菜单
            List<SysMenu> menus = menuService.getMenusByUserId(userId);
            if (!CollectionUtils.isEmpty(menus)) {
                redisTemplate.opsForValue().set(cacheKey, menus, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                log.info("用户菜单预热完成: userId={}, menuCount={}", userId, menus.size());
            }
        } catch (Exception e) {
            log.error("用户菜单预热失败: userId={}", userId, e);
        }
    }

    @Override
    @Async("permissionWarmupExecutor")
    public void warmupUserPermissions(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        log.debug("开始批量预热用户权限数据: count={}", userIds.size());

        for (Long userId : userIds) {
            warmupUserPermission(userId);
        }

        log.info("批量预热用户权限完成: count={}", userIds.size());
    }

}