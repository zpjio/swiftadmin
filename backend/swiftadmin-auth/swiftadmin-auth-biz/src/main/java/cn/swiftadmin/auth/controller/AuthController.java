package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.service.AuthService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.strategy.AuthStrategyFactory;
import cn.swiftadmin.common.core.annotation.RateLimit;
import cn.swiftadmin.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * <p>处理用户登录、登出、Token 刷新等认证相关操作</p>
 * <p>支持多种认证方式：账号密码、短信验证码、第三方社交登录</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "认证管理", description = "用户登录、登出、Token 刷新等认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthStrategyFactory strategyFactory;

    /**
     * 用户登录
     * <p>根据登录类型（账号密码/短信/社交）调用对应的认证策略</p>
     * <p>登录成功后返回访问 Token 和刷新 Token</p>
     *
     * @param dto 登录请求参数（包含登录类型、账号、密码等）
     * @return 登录结果（包含 Token 信息）
     */
    @Operation(summary = "用户登录", description = "根据登录类型（账号密码/短信/社交）调用对应的认证策略")
    @PostMapping("/login")
    @RateLimit(timeWindow = 60, maxRequests = 5, message = "登录请求过于频繁，请 60 秒后再试")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(strategyFactory.getStrategyByCode(dto.getType()).authenticate(dto));
    }

    /**
     * 用户登出
     * <p>将当前 Token 加入黑名单，使其失效</p>
     *
     * @return 操作结果
     */
    @Operation(summary = "用户登出", description = "将当前 Token 加入黑名单，使其失效")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 刷新 Token
     * <p>使用刷新 Token 获取新的访问 Token</p>
     * <p>刷新 Token 有效期为 7 天，访问 Token 有效期为 2 小时</p>
     *
     * @param refreshToken 刷新 Token（从请求头获取）
     * @return 新的 Token 信息
     */
    @Operation(summary = "刷新 Token", description = "使用刷新 Token 获取新的访问 Token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return R.ok(authService.refresh(refreshToken));
    }
}
