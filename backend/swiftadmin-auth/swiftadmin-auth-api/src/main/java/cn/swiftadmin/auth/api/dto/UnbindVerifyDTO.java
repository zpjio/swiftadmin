package cn.swiftadmin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解绑验证请求 DTO
 * <p>解绑社交账号前需要进行安全验证（密码或短信验证码）</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class UnbindVerifyDTO {

    /** 验证类型：password|sms */
    @NotBlank(message = "验证类型不能为空")
    private String verifyType;

    /** 密码（verifyType=password 时必填） */
    private String password;

    /** 短信验证码（verifyType=sms 时必填） */
    private String smsCode;

    /** 手机号（verifyType=sms 时必填） */
    private String phone;

}