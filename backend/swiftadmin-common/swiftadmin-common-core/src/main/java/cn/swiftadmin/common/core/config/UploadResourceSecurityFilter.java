package cn.swiftadmin.common.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 给本地上传文件的静态访问补上隔离响应头。
 * <p>即使历史文件扩展名可被浏览器执行，也不在同源页面里跑脚本</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class UploadResourceSecurityFilter extends OncePerRequestFilter {

    private final FileStorageProperties fileStorageProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String accessUrl = fileStorageProperties.getLocal().getAccessUrl();
        if (accessUrl == null || accessUrl.isBlank()) {
            return true;
        }
        String prefix = accessUrl.endsWith("/") ? accessUrl : accessUrl + "/";
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(prefix);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
        response.setHeader("X-Frame-Options", "DENY");
        filterChain.doFilter(request, response);
    }
}
