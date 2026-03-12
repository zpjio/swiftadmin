package cn.swiftadmin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码请求 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class SmsSendDTO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    /** 使用场景：login|register|bind */
    @NotBlank(message = "使用场景不能为空")
    private String scene;

    /** 图形验证码 ID（触发图形验证码后必填） */
    private String captchaId;

    /** 图形验证码（触发图形验证码后必填） */
    private String captchaCode;

}
