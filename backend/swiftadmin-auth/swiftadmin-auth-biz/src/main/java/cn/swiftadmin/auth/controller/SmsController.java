package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.dto.SmsSendDTO;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.common.core.annotation.RateLimit;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.core.util.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码控制器
 * <p>提供短信验证码的发送功能</p>
 * <p>支持多种业务场景：登录、注册、找回密码、绑定手机等</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "短信验证码", description = "短信验证码发送管理接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * 发送短信验证码
     * <p>根据业务场景发送短信验证码</p>
     * <p>限流策略：同一 IP 60 秒内只能发送 1 次</p>
     * <p>验证码有效期为 5 分钟</p>
     *
     * @param dto 短信发送请求（包含手机号、业务场景、图形验证码等）
     * @param request HTTP 请求（用于获取客户端 IP 进行限流）
     * @return 发送结果
     */
    @Operation(summary = "发送短信验证码", description = "根据业务场景发送短信验证码，限流：同一 IP 60 秒内只能发送 1 次")
    @PostMapping("/sms/send")
    @RateLimit(timeWindow = 60, maxRequests = 1, message = "发送过于频繁，请 60 秒后再试")
    public R<Void> sendSms(@Valid @RequestBody SmsSendDTO dto, HttpServletRequest request) {
        smsService.sendCode(dto.getPhone(), dto.getScene(), ServletUtils.getClientIp(request),
                dto.getCaptchaId(), dto.getCaptchaCode());
        return R.ok();
    }
}
