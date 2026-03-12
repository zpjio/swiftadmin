package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.vo.LoginTypeConfigVO;
import cn.swiftadmin.auth.api.vo.LoginConfigVO;
import cn.swiftadmin.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录配置控制器
 * <p>提供登录方式配置查询接口，支持前端动态展示登录选项</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Tag(name = "登录配置", description = "系统登录方式配置查询接口")
@RestController
@RequestMapping("/api/auth/login-config")
public class LoginConfigController {

    @Value("${login.password.enabled:true}")
    private Boolean passwordEnabled;

    @Value("${login.sms.enabled:false}")
    private Boolean smsEnabled;

    @Value("${login.wecom.enabled:false}")
    private Boolean wecomEnabled;

    @Value("${login.dingtalk.enabled:false}")
    private Boolean dingtalkEnabled;

    @Value("${login.feishu.enabled:false}")
    private Boolean feishuEnabled;

    /**
     * 获取系统支持的登录方式列表
     */
    @Operation(summary = "获取登录方式列表", description = "返回系统支持的所有登录方式配置")
    @GetMapping("/types")
    public R<List<LoginTypeConfigVO>> getLoginTypes() {
        List<LoginTypeConfigVO> loginTypes = new ArrayList<>();

        if (Boolean.TRUE.equals(passwordEnabled)) {
            loginTypes.add(LoginTypeConfigVO.builder()
                    .code("password")
                    .name("账号密码登录")
                    .enabled(true)
                    .build());
        }

        if (Boolean.TRUE.equals(smsEnabled)) {
            loginTypes.add(LoginTypeConfigVO.builder()
                    .code("sms")
                    .name("短信验证码登录")
                    .enabled(true)
                    .build());
        }

        if (Boolean.TRUE.equals(wecomEnabled)) {
            loginTypes.add(LoginTypeConfigVO.builder()
                    .code("wecom")
                    .name("企业微信登录")
                    .enabled(true)
                    .build());
        }

        if (Boolean.TRUE.equals(dingtalkEnabled)) {
            loginTypes.add(LoginTypeConfigVO.builder()
                    .code("dingtalk")
                    .name("钉钉登录")
                    .enabled(true)
                    .build());
        }

        if (Boolean.TRUE.equals(feishuEnabled)) {
            loginTypes.add(LoginTypeConfigVO.builder()
                    .code("feishu")
                    .name("飞书登录")
                    .enabled(true)
                    .build());
        }

        log.debug("返回登录方式配置，共 {} 种", loginTypes.size());
        return R.ok(loginTypes);
    }

    /**
     * 获取登录配置信息
     */
    @Operation(summary = "获取登录配置", description = "返回系统登录配置信息")
    @GetMapping("/config")
    public R<LoginConfigVO> getLoginConfig() {
        LoginConfigVO config = LoginConfigVO.builder()
                .captchaEnabled(false)
                .rememberMeEnabled(true)
                .forgetPasswordEnabled(true)
                .registerEnabled(false)
                .build();

        return R.ok(config);
    }
}