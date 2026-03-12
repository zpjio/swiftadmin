package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.api.service.CaptchaService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.util.IpRateLimiter;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserDetailsService;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 账号密码认证策略
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordAuthStrategy implements AuthStrategy {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final ConfigService configService;
    private final AuthSecurityProperties securityProperties;
    private final CaptchaService captchaService;
    private final IpRateLimiter ipRateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    @Override
    public LoginType getLoginType() {
        return LoginType.PASSWORD;
    }

    @Override
    public LoginVO authenticate(LoginDTO dto) {
        String username = dto.getUsername();
        String ip = dto.getIp() != null ? dto.getIp() : ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);

        if (ip != null && !ip.isEmpty()) {
            if (ipRateLimiter.isIpBlacklisted(ip)) {
                log.warn("LOGIN_FAILED event=IP_BLACKLISTED ip={}", ip);
                eventPublisher.publishEvent(LoginEvent.failure(username, "IP 已被加入黑名单", ip, userAgent, "password"));
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
            ipRateLimiter.checkIpLoginLimit(ip);
        }

        SysUser user = userService.getByUsername(username);

        if (user == null) {
            log.warn("LOGIN_FAILED event=USER_NOT_FOUND username={}", username);
            eventPublisher.publishEvent(LoginEvent.failure(username, "用户不存在", ip, userAgent, "password"));
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        if (userService.isAccountLocked(username)) {
            log.warn("LOGIN_FAILED event=ACCOUNT_LOCKED username={}", username);
            eventPublisher.publishEvent(LoginEvent.failure(username, "账户已锁定", ip, userAgent, "password"));
            throw new BusinessException(ErrorCode.USER_DISABLED.getCode(),
                    "账户已锁定，请 " + securityProperties.getLockMinutes() + " 分钟后再试");
        }

        if (isCaptchaRequired(username)) {
            if (!validateCaptcha(dto.getCaptchaId(), dto.getCaptchaCode())) {
                log.warn("LOGIN_FAILED event=CAPTCHA_ERROR username={}", username);
                eventPublisher.publishEvent(LoginEvent.failure(username, "验证码错误", ip, userAgent, "password"));
                throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
            }
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            int failCount = userService.incrementLoginFailCount(username);
            log.warn("LOGIN_FAILED event=PASSWORD_ERROR username={} failCount={} remaining={}",
                    username, failCount, securityProperties.getMaxLoginFailCount() - failCount);

            if (failCount >= securityProperties.getMaxLoginFailCount()) {
                userService.lockAccount(username, securityProperties.getLockMinutes());
                log.warn("ACCOUNT_LOCKED username={} lockMinutes={}",
                        username, securityProperties.getLockMinutes());
                eventPublisher.publishEvent(LoginEvent.failure(username, "账户已锁定", ip, userAgent, "password"));
                throw new BusinessException(ErrorCode.USER_DISABLED.getCode(),
                        "登录失败次数过多，账户已锁定 " + securityProperties.getLockMinutes() + " 分钟");
            }

            eventPublisher.publishEvent(LoginEvent.failure(username, "密码错误", ip, userAgent, "password"));
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(),
                    "用户名或密码错误，还剩 " + (securityProperties.getMaxLoginFailCount() - failCount) + " 次机会");
        }

        if (!user.getStatus().isEnabled()) {
            log.warn("LOGIN_FAILED event=USER_DISABLED username={} status={}", username, user.getStatus());
            eventPublisher.publishEvent(LoginEvent.failure(username, "用户已禁用", ip, userAgent, "password"));
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        userService.resetLoginFailCount(username);

        if (user.getForceChangePassword() != null && user.getForceChangePassword().isYes()) {
            log.info("LOGIN_SUCCESS event=FORCE_CHANGE_PASSWORD_REQUIRED username={}", username);
            eventPublisher.publishEvent(LoginEvent.success(username, ip, userAgent, "password"));
            return tokenManager.generateTempToken(username);
        }

        if (userService.isPasswordExpired(user)) {
            log.info("LOGIN_SUCCESS event=PASSWORD_EXPIRED username={}", username);
            eventPublisher.publishEvent(LoginEvent.success(username, ip, userAgent, "password"));
            return tokenManager.generatePasswordExpiredToken(username);
        }

        LoginVO loginVO = tokenManager.generateLoginTokens(user.getUsername());

        String sessionId = tokenManager.getJtiFromToken(loginVO.getAccessToken());
        LoginUser loginUser = userDetailsService.loadUserByUsername(user.getUsername());
        if (loginUser != null) {
            userSessionService.createSession(
                    sessionId,
                    loginUser,
                    ip,
                    userAgent,
                    tokenManager.getAccessTokenExpiration()
            );
        }

        int remainingDays = userService.getPasswordRemainingDays(user);
        int warningDays = configService.getPasswordExpireWarningDays();

        log.info("LOGIN_SUCCESS username={} ip={}", username, ip);
        eventPublisher.publishEvent(LoginEvent.success(username, ip, userAgent, "password"));

        if (remainingDays <= warningDays && remainingDays > 0) {
            loginVO.setPasswordRemainingDays(remainingDays);
            log.info("PASSWORD_EXPIRING_SOON username={} remainingDays={}", username, remainingDays);
        }

        return loginVO;
    }

    private boolean isCaptchaRequired(String username) {
        String failCountKey = "login:fail:count:" + username;
        String failCountStr = redisTemplate.opsForValue().get(failCountKey);

        if (failCountStr == null) {
            return false;
        }

        int failCount = Integer.parseInt(failCountStr);
        return failCount >= securityProperties.getCaptchaTriggerFailCount();
    }

    private boolean validateCaptcha(String captchaId, String code) {
        if (captchaId == null || code == null || code.trim().isEmpty()) {
            log.warn("CAPTCHA_INVALID captchaId={}", captchaId);
            return false;
        }
        return captchaService.verify(captchaId, code);
    }

}