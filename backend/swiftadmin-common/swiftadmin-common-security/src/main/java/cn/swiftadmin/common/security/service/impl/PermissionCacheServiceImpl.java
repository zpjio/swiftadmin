package cn.swiftadmin.common.security.service.impl;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 权限缓存服务实现类
 * <p>使用 Redis 缓存用户权限信息</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheServiceImpl implements PermissionCacheService {

    /**
     * 缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void cacheUserPermission(Long userId, LoginUser loginUser) {
        if (userId == null || loginUser == null) {
            return;
        }
        String key = getUserKey(userId);
        redisTemplate.opsForValue().set(key, loginUser, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("缓存用户权限信息: userId={}", userId);
    }

    @Override
    public LoginUser getCachedUserPermission(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = getUserKey(userId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof LoginUser) {
            return (LoginUser) value;
        }
        return null;
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        LoginUser loginUser = getCachedUserPermission(userId);
        return loginUser != null ? loginUser.getPermissions() : null;
    }

    @Override
    public Set<String> getUserRoles(Long userId) {
        LoginUser loginUser = getCachedUserPermission(userId);
        return loginUser != null ? loginUser.getRoles() : null;
    }

    @Override
    public Integer getUserDataScope(Long userId) {
        LoginUser loginUser = getCachedUserPermission(userId);
        return loginUser != null ? loginUser.getDataScope() : null;
    }

    @Override
    public List<Long> getUserCustomDeptIds(Long userId) {
        LoginUser loginUser = getCachedUserPermission(userId);
        return loginUser != null ? loginUser.getCustomDeptIds() : null;
    }

    @Override
    public void clearUserPermissionCache(Long userId) {
        if (userId == null) {
            return;
        }
        String key = getUserKey(userId);
        redisTemplate.delete(key);
        log.debug("清除用户权限缓存: userId={}", userId);
    }

    @Override
    public void clearAllPermissionCache() {
        Set<String> keys = redisTemplate.keys(RedisKeyConstants.USER_PERMISSION + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("清除所有用户权限缓存: count={}", keys.size());
        }
    }

    @Override
    public LoginUser refreshUserPermission(Long userId) {
        // 这个方法需要调用 PermissionService.buildLoginUserPermission
        // 但这里存在循环依赖问题，所以这个方法在 PermissionServiceImpl 中实现
        throw new UnsupportedOperationException("请使用 PermissionService.buildLoginUserPermission 方法");
    }

    /**
     * 获取用户缓存 Key
     */
    private String getUserKey(Long userId) {
        return RedisKeyConstants.userPermission(userId);
    }

}