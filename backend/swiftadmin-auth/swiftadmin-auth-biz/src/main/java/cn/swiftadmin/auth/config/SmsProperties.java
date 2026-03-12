package cn.swiftadmin.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信服务配置
 * <p>支持阿里云和腾讯云短信服务</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /** 短信服务提供商：aliyun|tencent */
    private String provider = "aliyun";

    /** 阿里云配置 */
    private Aliyun aliyun = new Aliyun();

    /** 腾讯云配置 */
    private Tencent tencent = new Tencent();

    /**
     * 阿里云短信配置
     */
    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String signName = "SwiftAdmin";
        private String templateCode = "SMS_123456789";
    }

    /**
     * 腾讯云短信配置
     */
    @Data
    public static class Tencent {
        private String secretId;
        private String secretKey;
        private String appId;
        private String signName = "SwiftAdmin";
        private String templateId = "123456";
    }

}
