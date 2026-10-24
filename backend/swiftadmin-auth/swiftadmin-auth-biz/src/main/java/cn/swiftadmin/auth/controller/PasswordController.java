package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.context.LoginContext;
import cn.swiftadmin.auth.service.LoginSessionService;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.jwt.JwtTokenProvider;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 密码管理控制器
 * <p>提供密码修改、强制修改密码等功能</p>
 * <p>用于用户自主修改密码和临时 Token 强制修改密码场景</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "密码管理", description = "用户密码修改和管理接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final LoginSessionService loginSessionService;
    private final UserSessionService userSessionService;

    /**
     * 修改密码
     * <p>用户自主修改密码，需要验证旧密码</p>
     * <p>用户身份取自当前登录上下文，不接受前端传入的用户 ID</p>
     * <p>修改成功后该用户所有会话失效，需要重新登录</p>
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    @Operation(summary = "修改密码", description = "用户自主修改密码，需要验证旧密码")
    @PostMapping("/password/change")
    public R<Void> changePassword(@RequestParam String oldPassword,
                                   @RequestParam String newPassword) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        userService.changePassword(userId, oldPassword, newPassword);

        // 密码已变更，使该用户所有会话失效，强制重新登录
        List<String> sessionIds = userSessionService.getUserSessionIds(userId);
        userSessionService.removeSessions(sessionIds);

        return R.ok();
    }

    /**
     * 强制修改密码
     * <p>使用临时 Token 强制修改密码（用于管理员重置密码后首次登录）</p>
     * <p>临时 Token 只能使用一次，使用后会失效</p>
     * <p>修改成功后直接返回新的 Token，无需重新登录</p>
     *
     * @param tempToken 临时 Token（管理员重置密码后生成）
     * @param newPassword 新密码
     * @return 修改结果（包含新的 Token 信息）
     * @throws BusinessException 当 Token 无效或已被使用时抛出异常
     */
    @Operation(summary = "强制修改密码", description = "使用临时 Token 强制修改密码，用于管理员重置密码后首次登录")
    @PostMapping("/password/force-change")
    public R<LoginVO> forceChangePassword(@RequestParam String tempToken,
                                           @RequestParam String newPassword) {
        // 验证 Token 有效性
        if (!tokenProvider.validateToken(tempToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 验证是否为临时 Token
        if (!tokenProvider.isTempToken(tempToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 验证 Token 是否已被使用
        if (!tokenProvider.canUseTempToken(tempToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "临时 Token 已被使用，请重新获取");
        }

        // 获取用户信息
        String username = tokenProvider.getUsernameFromToken(tempToken);
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 强制修改密码
        userService.forceChangePassword(user.getId(), newPassword);
        // 将临时 Token 加入黑名单
        tokenProvider.addToBlacklist(tempToken);

        // 生成新的 Token 并建立会话（否则新 Token 无法通过会话校验）
        LoginContext context = LoginContext.ofPassword(
                ServletUtils.getClientIp(), ServletUtils.getUserAgent());
        return R.ok(loginSessionService.createSession(username, context));
    }
}
