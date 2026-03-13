package cn.swiftadmin.auth.controller;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.PermissionCacheService;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 权限控制器
 * <p>提供权限检查、刷新等接口</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Tag(name = "权限管理", description = "权限检查、刷新等接口")
@RestController
@RequestMapping("/api/auth/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final PermissionCacheService permissionCacheService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 检查权限是否需要刷新
     * <p>前端轮询此接口，检查权限是否有变更</p>
     *
     * @return 是否需要刷新
     */
    @Operation(summary = "检查权限刷新", description = "检查用户权限是否需要刷新")
    @GetMapping("/check-refresh")
    public R<Map<String, Object>> checkRefresh() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return R.ok(Map.of("needRefresh", false));
        }

        // 检查是否有权限刷新标记
        String refreshKey = RedisKeyConstants.userPermission(userId) + ":refresh";
        String refreshFlag = stringRedisTemplate.opsForValue().get(refreshKey);

        Map<String, Object> result = new HashMap<>();
        if (StringUtils.hasText(refreshFlag)) {
            // 清除刷新标记
            stringRedisTemplate.delete(refreshKey);
            result.put("needRefresh", true);
            result.put("timestamp", refreshFlag);
        } else {
            result.put("needRefresh", false);
        }

        return R.ok(result);
    }

    /**
     * 刷新当前用户权限
     * <p>清除缓存并重新加载用户权限</p>
     *
     * @return 最新的权限信息
     */
    @Operation(summary = "刷新权限", description = "刷新当前用户的权限信息")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refreshPermission() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return R.fail(401, "用户未登录");
        }

        log.info("刷新用户权限: userId={}", userId);

        // 1. 清除权限缓存
        permissionCacheService.clearUserPermissionCache(userId);

        // 2. 重新构建用户权限
        LoginUser loginUser = permissionService.buildLoginUserPermission(userId);

        if (loginUser == null) {
            return R.fail(500, "获取用户权限失败");
        }

        // 3. 返回最新权限信息
        Map<String, Object> result = new HashMap<>();
        result.put("permissions", loginUser.getPermissions());
        result.put("roles", loginUser.getRoles());
        result.put("dataScope", loginUser.getDataScope());

        return R.ok(result);
    }

    /**
     * 获取当前用户权限信息
     *
     * @return 权限信息
     */
    @Operation(summary = "获取权限信息", description = "获取当前用户的权限和角色信息")
    @GetMapping("/info")
    public R<Map<String, Object>> getPermissionInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return R.fail(401, "用户未登录");
        }

        // 获取缓存的权限信息
        LoginUser loginUser = permissionCacheService.getCachedUserPermission(userId);

        Map<String, Object> result = new HashMap<>();
        if (loginUser != null) {
            result.put("permissions", loginUser.getPermissions());
            result.put("roles", loginUser.getRoles());
            result.put("dataScope", loginUser.getDataScope());
            result.put("isSuperAdmin", loginUser.isSuperAdmin());
        } else {
            // 缓存不存在，重新构建
            Set<String> permissions = permissionService.getPermissionsByUserId(userId);
            Set<String> roles = permissionService.getRolesByUserId(userId);
            Integer dataScope = permissionService.getDataScopeByUserId(userId);

            result.put("permissions", permissions);
            result.put("roles", roles);
            result.put("dataScope", dataScope);
            result.put("isSuperAdmin", permissionService.isSuperAdmin(userId));
        }

        return R.ok(result);
    }

}