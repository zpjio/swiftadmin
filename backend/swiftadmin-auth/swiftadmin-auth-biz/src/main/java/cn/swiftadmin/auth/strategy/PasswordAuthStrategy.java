package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.service.LoginEventService;
import cn.swiftadmin.auth.service.LoginSessionService;
import cn.swiftadmin.auth.service.LoginValidationService;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 账号密码认证策略
 * <p>重构版：依赖注入从 11 个减少到 8 个，职责更清晰</p>
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
    private final LoginValidationService validationService;
    private final LoginSessionService sessionService;
    private final LoginEventService eventService;
    private final HttpServletRequest request;

    @Override
    public LoginType getLoginType() {
        return LoginType.PASSWORD;
    }

    @Override
    public LoginVO authenticate(LoginDTO dto) {
        String username = dto.getUsername();
        String ip = dto.getIp() != null ? dto.getIp() : ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);

        // 构建登录上下文
        LoginContext context = LoginContext.ofPassword(ip, userAgent).withUsername(username);

        // 1. IP 限流验证
        validationService.validateIpRateLimit(context);

        // 2. 查询用户
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            log.warn("LOGIN_FAILED username={} reason=USER_NOT_FOUND", username);
            eventService.publishFailure(username, "用户不存在", context);
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 3. 检查账户锁定
        if (userService.isAccountLocked(username)) {
            log.warn("LOGIN_FAILED username={} reason=ACCOUNT_LOCKED", username);
            eventService.publishFailure(username, "账户已锁定", context);
            throw new BusinessException(ErrorCode.USER_DISABLED.getCode(),
                    "账户已锁定，请 " + securityProperties.getLockMinutes() + " 分钟后再试");
        }

        // 4. 验证码检查（如果需要）
        validationService.checkCaptchaIfNeeded(username, dto.getCaptchaId(), dto.getCaptchaCode());

        // 5. 密码验证
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            handlePasswordError(username, context);
        }

        // 6. 检查用户状态
        if (!user.getStatus().isEnabled()) {
            log.warn("LOGIN_FAILED username={} reason=USER_DISABLED status={}", username, user.getStatus());
            eventService.publishFailure(username, "用户已禁用", context);
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 7. 重置登录失败计数
        userService.resetLoginFailCount(username);

        // 8. 检查是否需要强制修改密码
        if (user.getForceChangePassword() != null && user.getForceChangePassword().isYes()) {
            log.info("LOGIN_SUCCESS username={} action=FORCE_CHANGE_PASSWORD", username);
            eventService.publishSuccess(context);
            return sessionService.createTempSession(username);
        }

        // 9. 检查密码是否过期
        if (userService.isPasswordExpired(user)) {
            log.info("LOGIN_SUCCESS username={} action=PASSWORD_EXPIRED", username);
            eventService.publishSuccess(context);
            return sessionService.createPasswordExpiredSession(username);
        }

        // 10. 创建登录会话
        LoginVO loginVO = sessionService.createSession(user.getUsername(), context);

        // 11. 设置密码剩余天数提醒
        int remainingDays = userService.getPasswordRemainingDays(user);
        int warningDays = configService.getPasswordExpireWarningDays();

        log.info("LOGIN_SUCCESS username={} ip={}", username, ip);
        eventService.publishSuccess(context);

        if (remainingDays <= warningDays && remainingDays > 0) {
            loginVO.setPasswordRemainingDays(remainingDays);
            log.info("PASSWORD_EXPIRING_SOON username={} remainingDays={}", username, remainingDays);
        }

        return loginVO;
    }

    /**
     * 处理密码错误
     */
    private void handlePasswordError(String username, LoginContext context) {
        int failCount = userService.incrementLoginFailCount(username);
        int maxFailCount = securityProperties.getMaxLoginFailCount();
        int remaining = maxFailCount - failCount;

        log.warn("LOGIN_FAILED username={} reason=PASSWORD_ERROR failCount={} remaining={}",
                username, failCount, remaining);

        if (failCount >= maxFailCount) {
            userService.lockAccount(username, securityProperties.getLockMinutes());
            log.warn("ACCOUNT_LOCKED username={} lockMinutes={}", username, securityProperties.getLockMinutes());
            eventService.publishFailure(username, "账户已锁定", context);
            throw new BusinessException(ErrorCode.USER_DISABLED.getCode(),
                    "登录失败次数过多，账户已锁定 " + securityProperties.getLockMinutes() + " 分钟");
        }

        eventService.publishFailure(username, "密码错误", context);
        throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(),
                "用户名或密码错误，还剩 " + remaining + " 次机会");
    }
}