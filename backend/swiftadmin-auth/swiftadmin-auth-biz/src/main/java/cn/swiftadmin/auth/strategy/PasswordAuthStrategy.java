package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.service.LoginEventService;
import cn.swiftadmin.auth.service.LoginSessionService;
import cn.swiftadmin.auth.service.LoginValidationService;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.core.util.SensitiveUtils;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 账号密码认证策略
 * <p>重构版：依赖注入从 11 个减少到 7 个，职责更清晰</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordAuthStrategy implements AuthStrategy {

    /** 连续失败告警阈值（达到此值记录 ERROR 日志） */
    private static final int CONTINUOUS_FAILURE_WARN_THRESHOLD = 3;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final AuthSecurityProperties securityProperties;
    private final LoginValidationService validationService;
    private final LoginSessionService sessionService;
    private final LoginEventService eventService;

    @Override
    public LoginType getLoginType() {
        return LoginType.PASSWORD;
    }

    @Override
    public LoginVO authenticate(LoginDTO dto) {
        String username = dto.getUsername();
        String ip = dto.getIp() != null ? dto.getIp() : ServletUtils.getClientIp();
        String userAgent = ServletUtils.getUserAgent();

        // 构建登录上下文
        LoginContext context = LoginContext.ofPassword(ip, userAgent).withUsername(username);

        // 1. IP 限流验证
        validationService.validateIpRateLimit(context);

        // 2. 查询用户
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            log.warn("LOGIN_FAILED username={} reason=USER_NOT_FOUND ip={}", 
                    SensitiveUtils.maskUsername(username), SensitiveUtils.maskIp(context.getIp()));
            eventService.publishFailure(username, "用户不存在", context);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 检查账户锁定
        if (userService.isAccountLocked(username)) {
            log.warn("LOGIN_FAILED username={} reason=ACCOUNT_LOCKED ip={}", 
                    SensitiveUtils.maskUsername(username), SensitiveUtils.maskIp(context.getIp()));
            eventService.publishFailure(username, "账户已锁定", context);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED.getCode(),
                    "账户已锁定，请稍后再试");
        }

        // 4. 验证码检查（如果需要）
        validationService.checkCaptchaIfNeeded(username, dto.getCaptchaId(), dto.getCaptchaCode());

        // 5. 密码验证
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            handlePasswordError(username, context);
        }

        // 6. 检查用户状态
        if (!user.getStatus().isEnabled()) {
            log.warn("LOGIN_FAILED username={} reason=USER_DISABLED status={} ip={}", 
                    SensitiveUtils.maskUsername(username), user.getStatus(), SensitiveUtils.maskIp(context.getIp()));
            eventService.publishFailure(username, "用户已禁用", context);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 7. 重置登录失败计数
        userService.resetLoginFailCount(username);

        // 8. 检查是否需要强制修改密码
        if (user.getForceChangePassword() != null && user.getForceChangePassword().isYes()) {
            log.info("LOGIN_SUCCESS username={} action=FORCE_CHANGE_PASSWORD ip={}", 
                    SensitiveUtils.maskUsername(username), SensitiveUtils.maskIp(context.getIp()));
            eventService.publishSuccess(context);
            return sessionService.createTempSession(username);
        }

        // 9. 检查密码是否过期
        if (userService.isPasswordExpired(user)) {
            log.info("LOGIN_SUCCESS username={} action=PASSWORD_EXPIRED ip={}", 
                    SensitiveUtils.maskUsername(username), SensitiveUtils.maskIp(context.getIp()));
            eventService.publishSuccess(context);
            return sessionService.createPasswordExpiredSession(username);
        }

        // 10. 创建登录会话
        LoginVO loginVO = sessionService.createSession(user.getUsername(), context);

        // 11. 设置密码剩余天数提醒
        int remainingDays = userService.getPasswordRemainingDays(user);
        int warningDays = configService.getPasswordExpireWarningDays();

        log.info("LOGIN_SUCCESS username={} reason=AUTHENTICATED ip={}", SensitiveUtils.maskUsername(username), SensitiveUtils.maskIp(ip));
        eventService.publishSuccess(context);

        if (remainingDays <= warningDays && remainingDays > 0) {
            loginVO.setPasswordRemainingDays(remainingDays);
            log.info("PASSWORD_EXPIRING_SOON username={} remainingDays={}", 
                    SensitiveUtils.maskUsername(username), remainingDays);
        }

        return loginVO;
    }

    /**
     * 处理密码错误
     */
    private void handlePasswordError(String username, LoginContext context) {
        int failCount = userService.incrementLoginFailCount(username);
        int maxFailCount = securityProperties.getMaxLoginFailCount();

        // 连续失败达到阈值记录为 ERROR 级别（可能是攻击行为）
        if (failCount >= CONTINUOUS_FAILURE_WARN_THRESHOLD) {
            log.error("LOGIN_FAILED username={} reason=PASSWORD_ERROR failCount={} ip={}", 
                    SensitiveUtils.maskUsername(username), failCount, SensitiveUtils.maskIp(context.getIp()));
        } else {
            log.warn("LOGIN_FAILED username={} reason=PASSWORD_ERROR failCount={} ip={}", 
                    SensitiveUtils.maskUsername(username), failCount, SensitiveUtils.maskIp(context.getIp()));
        }

        if (failCount >= maxFailCount) {
            userService.lockAccount(username, securityProperties.getLockMinutes());
            log.error("ACCOUNT_LOCKED username={} lockMinutes={} ip={}", 
                    SensitiveUtils.maskUsername(username), securityProperties.getLockMinutes(), 
                    SensitiveUtils.maskIp(context.getIp()));
            eventService.publishFailure(username, "账户已锁定", context);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED.getCode(),
                    "账户已锁定，请稍后再试")
                    .withContext("username", username)
                    .withContext("failCount", failCount)
                    .withContext("ip", context.getIp());
        }

        eventService.publishFailure(username, "密码错误", context);
        // 对外统一消息，不暴露剩余次数
        throw new BusinessException(ErrorCode.LOGIN_FAILED)
                .withContext("username", username)
                .withContext("failCount", failCount);
    }
}