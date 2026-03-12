package cn.swiftadmin.common.security.filter;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.jwt.JwtTokenProvider;
import cn.swiftadmin.common.security.service.UserDetailsService;
import cn.swiftadmin.common.security.service.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>拦截请求，解析 Authorization 头中的 JWT Token，验证通过后设置 Spring Security 上下文</p>
 * <p>支持 Token 黑名单检查，已登出的 Token 将被拒绝</p>
 * <p>支持会话检查，被强制下线的用户将无法访问</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            // 1. 验证 Token 格式和签名
            if (!tokenProvider.validateToken(token)) {
                log.debug("Token 验证失败：{}", request.getRequestURI());
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "Token 无效或已过期");
                return;
            }

            // 2. 检查 Token 是否在黑名单中
            if (tokenProvider.isTokenBlacklisted(token)) {
                log.debug("Token 已在黑名单中：{}", request.getRequestURI());
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "Token 已失效，请重新登录");
                return;
            }

            // 3. 检查会话是否存在（强制下线检查）
            String sessionId = tokenProvider.getJtiFromToken(token);
            LoginUser sessionUser = userSessionService.getSession(sessionId);
            if (sessionUser == null) {
                log.debug("会话不存在或已被强制下线：sessionId={}, uri={}", sessionId, request.getRequestURI());
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "会话已失效，请重新登录");
                return;
            }

            // 4. 加载用户信息
            String username = tokenProvider.getUsernameFromToken(token);
            LoginUser loginUser = userDetailsService.loadUserByUsername(username);

            if (loginUser != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(loginUser);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("用户认证成功：{}", username);
            } else {
                log.warn("用户不存在：{}", username);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "用户不存在");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 发送错误响应
     *
     * @param response HTTP 响应
     * @param status 状态码
     * @param message 错误消息
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(R.fail(status, message).toString());
    }

    /**
     * 从请求头中提取 Token
     *
     * @param request HTTP 请求
     * @return Token（不含 Bearer 前缀），无则返回 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
