package cn.swiftadmin.common.security.filter;

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

        // 只把访问令牌写入安全上下文。刷新令牌、临时令牌和无效令牌都不在这里直接 401，
        // 否则登录、刷新、强制改密等匿名接口会因为浏览器还带着旧 Authorization 而被拦截。
        // 受保护接口没有有效身份时，由 Spring Security 统一返回 401。
        if (StringUtils.hasText(token)
                && tokenProvider.validateToken(token)
                && tokenProvider.isAccessToken(token)
                && !tokenProvider.isTokenBlacklisted(token)) {
            String sessionId = tokenProvider.getJtiFromToken(token);
            LoginUser sessionUser = userSessionService.getSession(sessionId);
            if (sessionUser == null) {
                log.debug("会话不存在或已被强制下线：sessionId={}, uri={}", sessionId, request.getRequestURI());
            } else {
                String username = tokenProvider.getUsernameFromToken(token);
                LoginUser loginUser = userDetailsService.loadUserByUsername(username);
                if (loginUser != null && loginUser.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(loginUser);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("用户认证成功：{}", username);
                } else if (loginUser == null) {
                    log.warn("用户不存在：{}", username);
                } else {
                    log.info("拒绝已禁用用户的访问：{}", username);
                }
            }
        }

        filterChain.doFilter(request, response);
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
