package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.dto.UnbindVerifyDTO;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.api.vo.SocialBindVO;
import cn.swiftadmin.auth.handler.SocialBindHandler;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社交账号绑定控制器
 * <p>提供社交账号的绑定和解绑功能</p>
 * <p>支持绑定多个第三方账号，解绑时需要安全验证</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "社交账号绑定", description = "第三方社交账号绑定和解绑管理接口")
@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialBindController {

    private final SocialBindHandler socialBindHandler;
    private final UserService userService;
    private final SmsService smsService;

    /**
     * 绑定社交账号
     * <p>将第三方账号绑定到指定用户，需要临时 Token 验证</p>
     *
     * @param source 第三方平台标识
     * @param tempToken 临时 Token（从社交授权回调获取）
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Operation(summary = "绑定社交账号", description = "将第三方账号绑定到指定用户")
    @PostMapping("/bind/{source}")
    public R<Void> bindSocial(@PathVariable String source,
                              @RequestParam String tempToken,
                              @RequestParam Long userId) {
        socialBindHandler.bind(source, userId, tempToken);
        return R.ok();
    }

    /**
     * 解绑社交账号
     * <p>解绑第三方账号，需要密码或短信验证码验证</p>
     * <p>确保解绑操作是用户本人执行</p>
     *
     * @param source 第三方平台标识
     * @param verifyDTO 验证信息（密码或短信验证码）
     * @param userId 用户 ID
     * @return 操作结果
     * @throws BusinessException 当验证失败时抛出异常
     */
    @Operation(summary = "解绑社交账号", description = "解绑第三方账号，需要密码或短信验证码验证")
    @PostMapping("/unbind/{source}")
    public R<Void> unbindSocial(@PathVariable String source,
                                @Valid @RequestBody(required = false) UnbindVerifyDTO verifyDTO,
                                @RequestParam Long userId) {
        // 检查用户是否存在
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证解绑操作
        if (verifyDTO != null) {
            verifyUnbindRequest(user, verifyDTO);
        } else {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "解绑操作需要安全验证，请提供密码或验证码");
        }

        socialBindHandler.unbind(source, userId);
        return R.ok();
    }

    /**
     * 强制解绑社交账号
     * <p>无需验证直接解绑，仅限管理员使用</p>
     *
     * @param source 第三方平台标识
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Operation(summary = "强制解绑社交账号", description = "无需验证直接解绑，仅限管理员使用")
    @PostMapping("/unbind/{source}/force")
    public R<Void> unbindSocialForce(@PathVariable String source,
                                     @RequestParam Long userId) {
        socialBindHandler.unbind(source, userId);
        return R.ok();
    }

    /**
     * 获取用户的社交账号绑定列表
     * <p>返回用户已绑定的所有第三方账号信息</p>
     *
     * @param userId 用户 ID
     * @return 社交账号绑定列表
     */
    @Operation(summary = "获取社交账号绑定列表", description = "返回用户已绑定的所有第三方账号信息")
    @GetMapping("/binds")
    public R<List<SocialBindVO>> getSocialBinds(@RequestParam Long userId) {
        return R.ok(socialBindHandler.getBinds(userId));
    }

    /**
     * 验证解绑请求
     * <p>支持密码验证和短信验证码验证两种方式</p>
     *
     * @param user 用户信息
     * @param verifyDTO 验证信息
     * @throws BusinessException 验证失败时抛出异常
     */
    private void verifyUnbindRequest(SysUser user, UnbindVerifyDTO verifyDTO) {
        // 验证密码
        if ("password".equals(verifyDTO.getVerifyType()) && verifyDTO.getPassword() != null) {
            // 通过尝试修改密码来验证（不实际修改）
            try {
                userService.changePassword(user.getId(), verifyDTO.getPassword(), verifyDTO.getPassword());
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED);
            }
            return;
        }

        // 验证短信验证码
        if ("sms".equals(verifyDTO.getVerifyType()) && verifyDTO.getPhone() != null && verifyDTO.getSmsCode() != null) {
            if (!smsService.verifyCode(verifyDTO.getPhone(), verifyDTO.getSmsCode(), "unbind")) {
                throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
            }
            return;
        }

        throw new BusinessException(ErrorCode.PARAM_INVALID, "请提供密码或短信验证码进行验证");
    }
}
