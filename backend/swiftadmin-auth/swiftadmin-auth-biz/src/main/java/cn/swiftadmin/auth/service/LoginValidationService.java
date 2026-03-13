package cn.swiftadmin.auth.service;

import cn.swiftadmin.auth.api.service.CaptchaService;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.util.IpRateLimiter;
import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 登录验证服务
 * <p>封装登录前的安全验证逻辑：IP限流、验证码验证、账户锁定检查</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginValidationService {

    private final IpRateLimiter ipRateLimiter;
    private final CaptchaService captchaService;
    private final AuthSecurityProperties securityProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * 验证 IP 限流
     *
     * @param context 登录上下文
     */
    public void validateIpRateLimit(LoginContext context) {
        String ip = context.getIp();
        if (!StringUtils.hasLength(ip)) {
            return;
        }

        if (ipRateLimiter.isIpBlacklisted(ip)) {
            log.warn("LOGIN_BLOCKED ip={} reason=IP_BLACKLISTED", ip);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        ipRateLimiter.checkIpLoginLimit(ip);
    }

    /**
     * 检查是否需要验证码
     *
     * @param username 用户名
     * @return true=需要验证码
     */
    public boolean isCaptchaRequired(String username) {
        String failCountKey = RedisKeyConstants.loginFailCount(username);
        String failCountStr = redisTemplate.opsForValue().get(failCountKey);

        if (failCountStr == null) {
            return false;
        }

        int failCount = Integer.parseInt(failCountStr);
        return failCount >= securityProperties.getCaptchaTriggerFailCount();
    }

    /**
     * 验证验证码
     *
     * @param captchaId 验证码 ID
     * @param code 验证码
     * @return true=验证通过
     */
    public boolean validateCaptcha(String captchaId, String code) {
        if (captchaId == null || !StringUtils.hasText(code)) {
            log.warn("CAPTCHA_INVALID captchaId={}", captchaId);
            return false;
        }
        return captchaService.verify(captchaId, code);
    }

    /**
     * 检查验证码（如果需要）
     *
     * @param username 用户名
     * @param captchaId 验证码 ID
     * @param captchaCode 验证码
     */
    public void checkCaptchaIfNeeded(String username, String captchaId, String captchaCode) {
        if (!isCaptchaRequired(username)) {
            return;
        }

        if (!validateCaptcha(captchaId, captchaCode)) {
            log.warn("LOGIN_FAILED username={} reason=CAPTCHA_ERROR", username);
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
    }
}