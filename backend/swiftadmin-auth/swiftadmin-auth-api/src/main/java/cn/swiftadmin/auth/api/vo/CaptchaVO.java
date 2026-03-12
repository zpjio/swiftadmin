package cn.swiftadmin.auth.api.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 图形验证码响应 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
public class CaptchaVO {

    private String captchaId;
    private String image;
    private Boolean required;

}