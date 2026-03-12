package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.dto.OAuthStateDTO;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.api.vo.SocialUrlVO;
import cn.swiftadmin.auth.strategy.SocialAuthStrategy;
import cn.swiftadmin.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社交认证控制器
 * <p>提供第三方社交账号登录功能</p>
 * <p>支持企业微信、钉钉、飞书等第三方平台</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "社交认证", description = "第三方社交账号登录管理接口")
@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthStrategy socialAuthStrategy;

    /**
     * 获取社交登录授权 URL
     * <p>生成第三方登录的授权链接，用户点击后跳转到第三方平台授权</p>
     *
     * @param source 第三方平台标识（wecom/dingtalk/feishu）
     * @param sessionId 会话 ID（可选，用于绑定 state 参数）
     * @return 授权 URL 和 state 参数
     */
    @Operation(summary = "获取社交授权 URL", description = "生成第三方登录的授权链接")
    @GetMapping("/url/{source}")
    public R<SocialUrlVO> getSocialUrl(@PathVariable String source,
                                        @RequestParam(required = false) String sessionId) {
        SocialAuthStrategy.SocialUrlInfo urlInfo = socialAuthStrategy.getAuthUrl(source, sessionId);
        return R.ok(SocialUrlVO.builder()
                .url(urlInfo.url())
                .state(urlInfo.state())
                .build());
    }

    /**
     * 获取社交账号绑定 URL
     * <p>生成绑定第三方账号的授权链接，用于将第三方账号绑定到现有账号</p>
     *
     * @param source 第三方平台标识
     * @param userId 用户 ID
     * @return 绑定 URL 和 state 参数
     */
    @Operation(summary = "获取社交绑定 URL", description = "生成绑定第三方账号的授权链接")
    @GetMapping("/bind/url/{source}")
    public R<SocialUrlVO> getBindUrl(@PathVariable String source,
                                      @RequestParam Long userId) {
        SocialAuthStrategy.SocialUrlInfo urlInfo = socialAuthStrategy.getBindUrl(source, userId);
        return R.ok(SocialUrlVO.builder()
                .url(urlInfo.url())
                .state(urlInfo.state())
                .build());
    }

    /**
     * 处理社交登录回调
     * <p>第三方平台授权完成后回调此接口，验证 state 并登录</p>
     *
     * @param source 第三方平台标识
     * @param authCode 授权码
     * @param state 状态参数（用于防止 CSRF 攻击）
     * @return 登录结果（包含 Token 信息）
     * @throws SocialAuthException 当 state 验证失败时抛出异常
     */
    @Operation(summary = "社交登录回调", description = "处理第三方平台授权回调，验证 state 并登录")
    @PostMapping("/callback/{source}")
    public R<LoginVO> socialCallback(@PathVariable String source,
                                     @RequestParam String authCode,
                                     @RequestParam(required = false) String state) {
        // 验证 state 参数
        OAuthStateDTO stateDTO = socialAuthStrategy.validateState(state);

        // 验证 state 的 source 是否匹配
        if (!stateDTO.getSource().equalsIgnoreCase(source)) {
            throw new SocialAuthException(SocialAuthError.STATE_SOURCE_MISMATCH);
        }

        // 构建登录参数
        LoginDTO dto = new LoginDTO();
        dto.setType(source.toLowerCase());
        dto.setAuthCode(authCode);
        dto.setSource(source.toLowerCase());
        dto.setState(state);

        return R.ok(socialAuthStrategy.authenticateWithState(dto, stateDTO));
    }
}
