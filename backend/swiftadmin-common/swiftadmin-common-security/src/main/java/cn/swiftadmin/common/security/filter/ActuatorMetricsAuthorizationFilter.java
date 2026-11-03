package cn.swiftadmin.common.security.filter;

import cn.swiftadmin.common.core.util.ErrorResponseWriter;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 运行指标只对超级管理员、admin 角色或 monitor:metrics 权限开放。
 * <p>健康检查不经过这里，保持匿名可访问。本过滤器排在 Spring Security 之后，以便读到 JWT 建立的登录态。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
@RequiredArgsConstructor
public class ActuatorMetricsAuthorizationFilter extends OncePerRequestFilter {

    static final String METRICS_PERMISSION = "monitor:metrics";

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return !path.equals("/actuator/metrics")
                && !path.startsWith("/actuator/metrics/")
                && !path.equals("/actuator/prometheus")
                && !path.startsWith("/actuator/prometheus/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            errorResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期");
            return;
        }
        if (!canReadMetrics(loginUser)) {
            errorResponseWriter.writeError(response, HttpStatus.FORBIDDEN.value(), "没有访问权限");
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean canReadMetrics(LoginUser loginUser) {
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        Set<String> roles = loginUser.getRoles();
        if (roles != null && roles.contains("admin")) {
            return true;
        }
        Set<String> permissions = loginUser.getPermissions();
        return permissions != null && permissions.contains(METRICS_PERMISSION);
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
