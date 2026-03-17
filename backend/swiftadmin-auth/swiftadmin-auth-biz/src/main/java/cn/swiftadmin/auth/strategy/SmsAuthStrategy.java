package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.enums.LoginType;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.service.LoginEventService;
import cn.swiftadmin.auth.service.LoginSessionService;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 手机验证码认证策略
 * <p>重构版：依赖注入从 7 个减少到 6 个</p>
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
    private final LoginSessionService sessionService;
    private final LoginEventService eventService;
    private final HttpServletRequest request;

    @Override
    public LoginType getLoginType() {
        return LoginType.SMS;
    }

    @Override
    public LoginVO authenticate(LoginDTO dto) {
        String ip = ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);
        String phone = dto.getPhone();

        LoginContext context = LoginContext.ofSms(ip, userAgent);

        // 1. 验证短信验证码
        boolean verified = smsService.verifyCode(phone, dto.getCode(), "login");
        if (!verified) {
            log.warn("SMS_VERIFY_FAILED phone={}", phone);
            eventService.publishFailure(phone, "验证码错误", context);
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }

        // 2. 查询用户
        SysUser user = userService.getByPhone(phone);
        if (user == null) {
            log.warn("USER_NOT_REGISTERED phone={}", phone);
            eventService.publishFailure(phone, "手机号未注册", context);
            throw new BusinessException(ErrorCode.USER_NOT_REGISTERED);
        }

        context.withUsername(user.getUsername());

        // 3. 检查用户状态
        if (!user.getStatus().isEnabled()) {
            log.warn("USER_DISABLED phone={}", phone);
            eventService.publishFailure(user.getUsername(), "用户已禁用", context);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. 创建登录会话
        LoginVO loginVO = sessionService.createSession(user.getUsername(), context);

        log.info("SMS_LOGIN_SUCCESS phone={}", phone);
        eventService.publishSuccess(context);

        return loginVO;
    }
}