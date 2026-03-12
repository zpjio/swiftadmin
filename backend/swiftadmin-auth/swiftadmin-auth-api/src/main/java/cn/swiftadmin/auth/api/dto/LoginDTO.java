package cn.swiftadmin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 统一登录请求 DTO
 * <p>支持多种登录方式：账号密码、手机验证码、企业微信、钉钉、飞书</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
public class LoginDTO {

    /** 登录方式：password|sms|wecom|dingtalk|feishu */
    @NotBlank(message = "登录方式不能为空")
    private String type;

    /** 用户名（账号密码登录） */
    private String username;

    /** 密码（账号密码登录） */
    private String password;

    /** 手机号（短信登录/验证码发送） */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    /** 验证码（短信登录） */
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误")
    private String code;

    /** 图形验证码 ID（账号密码登录时需要） */
    private String captchaId;

    /** 图形验证码（账号密码登录时需要） */
    private String captchaCode;

    /** 第三方授权码（OAuth2 回调） */
    private String authCode;

    /** 第三方来源（用于区分不同平台） */
    private String source;

    /** 状态码（OAuth2 回调校验） */
    private String state;

    /** 客户端 IP 地址（用于限流和风控） */
    private String ip;

    /** 客户端 User-Agent（用于安全验证） */
    private String userAgent;

}
