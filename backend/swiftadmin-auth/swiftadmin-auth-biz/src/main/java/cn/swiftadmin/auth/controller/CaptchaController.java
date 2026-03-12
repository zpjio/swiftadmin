package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.service.CaptchaService;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.api.vo.CaptchaVO;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.core.util.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码控制器
 * <p>提供图形验证码的生成和校验功能</p>
 * <p>用于防止暴力破解和机器注册</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "验证码管理", description = "图形验证码和短信验证码管理接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final SmsService smsService;

    /**
     * 获取图形验证码
     * <p>生成随机图形验证码，返回 Base64 编码的图片和验证码 ID</p>
     * <p>验证码有效期为 5 分钟</p>
     *
     * @return 验证码信息（包含验证码 ID 和 Base64 图片）
     */
    @Operation(summary = "获取图形验证码", description = "生成随机图形验证码，返回 Base64 编码的图片和验证码 ID")
    @GetMapping("/captcha")
    public R<CaptchaVO> getCaptcha() {
        return R.ok(captchaService.generate());
    }

    /**
     * 检查是否需要验证码
     * <p>根据 IP 限流策略判断当前 IP 是否需要验证码</p>
     * <p>连续失败 5 次后会强制要求验证码</p>
     *
     * @param request HTTP 请求（用于获取客户端 IP）
     * @return 是否需要验证码的标识
     */
    @Operation(summary = "检查是否需要验证码", description = "根据 IP 限流策略判断当前 IP 是否需要验证码")
    @GetMapping("/captcha/required")
    public R<CaptchaVO> checkCaptchaRequired(HttpServletRequest request) {
        return R.ok(CaptchaVO.builder()
                .required(smsService.isCaptchaRequired(ServletUtils.getClientIp(request)))
                .build());
    }
}