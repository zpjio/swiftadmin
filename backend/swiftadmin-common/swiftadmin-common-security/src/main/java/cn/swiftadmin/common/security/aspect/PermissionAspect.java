package cn.swiftadmin.common.security.aspect;

import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.common.security.annotation.RequiresRole;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.exception.PermissionDeniedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 权限校验切面
 * <p>处理 @RequiresPermission 和 @RequiresRole 注解</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Aspect
@Component
public class PermissionAspect {

    /**
     * 处理 @RequiresPermission 注解
     */
    @Around("@annotation(cn.swiftadmin.common.security.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        // 获取当前用户
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new PermissionDeniedException("用户未登录");
        }

        // 超级管理员跳过权限检查
        if (isSuperAdmin(loginUser)) {
            return joinPoint.proceed();
        }

        // 校验权限
        String[] permissions = annotation.value();
        Set<String> userPermissions = loginUser.getPermissions();

        if (CollectionUtils.isEmpty(userPermissions)) {
            throw new PermissionDeniedException("用户无任何权限");
        }

        boolean hasPermission;
        if (annotation.logical() == RequiresPermission.Logical.AND) {
            // 需要满足所有权限
            hasPermission = Arrays.stream(permissions)
                    .allMatch(userPermissions::contains);
        } else {
            // 满足其中一个权限即可
            hasPermission = Arrays.stream(permissions)
                    .anyMatch(userPermissions::contains);
        }

        if (!hasPermission) {
            throw new PermissionDeniedException("权限不足，需要权限：" + Arrays.toString(permissions));
        }

        return joinPoint.proceed();
    }

    /**
     * 处理 @RequiresRole 注解
     */
    @Around("@annotation(cn.swiftadmin.common.security.annotation.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);

        // 获取当前用户
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new PermissionDeniedException("用户未登录");
        }

        // 超级管理员跳过角色检查
        if (isSuperAdmin(loginUser)) {
            return joinPoint.proceed();
        }

        // 校验角色
        String[] roles = annotation.value();
        Set<String> userRoles = loginUser.getRoles();

        if (CollectionUtils.isEmpty(userRoles)) {
            throw new PermissionDeniedException("用户无任何角色");
        }

        boolean hasRole;
        if (annotation.logical() == RequiresPermission.Logical.AND) {
            // 需要满足所有角色
            hasRole = Arrays.stream(roles)
                    .allMatch(userRoles::contains);
        } else {
            // 满足其中一个角色即可
            hasRole = Arrays.stream(roles)
                    .anyMatch(userRoles::contains);
        }

        if (!hasRole) {
            throw new PermissionDeniedException("角色不足，需要角色：" + Arrays.toString(roles));
        }

        return joinPoint.proceed();
    }

    /**
     * 获取当前登录用户
     */
    private LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 判断是否为超级管理员
     * <p>优先使用 LoginUser 的 superAdmin 标识，其次检查是否有 admin 角色</p>
     */
    private boolean isSuperAdmin(LoginUser loginUser) {
        // 优先使用 isSuperAdmin 标识（数据库 is_super 字段）
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        // 兼容：角色编码为 "admin" 的用户也拥有全部权限
        Set<String> roles = loginUser.getRoles();
        return roles != null && roles.contains("admin");
    }

}