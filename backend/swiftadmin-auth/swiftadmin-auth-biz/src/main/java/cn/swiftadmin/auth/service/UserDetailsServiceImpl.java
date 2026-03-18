package cn.swiftadmin.auth.service;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserDetailsService;
import cn.swiftadmin.system.api.entity.SysDept;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.DeptService;
import cn.swiftadmin.system.api.service.PermissionService;
import cn.swiftadmin.system.api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户详情服务实现类
 * <p>加载用户信息用于 Spring Security 认证，构建 LoginUser 对象</p>
 * <p>支持 Redis 缓存，减少数据库查询</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;
    private final DeptService deptService;
    private final PermissionService permissionService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 缓存有效期（分钟） */
    private static final int CACHE_EXPIRE_MINUTES = 30;

    /** 数据权限范围常量 */
    private static final int DATA_SCOPE_ALL = 1;
    private static final int DATA_SCOPE_CUSTOM = 2;
    private static final int DATA_SCOPE_SELF = 5;

    /**
     * 根据用户名加载用户信息
     */
    @Override
    public LoginUser loadUserByUsername(String username) {
        // 1. 尝试从缓存获取
        LoginUser cachedUser = getUserFromCache(username);
        if (cachedUser != null) {
            log.debug("从缓存加载用户信息: {}", username);
            return cachedUser;
        }

        // 2. 从数据库加载
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            return null;
        }

        LoginUser loginUser = buildLoginUser(user);

        // 3. 存入缓存
        cacheUser(username, loginUser);

        return loginUser;
    }

    /**
     * 根据用户 ID 加载用户信息
     */
    @Override
    public LoginUser loadUserByUserId(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            return null;
        }
        return buildLoginUser(user);
    }

    /**
     * 刷新用户权限缓存
     * <p>当用户权限变更时调用，清除缓存并重新加载</p>
     *
     * @param userId 用户 ID
     */
    @Override
    public void refreshUserPermissions(Long userId) {
        // 1. 查询用户信息
        SysUser user = userService.getById(userId);
        if (user == null) {
            log.warn("用户不存在，无法刷新权限：userId={}", userId);
            return;
        }

        // 2. 清除用户信息缓存
        String cacheKey = RedisKeyConstants.userInfoByUsername(user.getUsername());
        redisTemplate.delete(cacheKey);
        log.info("已清除用户信息缓存：userId={}, username={}", userId, user.getUsername());

        // 3. 清除权限缓存（PermissionService 中的缓存）
        String permCacheKey = RedisKeyConstants.userPermission(userId);
        redisTemplate.delete(permCacheKey);
        log.info("已清除用户权限缓存：userId={}", userId);

        // 4. 重新加载用户信息到缓存（可选，下次访问时会自动加载）
        // 这里可以选择立即重新加载，或者等待下次访问时懒加载
        try {
            LoginUser loginUser = buildLoginUser(user);
            cacheUser(user.getUsername(), loginUser);
            log.info("已重新加载用户信息到缓存：userId={}, username={}", userId, user.getUsername());
        } catch (Exception e) {
            log.error("重新加载用户缓存失败：userId={}", userId, e);
        }
    }

    /**
     * 清除用户缓存
     *
     * @param username 用户名
     */
    public void clearUserCache(String username) {
        String cacheKey = RedisKeyConstants.userInfoByUsername(username);
        redisTemplate.delete(cacheKey);
        log.debug("清除用户缓存: {}", username);
    }

    /**
     * 从缓存获取用户信息
     */
    private LoginUser getUserFromCache(String username) {
        try {
            String cacheKey = RedisKeyConstants.userInfoByUsername(username);
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, LoginUser.class);
            }
        } catch (Exception e) {
            log.warn("从缓存读取用户信息失败: {}", username, e);
        }
        return null;
    }

    /**
     * 缓存用户信息
     */
    private void cacheUser(String username, LoginUser loginUser) {
        try {
            String cacheKey = RedisKeyConstants.userInfoByUsername(username);
            String json = objectMapper.writeValueAsString(loginUser);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("用户信息已缓存: {}", username);
        } catch (Exception e) {
            log.warn("缓存用户信息失败: {}", username, e);
        }
    }

    /**
     * 构建 LoginUser 对象
     *
     * @param user 用户实体
     * @return 登录用户信息
     */
    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setStatus(user.getStatus() != null ? user.getStatus().getCode() : null);

        // 设置超级管理员标识
        boolean isSuperAdmin = user.getIsSuper() != null && user.getIsSuper().isYes();
        loginUser.setSuperAdmin(isSuperAdmin);

        // 查询部门名称和祖级路径
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                loginUser.setDeptName(dept.getName());
                loginUser.setDeptAncestors(dept.getAncestors());
            }
        }

        // 加载权限和角色
        try {
            Set<String> permissions = permissionService.getPermissionsByUserId(user.getId());
            Set<String> roles = permissionService.getRolesByUserId(user.getId());
            loginUser.setPermissions(permissions);
            loginUser.setRoles(roles);
            log.debug("加载用户权限: userId={}, permissions={}, roles={}", user.getId(), permissions, roles);
        } catch (Exception e) {
            log.warn("加载用户权限失败: userId={}", user.getId(), e);
            loginUser.setPermissions(Set.of());
            loginUser.setRoles(Set.of());
        }

        // 设置数据权限范围
        log.debug("设置数据权限: userId={}, isSuperAdmin={}", user.getId(), isSuperAdmin);
        if (isSuperAdmin) {
            // 超级管理员拥有全部数据权限
            loginUser.setDataScope(DATA_SCOPE_ALL);
        } else {
            try {
                Integer dataScope = permissionService.getDataScopeByUserId(user.getId());
                loginUser.setDataScope(dataScope);

                // 如果是自定义数据权限，加载自定义部门ID列表
                if (dataScope != null && dataScope == DATA_SCOPE_CUSTOM) {
                    java.util.List<Long> customDeptIds = permissionService.getCustomDeptIdsByUserId(user.getId());
                    loginUser.setCustomDeptIds(customDeptIds);
                }
            } catch (Exception e) {
                log.warn("加载数据权限失败: userId={}", user.getId(), e);
                loginUser.setDataScope(DATA_SCOPE_SELF); // 默认仅本人
            }
        }

        return loginUser;
    }

}
