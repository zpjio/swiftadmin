package cn.swiftadmin.common.security.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * 权限拒绝异常
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public class PermissionDeniedException extends AccessDeniedException {

    public PermissionDeniedException(String msg) {
        super(msg);
    }

    public PermissionDeniedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}