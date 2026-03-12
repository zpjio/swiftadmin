package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserDetailsService;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 手机验证码认证策略
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsAuthStrategy implements AuthStrategy {

    private final SmsService smsService;
    private final UserService userService;
    private final TokenManager tokenManager;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    @Override
    public LoginType getLoginType() {
        return LoginType.SMS;
    }

    @Override
    public LoginVO authenticate(LoginDTO dto) {
        String ip = ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);
        String phone = dto.getPhone();

        // 1. 验证验证码
        boolean verified = smsService.verifyCode(phone, dto.getCode(), "login");
        if (!verified) {
            log.warn("短信验证码验证失败：phone={}", phone);
            eventPublisher.publishEvent(LoginEvent.failure(phone, "验证码错误", ip, userAgent, "sms"));
            throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
        }

        // 2. 查询用户（不再自动注册，需要预先注册）
        SysUser user = userService.getByPhone(phone);
        if (user == null) {
            log.warn("手机号未注册，请先注册：{}", phone);
            eventPublisher.publishEvent(LoginEvent.failure(phone, "手机号未注册", ip, userAgent, "sms"));
            throw new BusinessException(ErrorCode.USER_NOT_REGISTERED);
        }

        // 3. 检查用户状态
        if (!user.getStatus().isEnabled()) {
            log.warn("用户已禁用：phone={}", phone);
            eventPublisher.publishEvent(LoginEvent.failure(user.getUsername(), "用户已禁用", ip, userAgent, "sms"));
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 4. 生成 Token
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

        log.info("手机号登录成功：phone={}", phone);
        eventPublisher.publishEvent(LoginEvent.success(user.getUsername(), ip, userAgent, "sms"));

        return loginVO;
    }

}
