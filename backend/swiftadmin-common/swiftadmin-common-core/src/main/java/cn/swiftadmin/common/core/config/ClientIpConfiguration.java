package cn.swiftadmin.common.core.config;

import cn.swiftadmin.common.core.util.ServletUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 客户端 IP 解析策略。
 * <p>默认只使用套接字对端地址。仅当显式打开
 * {@code swiftadmin.security.trust-forwarded-headers} 时才读取代理头，
 * 且取 {@code X-Forwarded-For} 最右侧（由最近一层代理追加）的地址。</p>
 */
@Configuration
public class ClientIpConfiguration {

    public ClientIpConfiguration(
            @Value("${swiftadmin.security.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        ServletUtils.setTrustForwardedHeaders(trustForwardedHeaders);
    }
}
