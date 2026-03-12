package cn.swiftadmin.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信配置属性
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "social.wecom")
public class WeComProperties {

    /** 企业 ID */
    private String corpId;

    /** 应用 ID */
    private String agentId;

    /** 应用 Secret */
    private String secret;

    /** OAuth2 回调地址 */
    private String redirectUri;

    /** 获取 Access Token 的 URL */
    private String tokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";

    /** 获取用户信息的 URL */
    private String userInfoUrl = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo";

    /** 获取用户详情的 URL */
    private String userDetailUrl = "https://qyapi.weixin.qq.com/cgi-bin/user/get";

}
