package cn.swiftadmin.auth.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录配置 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginConfigVO {

    /** 是否启用图形验证码 */
    private Boolean captchaEnabled;

    /** 是否启用记住我 */
    private Boolean rememberMeEnabled;

    /** 是否启用忘记密码 */
    private Boolean forgetPasswordEnabled;

    /** 是否启用注册 */
    private Boolean registerEnabled;
}