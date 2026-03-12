package cn.swiftadmin.auth.util;

import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统一的 Token 管理器
 * <p>封装 Token 生成、刷新、验证等所有操作，提供统一的业务接口</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenManager {

    private final JwtTokenProvider tokenProvider;

    /**
     * 生成登录凭证（访问 Token + 刷新 Token）
     *
     * @param username 用户名
     * @return 登录凭证
     */
    public LoginVO generateLoginTokens(String username) {
        String accessToken = tokenProvider.generateToken(username);
        String refreshToken = tokenProvider.generateRefreshToken(username);

        log.debug("为用户生成登录 Token：username={}", username);

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(getAccessTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 生成临时 Token（用于强制修改密码等场景）
     *
     * @param username 用户名
     * @return 仅包含临时 Token 的 LoginVO
     */
    public LoginVO generateTempToken(String username) {
        String tempToken = tokenProvider.generateTempToken(username);

        log.debug("为用户生成临时 Token：username={}", username);

        return LoginVO.builder()
                .tempToken(tempToken)
                .requirePasswordChange(true)
                .build();
    }

    /**
     * 生成密码过期临时 Token
     *
     * @param username 用户名
     * @return 包含密码过期标识和临时 Token 的 LoginVO
     */
    public LoginVO generatePasswordExpiredToken(String username) {
        String tempToken = tokenProvider.generateTempToken(username);

        log.debug("为用户生成密码过期临时 Token：username={}", username);

        return LoginVO.builder()
                .tempToken(tempToken)
                .passwordExpired(true)
                .build();
    }

    /**
     * 刷新 Token
     *
     * @param refreshToken 刷新 Token
     * @return 新的登录凭证
     */
    public LoginVO refreshToken(String refreshToken) {
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        String newAccessToken = tokenProvider.generateToken(username);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);

        log.debug("刷新 Token 成功：username={}", username);

        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(getAccessTokenExpiration())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 获取访问 Token 有效期（秒）
     *
     * @return 有效期（秒）
     */
    public long getAccessTokenExpiration() {
        return tokenProvider.getExpiration() / 1000;
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token Token
     * @return true=有效
     */
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return tokenProvider.getUsernameFromToken(token);
    }

    /**
     * 从 Token 中获取 JWT ID
     *
     * @param token Token
     * @return JWT ID
     */
    public String getJtiFromToken(String token) {
        return tokenProvider.getJtiFromToken(token);
    }

    /**
     * 检查是否为刷新 Token
     *
     * @param token Token
     * @return true=刷新 Token
     */
    public boolean isRefreshToken(String token) {
        return tokenProvider.isRefreshToken(token);
    }

    /**
     * 检查是否为临时 Token
     *
     * @param token Token
     * @return true=临时 Token
     */
    public boolean isTempToken(String token) {
        return tokenProvider.isTempToken(token);
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token Token
     * @return true=在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenProvider.isTokenBlacklisted(token);
    }

    /**
     * 将 Token 加入黑名单
     *
     * @param token Token
     */
    public void addToBlacklist(String token) {
        tokenProvider.addToBlacklist(token);
        log.debug("Token 已加入黑名单");
    }

    /**
     * 检查临时 Token 是否可用
     *
     * @param token 临时 Token
     * @return true=可用
     */
    public boolean canUseTempToken(String token) {
        return tokenProvider.canUseTempToken(token);
    }

    /**
     * 获取 Token 剩余有效期（毫秒）
     *
     * @param token Token
     * @return 剩余有效期
     */
    public long getTokenRemainingTime(String token) {
        return tokenProvider.getTokenRemainingTime(token);
    }
}
