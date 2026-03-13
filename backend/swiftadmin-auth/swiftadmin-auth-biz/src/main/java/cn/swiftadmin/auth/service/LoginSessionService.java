package cn.swiftadmin.auth.service;

import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserDetailsService;
import cn.swiftadmin.common.security.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录会话服务
 * <p>封装登录成功后的会话创建逻辑</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSessionService {

    private final TokenManager tokenManager;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    /**
     * 创建登录会话
     *
     * @param username 用户名
     * @param context 登录上下文
     * @return 登录凭证
     */
    public LoginVO createSession(String username, LoginContext context) {
        LoginVO loginVO = tokenManager.generateLoginTokens(username);

        String sessionId = tokenManager.getJtiFromToken(loginVO.getAccessToken());
        LoginUser loginUser = userDetailsService.loadUserByUsername(username);

        if (loginUser != null) {
            userSessionService.createSession(
                    sessionId,
                    loginUser,
                    context.getIp(),
                    context.getUserAgent(),
                    tokenManager.getAccessTokenExpiration()
            );
        }

        return loginVO;
    }

    /**
     * 创建临时 Token 会话（用于强制修改密码等场景）
     *
     * @param username 用户名
     * @return 临时 Token
     */
    public LoginVO createTempSession(String username) {
        return tokenManager.generateTempToken(username);
    }

    /**
     * 创建密码过期 Token 会话
     *
     * @param username 用户名
     * @return 密码过期 Token
     */
    public LoginVO createPasswordExpiredSession(String username) {
        return tokenManager.generatePasswordExpiredToken(username);
    }
}