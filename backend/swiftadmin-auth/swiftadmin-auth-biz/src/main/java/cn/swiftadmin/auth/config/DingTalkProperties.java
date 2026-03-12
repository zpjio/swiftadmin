package cn.swiftadmin.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 钉钉配置属性
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "social.dingtalk")
public class DingTalkProperties {

    /** 应用 Key */
    private String appKey;

    /** 应用 Secret */
    private String appSecret;

    /** OAuth2 回调地址 */
    private String redirectUri;

    /** 获取用户信息的 URL */
    private String userInfoUrl = "https://oapi.dingtalk.com/sns/getuserinfo_bycode";

    /** 获取 Access Token 的 URL */
    private String tokenUrl = "https://oapi.dingtalk.com/sns/gettoken";

}
