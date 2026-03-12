package cn.swiftadmin.common.security.exception;

import cn.swiftadmin.common.core.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 异常处理器
 * <p>统一处理认证和授权异常，优先级最高</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    /**
     * 处理自定义权限拒绝异常
     *
     * @param e 权限拒绝异常
     * @return 统一响应
     */
    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handlePermissionDeniedException(PermissionDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return R.fail(403, e.getMessage());
    }

    /**
     * 处理访问拒绝异常（403）
     *
     * @param e 访问拒绝异常
     * @return 统一响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return R.fail(403, "权限不足");
    }

    /**
     * 处理认证异常（401）
     *
     * @param e 认证异常
     * @return 统一响应
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败：{}", e.getMessage());
        return R.fail(401, "认证失败，请重新登录");
    }

}
