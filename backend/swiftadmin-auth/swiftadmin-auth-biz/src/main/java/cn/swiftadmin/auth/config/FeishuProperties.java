package cn.swiftadmin.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 飞书配置属性
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "social.feishu")
public class FeishuProperties {

    /** 应用 ID */
    private String appId;

    /** 应用 Secret */
    private String appSecret;

    /** OAuth2 回调地址 */
    private String redirectUri;

    /** 获取 Access Token 的 URL */
    private String accessTokenUrl = "https://open.feishu.cn/open-apis/authen/v1/access_token";

    /** 获取用户信息的 URL */
    private String userInfoUrl = "https://open.feishu.cn/open-apis/authen/v1/user_info";

}
